import Foundation

struct ChatStorage {
    static let didChangeNotification = Notification.Name("ChatStorageDidChange")

    private static func lastMessageKey(for chatName: String) -> String {
        "chat_last_message_" + chatName
    }

    private static func unreadKey(for chatName: String) -> String {
        "chat_unread_" + chatName
    }

    private static func messagesKey(for chatName: String) -> String {
        "chat_messages_" + chatName
    }

    static func lastMessage(for chatName: String) -> String? {
        UserDefaults.standard.string(forKey: lastMessageKey(for: chatName))
    }

    static func saveLastMessage(_ text: String, for chatName: String) {
        UserDefaults.standard.set(text, forKey: lastMessageKey(for: chatName))
        notifyChange()
    }

    static func unreadCount(for chatName: String) -> Int {
        UserDefaults.standard.integer(forKey: unreadKey(for: chatName))
    }

    static func hasUnreadRecord(for chatName: String) -> Bool {
        UserDefaults.standard.object(forKey: unreadKey(for: chatName)) != nil
    }

    static func addUnread(for chatName: String) {
        let count = unreadCount(for: chatName)
        UserDefaults.standard.set(count + 1, forKey: unreadKey(for: chatName))
        notifyChange()
    }

    static func markAsRead(for chatName: String) {
        UserDefaults.standard.set(0, forKey: unreadKey(for: chatName))
        notifyChange()
    }

    static func loadMessages(for chatName: String) -> [Message] {
        guard let data = UserDefaults.standard.data(forKey: messagesKey(for: chatName)),
              let messages = try? JSONDecoder().decode([Message].self, from: data) else {
            return []
        }
        return messages
    }

    static func saveMessages(_ messages: [Message], for chatName: String) {
        guard let data = try? JSONEncoder().encode(messages) else { return }
        UserDefaults.standard.set(data, forKey: messagesKey(for: chatName))
        notifyChange()
    }

    static func saveIncomingMessage(_ message: Message, for chatName: String) {
        var messages = loadMessages(for: chatName)
        messages.append(message)
        saveMessages(messages, for: chatName)
        saveLastMessage(message.text, for: chatName)
        addUnread(for: chatName)
    }

    private static func notifyChange() {
        DispatchQueue.main.async {
            NotificationCenter.default.post(name: didChangeNotification, object: nil)
        }
    }
}
