package com.atguigu.gulimail.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.mq.OrderVo;
import com.atguigu.common.mq.SeckillOrderTo;
import com.atguigu.common.to.SkuStockVo;
import com.atguigu.common.to.session.MemberResponseVo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimail.order.config.AlipayTemplate;
import com.atguigu.gulimail.order.constant.OrderConstant;
import com.atguigu.gulimail.order.entity.OrderItemEntity;
import com.atguigu.gulimail.order.entity.PaymentInfoEntity;
import com.atguigu.gulimail.order.feign.CartFeignService;
import com.atguigu.gulimail.order.feign.MemberFeignService;
import com.atguigu.gulimail.order.feign.ProduceFeignService;
import com.atguigu.gulimail.order.feign.WareFeignService;
import com.atguigu.gulimail.order.ienum.OrderStatusEnum;
import com.atguigu.gulimail.order.interceptor.OrderInterceptor;
import com.atguigu.gulimail.order.service.OrderItemService;
import com.atguigu.gulimail.order.service.PaymentInfoService;
import com.atguigu.gulimail.order.to.OrderCreateTo;
import com.atguigu.gulimail.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimail.order.dao.OrderDao;
import com.atguigu.gulimail.order.entity.OrderEntity;
import com.atguigu.gulimail.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    public static ThreadLocal<OrderSubmitVo> orderSubmitInfo = new ThreadLocal<>();

    @Autowired
    ThreadPoolExecutor executor;
    @Autowired
    OrderItemService orderItemService;

    @Autowired
    MemberFeignService memberFeignService;
    @Autowired
    CartFeignService cartFeignService;
    @Autowired
    WareFeignService wareFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProduceFeignService produceFeignService;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    PaymentInfoService paymentInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 验证订单信息：检查收货人地址信息，购物项信息，库存信息，最后生成一个订单，对于此订单生成一个token放入到redis中去
     * 这里需要注意的有两个点，1、第一使用feign进行远程调用的时候会丢失掉请求头的cookie信息，所以就没有之前的登陆信息到cart模块就会被拦截
     * 需要陪只requstTemplate手动添加登陆信息到请求头中去；
     * 2、在使用异步任务编排的时候会启动新的线程，threadLocal中的内容需要手动添加，RequestContextHolder这个类属于线程共享的，将浏览器中的请求内容手动添加到异步线程中去
     * @return
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        //需要返回的vo类
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        //主线程
        MemberResponseVo loginUser = OrderInterceptor.loginUser.get();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        //分线程1
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            //通过查询gulimail-member模块获取地址信息
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> addresses = memberFeignService.getAddresses(loginUser.getId());
            confirmVo.setMemberAddressVos(addresses);
        }, executor);
        //分线程2
        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            //gulimail-cart模块找到所有的购物项信息
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> carsByUserId = cartFeignService.getCarsByUserId(loginUser.getId());
            confirmVo.setItems(carsByUserId);
        }, executor).thenRunAsync(()->{
            //找到所有的购物项信息之后去gulimail-ware模块查看是否有库存
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> skuIds = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R<List<SkuStockVo>> skuHasStock = wareFeignService.getSkuHasStock(skuIds);
            if (skuHasStock.getCode() == 0) {
                List<SkuStockVo> skuStockVos = skuHasStock.getData(new TypeReference<List<SkuStockVo>>() {});
                Map<Long, Boolean> map = skuStockVos.stream().collect(Collectors.toMap(item -> item.getSkuId(), val -> val.getHasStock()));
                confirmVo.setStocks(map);
            }
        },executor);
        CompletableFuture.allOf(addressFuture,cartFuture).get();
        //获取用户的积分信息
        Integer integration = loginUser.getIntegration();
        confirmVo.setIntegration(integration);
        //加入幂等性校验功能，给每一个订单创建一个Token令牌防止重复
        String token = UUID.randomUUID().toString().replace("-", "");
        confirmVo.setOrderToken(token);
        //将令牌加入到redis中去
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getId(),token,30, TimeUnit.MINUTES);

        return confirmVo;
    }

    /**接口幂等性：
         * 原子删除保证订单提交的幂等性，如果令牌相同了会立刻删除这个令牌，连续点击多余的提交就会找不到令牌从而无法继续的创建订单
         * 但是要注意删除订单和比对两个token的过程，必须是一个原子操作，不然如果同时有多个提交订单的请求进来有可能先获取到了token
         * 然后token被从redis中删除掉，依旧会通过token的验证，从而创建了多个订单，使这个接口失去幂等性，使用redis + lua 脚本解决
     * 分布式事务：
         * 远程服务失败：远程锁定库存的服务其实成功了，但是由于网络的故障没有返回，会导致订单回滚，但是库存已经锁定
         * 远程服务执行成功：但是后面的方法却出现异常，会倒是已经执行的远程请求无法回滚
         * 解决：不能使用本地事务，本地事务只能控制自己的回滚，控制不了其他业务的回滚。采用分布式事务来解决这个问题。
     * @param orderSubmitVo
     * @return
     */
//    @GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo orderSubmitVo) {
        //放入到线程中去
        orderSubmitInfo.set(orderSubmitVo);
        SubmitOrderResponseVo response = new SubmitOrderResponseVo();
        response.setCode(0);
        //前台传入的token
        String token = orderSubmitVo.getOrderToken();
        //取出来redis的token
        MemberResponseVo loginUser = OrderInterceptor.loginUser.get();
        //redis + lua 实现原子删除操作
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script,Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getId()), token);
        //原始操作-非原子
        //String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getId());
        //如果token相同就删除
        if (result == 0L) {
            //原始操作-非原子
//            redisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX);
            //说明原子删除失败，令牌验证失败，需要重新下订单
            response.setCode(1);
            return response;
        } else {
            //原子删除成功,令牌验证成功，继续下面的内容
            //创建订单号//获取购物车的购物项信息，创建订单项
            //获取收获地址信息，填充订单、计算订单总额
            OrderCreateTo order = createOrder();
            //验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = orderSubmitVo.getPayPrice();
            if (Math.abs(payAmount.subtract(payAmount).doubleValue()) < 0.01) {
                //金额比对成功
                //保存订单
                saveOrder(order);
                //锁定库存，如果失败抛异常回滚事务
                //订单号、订单项的(skuId、skuName、num)
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setSkuId(item.getSkuId());
                    return orderItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(orderItemVos);
                // TODO 远程调用锁库存操作
                R r = wareFeignService.orderLockStock(wareSkuLockVo);
                if (r.getCode() == 0) {
                    //库存锁定成功
                    // TODO 远程扣除积分
//                    int i = 10 / 0;
                    response.setOrder(order.getOrder());
                    // TODO 订单创建成功之后就往mq中发送订单的消息
                    rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order.getOrder());
                    return response;
                } else {
                    //锁定失败
                    throw new NoStockException(0L);
//                    response.setCode(3);
//                    return response;
                }
            } else {
                //金额失败返回失败状态码
                response.setCode(2);
                return response;
            }
        }
    }

    @Override
    public OrderEntity getOrderStatus(String orderSn) {
        OrderEntity orderEntity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return orderEntity;
    }

    /**
     * 关闭订单的业务逻辑
     * @param orderEntity
     */
    @Override
    public void closeOrder(OrderEntity orderEntity) {
        //每一个成功的订单都会进入到这里，需要做判断，是否支付如果没有支付才需要将订单进行关闭
        //首先去数据库中查询到这个订单的状态
        OrderEntity order = this.getById(orderEntity.getId());
        if (order.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            //订单还是新建状态->关闭订单
            OrderEntity update = new OrderEntity();
            update.setId(orderEntity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            //TODO 为了防止因为网络延迟导致的先调用库存解锁后调用订单关闭，因为库存解锁的时候发现订单还没有关闭导致的后续库存无法解锁的问题这里每关闭一个订单就需要发送一个库存解锁的消息给队列去完成库存解锁服务
            OrderVo orderVo = new OrderVo();
            BeanUtils.copyProperties(order,orderVo);
            rabbitTemplate.convertAndSend("order-event-exchange","order.release.other",orderVo);
        }
    }

    /**
     * 根据订单id构建出来订单的vo
     * @param orderSn
     * @return
     */
    @Override
    public AlipayTemplate.PayVo getOrderPay(String orderSn) {
        OrderEntity orderEntity = this.getOrderStatus(orderSn);
        AlipayTemplate.PayVo payVo = new AlipayTemplate.PayVo();

        List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity entity = orderItemEntities.get(0);
        payVo.setSubject(entity.getSkuName());
        BigDecimal amount = orderEntity.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(amount.toString());
        payVo.setOut_trade_no(orderSn);
        payVo.setBody(entity.getSkuAttrsVals());
        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        //所有调用的进来的线程必须先进行登陆
        MemberResponseVo memberResponseVo = OrderInterceptor.loginUser.get();
        Long memberId = memberResponseVo.getId();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id",memberId).orderByDesc("id")
        );
        //将每一个订单的订单项数据获得进行封装
        List<OrderEntity> orderEntityList = page.getRecords().stream().map(order -> {
            String orderSn = order.getOrderSn();
            List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
            order.setItemEntities(orderItemEntities);
            return order;
        }).collect(Collectors.toList());
        page.setRecords(orderEntityList);

        return new PageUtils(page);
    }

    /**
     * 处理支付成功的结果
     * @param vo
     * @return
     */
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        //保存交易流水
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setAlipayTradeNo(vo.getTrade_no());
        paymentInfoEntity.setOrderSn(vo.getOut_trade_no());
        paymentInfoEntity.setPaymentStatus(vo.getTrade_status());
        paymentInfoEntity.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(paymentInfoEntity);
        //修改订单状态
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            //支付成功
            String orderSn = vo.getOut_trade_no();
            this.updateOrderStauts(orderSn,OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }

    /**
     * 修改订单号的订单状态
     * @param orderSn
     * @param code
     */
    @Override
    public void updateOrderStauts(String orderSn, Integer code) {
        baseMapper.updateOrderStauts(orderSn,code);
    }

    /**
     * 创建秒杀单的信息
     * @param seckillOrderTo
     */
    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrderTo) {
        //创建订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrderTo.getOrderSn());
        orderEntity.setMemberId(seckillOrderTo.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal multiply = seckillOrderTo.getSeckillPrice().multiply(new BigDecimal(seckillOrderTo.getNum()));
        orderEntity.setPayAmount(multiply);
        this.save(orderEntity);
        //创建订单项信息
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrderSn(seckillOrderTo.getOrderSn());
        orderItem.setSkuId(seckillOrderTo.getSkuId());
        orderItem.setRealAmount(multiply);
        orderItem.setSkuPrice(seckillOrderTo.getSeckillPrice());
        orderItem.setSkuQuantity(seckillOrderTo.getNum());
        orderItemService.save(orderItem);

    }

    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        List<OrderItemEntity> orderItems = order.getOrderItems();
        this.save(orderEntity);
        orderItems.stream().forEach((item)->{
            orderItemService.save(item);
        });
        //0.71seata-all不能批量保存
//        orderItemService.saveBatch(orderItems);
    }

    /**
     * 创建返回页面的订单TO
     * @return
     */
    private OrderCreateTo createOrder() {
        //创建订单实体类
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //1、封装订单实体类
        OrderEntity orderEntity = buildOrder();
        orderCreateTo.setOrder(orderEntity);
        //2、封装购物项信息
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderEntity.getOrderSn());
        orderCreateTo.setOrderItems(orderItemEntities);
        //3、封装应付价格
        computePrice(orderEntity,orderItemEntities);
        //4、封装运费
        return orderCreateTo;

    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        //所有订单项的总体价格
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        Integer gift = 0;
        Integer growth = 0;
        for (OrderItemEntity entity : orderItemEntities) {
            //每一个订单项的总额
            BigDecimal realAmount = entity.getRealAmount();
            total = total.add(realAmount);
            //订单项优惠券
            BigDecimal couponAmount = entity.getCouponAmount();
            coupon = coupon.add(couponAmount);
            //订单项促销
            BigDecimal promotionAmount = entity.getPromotionAmount();
            promotion = promotion.add(promotionAmount);
            //订单项满减
            BigDecimal integrationAmount = entity.getIntegrationAmount();
            integration = integration.add(integrationAmount);
            //设置订单的总积分
            gift += entity.getGiftIntegration();
            growth += entity.getGiftGrowth();
        }
        //设置金额的所有信息
        orderEntity.setTotalAmount(total);
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setCouponAmount(coupon);
        orderEntity.setGrowth(growth);
        orderEntity.setIntegration(gift);

    }

    /**
     * 创建多个订单项
     * @param orderSn
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        MemberResponseVo loginUser = OrderInterceptor.loginUser.get();
        //一个用户购物车中所有选定的购物项
        List<OrderItemVo> carsByUserId = cartFeignService.getCarsByUserId(loginUser.getId());
        if (carsByUserId != null && carsByUserId.size() > 0) {
            List<OrderItemEntity> orderCarts = carsByUserId.stream().map(cart -> {
                //将购物项信息封装成一个新的实体类
                OrderItemEntity orderItemEntity = buildOrderItem(cart);
                //设置订单号
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return orderCarts;
        }
        return null;
    }

    /**
     * 创建订单项
     * @param cart
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cart) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        //封装sku信息
        orderItemEntity.setSkuId(cart.getSkuId());
        orderItemEntity.setSkuName(cart.getTitle());
        orderItemEntity.setSkuPic(cart.getImage());
        orderItemEntity.setSkuPrice(cart.getPrice());
        orderItemEntity.setSkuQuantity(cart.getCount());
        orderItemEntity.setSkuAttrsVals(StringUtils.collectionToDelimitedString(cart.getSkuAttrValues(),";"));
        //封装spu信息
        R r = produceFeignService.getSpuInfoBySkuId(cart.getSkuId());
        if (r.getCode() == 0) {
            SpuInfoVo spuInfoVo = (SpuInfoVo) r.getData(new TypeReference<SpuInfoVo>() {});
            orderItemEntity.setSpuId(spuInfoVo.getId());
            orderItemEntity.setSpuName(spuInfoVo.getSpuName());
            orderItemEntity.setSpuBrand(spuInfoVo.getBrandId().toString());
            orderItemEntity.setCategoryId(spuInfoVo.getCatalogId());
        }
        //封装积分信息
        orderItemEntity.setGiftGrowth(cart.getPrice().multiply(new BigDecimal(cart.getCount().toString())).intValue());
        orderItemEntity.setGiftIntegration(cart.getPrice().multiply(new BigDecimal(cart.getCount().toString())).intValue());

        //封装订单项的价格信息
        orderItemEntity.setPromotionAmount(new BigDecimal("0.0"));
        orderItemEntity.setCouponAmount(new BigDecimal("0.0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0.0"));
        //当前订单项的实际金额 总额-各种优惠信息 这里优惠信息都写死了，可以通过查数据库得到具体的数据
        BigDecimal orgin = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        BigDecimal real_amount = orgin.subtract(orderItemEntity.getCouponAmount()).subtract(orderItemEntity.getPromotionAmount()).subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(real_amount);

        return orderItemEntity;
    }

    /**
     * 创建订单实体
     * @return
     */
    private OrderEntity buildOrder() {
        OrderEntity orderEntity = new OrderEntity();
        //生成订单号
        String orderSn = IdWorker.getTimeId();
        orderEntity.setOrderSn(orderSn);
        OrderSubmitVo orderSubmitVo = orderSubmitInfo.get();
        //设置用户信息
        orderEntity.setMemberId(OrderInterceptor.loginUser.get().getId());
        R r = wareFeignService.fare(orderSubmitVo.getAddrId());
        if (r.getCode() == 0) {
            FareVo fareVo = (FareVo) r.getData(new TypeReference<FareVo>() {});
            //设置收货人信息
            orderEntity.setReceiverCity(fareVo.getAddress().getCity());
            orderEntity.setReceiverName(fareVo.getAddress().getName());
            orderEntity.setReceiverPhone(fareVo.getAddress().getPhone());
            orderEntity.setReceiverDetailAddress(fareVo.getAddress().getDetailAddress());
            orderEntity.setReceiverPostCode(fareVo.getAddress().getPostCode());
            orderEntity.setReceiverProvince(fareVo.getAddress().getProvince());
            orderEntity.setReceiverRegion(fareVo.getAddress().getRegion());
            //设置运费信息
            orderEntity.setFreightAmount(fareVo.getFare());
        }
        //设置订单状态为待付款
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);
        orderEntity.setDeleteStatus(0);
        return orderEntity;
    }

}