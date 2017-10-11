@echo off
REM Author: Sean Pesce

set script_start_dir=%cd%
cd "%~dp0"

REM Read version file
set /p restyoureyes_version= < "%cd%/../rsrc/version"

REM Check that a JDK is installed
javac -help >nul 2>&1 && (
	echo Found JDK.
) || (
	echo:
    echo ERROR: JDK must be installed to compile Java projects. Get the JDK here:
	echo https://www.oracle.com/technetwork/java/javase/downloads/index.html
	echo:
	if "%1"=="-nopause" goto no_pause
	if "%2"=="-nopause" goto no_pause
	goto done
)


REM Check that Apache Ant is installed
REM Check that Apache Ant is installed
set temp_test_ant=
cmd /c "ant -h  2> nul > nul"
set temp_test_ant=%errorlevel%
if %temp_test_ant%==0 (
	echo Found Apache Ant.
) else (
	echo:
    echo ERROR: Apache Ant must be installed to run Ant build scripts. Get Apache Ant here:
	echo https://ant.apache.org/manual/install.html
	echo:
	if "%1"=="-nopause" goto no_pause
	if "%2"=="-nopause" goto no_pause
	goto done
)


REM Remove old build files:
del /f /s /q "%cd%/../build" 2> nul > nul
rmdir /s /q "%cd%/../build" 2> nul > nul
del /f /s /q "%cd%/../dist" 2> nul > nul
rmdir /s /q "%cd%/../dist" 2> nul > nul

mkdir "%cd%/../dist" 2> nul
mkdir "%cd%/../build" 2> nul

echo:
echo Task: Build ^(Ant^)
echo:



copy /y "%cd%\build.xml" ..\build\build.xml > nul
cd "%cd%/../build"
cmd /c "ant"

cd ..
del /f /s /q "%cd%/build" 2> nul > nul
rmdir /s /q "%cd%/build" 2> nul > nul


if "%1"=="-nopause" goto no_pause
if "%2"=="-nopause" goto no_pause
:done
pause

:no_pause

echo:
echo Finished task ^(Ant^)
echo:

cd "%script_start_dir%"
