import subprocess
import time
import json
from typing import Dict, List, Optional
from datetime import datetime, timedelta


class UsageMonitor:
    def __init__(self):
        self.current_app = None
        self.app_start_time = None
        self.usage_history = []
    
    def get_current_app(self) -> Optional[str]:
        try:
            result = subprocess.run(
                ['termux-am', 'current'],
                capture_output=True,
                text=True,
                timeout=5
            )
            if result.returncode == 0:
                output = result.stdout.strip()
                if output:
                    return output
        except (subprocess.TimeoutExpired, FileNotFoundError):
            pass
        return None
    
    def get_app_usage_stats(self, package_name: Optional[str] = None) -> Dict:
        try:
            cmd = ['termux-usage-app']
            if package_name:
                cmd.append(package_name)
            
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=10
            )
            
            if result.returncode == 0:
                lines = result.stdout.strip().split('\n')
                stats = {}
                for line in lines[1:]:  # Skip header
                    parts = line.split(',')
                    if len(parts) >= 2:
                        stats[parts[0]] = {
                            'time_used': int(parts[1]) if parts[1].isdigit() else 0,
                            'last_used': parts[2] if len(parts) > 2 else None
                        }
                return stats
        except (subprocess.TimeoutExpired, FileNotFoundError):
            pass
        return {}
    
    def get_daily_screen_time(self) -> int:
        try:
            result = subprocess.run(
                ['termux-usage-summary'],
                capture_output=True,
                text=True,
                timeout=10
            )
            
            if result.returncode == 0:
                output = result.stdout.strip()
                if 'total time' in output.lower():
                    for line in output.split('\n'):
                        if 'total time' in line.lower():
                            parts = line.split(':')
                            if len(parts) > 1:
                                time_str = parts[1].strip()
                                if time_str.isdigit():
                                    return int(time_str)
        except (subprocess.TimeoutExpired, FileNotFoundError):
            pass
        return 0
    
    def get_continuous_usage(self, app_package: str) -> int:
        current_app = self.get_current_app()
        
        if current_app == app_package:
            if self.current_app == app_package and self.app_start_time:
                duration = int((datetime.now() - self.app_start_time).total_seconds() / 60)
                return duration
            else:
                self.current_app = app_package
                self.app_start_time = datetime.now()
                return 0
        else:
            if self.current_app == app_package and self.app_start_time:
                duration = int((datetime.now() - self.app_start_time).total_seconds() / 60)
                self.usage_history.append({
                    'app': app_package,
                    'duration': duration,
                    'start_time': self.app_start_time.isoformat(),
                    'end_time': datetime.now().isoformat()
                })
            
            self.current_app = current_app
            self.app_start_time = datetime.now() if current_app else None
            
            if current_app:
                return 0
        
        return 0
    
    def get_app_category_usage(self, app_categories: Dict[str, str]) -> Dict[str, int]:
        usage_stats = self.get_app_usage_stats()
        category_usage = {}
        
        for app_package, stats in usage_stats.items():
            category = app_categories.get(app_package, 'other')
            time_used = stats.get('time_used', 0)
            
            if category not in category_usage:
                category_usage[category] = 0
            category_usage[category] += time_used
        
        return category_usage
    
    def detect_patterns(self) -> Dict:
        patterns = {
            'social_media_binge': False,
            'entertainment_late_night': False,
            'productivity_neglect': False,
            'high_screen_time': False
        }
        
        current_app = self.get_current_app()
        now = datetime.now()
        hour = now.hour
        
        if current_app:
            duration = self.get_continuous_usage(current_app)
            patterns['social_media_binge'] = duration > 30
            patterns['high_screen_time'] = duration > 60
        
        if hour >= 23 or hour < 6:
            patterns['entertainment_late_night'] = True
        
        return patterns
