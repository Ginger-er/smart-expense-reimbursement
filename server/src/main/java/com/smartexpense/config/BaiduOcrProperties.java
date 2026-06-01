package com.smartexpense.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "baidu.ocr")
public class BaiduOcrProperties {
    private String appId;
    private String apiKey;
    private String secretKey;
}
