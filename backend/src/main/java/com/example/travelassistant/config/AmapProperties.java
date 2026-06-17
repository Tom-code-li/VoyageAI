package com.example.travelassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.amap")
public class AmapProperties {

    /** 高德 Web Service Key，用于 POI 搜索和路线规划。 */
    private String webServiceKey;
}
