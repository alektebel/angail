# Quick Start Guide for Angail

## Setup

### 1. Install Prerequisites

#### On your computer (development):
- Install Android Studio
- Install Java JDK 11 or higher
- Set up Android SDK

#### On your Android device:
- Install Termux from F-Droid or Google Play Store
- Install Termux:API package from F-Droid
- Install the Angail APK

### 2. Configure Development Environment

```bash
# Copy the local.properties example and set your SDK path
cp local.properties.example local.properties

# Edit local.properties and set your Android SDK path
# Example: sdk.dir=/home/user/Android/Sdk
```

### 3. Build the Android App

```bash
# Make the build script executable
chmod +x build.sh

# Build the APK
./build.sh
```

### 4. Install and Run

1. Install the APK on your Android device
2. Open Termux and run:
   ```bash
   pkg install termux-api
   pkg install python
   cd ~/angail/termux-agent
   pip install -r requirements.txt
   python model_inference.py
   ```
3. Open the Angail app
4. Grant all requested permissions
5. Toggle the agent switch to activate

## Testing

### Test Notification System

In Termux:
```bash
echo "NOTIFICATION:Test message" | nc 127.0.0.1 9999
```

### Test App Launching

In Termux:
```bash
echo "OPEN_APP:org.koreader.android|Time to read!" | nc 127.0.0.1 9999
```

## Troubleshooting

### Android Build Fails
- Check that ANDROID_HOME is set correctly in local.properties
- Ensure Android SDK, Build Tools, and Platform Tools are installed

### Termux:API Commands Don't Work
```bash
pkg upgrade termux-api
```

### Permissions Denied
- Check Android Settings > Apps > Termux > Special access > Usage access
- Check Android Settings > Apps > Angail > Display over other apps

## Development Tips

- The agent checks every 30 seconds by default (adjust in `model_inference.py`)
- Thresholds are in `config/thresholds.json`
- App categories are in `config/app_categories.json`
- Usage data is stored in `data/usage_stats.json`

## Android Studio

To open in Android Studio:
1. File > Open
2. Select the `angail` directory
3. Wait for Gradle sync to complete
4. Click Run or press Shift+F10
