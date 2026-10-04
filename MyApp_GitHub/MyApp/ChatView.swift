import SwiftUI

struct Message: Identifiable, Codable {
    let id: UUID
    let text: String
    let isMe: Bool

    init(id: UUID = UUID(), text: String, isMe: Bool) {
        self.id = id
        self.text = text
        self.isMe = isMe
    }
}

struct ChatView: View {
    let chat: ChatItem
    private let aiService = AIService()
    @State private var messageText = ""
    @State private var messages: [Message]
    @State private var isChatActive = false

    init(chat: ChatItem) {
        self.chat = chat
        let saved = ChatStorage.loadMessages(for: chat.name)
        if saved.isEmpty {
            _messages = State(initialValue: [
                Message(text: "你h", isMe: false),
                Message(text: "你好！", isMe: true)
            ])
        } else {
            _messages = State(initialValue: saved)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(spacing: 12) {
                        ForEach(messages) { message in
                            HStack(alignment: .top, spacing: 10) {
                                if !message.isMe {
                                    ChatAvatar(systemName: chat.avatar)
                                        .frame(width: 50, height: 50)
                                    Text(message.text)
                                        .frame(alignment: .center)
                                        .font(.system(size: 16))
                                        .foregroundStyle(.primary)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 10)
                                        .background(Color.black.tertiary)
                                        .clipShape(RoundedRectangle(cornerRadius: 5))
                                    Spacer()
                                } else {
                                    Spacer()
                                    Text(message.text)
                                        .font(.system(size: 16))
                                        .foregroundStyle(.primary)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 9)
                                        .background(Color.green.opacity(0.2))
                                        .clipShape(RoundedRectangle(cornerRadius: 5))
                                }
                            }
                            .id(message.id)
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.top, 15)
                }
                .background(Color(.systemGroupedBackground))
                .onChange(of: messages.count) {
                    if let last = messages.last {
                        withAnimation { proxy.scrollTo(last.id, anchor: .bottom) }
                    }
                }
            }

            HStack(spacing: 10) {
                Image(systemName: "mic")
                    .font(.system(size: 22))
                    .foregroundStyle(.primary)

                TextField("输入消息", text: $messageText)
                    .padding(.horizontal, 10)
                    .frame(height: 38)
                    .background(Color.black)
                    .clipShape(RoundedRectangle(cornerRadius: 5))
                    .onSubmit { sendMessage() }

                Image(systemName: "face.smiling")
                    .font(.system(size: 22))
                    .foregroundStyle(.primary)

                if !messageText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Button { sendMessage() } label: {
                        Text("发送")
                            .font(.system(size: 15))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 10)
                            .frame(height: 32)
                            .background(Color.green)
                            .clipShape(RoundedRectangle(cornerRadius: 5))
                    }
                } else {
                    Image(systemName: "plus.circle")
                        .font(.system(size: 22))
                        .foregroundStyle(.primary)
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(Color(.systemGray6))
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle(chat.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
        .onAppear {
            isChatActive = true
            ChatStorage.markAsRead(for: chat.name)
            reloadMessages()
            ProactiveMessageManager.shared.resetTimer(chatName: chat.name)
        }
        .onDisappear {
            isChatActive = false
        }
        .onReceive(NotificationCenter.default.publisher(for: ChatStorage.didChangeNotification)) { _ in
            reloadMessages()
        }
    }

    private func sendMessage() {
        let text = messageText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }

        messages.append(Message(text: text, isMe: true))
        ChatStorage.saveMessages(messages, for: chat.name)
        ChatStorage.saveLastMessage(text, for: chat.name)
        messageText = ""

        // 用户主动说话后，重新开始主动消息计时
        ProactiveMessageManager.shared.resetTimer(chatName: chat.name)

        aiService.sendMessage(messages) { reply in
            messages.append(Message(text: reply, isMe: false))
            ChatStorage.saveMessages(messages, for: chat.name)
            ChatStorage.saveLastMessage(reply, for: chat.name)
            if !isChatActive { ChatStorage.addUnread(for: chat.name) }

            // AI 回复完成后，再重新开始计时
            ProactiveMessageManager.shared.resetTimer(chatName: chat.name)
        }
    }

    private func reloadMessages() {
        let saved = ChatStorage.loadMessages(for: chat.name)
        guard !saved.isEmpty else { return }
        messages = saved
        if isChatActive { ChatStorage.markAsRead(for: chat.name) }
    }
}

#Preview {
    ChatView(chat: ChatItem(avatar: "person", name: "妈妈", lastMessage: "晚上回家吃饭吗", time: "09:15", unread: 2))
}
