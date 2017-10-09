@echo off
REM Author: Sean Pesce


echo:
echo Task: Build ^(JAR^)

REM Check that a JDK is installed
javac -help >nul 2>&1 && (
	echo Found JDK.
) || (
	echo:
    echo ERROR: JDK must be installed to compile Java projects. Get the JDK here:
	echo http://www.oracle.com/technetwork/java/javase/downloads/index.html
	echo:
	goto done
)


echo Compiling Java...
REM Compile the .java files:
mkdir "%~dp0build\classes" 2> nul
javac -encoding "UTF-8" -d "%~dp0build\classes" "%~dp0src/restyoureyes/"*.java


echo Copying resource files...
REM These files are packed into the executable jar file
copy /y "%~dp0rsrc\version" "%~dp0build\classes\version" > nul
copy /y "%~dp0style\*.css" "%~dp0build\classes\*.css" > nul
copy /y "%~dp0style\icon.png" "%~dp0build\classes\icon.png" > nul


echo Creating .jar package...
javapackager -createjar -nocss2bin -appclass restyoureyes.RestYourEyes -outdir "%~dp0build" -outfile RestYourEyes -srcdir "%~dp0build\classes"


echo Copying program files...
REM These files are read by the program at runtime
copy /y "%~dp0rsrc\Prefs.ini" "%~dp0build\Prefs.ini" > nul
copy /y "%~dp0style\icon_white.png" "%~dp0build\icon_white.png" > nul

echo Deleting leftover class files...
del /f /s /q "%~dp0build\classes\restyoureyes" 2> nul > nul
rmdir "%~dp0build\classes\restyoureyes" 2> nul > nul
del /f /s /q "%~dp0build\classes" 2> nul > nul
rmdir "%~dp0build\classes" 2> nul > nul


echo Finished task ^(JAR^)
echo:

:done



