package com.example.travelassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** 是否启用大模型能力。关闭时系统自动走本地兜底逻辑。 */
    private boolean enabled = true;
    /** 大模型服务地址，当前默认兼容 DeepSeek/OpenAI 风格接口。 */
    private String baseUrl = "https://api.deepseek.com";
    /** 大模型密钥。 */
    private String apiKey;
    /** 所使用的模型名称。 */
    private String model = "deepseek-chat";
}
