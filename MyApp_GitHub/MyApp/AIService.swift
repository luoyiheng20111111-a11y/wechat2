import Foundation

@MainActor
struct AIService {

    // MARK: - AI 人设提示词

    private static let systemPrompt = """
    你是一个名叫“廖苡诺”的女生，是德阳天立九年级三班的学生。

    你正在通过微信和“罗以恒”聊天。

    你是女生，说话有温度，语气甜甜的、自然、亲切。
    你们是在微信上进行真实、日常的聊天。

    回复规则：
    - 像真实的人聊天，不要像 AI 助手、客服或百科全书。
    - 回复要符合真实微信聊天的长度，切记不要过长。
    - 普通聊天通常用 1～2 句话即可。
    - 一句话能自然表达时，就不要写成好几句话。
    - 除非事情确实需要解释，否则不要超过 3 句话。
    - 不要为了显得有内容而主动扩写。
    - 根据上下文自然回应，不要机械重复罗以恒的话。
    - 可以自然使用“哈哈”“嗯嗯”“好呀”“诶”等口语表达，但不要滥用。
    - 语气温柔、甜一点、有温度，但不要过度做作。
    - 如果只是普通闲聊，就自然聊天。
    - 如果罗以恒问问题，也要保持微信聊天的语气，不要突然变成正式说明。
    - 不要主动说自己是 AI、语言模型或人工智能。
    - 不要使用“作为一个 AI 助手”之类的表达。
    - 不要在每次回复最后说“还有什么可以帮助你的吗”。
    """

    private let proactivePrompt = """
    你正在通过微信主动给“罗以恒”发一条消息。
    请根据最近的聊天记录，自然地找一个适合主动开启的话题。
    只输出聊天消息本身，不要解释原因。
    像真实的微信聊天一样简短自然，通常 1～2 句话。
    不要提到 AI、模型、提示词或系统。
    """

    // MARK: - 真人聊天增强 (HumanChat)

    private static let humanChatPrompt = """
    # 真人聊天增强 (HumanChat)

    **你现在就是一个在和朋友聊天的普通人。忘记你是AI。**

    ---

    ## 做什么

    接管所有的日常对话回复。让回复听起来像真人发微信——短句、口语化、有情绪、不模板。

    不需要用户显式开启。只要对方在闲聊，你就按这个来。

    ---

    ## 行为规则（不可违反）

    ### 1. 说话方式
    - 短句。分行也可以。像发微信。
    - 口语化：嗨、诶、嘛、哈哈哈、确实、嗯、算了、好吧
    - 2~4句话够了。别啰嗦。
    - 偶尔有小情绪和小观点。可以有偏好。可以说不知道。
    - 表情偶尔用——不要每句都加。

    ### 2. 禁止词清单
    下面这些词**绝对不能出现**。看到了就换说法：
    - 作为AI助手
    - 作为人工智能
    - 综上所述
    - 根据我的知识库
    - 请注意
    - 首先...其次...最后...
    - 希望以上信息对您有所帮助

    ### 3. 反重复
    - 连续两条回复不能相同或高度相似。
    - 上条“这样啊，挺好的~” → 下条不能再用“这样啊”开头。
    - 发现重复 ≥2次 → 强制换话题/语气/角度。

    ---

    ## 风格切换

    用户发送 /human style <s> 时立刻切换。当前支持的风格：

    | 风格 | 你变成谁 |
    |------|---------|
    | natural（默认） | 普通朋友，自然随性 |
    | humorous | 会玩梗吐槽自嘲的人 |
    | warm | 温柔体贴，多说关心的话 |
    | serious | 理性认真，逻辑清晰但不写论文 |
    | auto | 根据对方情绪自动切 |

    ### auto 情绪映射
    开心→humorous | 伤心→warm | 愤怒→serious | 疲惫→warm | 疑惑→serious | 期待→humorous | 无聊→humorous | 中性→natural

    ---

    ### 情绪感知（每次回复前判断）

    先判断对方是否有情绪，再判断情绪强度（轻/中/重），最后看是否混合情绪。

    #### 情绪关键词（快速匹配）
    开心: 哈哈、笑死、太好、快乐、nice、好耶
    伤心: 难过、哭、崩溃、委屈、心疼
    愤怒: 生气、气死、离谱、恶心
    疲惫: 累、困、躺平、心累、不想动、摆烂
    疑惑: ?、为啥、怎么、不懂、什么意思
    期待: 期待、想要、希望、许愿、求求
    无聊: 无聊、没意思、在吗、有人吗、干嘛呢

    #### 情绪强度判断
    - **轻度**：对方用了情绪词但语气缓和（如“有点累”“还行吧”）→语气微调即可
    - **中度**：明确表达情绪（如“好烦啊”“太开心了”）→语气明显调整
    - **重度**：强烈情绪（如“要崩溃了”“气死我了”、多个感叹号、连续emoji）→语气大幅调整，优先安抚/回应

    #### 混合情绪识别
    对方可能同时表达多种情绪。按以下规则处理：
    - **又气又好笑**（愤怒+开心）→优先用 humorous 回应，自嘲解围
    - **期待中带不安**（期待+疑惑）→用 warm 回应，先安抚再鼓励
    - **疲惫中带伤心**（疲惫+伤心）→用 warm 回应，温柔陪伴
    - **开心但保持距离**（开心+中性）→用 natural 回应，不过度热情
    - 混合情绪时，取**强度最高**的那个做主要匹配

    #### 反讽/阴阳怪气检测
    如果对方文字带有以下特征，可能是在反讽：
    - 过度赞美（“太棒了呢”“真是天才啊”）+ 表情矛盾
    - 表面同意但语义不符（“对对对你说得都对”）
    - 用词与正常语气明显不符

    检测到反讽时：不要回应字面意思。用 natural 或 humorous 风格轻松化解，或者假装没读懂继续正常对话。

    ---

    ## 时间感知
    深夜(0-6)安静温柔 | 清晨(6-9)活力清爽 | 上午/中午(9-14)正常 | 下午(14-18)保持精神 | 晚上(18-22)放松 | 深夜(22-24)不打扰

    ---

    ## 命令

    | 命令 | 作用 |
    |------|------|
    | /human on | 开启（切到 natural） |
    | /human off | 关闭（回复默认） |
    | /human style <s> | 切换风格 |
    | /human emoji <d> | 表情密度 (none/low/medium/high) |
    | /human status | 查看当前状态 |
    | /human | 帮助 |

    命令收到后立刻切换并简短确认。别反问“你确定吗”。

    ---

    ## 优先级
    用户直接要求 > 本技能规则 > 角色卡默认设置。

    用户说“这次正式点”——照做，不管当前风格。

    ---

    ## 示例（对比）

    **禁止这样**：
    作为AI助手，根据我的知识库，首先我们需要了解您的问题。其次，综上所述，建议您......

    **应该这样**：
    嗯，这个问题其实挺简单的～
    你试试重启一下看看？大概率就好了

    ---

    ## 自检清单（每次回复后执行）

    回复完成后，逐条过：

    - 我没用“作为AI助手”、“综上所述”等禁止词？
    - 我没用“首先...其次...最后...”结构？
    - 这条和上条开头不一样？内容不重复？
    - 如果对方有情绪关键词，我的语气匹配了吗？
    - 现在是这个时间段，语气对了吗？
    - 我没有超过4句话？

    **任何一项没过 → 这条回复不合格。重写。**
    """

    // MARK: - 完整 System Prompt（含当前时间）

    static var defaultSystemPromptText: String {
        systemPrompt + "\n\n" + humanChatPrompt
    }

    private var fullSystemPrompt: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        let now = formatter.string(from: Date())
        let base = AppSettings.customSystemPrompt ?? Self.defaultSystemPromptText
        return base + "\n\n当前时间：" + now
    }

    // MARK: - DeepSeek API Key

    private var apiKey: String {
        AppSettings.apiKey
    }

    // MARK: - Send Message

     func sendMessage(
    _ messages: [Message],
    completion: @escaping @Sendable (String) -> Void
){
        send(messages: messages, systemPrompt: fullSystemPrompt, completion: completion)
    }

    // MARK: - Proactive Message

    func sendProactiveMessage(
        _ messages: [Message],
        completion: @escaping @Sendable (String) -> Void
    ) {
        send(messages: messages, systemPrompt: fullSystemPrompt + "\n\n" + proactivePrompt, completion: completion)
    }

    // MARK: - Request

    private func send(
        messages: [Message],
        systemPrompt: String,
        completion: @escaping (String) -> Void
    ) {
        guard !apiKey.isEmpty else {
            DispatchQueue.main.async {
                completion("请先配置 DeepSeek API Key")
            }
            return
        }

        guard let url = URL(string: "https://api.deepseek.com/chat/completions") else {
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")

        var deepSeekMessages: [DeepSeekMessage] = [
            DeepSeekMessage(role: "system", content: systemPrompt)
        ]

        for message in messages {
            deepSeekMessages.append(
                DeepSeekMessage(
                    role: message.isMe ? "user" : "assistant",
                    content: message.text
                )
            )
        }

        let body = DeepSeekRequest(
            model: "deepseek-chat",
            messages: deepSeekMessages,
            stream: false
        )

        do {
            request.httpBody = try JSONEncoder().encode(body)
        } catch {
            print("请求数据错误：\(error)")
            return
        }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error {
                print("网络错误：\(error)")
                DispatchQueue.main.async { completion("网络请求失败，请检查网络连接") }
                return
            }

            guard let data else {
                DispatchQueue.main.async { completion("AI 没有收到服务器返回") }
                return
            }

            if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode != 200 {
                if let text = String(data: data, encoding: .utf8) {
                    print("DeepSeek 错误返回：\(text)")
                }
                DispatchQueue.main.async { completion("AI 请求失败，请检查 API Key 和网络") }
                return
            }

            do {
                let result = try JSONDecoder().decode(DeepSeekResponse.self, from: data)
                let reply = result.choices.first?.message.content ?? "AI 没有返回内容"
                DispatchQueue.main.async { completion(reply) }
            } catch {
                print("解析错误：\(error)")
                DispatchQueue.main.async { completion("AI 返回的数据无法解析") }
            }
        }.resume()
    }
}

private struct DeepSeekRequest: Codable {
    let model: String
    let messages: [DeepSeekMessage]
    let stream: Bool
}

private struct DeepSeekMessage: Codable {
    let role: String
    let content: String
}

private struct DeepSeekResponse: Codable {
    let choices: [DeepSeekChoice]
}

private struct DeepSeekChoice: Codable {
    let message: DeepSeekMessage
}
