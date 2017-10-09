@echo off
REM Author: Sean Pesce

REM Read version file
set /p restyoureyes_version= < "%~dp0rsrc/version"

set MAKE_ALL_START_TIME=%TIME%

echo:
echo Task: Build ^(All^)
echo:

REM Remove old build files:
del /f /s /q "%~dp0build" 2> nul > nul
rmdir "%~dp0build" /s /q 2> nul > nul
del /f /s /q "%~dp0dist" 2> nul > nul
rmdir "%~dp0dist" /s /q 2> nul > nul

REM Make Lite bundle (no JRE)
call make_lite.bat -nopause

REM Make full bundle (includes JRE)
call make.bat -nopause


echo:
echo Tasks complete.
echo Start time: %MAKE_ALL_START_TIME%
echo End time:   %TIME%
echo:

:done
pause



