#!/usr/bin/env python3

"""
Simple test script for Angail components.
Run this to verify all components are working correctly.
"""

import sys
import os

# Add termux-agent to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'termux-agent'))

def test_imports():
    """Test that all modules can be imported"""
    print("Testing imports...")
    try:
        from usage_monitor import UsageMonitor
        from trigger_system import TriggerSystem
        from notification_generator import NotificationGenerator
        from stats_analyzer import StatsAnalyzer
        from model_inference import ModelInference
        print("✓ All imports successful")
        return True
    except Exception as e:
        print(f"✗ Import failed: {e}")
        return False

def test_usage_monitor():
    """Test UsageMonitor class"""
    print("\nTesting UsageMonitor...")
    try:
        from usage_monitor import UsageMonitor
        monitor = UsageMonitor()
        assert monitor is not None
        assert monitor.current_app is None
        print("✓ UsageMonitor initialized successfully")
        return True
    except Exception as e:
        print(f"✗ UsageMonitor test failed: {e}")
        return False

def test_trigger_system():
    """Test TriggerSystem class"""
    print("\nTesting TriggerSystem...")
    try:
        from trigger_system import TriggerSystem
        trigger_system = TriggerSystem()
        assert trigger_system is not None
        assert trigger_system.config is not None
        print("✓ TriggerSystem initialized successfully")
        return True
    except Exception as e:
        print(f"✗ TriggerSystem test failed: {e}")
        return False

def test_notification_generator():
    """Test NotificationGenerator class"""
    print("\nTesting NotificationGenerator...")
    try:
        from notification_generator import NotificationGenerator
        generator = NotificationGenerator()
        assert generator is not None
        assert generator.app_categories is not None
        assert generator.message_templates is not None
        
        # Test message generation
        context = {'duration': 30}
        response = generator.generate_message('social_media_binge', context)
        assert 'message' in response
        print(f"  Generated message: {response['message']}")
        print("✓ NotificationGenerator working correctly")
        return True
    except Exception as e:
        print(f"✗ NotificationGenerator test failed: {e}")
        return False

def test_stats_analyzer():
    """Test StatsAnalyzer class"""
    print("\nTesting StatsAnalyzer...")
    try:
        from stats_analyzer import StatsAnalyzer
        analyzer = StatsAnalyzer()
        assert analyzer is not None
        assert isinstance(analyzer.usage_history, list)
        print("✓ StatsAnalyzer initialized successfully")
        return True
    except Exception as e:
        print(f"✗ StatsAnalyzer test failed: {e}")
        return False

def test_model_inference():
    """Test ModelInference class"""
    print("\nTesting ModelInference...")
    try:
        from model_inference import ModelInference
        inference = ModelInference()
        assert inference is not None
        assert inference.model_loaded == True
        
        # Test response generation
        context = {
            'trigger_rule': 'social_media_binge',
            'current_app': 'com.instagram.android',
            'duration': 45
        }
        response = inference.generate_response(context)
        assert 'message' in response
        assert 'trigger_rule' in response
        print(f"  Generated response: {response['message']}")
        print("✓ ModelInference working correctly")
        return True
    except Exception as e:
        print(f"✗ ModelInference test failed: {e}")
        return False

def test_config_files():
    """Test that config files exist and are valid JSON"""
    print("\nTesting configuration files...")
    import json
    
    config_files = [
        'config/thresholds.json',
        'config/model_config.json',
        'config/app_categories.json'
    ]
    
    all_ok = True
    for config_file in config_files:
        if os.path.exists(config_file):
            try:
                with open(config_file, 'r') as f:
                    data = json.load(f)
                print(f"✓ {config_file} - valid JSON ({len(data)} items)")
            except Exception as e:
                print(f"✗ {config_file} - invalid JSON: {e}")
                all_ok = False
        else:
            print(f"✗ {config_file} - file not found")
            all_ok = False
    
    return all_ok

def main():
    """Run all tests"""
    print("=" * 60)
    print("Angail Component Tests")
    print("=" * 60)
    
    results = []
    
    results.append(test_imports())
    results.append(test_config_files())
    results.append(test_usage_monitor())
    results.append(test_trigger_system())
    results.append(test_notification_generator())
    results.append(test_stats_analyzer())
    results.append(test_model_inference())
    
    print("\n" + "=" * 60)
    print(f"Tests passed: {sum(results)}/{len(results)}")
    print("=" * 60)
    
    if all(results):
        print("\n✓ All tests passed! System is ready to use.")
        return 0
    else:
        print("\n✗ Some tests failed. Please check the errors above.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
