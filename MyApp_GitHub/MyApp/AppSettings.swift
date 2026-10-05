import Foundation

struct AppSettings {
    
    private static let apiKeyKey = "deepseek_api_key"
    private static let proactiveMinutesKey = "proactive_minutes"
    private static let replyDelayMaxKey = "reply_delay_max_seconds"
    private static let customSystemPromptKey = "custom_system_prompt"
    
    // MARK: - DeepSeek API Key
    
    static var apiKey: String {
        get {
            UserDefaults.standard.string(
                forKey: apiKeyKey
            ) ?? ""
        }
        set {
            UserDefaults.standard.set(
                newValue,
                forKey: apiKeyKey
            )
        }
    }
    
    // MARK: - Proactive Message Interval
    
    static var proactiveMinutes: Int {
        get {
            let value = UserDefaults.standard.integer(
                forKey: proactiveMinutesKey
            )
            return value > 0 ? value : 30
        }
        set {
            UserDefaults.standard.set(
                max(1, newValue),
                forKey: proactiveMinutesKey
            )
        }
    }
    
    // MARK: - AI Reply Delay
    
    static var replyDelayMaxSeconds: Int {
        get {
            guard UserDefaults.standard.object(forKey: replyDelayMaxKey) != nil else {
                return 3
            }
            let value = UserDefaults.standard.integer(
                forKey: replyDelayMaxKey
            )
            return (0...600).contains(value) ? value : 3
        }
        set {
            UserDefaults.standard.set(
                min(600, max(0, newValue)),
                forKey: replyDelayMaxKey
            )
        }
    }
    
    // MARK: - Custom System Prompt
    
    static var customSystemPrompt: String? {
        get {
            if let value = UserDefaults.standard.string(forKey: customSystemPromptKey),
               !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                return value
            }
            return nil
        }
        set {
            if let newValue {
                UserDefaults.standard.set(
                    newValue,
                    forKey: customSystemPromptKey
                )
            } else {
                UserDefaults.standard.removeObject(
                    forKey: customSystemPromptKey
                )
            }
        }
    }
}
