@echo off
cd /d "%~dp0"
start "Bond of the Beast - Server" cmd /k "call gradlew.bat runLocalServer"
for /L %%I in (1,1,120) do (
  powershell -NoProfile -Command "$c=[Net.Sockets.TcpClient]::new();try{$c.Connect('127.0.0.1',25565);exit 0}catch{exit 1}finally{$c.Dispose()}" >nul 2>nul
  if not errorlevel 1 goto server_ready
  if exist "run\local-server\eula.txt" (
    findstr /i /c:"eula=true" "run\local-server\eula.txt" >nul
    if errorlevel 1 (
      echo Accept the Minecraft EULA in run\local-server\eula.txt, then run this file again.
      exit /b 1
    )
  )
  timeout /t 1 /nobreak >nul
)
echo Local server did not open port 25565. Check the server window.
exit /b 1
:server_ready
start "Bond of the Beast - Owner" cmd /k "call gradlew.bat runClientOwner"
timeout /t 2 /nobreak >nul
start "Bond of the Beast - Pet" cmd /k "call gradlew.bat runClientPet"
echo In both game clients, connect to 127.0.0.1:25565.
