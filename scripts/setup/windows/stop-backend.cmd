@echo off
setlocal enabledelayedexpansion
title WordRecall backend - stop

set "FOUND="
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /R /C:":8080 .*LISTENING"') do (
  if not defined P_%%p (
    set "P_%%p=1"
    set "FOUND=1"
    echo Stopping process PID %%p on port 8080 ...
    taskkill /PID %%p /F >nul 2>&1
  )
)

if not defined FOUND (
  echo Nothing is listening on port 8080.
) else (
  echo Done.
)
exit /b 0
