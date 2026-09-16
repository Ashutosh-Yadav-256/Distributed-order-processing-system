@echo off
setlocal
set "__BASE_DIR__=%~dp0"
if "%__BASE_DIR__:~-1%"=="\" set "__BASE_DIR__=%__BASE_DIR__:~0,-1%"

set "MVN_EXEC=%__BASE_DIR__%\.mvn\maven\bin\mvn.cmd"
if not exist "%MVN_EXEC%" (
    echo Downloading embedded Apache Maven 3.9.9...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $zip = '%__BASE_DIR__%\.mvn\maven.zip'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile $zip; Expand-Archive -Path $zip -DestinationPath '%__BASE_DIR__%\.mvn\temp' -Force; Move-Item '%__BASE_DIR__%\.mvn\temp\apache-maven-3.9.9' '%__BASE_DIR__%\.mvn\maven' -Force; Remove-Item -Recurse -Force '%__BASE_DIR__%\.mvn\temp', $zip }"
)

call "%MVN_EXEC%" %*
endlocal
