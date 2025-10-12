#!/bin/bash

echo "🚀 Setting up Library Management System Development Environment"
echo "=============================================================="

# Check if we're in a dev container
if [ -f /.dockerenv ]; then
    echo "✅ Running in dev container"
    echo "Java version:"
    java -version
    echo ""
    echo "Maven version:"
    mvn -version
    echo ""
else
    echo "⚠️  Not running in dev container. Please open this project in VS Code and use 'Dev Containers: Reopen in Container'"
    echo "Or run: code library-management-system.code-workspace"
    exit 1
fi

echo "📦 Installing dependencies..."
mvn clean install -DskipTests

echo ""
echo "🎯 Available commands:"
echo "  mvn spring-boot:run     - Start the application"
echo "  mvn test               - Run tests"
echo "  mvn clean package      - Build the application"
echo ""
echo "🌐 Application will be available at: http://localhost:8080"
echo ""
echo "👤 Demo credentials:"
echo "  Admin: admin / admin123"
echo "  Member: john.doe / password123"
echo ""
echo "✨ Setup complete! Run 'mvn spring-boot:run' to start the application."
