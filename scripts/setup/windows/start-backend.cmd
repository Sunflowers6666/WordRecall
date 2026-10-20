@echo off
setlocal
title WordRecall backend  http://127.0.0.1:8080

set "ROOT=%~dp0..\..\.."
set "BACKEND=%ROOT%\backend"
set "MVN=E:\Develop\Maven\apache-maven-3.10.0\bin\mvn.cmd"

if not exist "%BACKEND%\application-secret.yml" (
  echo [ERROR] backend\application-secret.yml not found.
  echo Copy backend\src\main\resources\application-secret.example.yml
  echo and fill in DB_PASSWORD / JWT_SECRET / WX_MINIAPP_APPID / WX_MINIAPP_SECRET.
  pause
  exit /b 1
)

netstat -ano | findstr /R /C:":8080 .*LISTENING" >nul
if not errorlevel 1 (
  echo [INFO] Port 8080 is already in use.
  echo        If it is this backend, just use it: http://127.0.0.1:8080/api/health
  echo        Otherwise run stop-backend.cmd first.
  exit /b 0
)

cd /d "%BACKEND%"
echo Starting WordRecall backend on http://127.0.0.1:8080
echo Keep this window open while testing the mini program.
echo Close this window or press Ctrl+C to stop.
echo.

if exist "%MVN%" (
  call "%MVN%" -B spring-boot:run
) else (
  call "%BACKEND%\mvnw.cmd" -B spring-boot:run
)

echo.
echo Backend stopped.
pause
