import json
from typing import Dict, Optional
from datetime import datetime


class TriggerSystem:
    def __init__(self, config_path: str = '../config/thresholds.json'):
        self.config = self.load_config(config_path)
        self.last_trigger_time = None
        self.cooldown_minutes = 10
    
    def load_config(self, config_path: str) -> Dict:
        try:
            with open(config_path, 'r') as f:
                return json.load(f)
        except (FileNotFoundError, json.JSONDecodeError):
            return {
                'social_media_continuous_minutes': 30,
                'total_screen_time_hourly_minutes': 120,
                'entertainment_after_hours': '23:00',
                'scrolling_detection_threshold': 15,
                'productivity_gap_hours': 4,
                'daily_limit_minutes': 300
            }
    
    def check_thresholds(
        self,
        usage_data: Dict,
        patterns: Dict,
        app_categories: Dict[str, str]
    ) -> bool:
        if self.in_cooldown():
            return False
        
        if patterns.get('social_media_binge', False):
            return True
        
        if usage_data.get('screen_time_minutes', 0) > self.config.get('total_screen_time_hourly_minutes', 120):
            return True
        
        if usage_data.get('social_media_minutes', 0) > self.config.get('social_media_continuous_minutes', 30):
            return True
        
        now = datetime.now()
        after_hours = self.config.get('entertainment_after_hours', '23:00')
        if now.hour >= 23 or now.hour < 6:
            if patterns.get('entertainment_late_night', False):
                return True
        
        return False
    
    def get_triggered_rule(
        self,
        usage_data: Dict,
        patterns: Dict,
        current_app: Optional[str] = None
    ) -> str:
        if patterns.get('social_media_binge', False):
            return 'social_media_binge'
        
        if usage_data.get('screen_time_minutes', 0) > self.config.get('total_screen_time_hourly_minutes', 120):
            return 'high_screen_time'
        
        if usage_data.get('social_media_minutes', 0) > self.config.get('social_media_continuous_minutes', 30):
            return 'social_media_continuous'
        
        if patterns.get('entertainment_late_night', False):
            return 'late_night_entertainment'
        
        return 'unknown'
    
    def in_cooldown(self) -> bool:
        if self.last_trigger_time is None:
            return False
        
        elapsed = (datetime.now() - self.last_trigger_time).total_seconds() / 60
        return elapsed < self.cooldown_minutes
    
    def record_trigger(self):
        self.last_trigger_time = datetime.now()
    
    def update_threshold(self, key: str, value: int):
        if key in self.config:
            self.config[key] = value
            try:
                with open('../config/thresholds.json', 'w') as f:
                    json.dump(self.config, f, indent=2)
            except Exception:
                pass
