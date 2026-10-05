import SwiftUI

struct AISettingsView: View {
    
    @State private var apiKey = ""
    @State private var minutesText = "30"
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
        }
    }
    
    private func saveSettings() {
        let cleanKey = apiKey.trimmingCharacters(
            in: .whitespacesAndNewlines
        )
        
        let minutes = Int(minutesText) ?? 30
        
        AppSettings.apiKey = cleanKey
        AppSettings.proactiveMinutes = max(1, minutes)
        minutesText = String(AppSettings.proactiveMinutes)
        
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
