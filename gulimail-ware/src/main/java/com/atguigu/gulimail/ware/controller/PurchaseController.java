package com.atguigu.gulimail.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.atguigu.gulimail.ware.vo.MergePurchaseVo;
import com.atguigu.gulimail.ware.vo.PurchaseDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimail.ware.entity.PurchaseEntity;
import com.atguigu.gulimail.ware.service.PurchaseService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;



/**
 * 采购信息
 *
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 20:21:11
 */
@RestController
@RequestMapping("ware/purchase")
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:purchase:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = purchaseService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * 展示没有被领取的采购单
     * @param params
     * @return
     */
    @GetMapping("/unreceive/list")
    public R getUnreceivePurchaseList(@RequestParam Map<String, Object> params) {
        PageUtils page = purchaseService.queryPageUnreceivePurchase(params);

        return R.ok().put("page", page);
    }

    /**
     * 将多个购买清单合并到一个采购单中
     * @param vo
     * @return
     */
    @PostMapping("/merge")
    public R merge(@RequestBody MergePurchaseVo vo) {
        purchaseService.mergePurchase(vo);
        return R.ok();
    }

    /**
     * 领取采购单
     * @param purchaseList
     * @return
     */
    @PostMapping("/received")
    public R receivedPurchse(@RequestBody List<Long> purchaseList) {
        purchaseService.receivedPurchse(purchaseList);
        return R.ok();
    }
    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:purchase:info")
    public R info(@PathVariable("id") Long id){
		PurchaseEntity purchase = purchaseService.getById(id);

        return R.ok().put("purchase", purchase);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:purchase:save")
    public R save(@RequestBody PurchaseEntity purchase){
		purchaseService.save(purchase);

        return R.ok();
    }
    @PostMapping("/done")
    public R done(@RequestBody PurchaseDoneVo vo) {
        purchaseService.donePurchase(vo);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:purchase:update")
    public R update(@RequestBody PurchaseEntity purchase){
		purchaseService.updateById(purchase);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:purchase:delete")
    public R delete(@RequestBody Long[] ids){
		purchaseService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
