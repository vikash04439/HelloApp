<#
.SYNOPSIS
    Provisions AWS Free-Tier infrastructure for HelloApp on an EC2 t2.micro instance.

.DESCRIPTION
    Uses the AWS CLI to create:
      * an EC2 key pair (saved locally as a .pem file)
      * a security group (SSH from your IP, HTTP 80 from anywhere)
      * a t2.micro Amazon Linux 2023 instance with Docker pre-installed (via user-data)

    All created resource identifiers are written to deploy\aws\instance-state.json,
    which deploy-app.ps1 then consumes.

.NOTES
    Requires: AWS CLI v2 configured with credentials (run `aws configure` first).
    Free Tier: t2.micro is free for 750 hrs/month for the first 12 months.
#>

[CmdletBinding()]
param(
    [string]$Region       = "us-east-1",
    [string]$InstanceType = "t3.micro",
    [string]$KeyName      = "helloapp-key",
    [string]$SgName       = "helloapp-sg",
    [string]$InstanceName = "HelloApp-EC2"
)

# Native (aws.exe) commands may write to stderr on expected "not found" probes; we
# handle failures explicitly via $LASTEXITCODE instead of letting them terminate.
$ErrorActionPreference = "Continue"

function Invoke-Aws {
    <# Runs the AWS CLI, merging stderr into the returned text. Sets $script:AwsExit. #>
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$CliArgs)
    $out = & aws @CliArgs 2>&1
    $script:AwsExit = $LASTEXITCODE
    return ($out | Out-String).Trim()
}

function Get-MyPublicIp {
    <# Auto-detects the caller's current public IPv4 via well-known echo endpoints. #>
    foreach ($url in @("https://checkip.amazonaws.com", "https://api.ipify.org", "https://ifconfig.me/ip")) {
        try {
            $ip = (Invoke-RestMethod -Uri $url -TimeoutSec 10 -ErrorAction Stop | Out-String).Trim()
            if ($ip -match '^\d{1,3}(\.\d{1,3}){3}$') { return $ip }
        } catch { }
    }
    throw "Could not auto-detect your public IP from any known endpoint."
}

function Set-SshIngress {
    <# Ensures the security group allows SSH (22) from $Ip/32; adds it if missing.
       Duplicate rules are treated as success. #>
    param([string]$SgId, [string]$Ip, [string]$Region)
    $res = Invoke-Aws ec2 authorize-security-group-ingress --group-id $SgId --protocol tcp --port 22 --cidr "$Ip/32" --region $Region
    if ($script:AwsExit -eq 0) {
        Write-Host "Authorized SSH (22) from $Ip/32."
    } elseif ($res -match "InvalidPermission.Duplicate") {
        Write-Host "SSH (22) from $Ip/32 already authorized."
    } else {
        throw "Failed to authorize SSH ingress for $Ip/32.`n$res"
    }
}

$here      = Split-Path -Parent $MyInvocation.MyCommand.Path
$stateFile = Join-Path $here "instance-state.json"
$pemFile   = Join-Path $here "$KeyName.pem"

Write-Host "== Verifying AWS credentials ==" -ForegroundColor Cyan
$who = Invoke-Aws sts get-caller-identity --region $Region --output json
if ($AwsExit -ne 0) { throw "AWS credentials not configured/invalid. Run 'aws configure' first.`n$who" }
Write-Host $who

# --- Key pair ---
Write-Host "== Ensuring key pair '$KeyName' ==" -ForegroundColor Cyan
$null = Invoke-Aws ec2 describe-key-pairs --key-names $KeyName --region $Region --output json
if ($AwsExit -eq 0) {
    Write-Host "Key pair already exists in AWS."
    if (-not (Test-Path $pemFile)) {
        Write-Warning "Local .pem '$pemFile' is missing; SSH will fail. Delete the key pair in AWS and re-run to regenerate."
    }
} else {
    $keyMaterial = Invoke-Aws ec2 create-key-pair --key-name $KeyName --region $Region --query "KeyMaterial" --output text
    if ($AwsExit -ne 0) { throw "Failed to create key pair.`n$keyMaterial" }
    $keyMaterial | Out-File -Encoding ascii $pemFile
    Write-Host "Created key pair and saved private key to $pemFile"
}

# --- Default VPC ---
$vpcId = Invoke-Aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --region $Region --query "Vpcs[0].VpcId" --output text
if ($AwsExit -ne 0 -or -not $vpcId -or $vpcId -eq "None") { throw "No default VPC found in $Region.`n$vpcId" }
Write-Host "Using default VPC: $vpcId"

# --- Security group ---
Write-Host "== Ensuring security group '$SgName' ==" -ForegroundColor Cyan
$myIp = Get-MyPublicIp
Write-Host "Your public IP detected as $myIp (SSH will be restricted to it)."

$sgId = Invoke-Aws ec2 describe-security-groups --filters "Name=group-name,Values=$SgName" "Name=vpc-id,Values=$vpcId" --region $Region --query "SecurityGroups[0].GroupId" --output text
if ($AwsExit -ne 0 -or -not $sgId -or $sgId -eq "None") {
    $sgId = Invoke-Aws ec2 create-security-group --group-name $SgName --description "HelloApp security group" --vpc-id $vpcId --region $Region --query "GroupId" --output text
    if ($AwsExit -ne 0) { throw "Failed to create security group.`n$sgId" }
    Write-Host "Created security group: $sgId"

    Set-SshIngress -SgId $sgId -Ip $myIp -Region $Region
    $null = Invoke-Aws ec2 authorize-security-group-ingress --group-id $sgId --protocol tcp --port 80 --cidr "0.0.0.0/0" --region $Region
    Write-Host "Ingress rules added: 22 (your IP), 80 (public)."
} else {
    Write-Host "Security group already exists: $sgId"
    # Reconcile SSH access in case your public IP changed since last run.
    Set-SshIngress -SgId $sgId -Ip $myIp -Region $Region
}

# --- Latest Amazon Linux 2023 AMI (via DescribeImages so only EC2 perms are needed) ---
$amiId = Invoke-Aws ec2 describe-images --owners amazon `
    --filters "Name=name,Values=al2023-ami-2023.*-x86_64" "Name=state,Values=available" "Name=architecture,Values=x86_64" `
    --query "reverse(sort_by(Images, &CreationDate))[0].ImageId" --output text --region $Region
if ($AwsExit -ne 0 -or -not $amiId -or $amiId -eq "None") { throw "Could not resolve Amazon Linux 2023 AMI.`n$amiId" }
Write-Host "Using Amazon Linux 2023 AMI: $amiId"

# --- Launch instance ---
Write-Host "== Launching $InstanceType instance ==" -ForegroundColor Cyan
$userDataPath = Join-Path $here "user-data.sh"
$userDataB64  = [Convert]::ToBase64String([IO.File]::ReadAllBytes($userDataPath))

$instanceId = Invoke-Aws ec2 run-instances `
    --image-id $amiId `
    --instance-type $InstanceType `
    --key-name $KeyName `
    --security-group-ids $sgId `
    --user-data $userDataB64 `
    --region $Region `
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$InstanceName}]" `
    --query "Instances[0].InstanceId" --output text
if ($AwsExit -ne 0 -or -not $instanceId -or $instanceId -eq "None") { throw "Failed to launch instance.`n$instanceId" }
Write-Host "Launched instance: $instanceId"

Write-Host "Waiting for instance to enter 'running' state..." -ForegroundColor Cyan
$null = Invoke-Aws ec2 wait instance-running --instance-ids $instanceId --region $Region

$publicIp = Invoke-Aws ec2 describe-instances --instance-ids $instanceId --region $Region --query "Reservations[0].Instances[0].PublicIpAddress" --output text

$state = [ordered]@{
    region     = $Region
    instanceId = $instanceId
    publicIp   = $publicIp
    keyName    = $KeyName
    pemFile    = $pemFile
    sgId       = $sgId
    createdAt  = (Get-Date).ToString("o")
}
$state | ConvertTo-Json | Out-File -Encoding utf8 $stateFile

Write-Host ""
Write-Host "==================== PROVISIONING COMPLETE ====================" -ForegroundColor Green
Write-Host "Instance ID : $instanceId"
Write-Host "Public IP   : $publicIp"
Write-Host "SSH key     : $pemFile"
Write-Host "State saved : $stateFile"
Write-Host ""
Write-Host "Docker is installing via user-data (takes ~1-2 min after boot)."
Write-Host "Next: run  .\deploy-app.ps1" -ForegroundColor Yellow
