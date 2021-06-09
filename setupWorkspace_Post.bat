@echo OFF

echo.
echo RENAMING ECLIPSE LAUNCH PROFILES..
echo.

SET mypath=%~dp0
SET mypath=%mypath:~0,-1%

setlocal enabledelayedexpansion

set num=0

:LOOP
call set tmpa=%%mypath:~%num%,1%%%
set /a num+=1
if not "%tmpa%" equ "" (
set rline=%tmpa%%rline%
goto LOOP
)

set mypath=%rline%

set "basename=%mypath:\=" & set "string2=%"

set num=0

:LOOPB
call set tmpa2=%%basename:~%num%,1%%%
set /a num+=1
if not "%tmpa2%" equ "" (
set rline2=%tmpa2%%rline2%
goto LOOPB
)

set "basename=%rline2%

set launchname=%basename%_Client.launch

ren runClient.launch %launchname%

echo RENAMED TO: %launchname%
echo.

PAUSE