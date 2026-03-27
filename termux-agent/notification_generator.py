import json
import random
from typing import Dict, List, Optional


class NotificationGenerator:
    def __init__(self, config_path: str = '../config/app_categories.json'):
        self.app_categories = self.load_app_categories(config_path)
        self.message_templates = self.load_message_templates()
    
    def load_app_categories(self, config_path: str) -> Dict[str, str]:
        try:
            with open(config_path, 'r') as f:
                return json.load(f)
        except (FileNotFoundError, json.JSONDecodeError):
            return {
                'com.instagram.android': 'social_media',
                'com.twitter.android': 'social_media',
                'com.reddit.frontpage': 'social_media',
                'com.facebook.katana': 'social_media',
                'com.tiktok.android': 'social_media',
                'com.netflix.mediaclient': 'entertainment',
                'com.spotify.music': 'entertainment',
                'com.duolingo': 'productivity',
                'org.koreader.android': 'reading',
                'com.google.android.youtube': 'entertainment'
            }
    
    def load_message_templates(self) -> Dict[str, List[str]]:
        return {
            'social_media_binge': [
                "You've been scrolling for a while. Maybe take a break?",
                "Stop scrolling on social media",
                "Time to put the phone down and breathe",
                "Your thumbs need a rest from all that scrolling"
            ],
            'high_screen_time': [
                "You've used the phone for 5 hours today. Touch some grass",
                "That's a lot of screen time. Go outside for 10 mins",
                "Time to disconnect and enjoy the real world",
                "5 hours online? Time for a digital detox break"
            ],
            'social_media_continuous': [
                "You've been on this app too long. Try something else",
                "Why not read a book instead?",
                "Let's switch up your screen time",
                "Time to change your digital scenery"
            ],
            'late_night_entertainment': [
                "It's getting late. Time to sleep, not scroll",
                "Your future self will thank you for sleeping now",
                "Better sleep starts with less screen time",
                "Put down the phone and get some rest"
            ],
            'reading_reminder': [
                "Great time to read! I'll open your book app",
                "Now is a good time to read. Opening the book app...",
                "Ready for a good story? Let's open your reading app",
                "Time for some literary adventures!"
            ],
            'default': [
                "How's your digital balance today?",
                "A gentle reminder to stay mindful of your time",
                "Be present in the moment",
                "Your digital well-being matters"
            ]
        }
    
    def get_app_category(self, app_package: str) -> str:
        return self.app_categories.get(app_package, 'other')
    
    def generate_message(
        self,
        trigger_rule: str,
        context: Dict,
        app_package: Optional[str] = None
    ) -> Dict:
        templates = self.message_templates.get(trigger_rule, self.message_templates['default'])
        message = random.choice(templates)
        
        result = {
            'message': message,
            'suggest_action': False,
            'app_to_open': None
        }
        
        if trigger_rule == 'social_media_binge' and random.random() < 0.3:
            reading_apps = [pkg for pkg, cat in self.app_categories.items() if cat == 'reading']
            if reading_apps:
                result['suggest_action'] = True
                result['app_to_open'] = random.choice(reading_apps)
                result['message'] = random.choice(self.message_templates['reading_reminder'])
        
        duration = context.get('duration', 0)
        if duration > 0:
            if '{duration}' in result['message']:
                result['message'] = result['message'].format(duration=duration)
        
        return result
    
    def format_notification_command(self, message: str, app_package: Optional[str] = None) -> str:
        if app_package:
            return f"OPEN_APP:{app_package}|{message}"
        return f"NOTIFICATION:{message}"
