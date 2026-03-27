# Angail - Android Personal Usage Guardian

An Android app that monitors phone usage patterns and provides gentle interventions to reduce doomscrolling and promote healthier digital habits. The app runs a lightweight AI agent (currently using simple template-based inference) that analyzes your usage and sends helpful notifications.

## Features

- **Usage Monitoring**: Tracks app usage, screen time, and patterns
- **Smart Interventions**: Detects problematic behaviors (social media binging, late-night scrolling)
- **Personalized Notifications**: Context-aware messages suggesting breaks or alternatives
- **App Launching**: Can suggest and open alternative apps (like reading apps)
- **Configurable Thresholds**: Customize when and how the agent intervenes
- **Privacy-First**: All data processed locally on your device

## Architecture

The app consists of two main components:

1. **Android App** (Kotlin/Compose): UI and notification system
2. **Termux Backend** (Python): Usage monitoring, trigger detection, and AI inference

Communication between components happens via a local socket connection.

## Prerequisites

- Android device with Android 7.0 (API 24) or higher
- Termux app installed
- Termux:API package installed

## Installation

### 1. Install Termux and Termux:API

```bash
# Install Termux from F-Droid or Google Play
# Then install Termux:API:
pkg install termux-api
```

### 2. Grant Termux:API Permissions

In Termux:

```bash
# Grant storage permission
termux-setup-storage

# Grant usage stats permission (may need to do this in Android settings)
termux-microphone-record
```

You may need to manually grant "Usage access" in Android Settings > Apps > Termux > Special access > Usage access.

### 3. Build and Install the Android App

```bash
# From the project root
cd app

# Build debug APK
./gradlew assembleDebug

# Or install directly to connected device
./gradlew installDebug
```

Alternatively, open the project in Android Studio and run it on your device.

### 4. Setup Termux Backend

```bash
# In Termux on your device
cd ~/angail/termux-agent

# Install Python dependencies
pip install -r requirements.txt

# Create necessary directories
mkdir -p ../models ../data
```

### 5. Configure the App

1. Open the Angail app on your phone
2. Grant the requested permissions (Usage Stats, Overlay)
3. Configure trigger thresholds using the sliders
4. Toggle the agent switch to activate

## Usage

### Starting the Agent

1. Make sure the Android app is open and permissions are granted
2. In Termux, run:

```bash
cd ~/angail/termux-agent
python model_inference.py
```

3. In the Android app, toggle the "Agent Active" switch

### Stopping the Agent

- Toggle the switch off in the Android app, or
- Press Ctrl+C in Termux

### Customizing Thresholds

Edit `config/thresholds.json`:

```json
{
  "social_media_continuous_minutes": 30,
  "total_screen_time_hourly_minutes": 120,
  "entertainment_after_hours": "23:00",
  "scrolling_detection_threshold": 15,
  "productivity_gap_hours": 4,
  "daily_limit_minutes": 300
}
```

### Adding App Categories

Edit `config/app_categories.json` to categorize your apps:

```json
{
  "com.example.app": "social_media",
  "com.example.reading": "reading",
  "com.example.work": "productivity"
}
```

## Troubleshooting

### Agent not sending notifications

1. Check that the Android app is running
2. Verify Usage Stats permission is granted
3. Check Termux error logs
4. Ensure both apps are using the same port (9999)

### Permission denied errors

- Grant Usage Stats permission in Settings > Apps > Termux > Special access
- Grant Overlay permission in Settings > Apps > Angail > Display over other apps

### Termux:API commands failing

```bash
# Update Termux:API
pkg upgrade termux-api

# Test if it works
termux-battery-status
```

## Project Structure

```
angail/
├── app/                          # Android app
│   ├── src/main/
│   │   ├── java/com/angail/
│   │   │   ├── MainActivity.kt          # Main UI
│   │   │   ├── AgentController.kt       # Socket server
│   │   │   ├── PermissionHandler.kt    # Permission management
│   │   │   ├── NotificationHelper.kt    # Notification creation
│   │   │   └── AppLauncher.kt           # App launching
│   │   ├── res/                         # Android resources
│   │   └── AndroidManifest.xml          # App manifest
│   └── build.gradle.kts
├── termux-agent/                # Python backend
│   ├── usage_monitor.py              # Usage tracking
│   ├── trigger_system.py             # Threshold checking
│   ├── notification_generator.py      # Message generation
│   ├── stats_analyzer.py             # Pattern analysis
│   ├── model_inference.py            # Main agent loop
│   └── requirements.txt
├── config/                      # Configuration files
│   ├── thresholds.json              # Trigger thresholds
│   ├── app_categories.json         # App categorization
│   └── model_config.json            # Model settings
├── models/                      # Model files (for future AI)
└── data/                       # Usage data storage
```

## Development

### Android App Development

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Install on device
./gradlew installDebug
```

### Termux Backend Development

```bash
# Run the agent
cd termux-agent
python model_inference.py

# Test individual components
python -c "from usage_monitor import UsageMonitor; um = UsageMonitor(); print(um.get_current_app())"
```

## Future Enhancements

- [ ] Integration with actual small language models (TinyLlama, Phi-2, etc.)
- [ ] Machine learning to personalize thresholds
- [ ] Weekly usage reports
- [ ] Calendar integration for context-aware suggestions
- [ ] Wear OS companion app
- [ ] Voice assistant integration
- [ ] Multi-user profile support

## License

MIT License - feel free to use and modify for your own needs.

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## Privacy

This app is designed with privacy in mind:
- All data is processed locally on your device
- No data is sent to external servers
- Usage statistics are stored only on your phone
- You can export or clear your data at any time

## Support

For issues or questions, please open an issue on GitHub.
