param(
    [switch]$Execute,
    [switch]$Rollback
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$dataName = 'User Data'
$targetRoot = 'E:\Develop\WeChatDevTools'
$target = Join-Path $targetRoot $dataName

# Find %LOCALAPPDATA%\<WeChat DevTools>\User Data without hard-coding the Chinese folder name.
$root = Get-ChildItem $env:LOCALAPPDATA -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match '\u5fae\u4fe1' -and (Test-Path (Join-Path $_.FullName $dataName)) } |
    Select-Object -First 1

if (-not $root) { throw "WeChat DevTools 'User Data' not found under $env:LOCALAPPDATA" }

$source = Join-Path $root.FullName $dataName
$sourceItem = Get-Item $source -Force

Write-Host "source = $source"
Write-Host "target = $target"
Write-Host ("source type = " + $(if ($sourceItem.LinkType) { "$($sourceItem.LinkType) -> $($sourceItem.Target)" } else { 'directory' }))

if ($sourceItem.LinkType) {
    Write-Host 'Already a junction; nothing to do.'
    exit 0
}

$running = Get-Process -ErrorAction SilentlyContinue | Where-Object {
    $_.Path -and $_.Path.StartsWith($root.FullName, [System.StringComparison]::OrdinalIgnoreCase)
}
if ($running) {
    throw "Close WeChat DevTools before running this script. Running: $($running.ProcessName -join ', ')"
}

if (-not $Execute -and -not $Rollback) {
    Write-Host ''
    Write-Host 'DRY RUN. This would:'
    Write-Host "  1. move '$source' to '$target'"
    Write-Host "  2. create a junction at '$source' pointing to '$target'"
    Write-Host ''
    Write-Host 'Run with -Execute to apply, or -Rollback to revert.'
    exit 0
}

if ($Rollback) {
    if (-not $sourceItem.LinkType) { throw "Not a junction, nothing to roll back: $source" }
    Write-Host 'Removing junction...'
    Remove-Item $source -Force
    if (-not (Test-Path $target)) { throw "Target missing, cannot restore: $target" }
    Write-Host 'Moving data back...'
    Move-Item $target $source
    Write-Host 'Rollback complete.'
    exit 0
}

if (Test-Path $target) { throw "Target already exists, refusing to overwrite: $target" }
New-Item -ItemType Directory -Force -Path $targetRoot | Out-Null

Write-Host 'Moving User Data to E: ...'
Move-Item $source $target
if (-not (Test-Path $target)) { throw "Move failed: $target" }

Write-Host 'Creating junction...'
New-Item -ItemType Junction -Path $source -Target $target | Out-Null

$check = Get-Item $source -Force
Write-Host ("junction = " + $check.LinkType + " -> " + $check.Target)
Write-Host 'Done. Start WeChat DevTools and confirm it opens normally.'
Write-Host 'To revert: run this script with -Rollback (close DevTools first).'
