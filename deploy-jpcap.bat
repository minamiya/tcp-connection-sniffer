@echo off
if "%ANT_HOME%"=="" goto CAN_NOT_DEPLOY
SET DLLNAME=Jpcap-x86_64.dll
COPY /Y %~dp0jpcap\src\c\win32\x64\Debug\Jpcap.dll %~dp0jpcap\lib\%DLLNAME%
CALL ANT -f %~dp0jpcap\build.xml jar-win
COPY /Y %~dp0jpcap\build\dest\jpcap_win.jar %~dp0lib\jpcap_win.jar
echo jpcap_win.jar is deployed.
goto END

:CAN_NOT_DEPLOY
echo ant is not been installed.
:END
pause