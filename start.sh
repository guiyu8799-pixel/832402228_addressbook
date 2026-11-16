#!/usr/bin/env bash
# One-click launcher for the address book (Linux / macOS).
#
# Starts the Spring Boot application, waits until /api/health answers, then
# prints the URL to open.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$ROOT/target/addressbook-1.0.0.jar"
PORT="${PORT:-8080}"

if ! command -v java >/dev/null 2>&1; then
    echo "[ERROR] java was not found on PATH" >&2
    exit 1
fi

if [ ! -f "$JAR" ]; then
    echo "[ERROR] missing $JAR" >&2
    echo "        build it first with: mvn clean package" >&2
    exit 1
fi

echo "Starting the application on port $PORT ..."
java -Dfile.encoding=UTF-8 -jar "$JAR" &
PID=$!

cleanup() {
    kill "$PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

for _ in $(seq 1 60); do
    if curl -sf "http://localhost:$PORT/api/health" >/dev/null 2>&1; then
        echo "Ready: http://localhost:$PORT/"
        if command -v open >/dev/null 2>&1; then
            open "http://localhost:$PORT/"
        elif command -v xdg-open >/dev/null 2>&1; then
            xdg-open "http://localhost:$PORT/"
        fi
        wait "$PID"
        exit 0
    fi
    sleep 1
done

echo "[ERROR] the application did not answer within 60 seconds" >&2
exit 1
