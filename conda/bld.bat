cd "%SRC_DIR%"

set "JAVA_HOME=%CONDA_PREFIX%\Library"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Java not found in %JAVA_HOME%\bin
  echo Searching for Java...
  
  if exist "%CONDA_PREFIX%\bin\java.exe" (
    set "JAVA_HOME=%CONDA_PREFIX%"
    echo Found Java in %JAVA_HOME%
  ) else (
    echo ERROR: Java not found in Conda environment
    exit 1
  )
)

echo Using JAVA_HOME: %JAVA_HOME%
"%JAVA_HOME%\bin\java" -version

cmd.exe /c mvn -version
cmd.exe /c mvn --batch-mode clean || echo ""
cmd.exe /c mvn --batch-mode package || echo ""

copy "%SRC_DIR%\target\denoptim-%PKG_VERSION%-jar-with-dependencies.jar" "%LIBRARY_LIB%\"

md "%SCRIPTS%\"

echo @echo off > "%SCRIPTS%\denoptim.cmd"
echo java -jar "%LIBRARY_LIB%\denoptim-%PKG_VERSION%-jar-with-dependencies.jar" %%* >> "%SCRIPTS%\denoptim.cmd"
echo IF %%ERRORLEVEL%% NEQ 0 EXIT /B %%ERRORLEVEL%% >> "%SCRIPTS%\denoptim.cmd"

echo #!/bin/bash > "%SCRIPTS%\denoptim"
echo java -jar "%LIBRARY_LIB%\denoptim-%PKG_VERSION%-jar-with-dependencies.jar" $@ >> "%SCRIPTS%\denoptim"

