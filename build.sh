#!/bin/bash

# Build script for Angail Android app

echo "Building Angail Android app..."

# Clean previous builds
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
    
    # Ask if user wants to install
    read -p "Install to connected device? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        ./gradlew installDebug
        echo "App installed!"
    fi
else
    echo "Build failed. Check the error messages above."
    exit 1
fi
