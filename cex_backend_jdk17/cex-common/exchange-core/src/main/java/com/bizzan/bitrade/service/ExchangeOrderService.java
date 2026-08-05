package com.bizzan.bitrade.service;


import com.alibaba.fastjson2.JSONObject;
import com.bizzan.bitrade.constant.BooleanEnum;
import com.bizzan.bitrade.constant.PromotionRewardType;
import com.bizzan.bitrade.constant.RewardRecordType;
import com.bizzan.bitrade.constant.TransactionType;
import com.bizzan.bitrade.dao.ExchangeOrderDetailRepository;
import com.bizzan.bitrade.dao.ExchangeOrderRepository;
import com.bizzan.bitrade.dao.OrderDetailAggregationRepository;
import com.bizzan.bitrade.entity.*;
import com.bizzan.bitrade.pagination.Criteria;
import com.bizzan.bitrade.pagination.PageResult;
import com.bizzan.bitrade.pagination.Restrictions;
import com.bizzan.bitrade.service.Base.BaseService;
import com.bizzan.bitrade.util.BigDecimalUtils;
import com.bizzan.bitrade.util.DateUtil;
import com.bizzan.bitrade.util.GeneratorUtil;
import com.bizzan.bitrade.util.MessageResult;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.sparkframework.sql.DB;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ExchangeOrderService extends BaseService {

    @Autowired
    private ExchangeOrderRepository exchangeOrderRepository;
    @Autowired
    private MemberWalletService memberWalletService;
    @Autowired
    private ExchangeOrderDetailRepository exchangeOrderDetailRepository;
    @Autowired
    private ExchangeCoinService exchangeCoinService;
    @Autowired
    private OrderDetailAggregationRepository orderDetailAggregationRepository;
    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberTransactionService transactionService;
    @Autowired
    private RewardPromotionSettingService rewardPromotionSettingService;
    @Autowired
    private RewardRecordService rewardRecordService;
    @Autowired
    private ExchangeOrderDetailService exchangeOrderDetailService;
    @Value("${channel.enable:false}")
    private Boolean channelEnable;
    @Value("${channel.exchange-rate:0.00}")
    private BigDecimal channelExchangeRate;

    @Autowired
    private LocaleMessageSourceService msService;

    public Page<ExchangeOrder> findAll(Predicate predicate, Pageable pageable) {
        return exchangeOrderRepository.findAll(predicate, pageable);
    }


    /**
     * 添加委托订单
     *
     * @param memberId
     * @param order
     * @return
     */
    @Transactional
    public MessageResult addOrder(Long memberId, ExchangeOrder order) {
        if (order.getDirection() == ExchangeOrderDirection.BUY) {
            MemberWallet wallet = memberWalletService.findByCoinUnitAndMemberId(order.getBaseSymbol(), memberId);
            if (wallet.getIsLock().equals(BooleanEnum.IS_TRUE)) {
                return MessageResult.success("钱包已锁定");
            }
            //计算成交额
            BigDecimal turnover;
            if (order.getType() == ExchangeOrderType.MARKET_PRICE) {
                //市价买单，用户在页面输入的是USDT成交额，这时候amount就是成交额
                turnover = order.getAmount();
            } else {
                turnover = order.getAmount().multiply(order.getPrice());
            }
            //判断用户钱包余额是否足够
            if (wallet.getBalance().compareTo(turnover) < 0) {
                return MessageResult.error(500, order.getBaseSymbol() + "余额不足");
            }
            //冻结余额
            MessageResult result = memberWalletService.freezeBalance(wallet, turnover);
            if (result.getCode() != 0) {
                return result;
            }
        } else {
            MemberWallet wallet = memberWalletService.findByCoinUnitAndMemberId(order.getCoinSymbol(), memberId);
            if (wallet.getIsLock().equals(BooleanEnum.IS_TRUE)) {
                return MessageResult.success("钱包已锁定");
            }
            //判断用户钱包币额是否足够
            if (wallet.getBalance().compareTo(order.getAmount()) < 0) {
                return MessageResult.error(500, order.getCoinSymbol() + "余额不足");
            }
            //冻结余额
            MessageResult result = memberWalletService.freezeBalance(wallet, order.getAmount());
            if (result.getCode() != 0) {
                return result;
            }
        }

        order = exchangeOrderRepository.saveAndFlush(order);
        if (order != null) {
            log.info("add order:{}", order);
            return MessageResult.success("success");
        } else {
            return MessageResult.error(500, "error");
        }
    }

    /**
     * @param uid
     * @param pageNo
     * @param pageSize
     * @return
     */
    public Page<ExchangeOrder> findHistory(Long uid, String symbol, int pageNo, int pageSize) {
        Sort orders = Sort.by(new Sort.Order(Sort.Direction.DESC, "time"));
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, orders);
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("symbol", symbol, true));
        specification.add(Restrictions.eq("memberId", uid, true));
        specification.add(Restrictions.ne("status", ExchangeOrderStatus.TRADING, false));
        return exchangeOrderRepository.findAll(specification, pageRequest);
    }

    /**
     * 查询固定时间前的可删除订单
     *
     * @param beforeTime
     * @return
     */
    public List<ExchangeOrder> queryHistoryDelete(long beforeTime) {
        return exchangeOrderRepository.queryHistoryDeleteList(beforeTime);
    }

    /**
     * 删除可删除订单
     *
     * @param beforeTime
     * @return
     */
    public int deleteHistory(long beforeTime) {
        return exchangeOrderRepository.deleteHistory(beforeTime);
    }

    /**
     * 个人中心历史委托
     *
     * @param uid
     * @param symbol
     * @param type
     * @param status
     * @param startTime
     * @param endTime
     * @param pageNo
     * @param pageSize
     * @return
     */
    public Page<ExchangeOrder> findPersonalHistory(Long uid, String symbol, ExchangeOrderType type, ExchangeOrderStatus status, String startTime, String endTime, ExchangeOrderDirection direction, int pageNo, int pageSize) {
        Sort orders = Sort.by(new Sort.Order(Sort.Direction.DESC, "time"));
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, orders);
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        if (StringUtils.isNotEmpty(symbol)) {
            specification.add(Restrictions.eq("symbol", symbol, true));
        }
        if (type != null && StringUtils.isNotEmpty(type.toString())) {
            specification.add(Restrictions.eq("type", type, true));
        }
        if (direction != null && StringUtils.isNotEmpty(direction.toString())) {
            specification.add(Restrictions.eq("direction", direction, true));
        }
        specification.add(Restrictions.eq("memberId", uid, true));
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            specification.add(Restrictions.gte("time", Long.valueOf(startTime), true));
            specification.add(Restrictions.lte("time", Long.valueOf(endTime), true));
        }

        if (status == null) {
            specification.add(Restrictions.ne("status", ExchangeOrderStatus.TRADING, false));
        } else {
            specification.add(Restrictions.eq("status", status, true));
        }

        return exchangeOrderRepository.findAll(specification, pageRequest);
    }


    /**
     * 个人中心当前委托
     *
     * @param uid
     * @param symbol
     * @param type
     * @param startTime
     * @param endTime
     * @param pageNo
     * @param pageSize
     * @return
     */
    public Page<ExchangeOrder> findPersonalCurrent(Long uid, String symbol, ExchangeOrderType type, String startTime, String endTime, ExchangeOrderDirection direction, int pageNo, int pageSize) {
        Sort orders = Sort.by(new Sort.Order(Sort.Direction.DESC, "time"));
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, orders);
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        if (StringUtils.isNotEmpty(symbol)) {
            specification.add(Restrictions.eq("symbol", symbol, true));
        }
        if (type != null && StringUtils.isNotEmpty(type.toString())) {
            specification.add(Restrictions.eq("type", type, true));
        }
        specification.add(Restrictions.eq("memberId", uid, false));
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            specification.add(Restrictions.gte("time", Long.valueOf(startTime), true));
            specification.add(Restrictions.lte("time", Long.valueOf(endTime), true));
        }
        if (direction != null && StringUtils.isNotEmpty(direction.toString())) {
            specification.add(Restrictions.eq("direction", direction, true));
        }
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        return exchangeOrderRepository.findAll(specification, pageRequest);
    }

    /**
     * 查询当前交易中的委托
     *
     * @param uid
     * @param pageNo
     * @param pageSize
     * @return
     */
    public Page<ExchangeOrder> findCurrent(Long uid, String symbol, int pageNo, int pageSize) {
        Sort orders = Sort.by(new Sort.Order(Sort.Direction.DESC, "time"));
        PageRequest pageRequest = PageRequest.of(pageNo, pageSize, orders);
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("symbol", symbol, true));
        specification.add(Restrictions.eq("memberId", uid, false));
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        return exchangeOrderRepository.findAll(specification, pageRequest);
    }


    /**
     * 处理交易匹配
     *
     * @param trade
     * @param secondReferrerAward 二级推荐人是否返回佣金 true 返回佣金
     * @return
     * @throws Exception
     */
    @Transactional
    public MessageResult processExchangeTrade(ExchangeTrade trade, boolean secondReferrerAward) throws Exception {
        log.info("processExchangeTrade,trade = {}", trade);
        ExchangeCoin coin = exchangeCoinService.findBySymbol(trade.getSymbol());
        if (coin == null) {
            log.error("order not found");
            return MessageResult.error(500, "invalid trade symbol {}" + trade.getSymbol());
        }
        ExchangeOrder buyOrder = exchangeOrderRepository.findByOrderId(trade.getBuyOrderId());
        ExchangeOrder sellOrder = exchangeOrderRepository.findByOrderId(trade.getSellOrderId());
        if (buyOrder == null || sellOrder == null) {
            log.error("order not found");
            return MessageResult.error(500, "order not found");
        }
        //根据memberId锁表，保证钱包余额的正确性
        DB.query("select id from member_wallet where member_id = ? for update;", buyOrder.getMemberId());
        //处理买入订单--手续费是交易币
        processOrder(buyOrder, trade, coin, secondReferrerAward);
        if (!buyOrder.getMemberId().equals(sellOrder.getMemberId())) {
            DB.query("select id from member_wallet where member_id = ? for update;", sellOrder.getMemberId());
        }
        //出入卖出订单--手续费是基准币
        processOrder(sellOrder, trade, coin, secondReferrerAward);
        return MessageResult.success("process success");
    }

    /**
     * 对发生交易的委托处理相应的钱包
     *
     * @param order               委托订单
     * @param trade               交易详情
     * @param coin                交易币种信息，包括手续费 交易币种信息等等
     * @param secondReferrerAward 二级推荐人是否返佣
     * @return
     */
    public void processOrder(ExchangeOrder order, ExchangeTrade trade, ExchangeCoin coin, boolean secondReferrerAward) {
        //添加成交详情
        ExchangeOrderDetail orderDetail = new ExchangeOrderDetail();
        orderDetail.setOrderId(order.getOrderId());
        orderDetail.setPrice(order.getPrice());
        orderDetail.setAmount(order.getAmount());
        orderDetail.setTime(System.currentTimeMillis());

        //成交额，手续费
        BigDecimal turnover, fee;
        if (trade.getDirection() == ExchangeOrderDirection.BUY) {
            turnover = trade.getBuyTurnover();
            //买入单手续费为交易币
            fee = trade.getAmount().multiply(coin.getFee());
        } else {
            turnover = trade.getSellTurnover();
            //卖出单手续费为基准币
            fee = turnover.multiply(coin.getFee());
        }
        // ID为1的用户为机器人，ID为10001的用户为超级管理员，不收取手续费
        if(order.getMemberId() == 1 || order.getMemberId() == 10001) {
            fee = BigDecimal.ZERO;
        }
        orderDetail.setTurnover(turnover);
        orderDetail.setFee(fee);
        exchangeOrderDetailService.save(orderDetail);

    }

    public List<ExchangeOrderDetail> getAggregation(String orderId) {
        return exchangeOrderDetailService.findAllByOrderId(orderId);
    }


    /**
     * 渠道币币交易返佣  新版本 方案
     * @param member
     * @param symbol
     * @param fee
     */
//    public void processChannelReward(Member member,String symbol,BigDecimal fee){
//        MemberWallet channelWallet =  memberWalletService.findByCoinUnitAndMemberId(symbol,member.getChannelId());
//        if(channelWallet != null && fee.compareTo(BigDecimal.ZERO) > 0 ){
//            BigDecimal amount = fee.multiply(channelExchangeRate);
//            memberWalletService.increaseBalance(channelWallet.getId(),amount);
//            MemberTransaction memberTransaction = new MemberTransaction();
//            memberTransaction.setAmount(amount);
//            memberTransaction.setFee(BigDecimal.ZERO);
//            memberTransaction.setMemberId(member.getChannelId());
//            memberTransaction.setSymbol(symbol);
//            memberTransaction.setType(TransactionType.CHANNEL_AWARD);
//            transactionService.save(memberTransaction);
//        }
//    }

    /**
     * 交易手续费返佣金
     *
     * @param fee                 手续费
     * @param member              订单拥有者
     * @param incomeSymbol        币种
     * @param secondReferrerAward 二级推荐人是否返佣控制
     */
    public void promoteReward(BigDecimal fee, Member member, String incomeSymbol, boolean secondReferrerAward) {
        RewardPromotionSetting rewardPromotionSetting = rewardPromotionSettingService.findByType(PromotionRewardType.EXCHANGE_TRANSACTION);
        if (rewardPromotionSetting != null && member.getInviterId() != null) {
            if (!(DateUtil.diffDays(new Date(), member.getRegistrationTime()) > rewardPromotionSetting.getEffectiveTime())) {
                Member member1 = memberService.findOne(member.getInviterId());
                MemberWallet memberWallet = memberWalletService.findByCoinUnitAndMemberId(incomeSymbol, member1.getId());
                JSONObject jsonObject = JSONObject.parseObject(rewardPromotionSetting.getInfo());
                BigDecimal reward = BigDecimalUtils.mulRound(fee, BigDecimalUtils.getRate(jsonObject.getBigDecimal("one")), 8);
                if (reward.compareTo(BigDecimal.ZERO) > 0) {
                    memberWalletService.increaseBalance(memberWallet.getId(), reward);
                    MemberTransaction memberTransaction = new MemberTransaction();
                    memberTransaction.setAmount(reward);
                    memberTransaction.setFee(BigDecimal.ZERO);
                    memberTransaction.setMemberId(member1.getId());
                    memberTransaction.setSymbol(incomeSymbol);
                    memberTransaction.setType(TransactionType.PROMOTION_AWARD);
                    memberTransaction.setDiscountFee("0");
                    memberTransaction.setRealFee("0");
                    memberTransaction = transactionService.save(memberTransaction);
                    RewardRecord rewardRecord1 = new RewardRecord();
                    rewardRecord1.setAmount(reward);
                    rewardRecord1.setCoin(memberWallet.getCoin());
                    rewardRecord1.setMember(member1);
                    rewardRecord1.setRemark(rewardPromotionSetting.getType().getCnName());
                    rewardRecord1.setType(RewardRecordType.PROMOTION);
                    rewardRecordService.save(rewardRecord1);
                }

                // 控制推荐人推荐是否返佣 等于false是二级推荐人不返佣
                if (secondReferrerAward == false) {
                    log.info("控制字段 : secondReferrerAward ={} , 跳过二级推荐人返佣", secondReferrerAward);
                    return;
                }
                if (member1.getInviterId() != null && !(DateUtil.diffDays(new Date(), member1.getRegistrationTime()) > rewardPromotionSetting.getEffectiveTime())) {
                    Member member2 = memberService.findOne(member1.getInviterId());
                    MemberWallet memberWallet1 = memberWalletService.findByCoinUnitAndMemberId(incomeSymbol, member2.getId());
                    BigDecimal reward1 = BigDecimalUtils.mulRound(fee, BigDecimalUtils.getRate(jsonObject.getBigDecimal("two")), 8);
                    if (reward1.compareTo(BigDecimal.ZERO) > 0) {
                        //memberWallet1.setBalance(BigDecimalUtils.add(memberWallet1.getBalance(), reward));
                        memberWalletService.increaseBalance(memberWallet1.getId(), reward);
                        MemberTransaction memberTransaction = new MemberTransaction();
                        memberTransaction.setAmount(reward1);
                        memberTransaction.setFee(BigDecimal.ZERO);
                        memberTransaction.setMemberId(member2.getId());
                        memberTransaction.setSymbol(incomeSymbol);
                        memberTransaction.setType(TransactionType.PROMOTION_AWARD);
                        transactionService.save(memberTransaction);

                        RewardRecord rewardRecord1 = new RewardRecord();
                        rewardRecord1.setAmount(reward1);
                        rewardRecord1.setCoin(memberWallet1.getCoin());
                        rewardRecord1.setMember(member2);
                        rewardRecord1.setRemark(rewardPromotionSetting.getType().getCnName());
                        rewardRecord1.setType(RewardRecordType.PROMOTION);
                        rewardRecordService.save(rewardRecord1);
                    }
                }
            }
        }
    }

    /**
     * 查询所有未完成的挂单
     *
     * @param symbol 交易对符号
     * @return
     */
    public List<ExchangeOrder> findAllTradingOrderBySymbol(String symbol) {
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("symbol", symbol, false));
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        return exchangeOrderRepository.findAll(specification);
    }

    @Override
    public List<ExchangeOrder> findAll() {
        return exchangeOrderRepository.findAll();
    }

    public ExchangeOrder findOne(String id) {
        return exchangeOrderRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResult<ExchangeOrder> queryWhereOrPage(List<Predicate> predicates, Integer pageNo, Integer pageSize) {
        List<ExchangeOrder> list;
        JPAQuery<ExchangeOrder> jpaQuery = queryFactory.selectFrom(QExchangeOrder.exchangeOrder);
        if (predicates != null) {
            jpaQuery.where(predicates.toArray(new BooleanExpression[predicates.size()]));
        }
        jpaQuery.orderBy(QExchangeOrder.exchangeOrder.time.desc());
        if (pageNo != null && pageSize != null) {
            list = jpaQuery.offset((pageNo - 1) * pageSize).limit(pageSize).fetch();
        } else {
            list = jpaQuery.fetch();
        }
        PageResult<ExchangeOrder> result = new PageResult<>(list, jpaQuery.fetchCount());
        result.setNumber(pageNo);
        result.setSize(pageSize);
        return result;
    }

    /**
     * 订单交易完成
     *
     * @param orderId
     * @return
     */
    @Transactional
    public MessageResult tradeCompleted(String orderId, BigDecimal tradedAmount, BigDecimal turnover) {
        ExchangeOrder order = exchangeOrderRepository.findByOrderId(orderId);
        if (order.getStatus() != ExchangeOrderStatus.TRADING) {
            return MessageResult.error(500, "invalid order(" + orderId + "),not trading status");
        }
        order.setTradedAmount(tradedAmount);
        order.setTurnover(turnover);
        order.setStatus(ExchangeOrderStatus.COMPLETED);
        order.setCompletedTime(Calendar.getInstance().getTimeInMillis());
        exchangeOrderRepository.saveAndFlush(order);

        //处理用户钱包,对冻结作处理，剩余成交额退回
        orderRefund(order, tradedAmount, turnover);

        return MessageResult.success("tradeCompleted success");
    }

    /**
     * 委托退款，如果取消订单或成交完成有剩余
     *
     * @param order
     * @param tradedAmount
     * @param turnover
     */
    public void orderRefund(ExchangeOrder order, BigDecimal tradedAmount, BigDecimal turnover) {

    }

    /**
     * 取消订单
     *
     * @param orderId 订单编号
     * @return
     */
    @Transactional
    public MessageResult cancelOrder(String orderId, BigDecimal tradedAmount, BigDecimal turnover) {
        ExchangeOrder order = findOne(orderId);
        if (order == null) {
            return MessageResult.error("order not exists");
        }
        if (order.getStatus() != ExchangeOrderStatus.TRADING) {
            return MessageResult.error(500, "order not in trading");
        }
        order.setTradedAmount(tradedAmount);
        order.setTurnover(turnover);
        order.setStatus(ExchangeOrderStatus.CANCELED);
        order.setCanceledTime(Calendar.getInstance().getTimeInMillis());
        //未成交的退款
        orderRefund(order, tradedAmount, turnover);
        return MessageResult.success();
    }


    /**
     * 获取某交易对当日已取消次数
     *
     * @param uid
     * @param symbol
     * @return
     */
    public long findTodayOrderCancelTimes(Long uid, String symbol) {
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("symbol", symbol, false));
        specification.add(Restrictions.eq("memberId", uid, false));
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.CANCELED, false));
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTick = calendar.getTimeInMillis();
        calendar.add(Calendar.HOUR_OF_DAY, 24);
        long endTick = calendar.getTimeInMillis();
        specification.add(Restrictions.gte("canceledTime", startTick, false));
        specification.add(Restrictions.lt("canceledTime", endTick, false));
        return exchangeOrderRepository.count(specification);
    }

    /**
     * 查询当前正在交易的订单数量
     *
     * @param uid
     * @param symbol
     * @return
     */
    public long findCurrentTradingCount(Long uid, String symbol) {
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("symbol", symbol, false));
        specification.add(Restrictions.eq("memberId", uid, false));
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        return exchangeOrderRepository.count(specification);
    }

    /**
     * 查询指定代币对方向的委托数
     *
     * @param uid       用户id
     * @param symbol    代币对
     * @param direction 方向（买入/卖出）
     * @return
     */
    public long findCurrentTradingCount(Long uid, String symbol, ExchangeOrderDirection direction) {
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("symbol", symbol, false));
        specification.add(Restrictions.eq("memberId", uid, false));
        specification.add(Restrictions.eq("direction", direction, false));
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        return exchangeOrderRepository.count(specification);
    }

    public List<ExchangeOrder> findOvertimeOrder(String symbol, int maxTradingTime) {
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        specification.add(Restrictions.eq("symbol", symbol, false));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -maxTradingTime);
        long tickTime = calendar.getTimeInMillis();
        specification.add(Restrictions.lt("time", tickTime, false));
        return exchangeOrderRepository.findAll(specification);
    }

    /**
     * 查询符合状态的订单
     *
     * @param cancelTime
     * @return
     */
    public List<ExchangeOrder> queryExchangeOrderByTime(long cancelTime) {
        return exchangeOrderRepository.queryExchangeOrderByTime(cancelTime);
    }

    public List<ExchangeOrder> queryExchangeOrderByTimeById(long cancelTime, long sellMemberId, long buyMemberId) {
        return exchangeOrderRepository.queryExchangeOrderByTimeById(cancelTime, sellMemberId, buyMemberId);
    }

    /**
     * API 添加订单接口
     *
     * @param memberId
     * @param order
     * @return
     */
    @Transactional
    public String addOrderForApi(Long memberId, ExchangeOrder order) {
        order.setTime(Calendar.getInstance().getTimeInMillis());
        order.setStatus(ExchangeOrderStatus.TRADING);
        order.setTradedAmount(BigDecimal.ZERO);
        order.setOrderId(GeneratorUtil.getOrderId("E"));
        log.info("add order:{}", order);
        if (order.getDirection() == ExchangeOrderDirection.BUY) {
            MemberWallet wallet = memberWalletService.findByCoinUnitAndMemberId(order.getBaseSymbol(), memberId);
            BigDecimal turnover;
            if (order.getType() == ExchangeOrderType.MARKET_PRICE) {
                turnover = order.getAmount();
            } else {
                turnover = order.getAmount().multiply(order.getPrice());
            }
            if (wallet.getBalance().compareTo(turnover) < 0) {
                return null;
            } else {
                memberWalletService.freezeBalance(wallet, turnover);
                //wallet.setBalance(wallet.getBalance().subtract(turnover));
                //wallet.setFrozenBalance(wallet.getFrozenBalance().add(turnover));
            }
        } else if (order.getDirection() == ExchangeOrderDirection.SELL) {
            MemberWallet wallet = memberWalletService.findByCoinUnitAndMemberId(order.getCoinSymbol(), memberId);
            if (wallet.getBalance().compareTo(order.getAmount()) < 0) {
                return null;
            } else {
                memberWalletService.freezeBalance(wallet, order.getAmount());
                //wallet.setBalance(wallet.getBalance().subtract(order.getAmount()));
                //wallet.setFrozenBalance(wallet.getFrozenBalance().add(order.getAmount()));
            }
        }
        order = exchangeOrderRepository.saveAndFlush(order);
        return order.getOrderId();
    }

    /**
     * Api 查询订单接口
     *
     * @param memberId
     * @param symbol
     * @param direction
     * @return
     */
    public Page<ExchangeOrder> findCurrentTradingOrderForApi(long memberId, String symbol, ExchangeOrderDirection direction, int pageNo, int pageSize) {
        Sort orders = Sort.by(new Sort.Order(Sort.Direction.DESC, "time"));
        PageRequest pageRequest = PageRequest.of(pageNo, pageSize, orders);
        Criteria<ExchangeOrder> specification = new Criteria<ExchangeOrder>();
        specification.add(Restrictions.eq("symbol", symbol, false));
        specification.add(Restrictions.eq("memberId", memberId, false));
        specification.add(Restrictions.eq("status", ExchangeOrderStatus.TRADING, false));
        specification.add(Restrictions.eq("direction", direction, false));
        return exchangeOrderRepository.findAll(specification, pageRequest);
    }


    /**
     * 强制取消订单,在撮合中心和数据库订单不一致的情况下使用
     *
     * @param order
     */
    @Transactional
    public void forceCancelOrder(ExchangeOrder order) {
        List<ExchangeOrderDetail> details = exchangeOrderDetailService.findAllByOrderId(order.getOrderId());
        BigDecimal tradedAmount = BigDecimal.ZERO;
        BigDecimal turnover = BigDecimal.ZERO;
        for (ExchangeOrderDetail trade : details) {
            tradedAmount = tradedAmount.add(trade.getAmount());
            turnover = turnover.add(trade.getAmount().multiply(trade.getPrice()));
        }
        order.setTradedAmount(tradedAmount);
        order.setTurnover(turnover);
        if (order.isCompleted()) {
            tradeCompleted(order.getOrderId(), order.getTradedAmount(), order.getTurnover());
        } else {
            cancelOrder(order.getOrderId(), order.getTradedAmount(), order.getTurnover());
        }
    }
}
