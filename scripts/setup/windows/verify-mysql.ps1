$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$mysql   = 'E:\Develop\MySQL\mysql-8.0.46-winx64\bin\mysql.exe'
$rootCnf = 'E:\Develop\MySQL\.secrets\client-root.cnf'
$appCnf  = 'E:\Develop\MySQL\.secrets\client-app.cnf'

if (-not (Test-Path $mysql))   { throw "mysql.exe not found: $mysql" }
if (-not (Test-Path $rootCnf)) { throw "client-root.cnf not found: $rootCnf" }
if (-not (Test-Path $appCnf))  { throw "client-app.cnf not found: $appCnf" }

Write-Host '== server =='
& $mysql --defaults-extra-file=$rootCnf -e "SELECT VERSION() AS version, @@datadir AS datadir, @@character_set_server AS charset, @@collation_server AS collation, @@time_zone AS tz;"
if ($LASTEXITCODE -ne 0) { throw 'cannot connect as root' }

& $mysql --defaults-extra-file=$rootCnf -e "SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = 'wordapp';"
if ($LASTEXITCODE -ne 0) { throw 'cannot read schema info' }

Write-Host '== users =='
& $mysql --defaults-extra-file=$rootCnf -e "SELECT user, host, plugin FROM mysql.user WHERE user IN ('root','wordapp') ORDER BY user, host;"
if ($LASTEXITCODE -ne 0) { throw 'cannot read mysql.user' }

Write-Host '== app connection and grants =='
& $mysql --defaults-extra-file=$appCnf -e "SELECT CURRENT_USER() AS cur_user, DATABASE() AS db; SHOW GRANTS;"
if ($LASTEXITCODE -ne 0) { throw 'cannot connect as wordapp' }

Write-Host '== grants =='
& $mysql --defaults-extra-file=$rootCnf -e "SHOW GRANTS FOR 'wordapp'@'localhost'; SHOW GRANTS FOR 'wordapp'@'127.0.0.1';"
if ($LASTEXITCODE -ne 0) { throw 'cannot read grants' }

Write-Host '== OK =='
