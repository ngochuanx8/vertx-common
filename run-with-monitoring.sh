#!/bin/bash

# Script to run the Vert.x application with JVM monitoring enabled for VisualVM

# JVM arguments for monitoring and profiling
JVM_OPTS="-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-Xmx2g \
-Xms1g \
-XX:+UnlockExperimentalVMOptions \
-XX:+UseStringDeduplication \
-Djava.rmi.server.hostname=localhost \
-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=9999 \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.local.only=false \
-XX:+FlightRecorder \
-XX:StartFlightRecording=duration=60m,filename=vertx-app.jfr \
-XX:FlightRecorderOptions=samplethreads=true \
-Djava.awt.headless=true"

echo "Starting Vert.x application with monitoring enabled..."
echo "JMX Port: 9999 (for VisualVM connection)"
echo "Flight Recorder will save to: vertx-app.jfr"
echo ""

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven (mvn) not found in PATH"
    echo ""
    echo "Please install Maven or run the application manually:"
    echo "1. Install Maven:"
    echo "   macOS: brew install maven"
    echo "   Ubuntu: sudo apt install maven"
    echo ""
    echo "2. Or compile and run manually with your IDE:"
    echo "   - Compile the project in your IDE"
    echo "   - Run org.example.App with these JVM arguments:"
    echo "   $JVM_OPTS"
    echo ""
    echo "3. Or if you have the compiled classes, run:"
    echo "   java $JVM_OPTS -cp target/classes:target/dependency/* org.example.App"
    exit 1
fi

# Compile the project first
echo "Compiling project..."
mvn compile -q

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Starting application..."
echo "Connect VisualVM to: localhost:9999"
echo "Press Ctrl+C to stop the application"
echo ""

# Run the application with monitoring JVM options
mvn exec:java -Dexec.mainClass="org.example.App" -Dexec.args="$JVM_OPTS"