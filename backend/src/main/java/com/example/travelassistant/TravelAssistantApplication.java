package com.example.travelassistant;

import com.example.travelassistant.config.AiProperties;
import com.example.travelassistant.config.AmapProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.travelassistant.mapper")
@EnableConfigurationProperties({AiProperties.class, AmapProperties.class})
public class TravelAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelAssistantApplication.class, args);
    }
}
