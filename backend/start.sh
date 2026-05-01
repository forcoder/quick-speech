#!/bin/bash
echo "=== Java Version ==="
java -version
echo "=== JAR Info ==="
ls -la /app/app.jar
echo "=== Environment ==="
env | sort
echo "=== Starting Application ==="
java -Xmx512m -jar /app/app.jar
