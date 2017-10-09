@echo off
REM Author: Sean Pesce

REM Read version file
set /p restyoureyes_version= < "%~dp0rsrc/version"

REM Check that a JDK is installed
javac -help >nul 2>&1 && (
	REM make_jar.bat will print the "Found" message
) || (
	echo:
    echo ERROR: JDK must be installed to compile Java projects. Get the JDK here:
	echo http://www.oracle.com/technetwork/java/javase/downloads/index.html
	echo:
	if "%1"=="-nopause" goto no_pause
	if "%2"=="-nopause" goto no_pause
	goto done
)

REM Remove old build files:
del /f /s /q "%~dp0dist\bundles\Portable\RestYourEyes-v%restyoureyes_version%" 2> nul > nul
rmdir /s /q "%~dp0dist\bundles\Portable\RestYourEyes-v%restyoureyes_version%" 2> nul > nul
del /f /q "%~dp0dist\bundles\Portable\RestYourEyes-v%restyoureyes_version%.zip" 2> nul > nul
del /f /q "%~dp0dist\bundles\Installer\RestYourEyes-%restyoureyes_version%_setup.exe" 2> nul > nul


call make_jar.bat

echo:
echo Task: Build ^(Standard Bundles^)
echo:


mkdir "%~dp0dist" 2> nul

echo Creating native .exe package...
javapackager -deploy -native image -outdir dist -outfile RestYourEyes -srcdir "%~dp0\build"  -appclass restyoureyes.RestYourEyes -name "RestYourEyes"  -title "RestYourEyes Reminder" -vendor "SeanP Software" -description "Reminds the user to take a break from their screen to avoid eye strain" -BappVersion="%restyoureyes_version%" -Bicon="rsrc\icons\package\windows\RestYourEyes.ico"


REM Check that Inno Setup is installed
echo Creating native installer...
set temp_test_iscc=
iscc 2> nul > nul
set temp_test_iscc=%errorlevel%
if %temp_test_iscc%==1 (
	echo Found Inno Setup.
) else (
	echo:
    echo ERROR: Inno Setup must be installed to generate a native installer. Get Inno Setup here:
	echo http://www.jrsoftware.org/isdl.php
	echo:
	if "%1"=="-nopause" goto no_pause
	if "%2"=="-nopause" goto no_pause
	goto done
)


REM Build native .exe installer
copy /y "%~dp0rsrc\RestYourEyes.iss" "%~dp0dist\bundles\RestYourEyes.iss"  2> nul > nul
iscc "%~dp0dist\bundles\RestYourEyes.iss"
del /f "%~dp0dist\bundles\RestYourEyes.iss" 2> nul


REM Move bundle to "Portable" folder and delete leftover bundle files
robocopy "%~dp0dist\bundles\RestYourEyes" "%~dp0dist\bundles\Portable\RestYourEyes-v%restyoureyes_version%" /s /e > nul
del /f /s /q "%~dp0dist\bundles\RestYourEyes" 2> nul > nul
rmdir /s /q "%~dp0dist\bundles\RestYourEyes" 2> nul > nul


REM Create compressed archive file for portable bundle
cd "%~dp0dist\bundles\Portable\RestYourEyes-v%restyoureyes_version%"
jar -cMf "RestYourEyes-v%restyoureyes_version%.zip" .\*
copy /y "RestYourEyes-v%restyoureyes_version%.zip" ..\"RestYourEyes-v%restyoureyes_version%.zip" > nul
del /f /q "RestYourEyes-v%restyoureyes_version%.zip" 2> nul > nul
cd "%~dp0"



if "%1"=="-nopause" goto no_pause
if "%2"=="-nopause" goto no_pause
:done
pause

:no_pause

echo:
echo Finished task ^(Standard Bundles^)
echo:


