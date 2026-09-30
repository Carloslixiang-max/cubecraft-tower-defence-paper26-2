@echo off
cd /d "%~dp0"
if not exist paper.jar (
  echo Put Paper 26.2 in this directory as paper.jar. See README.zh-CN.md.
  pause
  exit /b 1
)
java -Xms1G -Xmx4G -jar paper.jar --nogui
pause
