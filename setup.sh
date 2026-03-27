#!/bin/bash

echo "Setting up Angail..."

# Create necessary directories
mkdir -p models
mkdir -p data
mkdir -p termux-agent/logs

# Install Python dependencies
echo "Installing Python dependencies..."
pip install -r termux-agent/requirements.txt

# Grant storage permission (for Termux)
echo "Granting storage permission..."
termux-setup-storage

echo ""
echo "Setup complete!"
echo ""
echo "Next steps:"
echo "1. Build and install the Android app (see README.md)"
echo "2. Grant Usage Stats permission in Android Settings"
echo "3. Run 'cd termux-agent && python model_inference.py' to start the agent"
echo "4. Open the Angail app and grant permissions"
echo ""
