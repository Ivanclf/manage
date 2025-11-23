package com.activity.manage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "manage")
public class UrlConfig {

    private String activityPage;

    private String restorationPage;

    private String checkinPage;
}
