#!/bin/bash

# Build script for Render deployment

echo "Starting build process..."

# Set Java version
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Verify Java version
java -version

# Clean and build the application
echo "Building application..."
mvn clean package -DskipTests

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Build completed successfully!"
    ls -la target/
else
    echo "Build failed!"
    exit 1
fi

echo "Build process completed."
