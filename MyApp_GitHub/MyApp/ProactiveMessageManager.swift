import Foundation
import UserNotifications

@MainActor
final class ProactiveMessageManager: NSObject, UNUserNotificationCenterDelegate {
    static let shared = ProactiveMessageManager()

    private let notificationCenter = UNUserNotificationCenter.current()
    private var timer: Timer?
    private var currentChatName = "luo"
    private var isGenerating = false

    private override init() {
        super.init()
        notificationCenter.delegate = self
    }

    // MARK: - Start

    func start(chatName: String) {
        currentChatName = chatName
        requestPermission()
        resetTimer(chatName: chatName)
    }

    // MARK: - Reset Timer

    func resetTimer(chatName: String) {
    currentChatName = chatName
    timer?.invalidate()
    timer = nil

    let minutes = AppSettings.proactiveMinutes
    let delay = TimeInterval(minutes * 60)

    print("主动消息重新计时：\(minutes)分钟后检查")

    timer = Timer.scheduledTimer(
        withTimeInterval: delay,
        repeats: false
    ) { [weak self] _ in
        Task { @MainActor in
            self?.sendProactiveMessage(chatName: chatName)
        }
    }
}
    // MARK: - Notification Permission

    private func requestPermission() {
        notificationCenter.requestAuthorization(
            options: [.alert, .sound, .badge]
        ) { @Sendable granted, error in

            if let error {
                print("通知权限错误：\(error.localizedDescription)")
            } else {
                print("通知权限：\(granted)")
            }
        }
    }

    // MARK: - Generate Proactive Message

    private func sendProactiveMessage(chatName: String) {
        guard !isGenerating else {
            print("主动消息正在生成，跳过本次")
            return
        }

        isGenerating = true

        let messages = ChatStorage.loadMessages(
            for: chatName
        )

        print("===== 开始主动消息 =====")
        print("聊天：\(chatName)")

        AIService().sendProactiveMessage(messages) { [weak self] reply in

            Task { @MainActor in
                guard let self else {
                    return
                }

                self.isGenerating = false

                let message = Message(
                    text: reply,
                    isMe: false
                )

                ChatStorage.saveIncomingMessage(
                    message,
                    for: chatName
                )

                print("DeepSeek 主动消息：\(reply)")
                print("主动消息已经保存到聊天记录")

                self.scheduleNotification(
                    chatName: chatName,
                    message: reply
                )

                // 开始下一轮计时
                self.resetTimer(
                    chatName: chatName
                )
            }
        }
    }

    // MARK: - Schedule Notification

    private func scheduleNotification(
        chatName: String,
        message: String
    ) {
        notificationCenter.getNotificationSettings { @Sendable [weak self] settings in

            let status = settings.authorizationStatus

            Task { @MainActor in
                guard let self else {
                    return
                }

                print(
                    "通知状态：\(status.rawValue)"
                )

                guard status == .authorized ||
                        status == .provisional else {

                    print("没有通知权限")
                    return
                }

                let content = UNMutableNotificationContent()

                content.title = chatName
                content.body = message
                content.sound = .default
                content.badge = 1

                let trigger = UNTimeIntervalNotificationTrigger(
                    timeInterval: 2,
                    repeats: false
                )

                let identifier = "proactive_" + chatName

                let request = UNNotificationRequest(
                    identifier: identifier,
                    content: content,
                    trigger: trigger
                )

                self.notificationCenter
                    .removePendingNotificationRequests(
                        withIdentifiers: [identifier]
                    )

                self.notificationCenter.add(request) { @Sendable error in

                    if let error {
                        print(
                            "通知安排失败：\(error.localizedDescription)"
                        )
                    } else {
                        print(
                            "主动消息通知已安排，2秒后发送"
                        )
                    }
                }
            }
        }
    }

    // MARK: - Foreground Notification
nonisolated func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    willPresent notification: UNNotification,
    withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
) {
    completionHandler([
        .banner,
        .sound,
        .badge
    ])
}
 
}
