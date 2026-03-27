# Android Personal Usage Guardian Agent

## Project Overview
An Android app that runs a lightweight AI model to monitor phone usage patterns, detect procrastination behaviors, and intervene with notifications and app suggestions to promote healthier digital habits.

## Tech Stack
- **Platform**: Android (API 24+ for Termux:API compatibility)
- **Environment**: Termux + Termux:API
- **AI Runtime**: ONNX Runtime or TensorFlow Lite
- **Model**: Small language model (~1-3B parameters, quantized)
- **Language**: Kotlin (Android app) + Python (Termux backend)
- **Build Tools**: Gradle, Poetry
- **Background Service**: Android Foreground Service
- **Permissions Manager**: Android Jetpack ActivityResultContracts

## Architecture

### Components

#### 1. Android App Layer (`app/`)
- **MainActivity.kt**: Main UI for agent activation and configuration
- **PermissionHandler.kt**: Manages runtime permissions
- **NotificationHelper.kt**: Creates and displays notifications
- **AppLauncher.kt**: Opens external apps on agent command
- **AgentController.kt**: Communicates with Termux backend

#### 2. Termux Backend (`termux-agent/`)
- **usage_monitor.py**: Monitors app usage time, screen time, category analysis
- **trigger_system.py**: Evaluates thresholds and patterns
- **model_inference.py**: Runs the small model with context
- **notification_generator.py**: Generates personalized advice
- **stats_analyzer.py**: Analyzes usage patterns and trends

#### 3. Configuration (`config/`)
- **thresholds.json**: User-configurable trigger thresholds
- **model_config.json**: Model settings and prompts
- **app_categories.json**: App categorization mapping

## Core Features

### 1. Usage Statistics Collection
- Screen time tracking (total and per-app)
- App category tracking (social media, productivity, entertainment, etc.)
- Session duration analysis
- Time of day patterns
- Swipe/scroll intensity detection (where available via accessibility service)

### 2. Trigger Conditions
Agent activates when:
- Social media usage > 30 minutes without break
- Total screen time > 2 hours in last hour
- Late-night usage (> 11 PM) on entertainment apps
- Continuous scrolling on same app > 15 minutes
- Productivity apps unused for > 4 hours
- Weekly average exceeds user-set limit

### 3. Model Capabilities
- **Contextual Advice**: Generates personalized messages based on current activity
- **Pattern Recognition**: Identifies procrastination patterns
- **Gentle Interventions**: Non-intrusive, supportive tone
- **Action Suggestions**: Recommends specific activities or apps
- **Time Awareness**: Considers time of day, schedule, and history

### 4. Notification Types
- **Gentle Reminder**: "You've been scrolling for 20 mins. Maybe take a break?"
- **Direct Intervention**: "Stop scrolling on Instagram"
- **Positive Suggestion**: "Great time to read! Opening your book app..."
- **Health Check**: "5 hours of screen time today. Go outside for 10 mins?"
- **Pattern Alert**: "I notice you doomscroll at 10 PM every night"

### 5. Automated Actions
- Open specific apps (book reader, meditation, exercise apps)
- Suggest offline activities
- Provide motivational quotes
- Create usage summaries

## User Permissions Required

### Critical Permissions
```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

### Optional but Recommended
```xml
<uses-permission android:name="android.permission.ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

### Termux-Specific
- Termux:API installed and granted permissions
- Storage access for model files
- Network access (optional, for model updates)

## Model Configuration

### Model Requirements
- **Size**: 1-3B parameters (quantized to 4-bit or 8-bit)
- **Memory Target**: 500MB-1.5GB RAM usage
- **Inference Time**: < 3 seconds per response
- **Framework**: TensorFlow Lite or ONNX Runtime

### Suggested Models
- TinyLlama-1.1B (quantized)
- Phi-2 (2.7B, quantized)
- Gemma-2B (quantized)

### System Prompt Template
```
You are a gentle, supportive digital guardian angel helping the user maintain healthy phone habits. 
You have access to their current usage patterns and can suggest positive alternatives to doomscrolling.

Current context:
- App in use: {current_app}
- Usage duration: {duration}
- Time of day: {time}
- Recent patterns: {patterns}

Generate a short, supportive message (max 150 chars) and optionally suggest an app action.
Tone: Caring, non-judgmental, encouraging.
```

## Implementation Workflow

### 1. Agent Activation
1. User opens app and grants permissions
2. User sets preferences (active hours, trigger sensitivity)
3. Agent starts as foreground service
4. Usage monitoring begins

### 2. Monitoring Loop
```python
while agent_active:
    current_usage = get_current_app_usage()
    patterns = analyze_patterns(current_usage)
    
    if exceeds_threshold(patterns):
        context = build_context(current_usage, patterns)
        response = model.generate(context)
        notification = create_notification(response)
        send_to_android(notification)
        
        if response.suggests_action:
            open_app(response.app_package)
    
    sleep(check_interval)
```

### 3. Notification Flow
1. Termux backend generates message
2. Sends to Android app via local socket or file
3. Android app creates notification
4. User can dismiss or follow suggestion
5. Action logged for future learning

## Configuration Files

### thresholds.json
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

### app_categories.json
```json
{
  "com.instagram.android": "social_media",
  "com.twitter.android": "social_media",
  "com.reddit.frontpage": "social_media",
  "com.netflix.mediaclient": "entertainment",
  "com.spotify.music": "entertainment",
  "com.duolingo": "productivity",
  "org.koreader.android": "reading"
}
```

## UI Components

### MainActivity
- Toggle switch to activate/deactivate agent
- Permission request buttons
- Current usage statistics display
- Trigger threshold sliders
- Recent interventions history
- Agent sensitivity setting

### Notification Cards
- App icon + usage duration
- AI-generated message
- Action buttons ("Open Book", "Dismiss", "Snooze")
- Quick settings link

## Performance Considerations

### Memory Management
- Load model once on startup
- Use quantized weights
- Implement model unloading during long idle periods
- Cache frequent responses

### Battery Optimization
- Check usage every 5-10 minutes (not continuous)
- Use JobScheduler for periodic checks
- Wake lock only during inference
- Minimal background processing

### User Privacy
- All data stored locally
- No cloud syncing by default
- Option to export/clear data
- Granular permission controls

## File Structure

```
angail/
├── app/
│   ├── src/main/
│   │   ├── java/com/angail/
│   │   │   ├── MainActivity.kt
│   │   │   ├── AgentController.kt
│   │   │   ├── PermissionHandler.kt
│   │   │   ├── NotificationHelper.kt
│   │   │   └── AppLauncher.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── termux-agent/
│   ├── usage_monitor.py
│   ├── trigger_system.py
│   ├── model_inference.py
│   ├── notification_generator.py
│   ├── stats_analyzer.py
│   └── requirements.txt
├── config/
│   ├── thresholds.json
│   ├── model_config.json
│   └── app_categories.json
├── models/
│   └── [quantized model files]
└── README.md
```

## Development Workflow

### Setup
1. Install Android Studio
2. Install Termux:API on device/emulator
3. Clone and install app on device
4. Download and place quantized model in `models/`
5. Configure thresholds based on user needs

### Testing
1. Mock usage data for trigger testing
2. Test notification display and actions
3. Verify memory usage stays under 2GB
4. Test permission flows
5. Battery drain testing

### Deployment
1. Build signed APK
2. Install on target device
3. Grant permissions via app UI
4. Configure user preferences
5. Start agent

## Key Functions to Implement

### termux-agent/usage_monitor.py
```python
def get_current_app_usage() -> Dict:
    """Get currently running app and duration"""
    
def get_daily_screen_time() -> int:
    """Get total screen time today"""
    
def get_app_usage_by_category() -> Dict[str, int]:
    """Get usage grouped by app category"""
    
def detect_continuous_usage(app: str) -> int:
    """Get continuous duration for specific app"""
```

### termux-agent/trigger_system.py
```python
def check_thresholds(usage_data: Dict) -> bool:
    """Evaluate if any threshold is exceeded"""
    
def get_triggered_rule(usage_data: Dict) -> str:
    """Identify which rule was triggered"""
```

### termux-agent/model_inference.py
```python
def load_model() -> None:
    """Load quantized model into memory"""
    
def generate_advice(context: Dict) -> Dict:
    """Generate personalized advice message"""
```

## Edge Cases to Handle

1. **Multiple apps running**: Handle split-screen or floating windows
2. **Background audio**: Don't trigger for passive media consumption
3. **Emergency calls**: Never interrupt during calls
4. **Battery low**: Suspend non-critical monitoring
5. **User feedback loop**: Learn from dismissals vs. actions taken
6. **Model errors**: Fallback to pre-written messages
7. **Service restarts**: Maintain state across crashes

## User Customization

Users can configure:
- Active agent hours
- Trigger sensitivity thresholds
- Notification style (gentle vs direct)
- Preferred alternative activities
- App category preferences
- Weekly usage goals
- Exception list (apps to never interrupt)

## Future Enhancements

- Machine learning to personalize thresholds automatically
- Weekly usage reports and insights
- Integration with calendar for context-aware suggestions
- Community-shared positive intervention patterns
- Multi-user profile support
- Sync across devices (optional)
- Voice assistant integration
- Wear OS companion app

## Dependencies

### Android App
```kotlin
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("androidx.appcompat:appcompat:1.6.1")
```

### Termux Backend
```
onnxruntime==1.16.0
or
tensorflow-lite==2.15.0

termux-api
pydantic
```

## Safety Considerations

- Never auto-delete apps or data
- Always allow user to dismiss notifications
- Provide easy agent deactivation
- Respect "Do Not Disturb" mode
- Never interrupt emergency services
- Clear documentation of data collection
- Regular privacy reminders to user

## Testing Checklist

- [ ] Permission request flows complete successfully
- [ ] Agent starts and stops reliably
- [ ] Triggers fire at correct thresholds
- [ ] Notifications appear with correct messages
- [ ] App launching works from notifications
- [ ] Memory usage stays within limits
- [ ] Battery drain is minimal (< 5%/day)
- [ ] Model inference completes in < 3 seconds
- [ ] Agent respects user-defined active hours
- [ ] Data persists across service restarts
