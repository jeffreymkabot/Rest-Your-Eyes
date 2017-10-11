@echo off
REM Author: Sean Pesce
REM Lite version ships without a JRE, so the end user is expected to have a JRE installed.

set script_start_dir=%cd%
cd "%~dp0"
cd ..

REM Read version file
set /p restyoureyes_version= < "%cd%/rsrc/version"


REM Check that a JDK is installed
javac -help >nul 2>&1 && (
	REM make_jar.bat will print the "Found" message
) || (
	echo:
    echo ERROR: JDK must be installed to compile Java projects. Get the JDK here:
	echo https://www.oracle.com/technetwork/java/javase/downloads/index.html
	echo:
	if "%1"=="-nopause" goto no_pause
	if "%2"=="-nopause" goto no_pause
	goto done
)

if "%1"=="-fast" goto skipcompile
if "%2"=="-fast" goto skipcompile
call "%~dp0make_jar.bat"


mkdir "%cd%/dist" 2> nul

:skipcompile

echo:
echo Task: Build ^(Lite Bundles^)
echo:

REM Remove old build files:
del /f /s /q "%cd%/dist\bundles\Portable\RestYourEyes-v%restyoureyes_version%-Lite" 2> nul > nul
rmdir /s /q "%cd%/dist\bundles\Portable\RestYourEyes-v%restyoureyes_version%-Lite" 2> nul > nul
del /f /q "%cd%/dist\bundles\Portable\RestYourEyes-v%restyoureyes_version%-Lite.zip" 2> nul > nul
del /f /q "%cd%/dist\bundles\Installer\RestYourEyes-%restyoureyes_version%-Lite_setup.exe" 2> nul > nul

echo Creating native .exe package ^(Lite^)...
javapackager -deploy -native image -outdir dist -outfile RestYourEyes -srcdir "%cd%\build"  -appclass restyoureyes.RestYourEyes -name "RestYourEyes"  -title "RestYourEyes Reminder" -vendor "SeanP Software" -description "Reminds the user to take a break from their screen to avoid eye strain" -BappVersion="%restyoureyes_version%" -Bicon="rsrc\icons\package\windows\RestYourEyes.ico" -Bruntime=


REM Check that Inno Setup is installed
echo Creating native installer ^(Lite^)...
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
copy /y "%cd%/rsrc\RestYourEyes.iss" "%cd%/dist\bundles\RestYourEyes.iss"  2> nul > nul
iscc "%cd%/dist\bundles\RestYourEyes.iss"
del /f "%cd%/dist\bundles\RestYourEyes.iss" 2> nul


REM Move bundle to "Portable" folder, append package names with "-Lite", and delete leftover bundle files
robocopy "%cd%/dist\bundles\RestYourEyes" "%cd%/dist\bundles\Portable\RestYourEyes-v%restyoureyes_version%-Lite" /s /e > nul
ren "%cd%/dist\bundles\Installer\RestYourEyes-%restyoureyes_version%_setup.exe" "RestYourEyes-%restyoureyes_version%-Lite_setup.exe" > nul
del /f /s /q "%cd%/dist\bundles\RestYourEyes" 2> nul > nul
rmdir /s /q "%cd%/dist\bundles\RestYourEyes" 2> nul > nul


REM Create compressed archive file for portable bundle
cd "%cd%/dist\bundles\Portable\RestYourEyes-v%restyoureyes_version%-Lite"
jar -cMf "RestYourEyes-v%restyoureyes_version%-Lite.zip" .\*
copy /y "RestYourEyes-v%restyoureyes_version%-Lite.zip" ..\"RestYourEyes-v%restyoureyes_version%-Lite.zip" > nul
del /f /q "RestYourEyes-v%restyoureyes_version%-Lite.zip" 2> nul > nul
cd "%~dp0"
cd ..




if "%1"=="-nopause" goto no_pause
if "%2"=="-nopause" goto no_pause
:done
pause

:no_pause

echo:
echo Finished task ^(Lite Bundles^)
echo:

cd "%script_start_dir%"

