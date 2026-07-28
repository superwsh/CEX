package com.bizzan.bc.wallet.config;

import com.bizzan.bc.wallet.converter.BigDecimalToDecimal128Converter;
import com.bizzan.bc.wallet.converter.Decimal128ToBigDecimalConverter;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.Arrays;

@Configuration
public class MongodbConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri); // 更简洁
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        String db = new ConnectionString(mongoUri).getDatabase();
        if (db == null || db.isEmpty()) {
            throw new IllegalArgumentException("MongoDB URI must specify a database name");
        }
        return new SimpleMongoClientDatabaseFactory(mongoClient, db);
    }

    /**
     * 创建 MongoClient（新版驱动）
     */
//    @Bean
//    public MongoClient mongoClient() {
//        ConnectionString connectionString = new ConnectionString(mongoUri);
//        MongoClientSettings settings = MongoClientSettings.builder()
//                .applyConnectionString(connectionString)
//                .build();
//        return MongoClients.create(settings);
//    }
//
//    /**
//     * MongoDatabaseFactory 替代旧的 MongoDbFactory
//     */
//    @Bean
//    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
//        // 从 URI 自动提取数据库名
//        String databaseName = new ConnectionString(mongoUri).getDatabase();
//        if (databaseName == null || databaseName.isEmpty()) {
//            throw new IllegalArgumentException("MongoDB URI must specify a database name");
//        }
//        return MongoDatabaseFactory.create(mongoClient, databaseName);
//    }
//
//    /**
//     * 自定义 MappingMongoConverter，注册 BigDecimal ↔ Decimal128 转换器
//     */
//    @Bean
//    public MappingMongoConverter mappingMongoConverter(
//            MongoDatabaseFactory mongoDatabaseFactory,
//            MongoMappingContext mongoMappingContext) {
//
//        var converter = new MappingMongoConverter(
//                new DefaultDbRefResolver(mongoDatabaseFactory),
//                mongoMappingContext
//        );
//
//        // 注册自定义转换器
//        converter.setCustomConversions(new MongoCustomConversions(Arrays.asList(
//                new BigDecimalToDecimal128Converter(),
//                new Decimal128ToBigDecimalConverter()
//        )));
//
//        return converter;
//    }
//
//    /**
//     * MongoMappingContext（可选，Spring Boot 通常自动配置）
//     */
//    @Bean
//    public MongoMappingContext mongoMappingContext() {
//        return new MongoMappingContext();
//    }
//
//    /**
//     * MongoTemplate
//     */
//    @Bean
//    public MongoTemplate mongoTemplate(
//            MongoDatabaseFactory mongoDatabaseFactory,
//            MappingMongoConverter mappingMongoConverter) {
//        return new MongoTemplate(mongoDatabaseFactory, mappingMongoConverter);
//    }
}
