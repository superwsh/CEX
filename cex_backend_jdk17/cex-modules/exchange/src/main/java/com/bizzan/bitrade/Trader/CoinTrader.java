package com.bizzan.bitrade.Trader;

import com.alibaba.fastjson2.JSON;
import com.bizzan.bitrade.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CoinTrader {
    private String symbol;
    private KafkaTemplate<String, String> kafkaTemplate;
    //交易币种的精度
    private int coinScale = 4;
    //基币的精度
    private int baseCoinScale = 4;
    private static final int MAX_BATCH_SIZE = 1000;
    private Logger logger = LoggerFactory.getLogger(CoinTrader.class);
    //买入限价订单链表，价格从高到低排列
    private TreeMap<BigDecimal, MergeOrder> buyLimitPriceQueue;
    //卖出限价订单链表，价格从低到高排列
    private TreeMap<BigDecimal, MergeOrder> sellLimitPriceQueue;
    //买入市价订单链表，按时间从小到大排序
    private LinkedList<ExchangeOrder> buyMarketQueue;
    //卖出市价订单链表，按时间从小到大排序
    private LinkedList<ExchangeOrder> sellMarketQueue;
    //卖盘盘口信息
    private TradePlate sellTradePlate;
    //买盘盘口信息
    private TradePlate buyTradePlate;
    //是否暂停交易
    private boolean tradingHalt = false;
    private boolean ready = false;
    //交易对信息
    private ExchangeCoinPublishType publishType;
    private String clearTime;

    private SimpleDateFormat dateTimeFormat;


    public CoinTrader(String symbol) {
        this.symbol = symbol;
        initialize();
    }

    /**
     * 初始化交易线程
     */
    public void initialize() {
        logger.info("init CoinTrader for symbol {}", symbol);
        //买单队列价格降序排列
        buyLimitPriceQueue = new TreeMap<>(Comparator.reverseOrder());
        //卖单队列价格升序排列
        this.sellLimitPriceQueue = new TreeMap<>(Comparator.naturalOrder());
        this.buyMarketQueue = new LinkedList<>();
        this.sellMarketQueue = new LinkedList<>();
        this.sellTradePlate = new TradePlate(symbol, ExchangeOrderDirection.SELL);
        this.buyTradePlate = new TradePlate(symbol, ExchangeOrderDirection.BUY);
        this.dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 增加限价订单到队列，买入单按从价格高到低排，卖出单按价格从低到高排
     *
     * @param exchangeOrder
     */
    public void addLimitPriceOrder(ExchangeOrder exchangeOrder) {
        TreeMap<BigDecimal, MergeOrder> priceToOrderList;
        if (exchangeOrder.getDirection() == ExchangeOrderDirection.BUY) {
            priceToOrderList = buyLimitPriceQueue;
            buyTradePlate.add(exchangeOrder);
            if (ready) {
                sendTradePlateMessage(buyTradePlate);
            }
        } else {
            priceToOrderList = sellLimitPriceQueue;
            sellTradePlate.add(exchangeOrder);
            if (ready) {
                sendTradePlateMessage(sellTradePlate);
            }
        }
        // 将订单加入到对应价格的组合订单中
        synchronized (priceToOrderList) {
            MergeOrder mergeOrder = priceToOrderList.get(exchangeOrder.getPrice());
            if (mergeOrder == null) {
                mergeOrder = new MergeOrder();
                mergeOrder.add(exchangeOrder);
                priceToOrderList.put(exchangeOrder.getPrice(), mergeOrder);
            } else {
                mergeOrder.add(exchangeOrder);
            }
        }
    }

    public void addMarketPriceOrder(ExchangeOrder exchangeOrder) {
        if (exchangeOrder.getType() != ExchangeOrderType.MARKET_PRICE) {
            return;
        }
        logger.info("addMarketPriceOrder,orderId = {}", exchangeOrder.getOrderId());
        LinkedList<ExchangeOrder> list = exchangeOrder.getDirection() == ExchangeOrderDirection.BUY ? buyMarketQueue : sellMarketQueue;
        synchronized (list) {
            list.addLast(exchangeOrder);
        }
    }

    public void trade(List<ExchangeOrder> orders) throws ParseException {
        if (tradingHalt) {
            return;
        }
        for (ExchangeOrder order : orders) {
            trade(order);
        }
    }


    /**
     * 主动交易输入的订单，交易不完成的会输入到队列
     *
     * @param exchangeOrder
     * @throws ParseException
     */
    public void trade(ExchangeOrder exchangeOrder) {
        //由于一笔订单可能被撮合多次，所以需要判断成交量是否已完成
        if (exchangeOrder.getAmount().subtract(exchangeOrder.getTradedAmount()).compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        TreeMap<BigDecimal, MergeOrder> limitPriceOrderList;
        LinkedList<ExchangeOrder> marketPriceOrderList;
        if (exchangeOrder.getDirection() == ExchangeOrderDirection.BUY) {
            limitPriceOrderList = sellLimitPriceQueue;
            marketPriceOrderList = sellMarketQueue;
        } else {
            limitPriceOrderList = buyLimitPriceQueue;
            marketPriceOrderList = buyMarketQueue;
        }
        if (exchangeOrder.getType() == ExchangeOrderType.MARKET_PRICE) {
            //市价单与限价单匹配
            matchMarketPriceWithLPList(limitPriceOrderList, exchangeOrder);
        } else {
            //限价单价格必须大于0
            if (exchangeOrder.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                return;
            }
            //限价单优先和限价单撮合
            matchLimitPriceWithLPList(limitPriceOrderList, exchangeOrder);
            if (!exchangeOrder.isCompleted()) {
                //限价单与市价单撮合
                matchLimitPriceWithMPList(marketPriceOrderList, exchangeOrder);
            }
        }
    }


    /**
     * 限价委托单与限价队列匹配
     *
     * @param lpList       限价对手单队列
     * @param focusedOrder 交易订单
     */
    public void matchLimitPriceWithLPList(TreeMap<BigDecimal, MergeOrder> lpList, ExchangeOrder focusedOrder) {
        List<ExchangeTrade> exchangeTrades = new ArrayList<>();
        List<ExchangeOrder> completedOrders = new ArrayList<>();
        synchronized (lpList) {
            Iterator<Map.Entry<BigDecimal, MergeOrder>> mergeOrderIterator = lpList.entrySet().iterator();
            boolean exitLoop = false;
            while (!exitLoop && mergeOrderIterator.hasNext()) {
                Map.Entry<BigDecimal, MergeOrder> entry = mergeOrderIterator.next();
                MergeOrder mergeOrder = entry.getValue();
                Iterator<ExchangeOrder> orderIterator = mergeOrder.iterator();
                //买入单的价格为最高价，当匹配单价格大于买入价格时退出
                if (focusedOrder.getDirection() == ExchangeOrderDirection.BUY && mergeOrder.getPrice().compareTo(focusedOrder.getPrice()) > 0) {
                    return;
                }
                //卖出单的价格为最低价，当匹配单价格小于卖出价格时退出
                if (focusedOrder.getDirection() == ExchangeOrderDirection.SELL && mergeOrder.getPrice().compareTo(focusedOrder.getPrice()) < 0) {
                    return;
                }
                while (orderIterator.hasNext()) {
                    ExchangeOrder matchOrder = orderIterator.next();
                    //撮合匹配
                    ExchangeTrade trade = processMatch(focusedOrder, matchOrder);
                    if (trade != null) {
                        exchangeTrades.add(trade);
                    }
                    //判断匹配单是否完成
                    if (matchOrder.isCompleted()) {
                        orderIterator.remove();
                        completedOrders.add(matchOrder);

                    }
                    //判断委托单是否完成
                    if (focusedOrder.isCompleted()) {
                        completedOrders.add(focusedOrder);
                        exitLoop = true;
                        break;
                    }
                }
                //该价格的订单已全部撮合完成，从TreeMap中移除
                if (mergeOrder.size() == 0) {
                    mergeOrderIterator.remove();
                }
            }
        }
        //推送交易信息
        handleExchangeTrade(exchangeTrades);
        //推送订单完成信息（用于清算）
        orderCompleted(completedOrders);
        if (!exchangeTrades.isEmpty()) {
            TradePlate plate = focusedOrder.getDirection() == ExchangeOrderDirection.BUY ? buyTradePlate : sellTradePlate;
            sendTradePlateMessage(plate);
        }
    }

    /**
     * 限价委托单与市价队列匹配
     *
     * @param mpList       市价对手单队列
     * @param focusedOrder 交易订单
     */
    public void matchLimitPriceWithMPList(LinkedList<ExchangeOrder> mpList, ExchangeOrder focusedOrder) {
        List<ExchangeTrade> exchangeTrades = new ArrayList<>();
        List<ExchangeOrder> completedOrders = new ArrayList<>();//已完成订单--统一处理并通知
        synchronized (mpList) {
            Iterator<ExchangeOrder> orderIterator = mpList.iterator();
            while (orderIterator.hasNext()) {
                ExchangeOrder matchOrder = orderIterator.next();
                //处理匹配
                ExchangeTrade trade = processMatch(focusedOrder, matchOrder);
                if (matchOrder.isCompleted()) {
                    exchangeTrades.add(trade);
                    orderIterator.remove();
                }
                if (focusedOrder.isCompleted()) {
                    orderIterator.remove();
                    completedOrders.add(focusedOrder);
                    break;
                }
            }
        }
        //推送交易信息
        handleExchangeTrade(exchangeTrades);
        //推送已完成订单信息（用于清算）
        orderCompleted(completedOrders);
        if (!focusedOrder.isCompleted()) {
            addLimitPriceOrder(focusedOrder);
        }
        if (!exchangeTrades.isEmpty()) {
            TradePlate plate = focusedOrder.getDirection() == ExchangeOrderDirection.BUY ? buyTradePlate : sellTradePlate;
            sendTradePlateMessage(plate);
        }
    }


    /**
     * 市价委托单与限价对手单列表交易
     *
     * @param lpList       限价对手单列表
     * @param focusedOrder 待交易订单
     */
    public void matchMarketPriceWithLPList(TreeMap<BigDecimal, MergeOrder> lpList, ExchangeOrder focusedOrder) {
        List<ExchangeTrade> exchangeTrades = new ArrayList<>();
        List<ExchangeOrder> completedOrders = new ArrayList<>();//已完成订单--统一处理并通知
        synchronized (lpList) {
            Iterator<Map.Entry<BigDecimal, MergeOrder>> mergeOrderIterator = lpList.entrySet().iterator();
            boolean exitLoop = false;
            while (!exitLoop && mergeOrderIterator.hasNext()) {
                Map.Entry<BigDecimal, MergeOrder> entry = mergeOrderIterator.next();
                MergeOrder mergeOrder = entry.getValue();
                Iterator<ExchangeOrder> orderIterator = mergeOrder.iterator();
                while (orderIterator.hasNext()) {
                    ExchangeOrder matchOrder = orderIterator.next();
                    //处理匹配
                    ExchangeTrade trade = processMatch(focusedOrder, matchOrder);
                    if (trade != null) {
                        exchangeTrades.add(trade);
                    }
                    //判断匹配单是否完成
                    if (matchOrder.isCompleted()) {
                        orderIterator.remove();
                        completedOrders.add(matchOrder);
                    }
                    //判断委托单是否完成
                    if (focusedOrder.isCompleted()) {
                        completedOrders.add(focusedOrder);
                        exitLoop = true;
                        break;
                    }
                }
                //该价格的订单已全部撮合完成，从TreeMap中移除
                if (mergeOrder.size() == 0) {
                    mergeOrderIterator.remove();
                }
            }
        }
        //如果订单还没有完成，添加到市价单列表中，等待再次撮合
        if (!focusedOrder.isCompleted()) {
            addMarketPriceOrder(focusedOrder);
        }
        handleExchangeTrade(exchangeTrades);
        if (!exchangeTrades.isEmpty()) {
            orderCompleted(completedOrders);
            TradePlate plate = focusedOrder.getDirection() == ExchangeOrderDirection.BUY ? buyTradePlate : sellTradePlate;
            sendTradePlateMessage(plate);
        }
    }

    /**
     * 计算委托单剩余可成交的数量
     *
     * @param order     委托单
     * @param dealPrice 成交价
     * @return
     */
    private BigDecimal calculateTradedAmount(ExchangeOrder order, BigDecimal dealPrice) {
        if (order.getDirection() == ExchangeOrderDirection.BUY && order.getType() == ExchangeOrderType.MARKET_PRICE) {
            //剩余成交量
            BigDecimal leftTurnover = order.getAmount().subtract(order.getTurnover());
            return leftTurnover.divide(dealPrice, coinScale, BigDecimal.ROUND_DOWN);
        } else {
            return order.getAmount().subtract(order.getTradedAmount());
        }
    }

    /**
     * 调整市价单剩余成交额，当剩余成交额不足时设置订单完成
     *
     * @param order
     * @param dealPrice
     * @return
     */
    private BigDecimal adjustMarketOrderTurnover(ExchangeOrder order, BigDecimal dealPrice) {
        BigDecimal leftTurnover = order.getAmount().subtract(order.getTurnover());
        if (leftTurnover.divide(dealPrice, coinScale, BigDecimal.ROUND_DOWN)
                .compareTo(BigDecimal.ZERO) == 0) {
            order.setTurnover(order.getAmount());
            return leftTurnover;
        }

        return BigDecimal.ZERO;
    }

    /**
     * 处理两个匹配的委托订单
     *
     * @param focusedOrder 焦点单 需要进行匹配的单
     * @param matchOrder   匹配单
     * @return
     */
    private ExchangeTrade processMatch(ExchangeOrder focusedOrder, ExchangeOrder matchOrder) {
        //需要的数量，成交价，可用数量
        BigDecimal needAmount, dealPrice, availAmount;
        //成交价--焦点单和匹配单中至少有一个是限价单，成交价要以限价单的价格为准
        if (matchOrder.getType() == ExchangeOrderType.LIMIT_PRICE) {
            dealPrice = matchOrder.getPrice();
        } else {
            dealPrice = focusedOrder.getPrice();
        }
        needAmount = calculateTradedAmount(focusedOrder, dealPrice);
        availAmount = calculateTradedAmount(matchOrder, dealPrice);
        //计算成交量
        BigDecimal tradedAmount = needAmount.compareTo(availAmount) < 0 ? needAmount : availAmount;
        if (tradedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        //计算成交额,成交额要保留足够精度
        BigDecimal turnover = dealPrice.multiply(tradedAmount);
        matchOrder.setTradedAmount(matchOrder.getTradedAmount().add(tradedAmount));
        matchOrder.setTurnover(matchOrder.getTurnover().add(turnover));
        focusedOrder.setTradedAmount(focusedOrder.getTradedAmount().add(tradedAmount));
        focusedOrder.setTurnover(focusedOrder.getTurnover().add(turnover));

        ExchangeTrade exchangeTrade = new ExchangeTrade();
        exchangeTrade.setSymbol(focusedOrder.getSymbol());
        exchangeTrade.setPrice(dealPrice);
        exchangeTrade.setAmount(tradedAmount);
        exchangeTrade.setBuyTurnover(turnover);
        exchangeTrade.setSellTurnover(turnover);
        exchangeTrade.setDirection(focusedOrder.getDirection());
        //在市价买单场景下，剩余金额不够买最小单位币时，将剩余金额加到这次成交记录上，并标记该订单已完成
        //如果不这么做，这个订单会永远被视为未完成，永久存留在内存中，当这样的订单足够多就可能导致内存爆炸
        if (ExchangeOrderType.MARKET_PRICE == focusedOrder.getType() && focusedOrder.getDirection() == ExchangeOrderDirection.BUY) {
            BigDecimal adjustTurnover = adjustMarketOrderTurnover(focusedOrder, dealPrice);
            exchangeTrade.setBuyTurnover(turnover.add(adjustTurnover));
        } else if (ExchangeOrderType.MARKET_PRICE == matchOrder.getType() && matchOrder.getDirection() == ExchangeOrderDirection.BUY) {
            BigDecimal adjustTurnover = adjustMarketOrderTurnover(matchOrder, dealPrice);
            exchangeTrade.setBuyTurnover(turnover.add(adjustTurnover));
        }
        if (focusedOrder.getDirection() == ExchangeOrderDirection.BUY) {
            exchangeTrade.setBuyOrderId(focusedOrder.getOrderId());
            exchangeTrade.setSellOrderId(matchOrder.getOrderId());
        } else {
            exchangeTrade.setBuyOrderId(matchOrder.getOrderId());
            exchangeTrade.setSellOrderId(focusedOrder.getOrderId());
        }
        exchangeTrade.setTime(Calendar.getInstance().getTimeInMillis());
        if (matchOrder.getType() == ExchangeOrderType.LIMIT_PRICE) {
            if (matchOrder.getDirection() == ExchangeOrderDirection.BUY) {
                buyTradePlate.remove(matchOrder, tradedAmount);
            } else {
                sellTradePlate.remove(matchOrder, tradedAmount);
            }
        }
        return exchangeTrade;
    }

    /**
     * 推送订单消息
     *
     * @param trades
     */
    public void handleExchangeTrade(List<ExchangeTrade> trades) {
        if (trades.size() > 0) {
            int maxSize = 1000;
            int size = trades.size();
            //如果交易数超过1000，一次发1000个
            if (size > 1000) {
                for (int index = 0; index < size; index += maxSize) {
                    int length = Math.min((size - index), maxSize);
                    List<ExchangeTrade> subTrades = trades.subList(index, length);
                    kafkaTemplate.send("exchange-trade", JSON.toJSONString(subTrades));
                }
            } else {
                kafkaTemplate.send("exchange-trade", JSON.toJSONString(trades));
            }
        }
    }

    /**
     * 发送订单交易成功消息
     *
     * @param orders
     */
    private void sendCompletedMessage(List<ExchangeOrder> orders) {
        String payload = JSON.toJSONString(orders);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate
                .send("exchange-order-completed", payload)
                .toCompletableFuture();

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.warn("投递订单消息失败", throwable);
            } else {
                logger.info("投递订单消息成功, key={}, offset={}",
                        result.getProducerRecord().key(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    /**
     * 订单完成，执行消息通知,订单数超1000个要拆分发送
     *
     * @param orders
     */
    public void orderCompleted(List<ExchangeOrder> orders) {
        if (!orders.isEmpty()) {
            int maxSize = 1000;
            int size = orders.size();
            //如果交易数超过1000，一次发1000个
            if (size > 1000) {
                for (int index = 0; index < size; index += maxSize) {
                    int length = Math.min((size - index), maxSize);
                    List<ExchangeOrder> subOrders = orders.subList(index, length);
                    sendCompletedMessage(subOrders);
                }
            } else {
                sendCompletedMessage(orders);
            }
        }
    }

    /**
     * 发送盘口变化消息
     *
     * @param plate
     */
    public void sendTradePlateMessage(TradePlate plate) {
        //防止并发引起数组越界，造成盘口倒挂
        synchronized (plate.getItems()) {
            kafkaTemplate.send("exchange-trade-plate", JSON.toJSONString(plate));
        }
    }

    /**
     * 取消委托订单
     *
     * @param exchangeOrder
     * @return
     */
    public ExchangeOrder cancelOrder(ExchangeOrder exchangeOrder) {
        logger.info("cancelOrder,orderId={}", exchangeOrder.getOrderId());
        if (exchangeOrder.getType() == ExchangeOrderType.MARKET_PRICE) {
            //处理市价单
            Iterator<ExchangeOrder> orderIterator;
            List<ExchangeOrder> list = null;
            if (exchangeOrder.getDirection() == ExchangeOrderDirection.BUY) {
                list = this.buyMarketQueue;
            } else {
                list = this.sellMarketQueue;
            }
            synchronized (list) {
                orderIterator = list.iterator();
                while ((orderIterator.hasNext())) {
                    ExchangeOrder order = orderIterator.next();
                    if (order.getOrderId().equalsIgnoreCase(exchangeOrder.getOrderId())) {
                        orderIterator.remove();
                        onRemoveOrder(order);
                        return order;
                    }
                }
            }
        } else {
            //处理限价单
            TreeMap<BigDecimal, MergeOrder> list = null;
            Iterator<MergeOrder> mergeOrderIterator;
            if (exchangeOrder.getDirection() == ExchangeOrderDirection.BUY) {
                list = this.buyLimitPriceQueue;
            } else {
                list = this.sellLimitPriceQueue;
            }
            synchronized (list) {
                MergeOrder mergeOrder = list.get(exchangeOrder.getPrice());
                if (mergeOrder != null) {
                    Iterator<ExchangeOrder> orderIterator = mergeOrder.iterator();
                    while (orderIterator.hasNext()) {
                        ExchangeOrder order = orderIterator.next();
                        if (order.getOrderId().equalsIgnoreCase(exchangeOrder.getOrderId())) {
                            orderIterator.remove();
                            if (mergeOrder.size() == 0) {
                                list.remove(exchangeOrder.getPrice());
                            }
                            onRemoveOrder(order);
                            return order;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void onRemoveOrder(ExchangeOrder order) {
        if (order.getType() == ExchangeOrderType.LIMIT_PRICE) {
            if (order.getDirection() == ExchangeOrderDirection.BUY) {
                buyTradePlate.remove(order);
                sendTradePlateMessage(buyTradePlate);
            } else {
                sellTradePlate.remove(order);
                sendTradePlateMessage(sellTradePlate);
            }
        }
    }


    public TradePlate getTradePlate(ExchangeOrderDirection direction) {
        if (direction == ExchangeOrderDirection.BUY) {
            return buyTradePlate;
        } else {
            return sellTradePlate;
        }
    }


    /**
     * 查询交易器里的订单
     *
     * @param orderId
     * @param type
     * @param direction
     * @return
     */
    public ExchangeOrder findOrder(String orderId, ExchangeOrderType type, ExchangeOrderDirection direction) {
        if (type == ExchangeOrderType.MARKET_PRICE) {
            LinkedList<ExchangeOrder> list;
            if (direction == ExchangeOrderDirection.BUY) {
                list = this.buyMarketQueue;
            } else {
                list = this.sellMarketQueue;
            }
            synchronized (list) {
                Iterator<ExchangeOrder> orderIterator = list.iterator();
                while ((orderIterator.hasNext())) {
                    ExchangeOrder order = orderIterator.next();
                    if (order.getOrderId().equalsIgnoreCase(orderId)) {
                        return order;
                    }
                }
            }
        } else {
            TreeMap<BigDecimal, MergeOrder> list;
            if (direction == ExchangeOrderDirection.BUY) {
                list = this.buyLimitPriceQueue;
            } else {
                list = this.sellLimitPriceQueue;
            }
            synchronized (list) {
                Iterator<Map.Entry<BigDecimal, MergeOrder>> mergeOrderIterator = list.entrySet().iterator();
                while (mergeOrderIterator.hasNext()) {
                    Map.Entry<BigDecimal, MergeOrder> entry = mergeOrderIterator.next();
                    MergeOrder mergeOrder = entry.getValue();
                    Iterator<ExchangeOrder> orderIterator = mergeOrder.iterator();
                    while ((orderIterator.hasNext())) {
                        ExchangeOrder order = orderIterator.next();
                        if (order.getOrderId().equalsIgnoreCase(orderId)) {
                            return order;
                        }
                    }
                }
            }
        }
        return null;
    }

    public TreeMap<BigDecimal, MergeOrder> getBuyLimitPriceQueue() {
        return buyLimitPriceQueue;
    }

    public LinkedList<ExchangeOrder> getBuyMarketQueue() {
        return buyMarketQueue;
    }

    public TreeMap<BigDecimal, MergeOrder> getSellLimitPriceQueue() {
        return sellLimitPriceQueue;
    }

    public LinkedList<ExchangeOrder> getSellMarketQueue() {
        return sellMarketQueue;
    }

    public void setKafkaTemplate(KafkaTemplate<String, String> template) {
        this.kafkaTemplate = template;
    }

    public void setCoinScale(int scale) {
        this.coinScale = scale;
    }

    public void setBaseCoinScale(int scale) {
        this.baseCoinScale = scale;
    }

    public boolean isTradingHalt() {
        return this.tradingHalt;
    }

    /**
     * 暂停交易,不接收新的订单
     */
    public void haltTrading() {
        this.tradingHalt = true;
    }

    /**
     * 恢复交易
     */
    public void resumeTrading() {
        this.tradingHalt = false;
    }

    public void stopTrading() {
        //TODO:停止交易，取消当前所有订单
    }

    public boolean getReady() {
        return this.ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public void setPublishType(ExchangeCoinPublishType publishType) {
        this.publishType = publishType;
    }

    public void setClearTime(String clearTime) {
        this.clearTime = clearTime;
    }

    public int getLimitPriceOrderCount(ExchangeOrderDirection direction) {
        int count = 0;
        TreeMap<BigDecimal, MergeOrder> queue = direction == ExchangeOrderDirection.BUY ? buyLimitPriceQueue : sellLimitPriceQueue;
        Iterator<Map.Entry<BigDecimal, MergeOrder>> mergeOrderIterator = queue.entrySet().iterator();
        while (mergeOrderIterator.hasNext()) {
            Map.Entry<BigDecimal, MergeOrder> entry = mergeOrderIterator.next();
            MergeOrder mergeOrder = entry.getValue();
            count += mergeOrder.size();
        }
        return count;
    }
}
