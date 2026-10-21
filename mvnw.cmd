@REM Maven Wrapper script for Windows
@REM Downloads Maven if not present and runs it

@echo off
setlocal

set "MAVEN_WRAPPER_PROPERTIES=.mvn\wrapper\maven-wrapper.properties"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto runMaven

echo Downloading Maven...
if not exist "%USERPROFILE%\.m2\wrapper\dists" mkdir "%USERPROFILE%\.m2\wrapper\dists"

set "DIST_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
set "TMPFILE=%TEMP%\maven-wrapper-download.zip"

powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%TMPFILE%' }"
if errorlevel 1 (
    echo Error: Failed to download Maven
    exit /b 1
)

powershell -Command "& { Expand-Archive -Path '%TMPFILE%' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force }"
if errorlevel 1 (
    echo Error: Failed to extract Maven
    exit /b 1
)

del /f "%TMPFILE%" 2>nul
echo Maven installed to %MAVEN_HOME%

:runMaven
"%MAVEN_HOME%\bin\mvn.cmd" %*
