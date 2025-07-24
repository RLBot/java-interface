@echo off

cd /D "%~dp0"

echo Generating flatbuffers header file...

IF EXIST .\src\generated del /s /q .\src\generated

.\flatbuffers-schema\binaries\flatc.exe --java --gen-all --gen-object-api -o .\src\generated\java .\flatbuffers-schema\schema\rlbot.fbs

echo Done.