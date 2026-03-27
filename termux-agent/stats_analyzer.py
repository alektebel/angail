import json
from typing import Dict, List
from datetime import datetime, timedelta


class StatsAnalyzer:
    def __init__(self):
        self.usage_history = []
        self.patterns_detected = []
    
    def add_usage_data(self, usage_data: Dict):
        self.usage_history.append({
            **usage_data,
            'timestamp': datetime.now().isoformat()
        })
        
        if len(self.usage_history) > 1000:
            self.usage_history = self.usage_history[-1000:]
    
    def analyze_patterns(self) -> Dict:
        if len(self.usage_history) < 10:
            return {}
        
        patterns = {
            'peak_hours': self.find_peak_hours(),
            'most_used_categories': self.get_category_rankings(),
            'binge_tendency': self.calculate_binge_tendency(),
            'productivity_score': self.calculate_productivity_score()
        }
        
        return patterns
    
    def find_peak_hours(self) -> List[int]:
        if not self.usage_history:
            return []
        
        hour_counts = {}
        for entry in self.usage_history:
            try:
                timestamp = datetime.fromisoformat(entry.get('timestamp', ''))
                hour = timestamp.hour
                hour_counts[hour] = hour_counts.get(hour, 0) + entry.get('duration', 0)
            except (ValueError, TypeError):
                continue
        
        sorted_hours = sorted(hour_counts.items(), key=lambda x: x[1], reverse=True)
        return [hour for hour, _ in sorted_hours[:3]]
    
    def get_category_rankings(self) -> List[Dict]:
        category_usage = {}
        
        for entry in self.usage_history:
            category = entry.get('category', 'other')
            duration = entry.get('duration', 0)
            category_usage[category] = category_usage.get(category, 0) + duration
        
        sorted_categories = sorted(
            category_usage.items(),
            key=lambda x: x[1],
            reverse=True
        )
        
        return [{'category': cat, 'duration': dur} for cat, dur in sorted_categories]
    
    def calculate_binge_tendency(self) -> float:
        if not self.usage_history:
            return 0.0
        
        long_sessions = 0
        total_sessions = 0
        
        for entry in self.usage_history:
            duration = entry.get('duration', 0)
            total_sessions += 1
            if duration > 30:
                long_sessions += 1
        
        if total_sessions == 0:
            return 0.0
        
        return long_sessions / total_sessions
    
    def calculate_productivity_score(self) -> float:
        if not self.usage_history:
            return 0.5
        
        productive_categories = ['productivity', 'reading', 'education']
        unproductive_categories = ['social_media', 'entertainment']
        
        productive_time = 0
        unproductive_time = 0
        
        for entry in self.usage_history:
            category = entry.get('category', 'other')
            duration = entry.get('duration', 0)
            
            if category in productive_categories:
                productive_time += duration
            elif category in unproductive_categories:
                unproductive_time += duration
        
        total_time = productive_time + unproductive_time
        if total_time == 0:
            return 0.5
        
        return productive_time / total_time
    
    def get_daily_summary(self) -> Dict:
        today = datetime.now().date()
        today_entries = [
            entry for entry in self.usage_history
            if datetime.fromisoformat(entry.get('timestamp', '')).date() == today
        ]
        
        if not today_entries:
            return {
                'total_minutes': 0,
                'top_category': 'none',
                'intervention_count': 0
            }
        
        total_minutes = sum(entry.get('duration', 0) for entry in today_entries)
        
        category_rankings = self.get_category_rankings()
        top_category = category_rankings[0]['category'] if category_rankings else 'none'
        
        intervention_count = sum(
            1 for entry in today_entries if entry.get('intervention_triggered', False)
        )
        
        return {
            'total_minutes': total_minutes,
            'top_category': top_category,
            'intervention_count': intervention_count
        }
    
    def save_to_file(self, filepath: str = '../data/usage_stats.json'):
        try:
            data = {
                'usage_history': self.usage_history[-500:],
                'patterns_detected': self.patterns_detected[-100:],
                'export_date': datetime.now().isoformat()
            }
            
            with open(filepath, 'w') as f:
                json.dump(data, f, indent=2)
        except Exception as e:
            print(f"Error saving stats: {e}")
    
    def load_from_file(self, filepath: str = '../data/usage_stats.json'):
        try:
            with open(filepath, 'r') as f:
                data = json.load(f)
                self.usage_history = data.get('usage_history', [])
                self.patterns_detected = data.get('patterns_detected', [])
        except (FileNotFoundError, json.JSONDecodeError):
            pass
