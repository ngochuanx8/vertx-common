#!/bin/bash

# Script to set up dependencies for running the application

echo "Setting up dependencies for Vert.x application..."

# Check if Maven is available
if command -v mvn &> /dev/null; then
    echo "✅ Maven found, copying dependencies..."
    mvn dependency:copy-dependencies -DoutputDirectory=target/dependency
    
    if [ $? -eq 0 ]; then
        echo "✅ Dependencies copied to target/dependency"
        echo "You can now run: ./run-java-monitoring.sh"
    else
        echo "❌ Failed to copy dependencies"
        exit 1
    fi
else
    echo "❌ Maven not found. Please install Maven first:"
    echo "  macOS: brew install maven"
    echo "  Ubuntu: sudo apt install maven"
    echo ""
    echo "Or manually download and place these JARs in target/dependency/:"
    echo "  - vertx-core-4.4.9.jar"
    echo "  - vertx-web-4.4.9.jar"
    echo "  - jackson-databind-2.15.2.jar"
    echo "  - jackson-core-*.jar"
    echo "  - jackson-annotations-*.jar"
    echo "  - slf4j-api-2.0.9.jar"
    echo "  - logback-classic-1.4.14.jar"
    echo "  - logback-core-*.jar"
    echo "  - netty-*.jar (multiple netty dependencies)"
    exit 1
fi