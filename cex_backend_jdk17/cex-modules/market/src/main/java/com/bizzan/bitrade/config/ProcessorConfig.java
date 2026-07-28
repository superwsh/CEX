package com.bizzan.bitrade.config;

import com.bizzan.bitrade.component.CoinExchangeRate;
import com.bizzan.bitrade.entity.ExchangeCoin;
import com.bizzan.bitrade.handler.MongoMarketHandler;
import com.bizzan.bitrade.handler.NettyHandler;
import com.bizzan.bitrade.handler.WebsocketMarketHandler;
import com.bizzan.bitrade.processor.CoinProcessor;
import com.bizzan.bitrade.processor.CoinProcessorFactory;
import com.bizzan.bitrade.processor.DefaultCoinProcessor;
import com.bizzan.bitrade.service.ExchangeCoinService;
import com.bizzan.bitrade.service.MarketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 初始化并配置数字货币交易行情的处理器工厂（CoinProcessorFactory）
 *  根据数据库中启用的交易对（ExchangeCoin），为每个交易对创建独立的行情处理器（CoinProcessor），
 *      并绑定各类数据处理 / 推送处理器（MongoDB 存储、WebSocket 推送、Netty 推送等），
 *      最终将所有处理器统一管理在工厂中，支撑整个交易平台的实时行情处理流程。
 *
 *  责任链模式：每个 CoinProcessor 绑定多个 Handler，当该交易对有新行情（如成交、K 线更新）时，会依次触发：
 *      mongoMarketHandler：先把行情数据存到 MongoDB；
 *      wsHandler：推送给 WebSocket 客户端；
 *      nettyHandler：推送给 Netty 客户端（高并发实时推送）；
 * 交易对隔离：每个交易对有独立的 CoinProcessor，避免不同交易对的行情处理相互干扰；
 * 开关控制：setIsStopKLine(true) 是业务自定义开关，可能用于临时暂停 K 线生成（如维护时）。
 */
@Configuration
@Slf4j
public class ProcessorConfig {

    @Bean
    public CoinProcessorFactory processorFactory(MongoMarketHandler mongoMarketHandler,
                                                 WebsocketMarketHandler wsHandler,
                                                 NettyHandler nettyHandler,
                                                 MarketService marketService,
                                                 CoinExchangeRate exchangeRate,
                                                 ExchangeCoinService coinService,
                                                 RestTemplate restTemplate) {

        log.info("====initialized CoinProcessorFactory start==================================");

        CoinProcessorFactory factory = new CoinProcessorFactory();
        List<ExchangeCoin> coins = coinService.findAllEnabled();
        log.info("exchange-coin result:{}",coins);
        // 责任链模式：每个 CoinProcessor 绑定多个 Handler，当该交易对有新行情（如成交、K 线更新）时，会依次触发：
        for (ExchangeCoin coin : coins) {
            CoinProcessor processor = new DefaultCoinProcessor(coin.getSymbol(), coin.getBaseSymbol());
            processor.addHandler(mongoMarketHandler);
            processor.addHandler(wsHandler);
            processor.addHandler(nettyHandler);
            processor.setMarketService(marketService);
            processor.setExchangeRate(exchangeRate);
            processor.setIsStopKLine(true);
            
            factory.addProcessor(coin.getSymbol(), processor);
            log.info("new processor = ", processor);
        }

        log.info("====initialized CoinProcessorFactory completed====");
        log.info("CoinProcessorFactory = ", factory);
        exchangeRate.setCoinProcessorFactory(factory);
        return factory;
    }
}
