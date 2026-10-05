import SwiftUI
import PhotosUI
import UIKit

// MARK: - Chat Model

struct ChatItem: Identifiable {
    let id = UUID()
    let avatar: String
    let name: String
    let lastMessage: String
    let time: String
    let unread: Int
}

// MARK: - Main View

struct ContentView: View {
    
    @State private var chatList: [ChatItem]
    @State private var avatarPickerItem: PhotosPickerItem?
    
    private let defaultChatList: [ChatItem] = [
        ChatItem(
            avatar: "administerphoto",
            name: "luo",
            lastMessage: "[图片]",
            time: "10:20",
            unread: 0
        )
    ]
    
    init() {
        _chatList = State(
            initialValue: []
        )
    }
    
    var body: some View {
        
        TabView {
            
            // MARK: 微信
            
            NavigationStack {
                
                VStack(spacing: 0) {
                    
                    // 搜索框
                    SearchBar()
                    
                    List(chatList) { item in
                        
                        NavigationLink {
                            ChatView(chat: item)
                        } label: {
                            ChatCell(item: item)
                        }
                    }
                    .listStyle(.plain)
                }
                .background(
                    Color(.systemGroupedBackground)
                )
                .navigationTitle("微信")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    
                    ToolbarItem(
                        placement: .topBarTrailing
                    ) {
                        
                        Button {
                            // 暂时不处理
                        } label: {
                            
                            Image(
                                systemName: "plus"
                            )
                            .font(
                                .system(
                                    size: 18,
                                    weight: .medium
                                )
                            )
                        }
                    }
                }
                .onAppear {
                    refreshChatList()
                }
                .onReceive(
                    NotificationCenter.default.publisher(
                        for: ChatStorage.didChangeNotification
                    )
                ) { _ in
                    
                    refreshChatList()
                }
            }
            .tabItem {
                Image(systemName: "message.fill")
                Text("微信")
            }
            
            // MARK: 通讯录
            
            NavigationStack {
                
                Text("通讯录")
                    .foregroundStyle(.secondary)
                    .navigationTitle("通讯录")
            }
            .tabItem {
                Image(systemName: "person.2.fill")
                Text("通讯录")
            }
            
            // MARK: 发现
            
            NavigationStack {
                
                Text("发现")
                    .foregroundStyle(.secondary)
                    .navigationTitle("发现")
            }
            .tabItem {
                Image(systemName: "safari.fill")
                Text("发现")
            }
            
            // MARK: 我
            
            NavigationStack {
                
                List {
                    Section {
                        NavigationLink {
                            AISettingsView()
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: "brain.head.profile")
                                    .font(.system(size: 20))
                                    .foregroundStyle(.green)
                                    .frame(width: 28)
                                
                                VStack(
                                    alignment: .leading,
                                    spacing: 4
                                ) {
                                    Text("AI 设置")
                                        .foregroundStyle(.primary)
                                    
                                    Text("API Key、自动回复时间")
                                        .font(.system(size: 13))
                                        .foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                    
                    Section("聊天头像") {
                        PhotosPicker(
                            selection: $avatarPickerItem,
                            matching: .images
                        ) {
                            HStack(spacing: 12) {
                                Image(systemName: "photo.on.rectangle")
                                    .font(.system(size: 20))
                                    .foregroundStyle(.green)
                                    .frame(width: 28)
                                
                                VStack(
                                    alignment: .leading,
                                    spacing: 4
                                ) {
                                    Text("更换聊天头像")
                                        .foregroundStyle(.primary)
                                    
                                    Text("给 luo 上传一张照片")
                                        .font(.system(size: 13))
                                        .foregroundStyle(.secondary)
                                }
                            }
                        }
                        
                        Button {
                            ChatStorage.removeAvatar(for: "administerphoto")
                        } label: {
                            Text("恢复默认头像")
                                .foregroundStyle(.red)
                        }
                    }
                }
                .listStyle(.insetGrouped)
                .navigationTitle("我")
                .navigationBarTitleDisplayMode(.inline)
                .onChange(of: avatarPickerItem) { _, newItem in
                    guard let newItem else { return }
                    
                    Task {
                        if let data = try? await newItem.loadTransferable(type: Data.self),
                           let jpeg = AvatarImageStore.resizedJPEG(from: data) {
                            ChatStorage.saveAvatar(jpeg, for: "administerphoto")
                        }
                    }
                }
            }
            .tabItem {
                Image(systemName: "person.fill")
                Text("我")
            }
        }
        .tint(.green)
    }
    
    // MARK: - Refresh Chat List
    
    private func refreshChatList() {
        
        chatList = defaultChatList.map { item in
            
            let savedLastMessage =
            ChatStorage.lastMessage(
                for: item.name
            )
            
            let hasUnreadRecord =
            ChatStorage.hasUnreadRecord(
                for: item.name
            )
            
            let savedUnread =
            ChatStorage.unreadCount(
                for: item.name
            )
            
            return ChatItem(
                avatar: item.avatar,
                name: item.name,
                lastMessage:
                    savedLastMessage
                ?? item.lastMessage,
                time: item.time,
                unread:
                    hasUnreadRecord
                ? savedUnread
                : item.unread
            )
        }
    }
}

// MARK: - Search Bar

struct SearchBar: View {
    
    var body: some View {
        
        HStack(spacing: 8) {
            
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            
            Text("搜索")
                .font(.system(size: 16))
                .foregroundStyle(.secondary)
            
            Spacer()
        }
        //.padding(.horizontal, 12)
        .frame(height: 34)
        .background(
            Color(.systemGray5)
        )
        .clipShape(
            RoundedRectangle(
                cornerRadius: 6
            )
        )
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .background(
            Color(.systemGroupedBackground)
        )
    }
}

// MARK: - Chat Cell

struct ChatCell: View {
    
    let item: ChatItem
    
    var body: some View {
        
        HStack(spacing: 20) {
            
            // 头像
            ChatAvatar(
                systemName: item.avatar
            )
            
            // 中间文字
            VStack(
                alignment: .leading,
                spacing: 6
            ) {
                
                Text(item.name)
                    .font(
                        .system(
                            size: 17,
                            weight: .regular
                        )
                    )
                    .foregroundStyle(.primary)
                
                Text(item.lastMessage)
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            
            Spacer()
            
            // 右侧时间和未读
            VStack(
                alignment: .trailing,
                spacing: 8
            ) {
                
                Text(item.time)
                    .font(.system(size: 11))
                    .foregroundStyle(.secondary)
                
                if item.unread > 0 {
                    
                    UnreadBadge(
                        count: item.unread
                    )
                }
            }
        }
        //.padding(.horizontal, 14)
        //.padding(.vertical, 8)
        .frame(height: 72)
    }
}

// MARK: - Avatar

struct ChatAvatar: View {
    
    let systemName: String
    
    var body: some View {
        
        if let data = ChatStorage.avatarData(for: systemName),
           let image = UIImage(data: data) {
            
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
                .frame(
                    width: 60,
                    height: 60
                )
                .clipShape(
                    RoundedRectangle(
                        cornerRadius: 10
                    )
                )
            
        } else if systemName == "administerphoto" {
            
            Image(systemName)
                .resizable()
                .scaledToFill()
                .frame(
                    width: 60,
                    height: 60
                )
                .clipShape(
                    RoundedRectangle(
                        cornerRadius: 10
                    )
                )
            
        } else {
            
            Image(systemName: systemName)
                .font(.system(size: 26))
                .foregroundStyle(.gray)
                .frame(
                    width: 60,
                    height: 60
                )
                .background(
                    Color(.systemGray5)
                )
                .clipShape(
                    RoundedRectangle(
                        cornerRadius: 10
                    )
                )
        }
    }
}
// MARK: - Unread Badge

struct UnreadBadge: View {
    
    let count: Int
    
    var body: some View {
        
        Text(
            count > 99
            ? "99+"
            : "\(count)"
        )
        .font(
            .system(
                size: 11,
                weight: .medium
            )
        )
        .foregroundStyle(.white)
        .frame(
            minWidth: 18,
            minHeight: 18
        )
        .padding(.horizontal, 3)
        .background(Color.red)
        .clipShape(Capsule())
    }
}

// MARK: - Avatar Image Store

enum AvatarImageStore {
    
    static func resizedJPEG(
        from data: Data,
        maxDimension: CGFloat = 512,
        quality: CGFloat = 0.85
    ) -> Data? {
        
        guard let image = UIImage(data: data) else {
            return nil
        }
        
        let size: CGSize
        
        if image.size.width > maxDimension || image.size.height > maxDimension {
            let ratio = min(
                maxDimension / image.size.width,
                maxDimension / image.size.height
            )
            size = CGSize(
                width: image.size.width * ratio,
                height: image.size.height * ratio
            )
        } else {
            size = image.size
        }
        
        let renderer = UIGraphicsImageRenderer(size: size)
        let resized = renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: size))
        }
        
        return resized.jpegData(compressionQuality: quality)
    }
}

// MARK: - Preview

#Preview {
    ContentView()
}
