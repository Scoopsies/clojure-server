#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
HTTP_SERVER_DIR="$SCRIPT_DIR/../HTTP-Server"
TTT_DIR="$SCRIPT_DIR/../clojure-tic-tac-toe"

# Check sibling repos exist
if [ ! -d "$HTTP_SERVER_DIR" ]; then
  echo "Error: HTTP-Server repo not found at $HTTP_SERVER_DIR"
  echo "Please clone it as a sibling directory: git clone https://github.com/Scoopsies/HTTP-Server.git ../HTTP-Server"
  exit 1
fi

if [ ! -d "$TTT_DIR" ]; then
  echo "Error: clojure-tic-tac-toe repo not found at $TTT_DIR"
  echo "Please clone it as a sibling directory: git clone https://github.com/Scoopsies/clojure-tic-tac-toe.git ../clojure-tic-tac-toe"
  exit 1
fi

# Build HttpServer JAR
echo "Building HttpServer.jar..."
mvn -f "$HTTP_SERVER_DIR/pom.xml" package -q -DskipTests

# Build TicTacToe JAR
echo "Building TicTacToe.jar..."
(cd "$TTT_DIR" && clojure -T:build jar)

# Copy JARs into lib/
mkdir -p "$SCRIPT_DIR/lib"
cp "$HTTP_SERVER_DIR/target/HttpServer-1.0-SNAPSHOT.jar" "$SCRIPT_DIR/lib/HttpServer.jar"
cp "$TTT_DIR/target/TicTacToe.jar" "$SCRIPT_DIR/lib/TicTacToe.jar"

echo "Done! JARs copied to lib/"
ls -l "$SCRIPT_DIR/lib/"
