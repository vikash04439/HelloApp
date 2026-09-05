<#
.SYNOPSIS
    Builds the HelloApp Docker image locally and deploys it to the provisioned EC2 instance.

.DESCRIPTION
    Steps:
      1. Reads deploy\aws\instance-state.json (created by provision-ec2.ps1).
      2. Builds the Docker image locally (multi-stage Dockerfile).
      3. Exports the image to a tar and copies it to the instance via scp.
      4. Loads the image on the instance and (re)starts the container:
           - host port 80  -> container port 8080
           - SPRING_PROFILES_ACTIVE=prod
           - persistent volumes for /app/data (H2 DB) and /app/logs
      5. Curls the public endpoint to verify.

.NOTES
    Requires: Docker Desktop running locally, and OpenSSH (ssh/scp) on PATH.
#>

[CmdletBinding()]
param(
    [string]$ImageTag = "helloapp:latest"
)

# Native tools (docker, ssh, scp) write progress to stderr; handle failures via exit codes.
$ErrorActionPreference = "Continue"

$here      = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot  = (Resolve-Path (Join-Path $here "..\..")).Path
$stateFile = Join-Path $here "instance-state.json"

if (-not (Test-Path $stateFile)) { throw "State file not found. Run provision-ec2.ps1 first." }
$state = Get-Content $stateFile -Raw | ConvertFrom-Json
$ip  = $state.publicIp
$pem = $state.pemFile
Write-Host "Target instance $($state.instanceId) at $ip" -ForegroundColor Cyan

if (-not (Test-Path $pem)) { throw "Private key '$pem' not found." }

# Lock down the .pem so Windows OpenSSH accepts it
icacls $pem /inheritance:r | Out-Null
icacls $pem /grant:r "$($env:USERNAME):(R)" | Out-Null

$knownHosts = Join-Path $env:TEMP "helloapp_known_hosts"
$sshOpts = @("-i", $pem, "-o", "StrictHostKeyChecking=accept-new", "-o", "UserKnownHostsFile=$knownHosts")
$remote  = "ec2-user@$ip"

# --- 1. Build image locally ---
Write-Host "== Building Docker image '$ImageTag' ==" -ForegroundColor Cyan
Push-Location $repoRoot
try {
    docker build -t $ImageTag .
    if ($LASTEXITCODE -ne 0) { throw "docker build failed." }
} finally {
    Pop-Location
}

# --- 2. Export image ---
$tar = Join-Path $env:TEMP "helloapp-image.tar"
Write-Host "== Exporting image to $tar ==" -ForegroundColor Cyan
docker save $ImageTag -o $tar
if ($LASTEXITCODE -ne 0) { throw "docker save failed." }
$sizeMb = [math]::Round((Get-Item $tar).Length / 1MB, 1)
Write-Host "Image tar size: $sizeMb MB"

# --- 3. Wait for SSH + Docker readiness ---
Write-Host "== Waiting for SSH and Docker on the instance ==" -ForegroundColor Cyan
$ready = $false
for ($i = 1; $i -le 30; $i++) {
    $probe = ssh @sshOpts -o ConnectTimeout=5 $remote "test -f /home/ec2-user/USER_DATA_DONE && docker info >/dev/null 2>&1 && echo READY" 2>&1
    if ($probe -match "READY") { $ready = $true; break }
    Write-Host "  attempt $i/30 - not ready yet, retrying in 10s..."
    Start-Sleep -Seconds 10
}
if (-not $ready) { throw "Instance did not become ready (SSH/Docker). Check the EC2 console." }
Write-Host "Instance is ready."

# --- 4. Copy image and load ---
Write-Host "== Copying image to instance (this may take a minute) ==" -ForegroundColor Cyan
scp @sshOpts $tar "${remote}:/home/ec2-user/helloapp-image.tar"
if ($LASTEXITCODE -ne 0) { throw "scp failed." }

$remoteCmd = @'
set -e
mkdir -p /home/ec2-user/helloapp-data /home/ec2-user/helloapp-logs
docker load -i /home/ec2-user/helloapp-image.tar
docker rm -f helloapp 2>/dev/null || true
docker run -d --name helloapp --restart unless-stopped -p 80:8080 -e SPRING_PROFILES_ACTIVE=prod -e JAVA_TOOL_OPTIONS=-Xmx512m -v /home/ec2-user/helloapp-data:/app/data -v /home/ec2-user/helloapp-logs:/app/logs helloapp:latest
rm -f /home/ec2-user/helloapp-image.tar
docker ps --filter name=helloapp
'@
# Bash requires LF line endings; strip CRs introduced by the Windows here-string.
$remoteCmd = $remoteCmd -replace "`r", ""

Write-Host "== Loading image and starting container ==" -ForegroundColor Cyan
ssh @sshOpts $remote $remoteCmd
if ($LASTEXITCODE -ne 0) { throw "Remote docker run failed." }

# --- 5. Verify ---
Write-Host "== Verifying application (allowing ~30s for startup) ==" -ForegroundColor Cyan
$ok = $false
for ($i = 1; $i -le 12; $i++) {
    Start-Sleep -Seconds 10
    try {
        $resp = Invoke-WebRequest -Uri "http://$ip/" -TimeoutSec 8 -UseBasicParsing
        if ($resp.StatusCode -eq 200) { $ok = $true; break }
    } catch {
        Write-Host "  waiting for app... ($i/12)"
    }
}

Remove-Item $tar -ErrorAction SilentlyContinue

Write-Host ""
if ($ok) {
    Write-Host "==================== DEPLOYMENT SUCCESSFUL ====================" -ForegroundColor Green
    Write-Host "App URL      : http://$ip/"
    Write-Host "Employees UI : http://$ip/employees  (login: partner / partner123)"
} else {
    Write-Host "Container started but HTTP check did not return 200 yet." -ForegroundColor Yellow
    Write-Host "Check logs:  ssh -i `"$pem`" $remote 'docker logs helloapp'"
}
Write-Host "SSH access   : ssh -i `"$pem`" $remote"
