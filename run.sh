#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build
javac -encoding UTF-8 -d build server/src/servicedesk/Main.java
java -cp build servicedesk.Main 8080
