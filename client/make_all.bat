@echo off
REM Author: Sean Pesce

set script_start_dir=%cd%
cd "%~dp0"

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
del /f /s /q "%~dp0Release" 2> nul > nul
rmdir "%~dp0Release" /s /q 2> nul > nul

REM Make Lite bundle (no JRE)
call "%~dp0scripts/make_lite.bat" -nopause

REM Make full bundle (includes JRE)
call "%~dp0scripts/make.bat" -nopause

REM Remove build folder (contains redundant files):
del /f /s /q "%~dp0build" 2> nul > nul
rmdir "%~dp0build" /s /q 2> nul > nul

echo:
echo Tasks complete.
echo Start time: %MAKE_ALL_START_TIME%
echo End time:   %TIME%
echo:

:done
pause

cd "%script_start_dir%"

