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
cmd.exe /c "%JAVA_HOME%\bin\java" -version

cmd.exe /c mvn -version
cmd.exe /c mvn --batch-mode clean || echo ""
cmd.exe /c mvn --batch-mode package -DskipTests || echo ""

copy "%SRC_DIR%\target\denoptim-%PKG_VERSION%-jar-with-dependencies.jar" "%LIBRARY_LIB%\"
xcopy /E /I /Y "%SRC_DIR%\src\main\python\denoptim_rdkit" "%LIBRARY_LIB%\denoptim_rdkit"
if errorlevel 1 exit /b 1

md "%SCRIPTS%\"
copy "%SRC_DIR%\conda\denoptim_launcher.py" "%SCRIPTS%\denoptim-launch.py"
if errorlevel 1 exit /b 1

echo @echo off > "%SCRIPTS%\denoptim.cmd"
echo set "DENOPTIM_JAR=%LIBRARY_LIB%\denoptim-%PKG_VERSION%-jar-with-dependencies.jar">> "%SCRIPTS%\denoptim.cmd"
echo set "DENOPTIM_PYTHONPATH=%LIBRARY_LIB%">> "%SCRIPTS%\denoptim.cmd"
echo python "%SCRIPTS%\denoptim-launch.py" %%*>> "%SCRIPTS%\denoptim.cmd"
echo IF %%ERRORLEVEL%% NEQ 0 EXIT /B %%ERRORLEVEL%%>> "%SCRIPTS%\denoptim.cmd"

echo #!/bin/bash > "%SCRIPTS%\denoptim"
echo export DENOPTIM_JAR="%LIBRARY_LIB%\denoptim-%PKG_VERSION%-jar-with-dependencies.jar">> "%SCRIPTS%\denoptim"
echo export DENOPTIM_PYTHONPATH="%LIBRARY_LIB%">> "%SCRIPTS%\denoptim"
echo exec python "%SCRIPTS%\denoptim-launch.py" "$@" >> "%SCRIPTS%\denoptim"

