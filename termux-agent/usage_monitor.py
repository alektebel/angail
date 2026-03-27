import json
import socket
import time
from typing import Dict, List, Optional
from datetime import datetime


SOCKET_HOST = "127.0.0.1"
SOCKET_PORT = 9999
SOCKET_TIMEOUT = 5


def _send_command(command: str) -> Optional[str]:
    """Send a command to the Android app socket and return the response."""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.settimeout(SOCKET_TIMEOUT)
            s.connect((SOCKET_HOST, SOCKET_PORT))
            s.sendall((command + "\n").encode())
            chunks = []
            while True:
                try:
                    chunk = s.recv(65536)
                    if not chunk:
                        break
                    chunks.append(chunk)
                except socket.timeout:
                    break
            return b"".join(chunks).decode().strip() if chunks else None
    except (ConnectionRefusedError, OSError):
        return None


class UsageMonitor:
    def __init__(self):
        self.current_app = None
        self.app_start_time = None
        self.usage_history = []

    def get_usage_stats(self) -> Dict[str, dict]:
        """
        Fetch usage stats from the Android app via socket.
        Returns a dict keyed by package name:
          { "com.example.app": { "name": "App Name", "totalMs": 123456, "totalMin": 2 } }
        """
        response = _send_command("GET_USAGE_STATS")
        if not response:
            return {}
        try:
            return json.loads(response)
        except json.JSONDecodeError:
            return {}

    def get_current_app(self) -> Optional[str]:
        """Return the package with the highest recent foreground time as a proxy."""
        stats = self.get_usage_stats()
        if not stats:
            return None
        return max(stats, key=lambda pkg: stats[pkg].get("totalMs", 0), default=None)

    def get_daily_screen_time(self) -> int:
        """Return total screen time in minutes from the last 24h stats."""
        stats = self.get_usage_stats()
        total_ms = sum(v.get("totalMs", 0) for v in stats.values())
        return total_ms // 60_000

    def get_app_usage_minutes(self, package_name: str) -> int:
        """Return usage in minutes for a specific app."""
        stats = self.get_usage_stats()
        entry = stats.get(package_name, {})
        return entry.get("totalMin", 0)

    def get_app_category_usage(self, app_categories: Dict[str, str]) -> Dict[str, int]:
        """Return total minutes per category based on the category map."""
        stats = self.get_usage_stats()
        category_usage: Dict[str, int] = {}
        for package_name, data in stats.items():
            category = app_categories.get(package_name, "other")
            minutes = data.get("totalMin", 0)
            category_usage[category] = category_usage.get(category, 0) + minutes
        return category_usage

    def detect_patterns(self) -> Dict:
        patterns = {
            "social_media_binge": False,
            "entertainment_late_night": False,
            "productivity_neglect": False,
            "high_screen_time": False,
        }

        stats = self.get_usage_stats()
        if not stats:
            return patterns

        social_media_packages = {
            "com.instagram.android", "com.twitter.android", "com.facebook.katana",
            "com.tiktok.video", "com.snapchat.android", "com.reddit.frontpage",
            "com.zhiliaoapp.musically", "com.pinterest", "com.linkedin.android",
            "com.tumblr", "com.discord",
        }

        social_media_min = sum(
            stats[pkg].get("totalMin", 0)
            for pkg in stats
            if pkg in social_media_packages
        )
        total_min = sum(v.get("totalMin", 0) for v in stats.values())

        patterns["social_media_binge"] = social_media_min > 30
        patterns["high_screen_time"] = total_min > 120

        hour = datetime.now().hour
        patterns["entertainment_late_night"] = hour >= 23 or hour < 6

        return patterns
