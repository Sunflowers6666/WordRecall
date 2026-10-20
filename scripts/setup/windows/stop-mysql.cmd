@echo off
setlocal
set "MYSQL_HOME=E:\Develop\MySQL\mysql-8.0.46-winx64"
set "CNF=E:\Develop\MySQL\.secrets\client-root.cnf"

if not exist "%CNF%" (
  echo [ERROR] client-root.cnf not found: "%CNF%"
  endlocal & exit /b 1
)

"%MYSQL_HOME%\bin\mysqladmin.exe" --defaults-extra-file="%CNF%" shutdown
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" echo [ERROR] MySQL shutdown failed with exit code %RC%.
if "%RC%"=="0" echo [INFO] MySQL stopped.
endlocal & exit /b %RC%
