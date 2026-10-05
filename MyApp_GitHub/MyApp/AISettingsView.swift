import SwiftUI

struct AISettingsView: View {
    
    @State private var apiKey = ""
    @State private var minutesText = "30"
    @State private var delayText = "3"
    @State private var promptText = ""
    @State private var showSaved = false
    
    var body: some View {
        Form {
            Section("DeepSeek") {
                SecureField(
                    "请输入 API Key",
                    text: $apiKey
                )
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                
                Text("API Key 只保存在本机，不写入 GitHub 源码。")
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
            }
            
            Section("主动消息") {
                HStack {
                    Text("自动回复间隔")
                    
                    Spacer()
                    
                    TextField(
                        "分钟",
                        text: $minutesText
                    )
                    .keyboardType(.numberPad)
                    .multilineTextAlignment(.trailing)
                    .frame(width: 80)
                    
                    Text("分钟")
                        .foregroundStyle(.secondary)
                }
                
                Text("例如填写 30，就是每 30 分钟检查一次主动消息。最少 1 分钟。")
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
            }
            
            Section("回复延迟") {
                HStack {
                    Text("最大延迟")
                    
                    Spacer()
                    
                    TextField(
                        "秒",
                        text: $delayText
                    )
                    .keyboardType(.numberPad)
                    .multilineTextAlignment(.trailing)
                    .frame(width: 80)
                    
                    Text("秒")
                        .foregroundStyle(.secondary)
                }
                
                Text("AI 回复前会在 0 到该秒数之间随机等待。0 表示立即回复，最多 600 秒。")
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
            }
            
            Section("提示词") {
                TextEditor(text: $promptText)
                    .font(.system(size: 14))
                    .frame(height: 260)
                    .autocorrectionDisabled()
                
                Text("自定义 AI 的系统提示词，保存后立即生效。改回默认内容再保存，即可恢复内置提示词。")
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
            }
            
            Section {
                Button {
                    saveSettings()
                } label: {
                    HStack {
                        Spacer()
                        Text(showSaved ? "已保存" : "保存设置")
                            .fontWeight(.medium)
                        Spacer()
                    }
                }
                .foregroundStyle(.green)
            }
        }
        .navigationTitle("AI 设置")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            apiKey = AppSettings.apiKey
            minutesText = String(AppSettings.proactiveMinutes)
            delayText = String(AppSettings.replyDelayMaxSeconds)
            promptText = AppSettings.customSystemPrompt
                ?? AIService.defaultSystemPromptText
        }
    }
    
    private func saveSettings() {
        let cleanKey = apiKey.trimmingCharacters(
            in: .whitespacesAndNewlines
        )
        
        let minutes = Int(minutesText) ?? 30
        
        let delay = Int(delayText) ?? AppSettings.replyDelayMaxSeconds
        
        AppSettings.apiKey = cleanKey
        AppSettings.proactiveMinutes = max(1, minutes)
        AppSettings.replyDelayMaxSeconds = min(600, max(0, delay))
        minutesText = String(AppSettings.proactiveMinutes)
        delayText = String(AppSettings.replyDelayMaxSeconds)
        
        let trimmedPrompt = promptText.trimmingCharacters(
            in: .whitespacesAndNewlines
        )
        let defaultPrompt = AIService.defaultSystemPromptText.trimmingCharacters(
            in: .whitespacesAndNewlines
        )
        
        if trimmedPrompt.isEmpty || trimmedPrompt == defaultPrompt {
            AppSettings.customSystemPrompt = nil
        } else {
            AppSettings.customSystemPrompt = promptText
        }
        promptText = AppSettings.customSystemPrompt
            ?? AIService.defaultSystemPromptText
        
        // 保存后立即按照新的时间重新计时
        ProactiveMessageManager.shared.resetTimer(
            chatName: "luo"
        )
        
        showSaved = true
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            showSaved = false
        }
    }
}

#Preview {
    NavigationStack {
        AISettingsView()
    }
}
