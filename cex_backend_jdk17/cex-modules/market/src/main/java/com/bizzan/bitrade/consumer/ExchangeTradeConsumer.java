package com.bizzan.bitrade.consumer;

import com.alibaba.fastjson2.JSON;
import com.bizzan.bitrade.constant.NettyCommand;
import com.bizzan.bitrade.entity.ExchangeOrder;
import com.bizzan.bitrade.entity.ExchangeTrade;
import com.bizzan.bitrade.entity.TradePlate;
import com.bizzan.bitrade.handler.NettyHandler;
import com.bizzan.bitrade.job.ExchangePushJob;
import com.bizzan.bitrade.processor.CoinProcessor;
import com.bizzan.bitrade.processor.CoinProcessorFactory;
import com.bizzan.bitrade.service.ExchangeOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ExchangeTradeConsumer {
	private Logger logger = LoggerFactory.getLogger(ExchangeTradeConsumer.class);
	@Autowired
	private CoinProcessorFactory coinProcessorFactory;
	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	@Autowired
	private ExchangeOrderService exchangeOrderService;
	@Autowired
	private NettyHandler nettyHandler;
	@Value("${second.referrer.award}")
	private boolean secondReferrerAward;
	private ExecutorService executor = new ThreadPoolExecutor(30, 100, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>(1024), new ThreadPoolExecutor.AbortPolicy());
	@Autowired
	private ExchangePushJob pushJob;

	/**
	 * 处理成交明细
	 *
	 * @param records
	 */
	@KafkaListener(topics = "exchange-trade", containerFactory = "kafkaListenerContainerFactory")
	public void handleTrade(List<ConsumerRecord<String, String>> records) {

	}

	@KafkaListener(topics = "exchange-order-completed", containerFactory = "kafkaListenerContainerFactory")
	public void handleOrderCompleted(List<ConsumerRecord<String, String>> records) {

	}

	/**
	 * 处理模拟交易
	 *
	 * @param records
	 */
	@KafkaListener(topics = "exchange-trade-mocker", containerFactory = "kafkaListenerContainerFactory")
	public void handleMockerTrade(List<ConsumerRecord<String, String>> records) {
		try {
			for (int i = 0; i < records.size(); i++) {
				ConsumerRecord<String, String> record = records.get(i);
				logger.info("mock数据topic={},value={},size={}", record.topic(), record.value(), records.size());
				List<ExchangeTrade> trades = JSON.parseArray(record.value(), ExchangeTrade.class);
				String symbol = trades.get(0).getSymbol();
				// 处理行情
				CoinProcessor coinProcessor = coinProcessorFactory.getProcessor(symbol);
				if (coinProcessor != null) {
					coinProcessor.process(trades);
				}
				pushJob.addTrades(symbol, trades);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 消费交易盘口信息
	 *
	 * @param records
	 */
	@KafkaListener(topics = "exchange-trade-plate", containerFactory = "kafkaListenerContainerFactory")
	public void handleTradePlate(List<ConsumerRecord<String, String>> records) {

	}

	/**
	 * 订单取消成功
	 *
	 * @param records
	 */
	@KafkaListener(topics = "exchange-order-cancel-success", containerFactory = "kafkaListenerContainerFactory")
	public void handleOrderCanceled(List<ConsumerRecord<String, String>> records) {

	}

	public class HandleTradeThread implements Runnable {
		private ConsumerRecord<String, String> record;

		private HandleTradeThread(ConsumerRecord<String, String> record) {
			this.record = record;
		}

		@Override
		public void run() {

		}
	}
}
