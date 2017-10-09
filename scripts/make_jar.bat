@echo off
REM Author: Sean Pesce

set script_start_dir=%cd%
cd "%~dp0"
cd ..

echo:
echo Task: Build ^(JAR^)

REM Check that a JDK is installed
javac -help >nul 2>&1 && (
	echo Found JDK.
) || (
	echo:
    echo ERROR: JDK must be installed to compile Java projects. Get the JDK here:
	echo https://www.oracle.com/technetwork/java/javase/downloads/index.html
	echo:
	goto done
)


echo Compiling Java...
REM Compile the .java files:
mkdir "%cd%/build\classes" 2> nul
javac -encoding "UTF-8" -d "%cd%/build\classes" "%cd%/src/restyoureyes/"*.java


echo Copying resource files...
REM These files are packed into the executable jar file
copy /y "%cd%/rsrc\version" "%cd%/build\classes\version" > nul
copy /y "%cd%/style\*.css" "%cd%/build\classes\*.css" > nul
copy /y "%cd%/style\icon.png" "%cd%/build\classes\icon.png" > nul


echo Creating .jar package...
javapackager -createjar -nocss2bin -appclass restyoureyes.RestYourEyes -outdir "%cd%/build" -outfile RestYourEyes -srcdir "%cd%/build\classes"


echo Copying program files...
REM These files are read by the program at runtime
copy /y "%cd%/rsrc\Prefs.ini" "%cd%/build\Prefs.ini" > nul
copy /y "%cd%/style\icon_white.png" "%cd%/build\icon_white.png" > nul

echo Deleting leftover class files...
del /f /s /q "%cd%/build\classes\restyoureyes" 2> nul > nul
rmdir "%cd%/build\classes\restyoureyes" 2> nul > nul
del /f /s /q "%cd%/build\classes" 2> nul > nul
rmdir "%cd%/build\classes" 2> nul > nul


echo Finished task ^(JAR^)
echo:

:done


cd "%script_start_dir%"
