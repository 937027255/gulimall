package com.atguigu.gulimail.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.mq.OrderVo;
import com.atguigu.common.mq.StockDetailTo;
import com.atguigu.common.mq.StockLockedTo;
import com.atguigu.common.to.SkuStockVo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimail.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimail.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimail.ware.feign.OrderFeignService;
import com.atguigu.gulimail.ware.feign.ProductFeignService;
import com.atguigu.gulimail.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimail.ware.service.WareOrderTaskService;
import com.atguigu.gulimail.ware.vo.OrderItemVo;
import com.atguigu.gulimail.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimail.ware.dao.WareSkuDao;
import com.atguigu.gulimail.ware.entity.WareSkuEntity;
import com.atguigu.gulimail.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@RabbitListener(queues = "stock.release.stock.queue")
@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;
    @Autowired
    WareOrderTaskService wareOrderTaskService;
    @Autowired
    OrderFeignService orderFeignService;
    @Autowired
    WareSkuDao wareSkuDao;



    private void unLockStock(Long skuId,Long wardId,Integer num,Long taskDetailId) {
        //库存解锁数据库操作
        wareSkuDao.unLockStock(skuId,wardId,num,taskDetailId);
        WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity();
        detailEntity.setId(taskDetailId);
        detailEntity.setLockStatus(2);
        wareOrderTaskDetailService.updateById(detailEntity);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String wareId = (String) params.get("wareId");
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(wareId)) {
            wrapper.eq("ware_id",wareId);
        }
        if (!StringUtils.isEmpty(skuId)) {
            wrapper.eq("sku_id",skuId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }

    @Override
    public void addWare(Long skuId, Long wareId, Integer skuNum) {
        //先进行查询，如果没有就是新增操作，如果有结果就是一个修改操作
        Integer count = baseMapper.selectCount(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (count > 0) {
            baseMapper.addWare(skuId,wareId,skuNum);
        } else {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            try {
                //根据skuId找到对应的名字
                R info = productFeignService.info(skuId);
                Map<String,Object> skuInfo = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            } catch (Exception e) {
                //新增库存数据
                baseMapper.insert(wareSkuEntity);
            }
            baseMapper.insert(wareSkuEntity);
        }

    }

    @Override
    public List<SkuStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuStockVo> stockVos = null;
        try {
            stockVos  = skuIds.stream().map(skuId -> {
                SkuStockVo vo = new SkuStockVo();
                //查询当前的库存总量
                Long count = baseMapper.getSkuStock(skuId);
                vo.setSkuId(skuId);
                if (count == null) {
                    vo.setHasStock(false);
                } else {
                    vo.setHasStock(count > 0);
                }
                return vo;
            }).collect(Collectors.toList());
            return stockVos;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stockVos;
    }

    /**
     * 为某个订单锁定库存
     *
     * 库存解锁的场景：
     * 1)、下订单成功，订单过期没有支付被系统自动取消、被用户手动取消，都需要解锁库存
     * 2)、下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚，之前锁定的库存也需要自动解锁
     * @param vo
     * @return
     */
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        /**
         * 在锁库存之前，先将库存的工作单保存下来
         */
        //将订单号保存起来
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskService.save(wareOrderTaskEntity);

        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> collectVos = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            //按照skuId查询哪些库中有货
            List<Long> wareIds = baseMapper.listWareIdHasStock(skuId);
            stock.setWareIds(wareIds);
            stock.setNum(item.getCount());
            return stock;
        }).collect(Collectors.toList());
        //锁定库存
        for (SkuWareHasStock stock : collectVos) {
            //依次判断每一个库中能否成功扣除库存
            Long skuId = stock.getSkuId();
            List<Long> wareIds = stock.getWareIds();
            Boolean skuFlag = false;
            if (wareIds == null || wareIds.size() == 0 ) {
                throw new NoStockException(skuId);
            }
            //TODO 如果每一个商品都锁定成功了，当前商品锁定了几件的工作单发给MQ
            //TODO 锁定失败: 前面保存的工作单信息就回滚了，但是这个时候消息已经发送出去了，即使要解锁记录但是去数据库查询不到详情内容，也就不用解锁，所以不影响业务逻辑
            for (Long wareId : wareIds) {
                //对每一个商品，去每一个库中查找库存
                int count = baseMapper.lockStock(skuId,wareId,stock.getNum());
                if (count == 1) {
                    //有库存，无需继续查询是否有库存了，跳出即可
                    skuFlag = true;
                    //TODO 只要锁定库存成功就要给mq发送锁定成功的消息，放入到延时队列中去
                    //库存锁定成功之后添加锁定库存的详情信息
                    WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity(null,skuId,"",stock.getNum(),wareOrderTaskEntity.getId(),wareId,1);
                    wareOrderTaskDetailService.save(detailEntity);
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(wareOrderTaskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(detailEntity,stockDetailTo);
                    stockLockedTo.setDetailTo(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange","stock.locked",stockLockedTo);
                    break;
                } else {
                    //没有库存了,在下一个库存锁定

                }
            }
            //如果所有的仓库都没有成功锁库存，那就抛异常不用再继续下一个商品了
            if (skuFlag == false) {
                throw new NoStockException(skuId);
            }
        }
        //到这里说明全部都锁定成功了
        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {
        System.out.println("收到解锁库存的消息");
        Long id = to.getId();//工作单id
        StockDetailTo detail = to.getDetailTo();
        Long detailId = detail.getId();
        Long skuId = detail.getSkuId();
        //解锁库存
        //TODO 判断一下如果是下订单成功了，后续的业务失败导致事务回滚，那就要进行库存解锁
        //TODO 但是如果在锁定库存阶段就失败了，那就不回有订单信息了，这种情况无需解锁，所以需要先判断这个工作单是否存在于数据库中
        //一、查询数据库中没有：库存锁定失败，库存回滚，这种情况不需要解锁库存
        /**
         * 二、查询数据库有：库存锁定成功
         * 1、没有这个订单，必须解锁
         * 2、有这个订单，并不意味着就能解锁库存，如果订单状态是已经取消的状态就需要解锁库存，否则不需要解锁库存
         */

        WareOrderTaskDetailEntity detailEntity = wareOrderTaskDetailService.getById(detailId);
        //如果没有这个任务详情说明扣减库存都失败了，就不用进行事务补偿策略了
        if (detailEntity != null) {
            //工作单存在，进行解锁逻辑
            //查看订单信息是否存在
            //获取订单号,注意要从数据库中取
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            R r = orderFeignService.getOrderStatus(orderSn);
            //查看整个订单的状态
            if (r.getCode() == 0) {
                //订单数据返回成功
                OrderVo data = (OrderVo) r.getData(new TypeReference<OrderVo>() {});
                if (data == null || data.getStatus() == 4) {
                    //如果没有查到订单的数据或者状态是已经取消，就需要进行解锁操作
                    if (detailEntity.getLockStatus() == 1) {
                        unLockStock(skuId,detail.getWareId(),detail.getSkuNum(),detailId);
                    }
                    //注意解锁成功一定要手动ack
//                    channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                }
            } else {
                //消息拒绝之后重新放到队列，让别人继续消费
//                channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
                throw new RuntimeException("远程服务失败");
            }
        } else {
            //无需解锁
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }

    /**
     * 防止订单服务卡顿导致订单状态一直修改不了，库存优先到期，查订单状态为新建没有解锁库存，后续库存一直解锁不了
     * @param orderVo
     */
    @Transactional
    @Override
    public void unlockStock(OrderVo orderVo) {
        String orderSn = orderVo.getOrderSn();
        //查询一下最新的库存解锁状态，防止重复解锁
        WareOrderTaskEntity wareOrderTaskEntity  = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);
        //获取到工作单的id
        Long id = wareOrderTaskEntity.getId();
        //找到工作单中所有没有解锁的库存
        List<WareOrderTaskDetailEntity> entities = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().eq("task_id", id).eq("lock_status", 1));
        entities.stream().forEach(item->unLockStock(item.getSkuId(), item.getWareId(),item.getSkuNum(),item.getId()));

    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareIds;
    }

}