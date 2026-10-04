import Foundation

struct AppSettings {
    
    private static let apiKeyKey = "deepseek_api_key"
    private static let proactiveMinutesKey = "proactive_minutes"
    
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
}
