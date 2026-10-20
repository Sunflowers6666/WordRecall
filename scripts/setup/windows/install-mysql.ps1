param(
    [string]$Version = '8.0.46',
    [string]$InstallRoot = 'E:\Develop\MySQL',
    [string]$DownloadDir = 'E:\Develop\_downloads',
    [switch]$VerifyOnly
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$mysqlHome  = Join-Path $InstallRoot "mysql-$Version-winx64"
$dataDir    = Join-Path $InstallRoot 'data'
$tmpDir     = Join-Path $InstallRoot 'tmp'
$logDir     = Join-Path $InstallRoot 'logs'
$secretsDir = Join-Path $InstallRoot '.secrets'
$myIni      = Join-Path $InstallRoot 'my.ini'
$mysqld     = Join-Path $mysqlHome 'bin\mysqld.exe'
$mysql      = Join-Path $mysqlHome 'bin\mysql.exe'
$mysqladmin = Join-Path $mysqlHome 'bin\mysqladmin.exe'
$zipName    = "mysql-$Version-winx64.zip"
$zipPath    = Join-Path $DownloadDir $zipName
$url        = "https://cdn.mysql.com/Downloads/MySQL-8.0/$zipName"
$expectedMd5 = '003f527d5df61b663ff191038cd676bd'
$expectedSize = 248009349
$log = Join-Path $logDir 'install-mysql.log'

function Log([string]$Message) {
    $line = '{0} {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Message
    Write-Host $line
    try { Add-Content -Path $log -Value $line -Encoding UTF8 -ErrorAction SilentlyContinue } catch { }
}

function New-Secret([int]$Length = 32) {
    $chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
    $bytes = New-Object byte[] $Length
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $rng.Dispose()
    $sb = New-Object System.Text.StringBuilder
    foreach ($b in $bytes) { [void]$sb.Append($chars[$b % $chars.Length]) }
    return $sb.ToString()
}

function Test-Port3306 {
    return [bool](Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue)
}

New-Item -ItemType Directory -Force -Path $InstallRoot, $DownloadDir, $tmpDir, $logDir, $secretsDir | Out-Null

if ($VerifyOnly) {
    & (Join-Path $PSScriptRoot 'verify-mysql.ps1')
    exit $LASTEXITCODE
}

Log "start mysql $Version"

# 1. Download and extract if missing
if (-not (Test-Path $mysqld)) {
    if (-not (Test-Path $zipPath) -or (Get-Item $zipPath).Length -ne $expectedSize) {
        Log "downloading $url"
        & curl.exe --fail --location --retry 5 --retry-delay 3 --output $zipPath $url
        if ($LASTEXITCODE -ne 0) { throw "download failed with exit code $LASTEXITCODE" }
    }
    $size = (Get-Item $zipPath).Length
    $md5 = (Get-FileHash $zipPath -Algorithm MD5).Hash.ToLower()
    Log "zip size=$size md5=$md5"
    if ($size -ne $expectedSize) { throw "size mismatch: $size" }
    if ($md5 -ne $expectedMd5) { throw "md5 mismatch: $md5" }
    Log "extracting to $InstallRoot"
    Expand-Archive -Path $zipPath -DestinationPath $InstallRoot -Force
    if (-not (Test-Path $mysqld)) { throw "mysqld.exe not found after extraction: $mysqld" }
} else {
    Log "already extracted: $mysqlHome"
}

# 2. my.ini (never overwrite an existing one)
if (-not (Test-Path $myIni)) {
    $ini = @"
[mysqld]
basedir=$($mysqlHome -replace '\\','/')
datadir=$($dataDir -replace '\\','/')
tmpdir=$($tmpDir -replace '\\','/')
log-error=$((Join-Path $logDir 'mysql-error.log') -replace '\\','/')
port=3306
bind-address=127.0.0.1
character-set-server=utf8mb4
collation-server=utf8mb4_0900_ai_ci
default-time-zone='+08:00'
mysqlx=0

[client]
port=3306
default-character-set=utf8mb4
"@
    [System.IO.File]::WriteAllText($myIni, $ini, [System.Text.UTF8Encoding]::new($false))
    Log "wrote $myIni"
} else {
    Log "my.ini already exists: $myIni"
}

# 3. Initialize if the data directory is empty
$dataEntries = @()
if (Test-Path $dataDir) { $dataEntries = @(Get-ChildItem $dataDir -Force -ErrorAction SilentlyContinue) }
if ($dataEntries.Count -eq 0) {
    Log "initializing data directory"
    & $mysqld --defaults-file=$myIni --initialize-insecure --console
    if ($LASTEXITCODE -ne 0) { throw "mysqld --initialize-insecure failed: $LASTEXITCODE" }
} else {
    Log "data directory not empty, skip initialize"
}

# 4. Start if not listening
if (-not (Test-Port3306)) {
    Log "starting mysqld"
    Start-Process -FilePath $mysqld -ArgumentList "--defaults-file=$myIni" -WindowStyle Hidden | Out-Null
    $ok = $false
    for ($i = 0; $i -lt 40; $i++) {
        if (Test-Port3306) { $ok = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $ok) { throw "mysqld did not listen on 3306; see $logDir\mysql-error.log" }
    Log "mysqld is listening"
} else {
    Log "3306 already listening"
}

# 5. Bootstrap accounts and secrets on first run
$rootTxt = Join-Path $secretsDir 'root.txt'
$appTxt  = Join-Path $secretsDir 'app.txt'
$rootCnf = Join-Path $secretsDir 'client-root.cnf'
$appCnf  = Join-Path $secretsDir 'client-app.cnf'

if (-not (Test-Path $rootTxt)) {
    $rootPwd = New-Secret
    $appPwd  = New-Secret
    $sql = @"
ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPwd';
CREATE DATABASE IF NOT EXISTS wordapp CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'wordapp'@'localhost' IDENTIFIED BY '$appPwd';
ALTER USER 'wordapp'@'localhost' IDENTIFIED BY '$appPwd';
CREATE USER IF NOT EXISTS 'wordapp'@'127.0.0.1' IDENTIFIED BY '$appPwd';
ALTER USER 'wordapp'@'127.0.0.1' IDENTIFIED BY '$appPwd';
GRANT SELECT, INSERT, UPDATE, DELETE ON wordapp.* TO 'wordapp'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON wordapp.* TO 'wordapp'@'127.0.0.1';
FLUSH PRIVILEGES;
"@
    $sql | & $mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u root --skip-password --default-character-set=utf8mb4
    if ($LASTEXITCODE -ne 0) { throw "bootstrap failed: $LASTEXITCODE" }
    Set-Content -Path $rootTxt -Value $rootPwd -Encoding ASCII -NoNewline
    Set-Content -Path $appTxt  -Value $appPwd  -Encoding ASCII -NoNewline

    $rootCnfText = @"
[client]
host=127.0.0.1
port=3306
user=root
password=$rootPwd
default-character-set=utf8mb4
"@
    $appCnfText = @"
[client]
host=127.0.0.1
port=3306
user=wordapp
password=$appPwd
default-character-set=utf8mb4
database=wordapp
"@
    [System.IO.File]::WriteAllText($rootCnf, $rootCnfText, [System.Text.UTF8Encoding]::new($false))
    [System.IO.File]::WriteAllText($appCnf,  $appCnfText,  [System.Text.UTF8Encoding]::new($false))
    icacls $secretsDir /inheritance:r /grant:r "$($env:USERNAME):(OI)(CI)F" | Out-Null
    Log "bootstrap complete; secrets are in $secretsDir"
} else {
    Log "secrets already present, skip bootstrap"
}

# 6. Verify
& (Join-Path $PSScriptRoot 'verify-mysql.ps1')
if ($LASTEXITCODE -ne 0) { throw "verification failed: $LASTEXITCODE" }
Log 'done'
