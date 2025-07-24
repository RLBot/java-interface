cd "$(dirname "$0")"

echo Generating flatbuffers header file...

rm -rf src/generated/java

./flatbuffers-schema/binaries/flatc --gen-all --java --gen-object-api -o ./src/generated/java ./flatbuffers-schema/schema/rlbot.fbs

echo Done.