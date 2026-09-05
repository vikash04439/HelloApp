<#
.SYNOPSIS
    Tears down all AWS resources created for HelloApp (to stop any charges).

.DESCRIPTION
    Terminates the EC2 instance, deletes the security group and key pair,
    and removes the local state/key files.
#>

[CmdletBinding()]
param(
    [switch]$KeepKeyPair
)

$ErrorActionPreference = "Continue"

function Invoke-Aws {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$CliArgs)
    $out = & aws @CliArgs 2>&1
    $script:AwsExit = $LASTEXITCODE
    return ($out | Out-String).Trim()
}

$here      = Split-Path -Parent $MyInvocation.MyCommand.Path
$stateFile = Join-Path $here "instance-state.json"

if (-not (Test-Path $stateFile)) { throw "No instance-state.json found; nothing to tear down." }
$state  = Get-Content $stateFile -Raw | ConvertFrom-Json
$region = $state.region

Write-Host "== Terminating instance $($state.instanceId) ==" -ForegroundColor Cyan
$null = Invoke-Aws ec2 terminate-instances --instance-ids $state.instanceId --region $region
$null = Invoke-Aws ec2 wait instance-terminated --instance-ids $state.instanceId --region $region
Write-Host "Instance terminated."

Write-Host "== Deleting security group $($state.sgId) ==" -ForegroundColor Cyan
# Retry: the ENI can take a moment to detach after termination.
for ($i = 1; $i -le 6; $i++) {
    $null = Invoke-Aws ec2 delete-security-group --group-id $state.sgId --region $region
    if ($AwsExit -eq 0) { Write-Host "Security group deleted."; break }
    Start-Sleep -Seconds 10
}

if (-not $KeepKeyPair) {
    Write-Host "== Deleting key pair $($state.keyName) ==" -ForegroundColor Cyan
    $null = Invoke-Aws ec2 delete-key-pair --key-name $state.keyName --region $region
    if (Test-Path $state.pemFile) { Remove-Item $state.pemFile -Force }
    Write-Host "Key pair deleted."
}

Remove-Item $stateFile -Force
Write-Host "Teardown complete." -ForegroundColor Green
