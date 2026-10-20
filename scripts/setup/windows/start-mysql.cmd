@echo off
setlocal
set "MYSQL_HOME=E:\Develop\MySQL\mysql-8.0.46-winx64"
set "MYSQL_INI=E:\Develop\MySQL\my.ini"

if not exist "%MYSQL_HOME%\bin\mysqld.exe" (
  echo [ERROR] mysqld.exe not found: "%MYSQL_HOME%\bin\mysqld.exe"
  exit /b 1
)
if not exist "%MYSQL_INI%" (
  echo [ERROR] my.ini not found: "%MYSQL_INI%"
  exit /b 1
)

powershell.exe -NoProfile -WindowStyle Hidden -Command "Start-Process -FilePath '%MYSQL_HOME%\bin\mysqld.exe' -ArgumentList '--defaults-file=%MYSQL_INI%' -WindowStyle Hidden"
if errorlevel 1 (
  echo [ERROR] failed to start mysqld
  exit /b 1
)
echo [INFO] MySQL start requested. Run status-mysql.cmd to verify.
endlocal
