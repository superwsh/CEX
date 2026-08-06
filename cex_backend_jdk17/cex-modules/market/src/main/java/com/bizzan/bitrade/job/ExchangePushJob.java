package com.bizzan.bitrade.job;

import com.bizzan.bitrade.entity.CoinThumb;
import com.bizzan.bitrade.entity.ExchangeOrderDirection;
import com.bizzan.bitrade.entity.ExchangeTrade;
import com.bizzan.bitrade.entity.TradePlate;
import com.bizzan.bitrade.handler.NettyHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ExchangePushJob {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private NettyHandler nettyHandler;
    private Map<String, List<ExchangeTrade>> tradesQueue = new HashMap<>();
    private Map<String, List<TradePlate>> plateQueue = new HashMap<>();
    private Map<String, List<CoinThumb>> thumbQueue = new HashMap<>();

    public void addTrades(String symbol, List<ExchangeTrade> trades) {
        List<ExchangeTrade> list = tradesQueue.get(symbol);
        if (list == null) {
            list = new ArrayList<>();
            tradesQueue.put(symbol, list);
        }
        synchronized (list) {
            list.addAll(trades);
        }
    }

    public void addPlates(String symbol, TradePlate plate) {
        List<TradePlate> list = plateQueue.get(symbol);
        if (list == null) {
            list = new ArrayList<>();
            plateQueue.put(symbol, list);
        }
        synchronized (list) {
            list.add(plate);
        }
    }

    public void addThumb(String symbol, CoinThumb thumb) {
        List<CoinThumb> list = thumbQueue.get(symbol);
        if (list == null) {
            list = new ArrayList<>();
            thumbQueue.put(symbol, list);
        }
        synchronized (list) {
            list.add(thumb);
        }
    }

    /**
     * 定时任务：推送交易信息
     */
    @Scheduled(fixedRate = 500)
    public void pushTrade() {
        for (Map.Entry<String, List<ExchangeTrade>> entry : tradesQueue.entrySet()) {
            List<ExchangeTrade> trades = entry.getValue();
            String symbol = trades.get(0).getSymbol();
            // 每一个代币对的交易信息都存在并发可能
            synchronized (trades) {
                messagingTemplate.convertAndSend("/topic/market/trade/" + symbol, trades);
                trades.clear();
            }
        }
    }

    /**
     * 定时任务：推送盘口信息
     */
    @Scheduled(fixedDelay = 2000)
    public void pushPlate() {
        for (Map.Entry<String, List<TradePlate>> entry : plateQueue.entrySet()) {
            String symbol = entry.getKey();
            List<TradePlate> plates = entry.getValue();
            if (!plates.isEmpty()) {
                synchronized (plates) {
                    // 防止洪泛、采样策略、减少带宽
                    // 每个推送周期（2秒）内，每个交易对最多只推送1条买盘和1条卖盘，其余的全部丢弃。
                    boolean hasPushAskPlate = false;
                    boolean hasPushBidPlate = false;
                    for (TradePlate plate : plates) {
                        if (plate.getDirection() == ExchangeOrderDirection.BUY && !hasPushAskPlate) {
                            hasPushAskPlate = true;
                        } else if (plate.getDirection() == ExchangeOrderDirection.SELL && !hasPushBidPlate) {
                            hasPushBidPlate = true;
                        } else {
                            continue;
                        }
                        //websocket推送盘口信息
                        messagingTemplate.convertAndSend("/topic/market/trade-plate/" + symbol, plate.toJSON(24));
                        //websocket推送深度信息
                        messagingTemplate.convertAndSend("/topic/market/trade-depth/" + symbol, plate.toJSON(50));
                        //netty推送
                        nettyHandler.handlePlate(symbol, plate);
                    }
                    plates.clear();
                }
            }
        }
    }

    @Scheduled(fixedRate = 500)
    public void pushThumb() {
        Iterator<Map.Entry<String, List<CoinThumb>>> entryIterator = thumbQueue.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<String, List<CoinThumb>> entry = entryIterator.next();
            List<CoinThumb> thumbs = entry.getValue();
            if (thumbs.size() > 0) {
                synchronized (thumbs) {
                    messagingTemplate.convertAndSend("/topic/market/thumb", thumbs.get(thumbs.size() - 1));
                    thumbs.clear();
                }
            }
        }
    }
}
