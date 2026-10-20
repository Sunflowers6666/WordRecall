@echo off
setlocal
set "MYSQL_HOME=E:\Develop\MySQL\mysql-8.0.46-winx64"
set "CNF=E:\Develop\MySQL\.secrets\client-root.cnf"

if not exist "%CNF%" (
  echo [ERROR] client-root.cnf not found: "%CNF%"
  endlocal & exit /b 1
)

"%MYSQL_HOME%\bin\mysqladmin.exe" --defaults-extra-file="%CNF%" ping
set "RC=%ERRORLEVEL%"
endlocal & exit /b %RC%
