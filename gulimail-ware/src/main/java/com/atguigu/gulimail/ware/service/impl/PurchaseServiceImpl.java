package com.atguigu.gulimail.ware.service.impl;

import com.atguigu.common.consts.PurchaseDetailStatusConst;
import com.atguigu.common.consts.PurchaseStatusConst;
import com.atguigu.gulimail.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimail.ware.service.PurchaseDetailService;
import com.atguigu.gulimail.ware.service.WareSkuService;
import com.atguigu.gulimail.ware.vo.MergePurchaseVo;
import com.atguigu.gulimail.ware.vo.PurchaseDetailVo;
import com.atguigu.gulimail.ware.vo.PurchaseDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimail.ware.dao.PurchaseDao;
import com.atguigu.gulimail.ware.entity.PurchaseEntity;
import com.atguigu.gulimail.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseService purchaseService;
    @Autowired
    PurchaseDetailService purchaseDetailService;
    @Autowired
    WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceivePurchase(Map<String, Object> params) {

        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status",0).or().eq("status",1)
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void mergePurchase(MergePurchaseVo vo) {
        //?????????????????????????????????
        Long purchaseId = vo.getPurchaseId();
        if (purchaseId == null) {
            //???????????????????????????????????????????????????????????????????????????
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            purchaseEntity.setStatus(PurchaseStatusConst.CREATED.getCode());
            purchaseService.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }
        //????????????????????????????????????
        List<Long> items = vo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = items.stream().map(i -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            purchaseDetailEntity.setId(i);
            purchaseDetailEntity.setStatus(PurchaseDetailStatusConst.ASSIGNED.getCode());
            return purchaseDetailEntity;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(collect);
        //??????????????????????????????
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(finalPurchaseId);
        purchaseEntity.setUpdateTime(new Date());
        purchaseService.updateById(purchaseEntity);
    }

    @Transactional
    @Override
    public void receivedPurchse(List<Long> purchaseList) {
        //??????????????????id?????????????????????????????????0???1?????????????????????
        List<PurchaseEntity> purchases = purchaseList.stream().map(purchase -> {
            PurchaseEntity purchaseEntity = purchaseService.getById(purchase);
            return purchaseEntity;
        }).filter(pur -> {
            if (pur.getStatus() == PurchaseStatusConst.CREATED.getCode() || pur.getStatus() == PurchaseStatusConst.ASSIGNED.getCode())
                return true;
            else
                return false;
        }).map(p->{
            p.setStatus(PurchaseStatusConst.RECEIVED.getCode());
            p.setUpdateTime(new Date());
            return p;
        }).collect(Collectors.toList());
        purchaseService.updateBatchById(purchases);
        //???????????????????????????????????????purchase_id??????????????????status????????????
        List<PurchaseDetailEntity> purchaseDetails = purchaseDetailService.list(new QueryWrapper<PurchaseDetailEntity>().in("purchase_id", purchaseList));
        List<PurchaseDetailEntity> collect = purchaseDetails.stream().map(p -> {
            p.setStatus(PurchaseDetailStatusConst.BUYIONG.getCode());
            return p;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(collect);
    }
    @Transactional
    @Override
    public void donePurchase(PurchaseDoneVo vo) {

        Long purchaseId = vo.getId();
        List<PurchaseDetailVo> items = vo.getItems();

        //1???1?????????????????????????????????????????????
        boolean flag = true;
        for (PurchaseDetailVo item : items) {
            if (item.getStatus() == PurchaseDetailStatusConst.FAILED.getCode()) {
                flag = false;
            }
        }
        Integer purchaseStatus = PurchaseStatusConst.FINISHED.getCode();
        if (flag == false)
            purchaseStatus = PurchaseStatusConst.ERROR.getCode();
        //1???2??????????????????id????????????????????????
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setUpdateTime(new Date());
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setStatus(purchaseStatus);
        purchaseService.updateById(purchaseEntity);

        //2??????????????????id??????????????????????????????????????????
        List<PurchaseDetailEntity> details = items.stream().map(item -> {
            //?????????????????????????????????
            PurchaseDetailEntity purchaseDetail = purchaseDetailService.getOne(new QueryWrapper<PurchaseDetailEntity>().eq("id", item.getItemId()).eq("purchase_id", purchaseId));
            //???????????????????????????
            purchaseDetail.setStatus(item.getStatus());
            //????????????????????????????????????????????????????????????
            if (item.getStatus() == PurchaseDetailStatusConst.FINISHED.getCode()) {
                //3?????????????????????
                //????????????????????????????????????????????????????????????????????????????????????
                Long detailId = item.getItemId();
                PurchaseDetailEntity detailEntity = purchaseDetailService.getById(detailId);
                wareSkuService.addWare(detailEntity.getSkuId(),detailEntity.getWareId(),detailEntity.getSkuNum());
            }
            return purchaseDetail;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(details);



    }

}