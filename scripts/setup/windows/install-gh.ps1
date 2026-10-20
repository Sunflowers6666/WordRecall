param(
    [string]$Version = '2.102.0',
    [string]$InstallDir = 'E:\Develop\gh',
    [string]$DownloadDir = 'E:\Develop\_downloads'
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$zipName = "gh_${Version}_windows_amd64.zip"
$zipPath = Join-Path $DownloadDir $zipName
$url = "https://github.com/cli/cli/releases/download/v$Version/$zipName"
$expectedSha256 = 'ae64e556ecc240b200f7eba60d550e4bb60d78e860e69dd88c449405b86067f4'
$expectedSize = 15512013
$log = Join-Path $DownloadDir 'install-gh.log'

function Log([string]$Message) {
    $line = '{0} {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Message
    Write-Host $line
    Add-Content -Path $log -Value $line -Encoding UTF8
}

Log "start gh $Version"
New-Item -ItemType Directory -Force -Path $DownloadDir, $InstallDir | Out-Null

$ghExe = Join-Path $InstallDir 'bin\gh.exe'
if (Test-Path $ghExe) {
    Log "already installed: $ghExe"
    & $ghExe --version
    exit 0
}

if (-not (Test-Path $zipPath) -or (Get-Item $zipPath).Length -ne $expectedSize) {
    Log "downloading $url"
    & curl.exe --fail --location --http1.1 --retry 10 --retry-delay 5 --retry-all-errors --continue-at - --output $zipPath $url
    if ($LASTEXITCODE -ne 0) { throw "download failed with exit code $LASTEXITCODE" }
}

$size = (Get-Item $zipPath).Length
$hash = (Get-FileHash $zipPath -Algorithm SHA256).Hash.ToLower()
Log "zip size=$size sha256=$hash"
if ($size -ne $expectedSize) { throw "size mismatch: $size" }
if ($hash -ne $expectedSha256) { throw "sha256 mismatch: $hash" }

Log "extracting to $InstallDir"
Expand-Archive -Path $zipPath -DestinationPath $InstallDir -Force
if (-not (Test-Path $ghExe)) { throw "gh.exe not found after extraction: $ghExe" }

$binDir = Join-Path $InstallDir 'bin'
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$parts = @()
if ($userPath) { $parts = $userPath -split ';' | Where-Object { $_ } }
if ($parts -notcontains $binDir) {
    $newPath = (($parts + $binDir) -join ';')
    [Environment]::SetEnvironmentVariable('Path', $newPath, 'User')
    Log "added to user PATH: $binDir"
} else {
    Log "user PATH already contains: $binDir"
}

Log "verify"
& $ghExe --version
Log 'done'
