package com.atguigu.gulimail.ware.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimail.ware.feign.MemberFeignSerivce;
import com.atguigu.gulimail.ware.vo.FareVo;
import com.atguigu.gulimail.ware.vo.MemberAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimail.ware.entity.WareInfoEntity;
import com.atguigu.gulimail.ware.service.WareInfoService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;



/**
 * 仓库信息
 *
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 20:21:11
 */
@RestController
@RequestMapping("ware/wareinfo")
public class WareInfoController {
    @Autowired
    private WareInfoService wareInfoService;
    @Autowired
    private MemberFeignSerivce memberFeignSerivce;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:wareinfo:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareInfoService.queryPageByCondition(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:wareinfo:info")
    public R info(@PathVariable("id") Long id){
		WareInfoEntity wareInfo = wareInfoService.getById(id);

        return R.ok().put("wareInfo", wareInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:wareinfo:save")
    public R save(@RequestBody WareInfoEntity wareInfo){
		wareInfoService.save(wareInfo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:wareinfo:update")
    public R update(@RequestBody WareInfoEntity wareInfo){
		wareInfoService.updateById(wareInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:wareinfo:delete")
    public R delete(@RequestBody Long[] ids){
		wareInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    /**
     * 根据addrId查询出来运费信息，去
     * @param addrId
     * @return
     */
    @GetMapping("/fare")
    public R fare(@RequestParam("addrId") Long addrId) {
        R r = memberFeignSerivce.info(addrId);
        FareVo fareVo = new FareVo();
        if (r.getCode() == 0) {
            MemberAddressVo address = (MemberAddressVo) r.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() {});
            fareVo.setAddress(address);
            fareVo.setFare(new BigDecimal(String.valueOf(new Random().nextInt(10))));
        }
        return R.ok().put("data",fareVo);
    }

}
