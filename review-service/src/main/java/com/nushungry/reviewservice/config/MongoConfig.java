package com.nushungry.reviewservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB 配置类
 * 针对生产环境配置连接池和性能优化参数
 */
@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.database:review_service}")
    private String database;

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/review_service}")
    private String mongoUri;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    /**
     * 生产环境 MongoDB 客户端配置
     * 配置连接池、超时、重试等参数
     */
    @Bean
    @Profile("prod")
    public MongoClient prodMongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                // 连接池配置
                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(100)                           // 最大连接池大小
                        .minSize(10)                            // 最小连接池大小
                        .maxWaitTime(5000, TimeUnit.MILLISECONDS)     // 获取连接最大等待时间
                        .maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS)  // 连接最大空闲时间
                        .maxConnectionLifeTime(600000, TimeUnit.MILLISECONDS) // 连接最大生命周期
                )
                // Socket 配置
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(10000, TimeUnit.MILLISECONDS)  // 连接超时
                        .readTimeout(30000, TimeUnit.MILLISECONDS)     // 读超时
                )
                // 服务器配置
                .applyToServerSettings(builder -> builder
                        .heartbeatFrequency(10000, TimeUnit.MILLISECONDS)  // 心跳频率
                        .minHeartbeatFrequency(500, TimeUnit.MILLISECONDS) // 最小心跳频率
                )
                // 集群配置
                .applyToClusterSettings(builder -> builder
                        .serverSelectionTimeout(30000, TimeUnit.MILLISECONDS)  // 服务器选择超时
                )
                .retryWrites(true)   // 启用写重试
                .retryReads(true)    // 启用读重试
                .build();

        return MongoClients.create(settings);
    }

    /**
     * 开发和测试环境 MongoDB 客户端配置
     * 使用默认配置
     */
    @Bean
    @Profile({"dev", "test"})
    public MongoClient devMongoClient() {
        return MongoClients.create(mongoUri);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, getDatabaseName());
    }
}
