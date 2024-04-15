package com.sky.config;

import com.sky.properties.QiniuProperties;
import com.sky.utils.QiniuUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QiniuConfig {

    @Bean
    public QiniuUtil getQiniuUtil(QiniuProperties qiniuProperties) {
        return new QiniuUtil(qiniuProperties.getAccessKey(),
                qiniuProperties.getSecretKey(),
                qiniuProperties.getBucketName(),
                qiniuProperties.getDomainName());
    }
}
