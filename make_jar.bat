@echo off
REM Author: Sean Pesce



echo Task: Build

REM Remove old build files:
del /f /s /q "%~dp0build" 2> nul > nul
rmdir "%~dp0build" /s /q 2> nul > nul
del /f /s /q "%~dp0dist" 2> nul > nul
rmdir "%~dp0dist" /s /q 2> nul > nul


echo Compiling Java...
REM Compile the .java files:
mkdir "%~dp0build\classes" 2> nul
javac -d "%~dp0build\classes" -classpath "C:\Program Files\Java\jdk1.8.0_121" "%~dp0src/restYourEyes/"*.java


echo Copying resource files...
REM These files are packed into the executable jar file
copy /y "%~dp0style\*.css" "%~dp0build\classes\*.css" > nul
copy /y "%~dp0style\icon.png" "%~dp0build\classes\icon.png" > nul


echo Creating .jar package...
javapackager -createjar -nocss2bin -appclass restYourEyes.RestYourEyesUtil -outdir "%~dp0build" -outfile RestYourEyes -srcdir "%~dp0build\classes"


echo Copying program files...
REM These files are read by the program at runtime
copy /y "%~dp0Prefs.ini" "%~dp0build\Prefs.ini" > nul
copy /y "%~dp0style\icon_white.png" "%~dp0build\icon_white.png" > nul

echo Deleting leftover class files...
del /f /s /q "%~dp0build\classes\restYourEyes" 2> nul > nul
rmdir "%~dp0build\classes\restYourEyes" 2> nul > nul
del /f /s /q "%~dp0build\classes" 2> nul > nul
rmdir "%~dp0build\classes" 2> nul > nul




