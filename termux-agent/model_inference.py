import socket
import time
import json
from typing import Dict
from usage_monitor import UsageMonitor
from trigger_system import TriggerSystem
from notification_generator import NotificationGenerator
from stats_analyzer import StatsAnalyzer


class ModelInference:
    def __init__(self, model_path: str = '../models/'):
        self.model_path = model_path
        self.model_loaded = False
        self.notification_generator = NotificationGenerator()
        self.load_simple_model()
    
    def load_simple_model(self):
        self.model_loaded = True
    
    def generate_response(self, context: Dict) -> Dict:
        trigger_rule = context.get('trigger_rule', 'default')
        current_app = context.get('current_app', '')
        
        response = self.notification_generator.generate_message(
            trigger_rule=trigger_rule,
            context=context,
            app_package=current_app
        )
        
        response['trigger_rule'] = trigger_rule
        response['timestamp'] = time.time()
        
        return response


def send_to_android(message: str, host: str = '127.0.0.1', port: int = 9999) -> bool:
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(5)
        sock.connect((host, port))
        sock.sendall(message.encode())
        response = sock.recv(1024).decode()
        sock.close()
        return 'OK' in response
    except Exception as e:
        print(f"Error sending to Android: {e}")
        return False


def main():
    print("Starting Angail Agent...")
    
    usage_monitor = UsageMonitor()
    trigger_system = TriggerSystem()
    model_inference = ModelInference()
    stats_analyzer = StatsAnalyzer()
    notification_generator = NotificationGenerator()
    
    check_interval = 30
    running = True
    
    try:
        print("Agent is monitoring. Press Ctrl+C to stop.")
        
        while running:
            current_app = usage_monitor.get_current_app()
            app_usage_stats = usage_monitor.get_app_usage_stats()
            screen_time = usage_monitor.get_daily_screen_time()
            patterns = usage_monitor.detect_patterns()
            
            usage_data = {
                'current_app': current_app,
                'screen_time_minutes': screen_time,
                'social_media_minutes': 0,
                'category': notification_generator.get_app_category(current_app) if current_app else 'none'
            }
            
            for app_pkg, stats in app_usage_stats.items():
                if notification_generator.get_app_category(app_pkg) == 'social_media':
                    usage_data['social_media_minutes'] += stats.get('time_used', 0)
            
            if current_app:
                duration = usage_monitor.get_continuous_usage(current_app)
                usage_data['duration'] = duration
            else:
                usage_data['duration'] = 0
            
            if trigger_system.check_thresholds(usage_data, patterns, notification_generator.app_categories):
                print(f"Trigger detected! Current app: {current_app}")
                
                trigger_rule = trigger_system.get_triggered_rule(usage_data, patterns, current_app)
                
                context = {
                    'trigger_rule': trigger_rule,
                    'current_app': current_app,
                    'duration': usage_data.get('duration', 0),
                    'screen_time': screen_time,
                    'patterns': patterns
                }
                
                response = model_inference.generate_response(context)
                
                message = response['message']
                app_to_open = response.get('app_to_open')
                
                command = notification_generator.format_notification_command(
                    message=message,
                    app_package=app_to_open
                )
                
                success = send_to_android(command)
                
                if success:
                    print(f"Notification sent: {message}")
                    trigger_system.record_trigger()
                    
                    usage_data['intervention_triggered'] = True
                    stats_analyzer.add_usage_data(usage_data)
                    stats_analyzer.save_to_file()
                else:
                    print("Failed to send notification")
            
            time.sleep(check_interval)
            
    except KeyboardInterrupt:
        print("\nShutting down agent...")
        stats_analyzer.save_to_file()
        running = False


if __name__ == "__main__":
    main()
