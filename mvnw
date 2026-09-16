#!/bin/sh
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
MVN_EXEC="$BASE_DIR/.mvn/maven/bin/mvn"

if [ ! -f "$MVN_EXEC" ]; then
    echo "Downloading embedded Apache Maven 3.9.9..."
    mkdir -p "$BASE_DIR/.mvn"
    curl -sSLo "$BASE_DIR/.mvn/maven.zip" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
    unzip -q "$BASE_DIR/.mvn/maven.zip" -d "$BASE_DIR/.mvn/temp"
    mv "$BASE_DIR/.mvn/temp/apache-maven-3.9.9" "$BASE_DIR/.mvn/maven"
    rm -rf "$BASE_DIR/.mvn/temp" "$BASE_DIR/.mvn/maven.zip"
    chmod +x "$BASE_DIR/.mvn/maven/bin/mvn"
fi

exec "$MVN_EXEC" "$@"
