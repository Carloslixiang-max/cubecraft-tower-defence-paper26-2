$ErrorActionPreference = "Stop"

Write-Host "== CubeCraft TD Stage 4 verification =="

$java = & java -version 2>&1 | Out-String
if ($java -notmatch 'version "25') {
    throw "Java 25 is required. java -version returned:`n$java"
}

if (Test-Path ".\gradlew.bat") {
    & .\gradlew.bat clean test shadowJar
} elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
    & gradle clean test shadowJar
} else {
    throw "Gradle wrapper is not present and no system Gradle was found. Generate wrapper with Gradle 9.7.1 first."
}

if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed."
}

Write-Host "Build gate passed. Next gate: run Paper 26.2 and verify enable/disable/restart twice."
Write-Host "Suggested dev run: gradle runServer"
