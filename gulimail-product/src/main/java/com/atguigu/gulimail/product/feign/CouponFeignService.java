package com.atguigu.gulimail.product.feign;

import com.atguigu.common.to.MemberPriceTo;
import com.atguigu.common.to.SkuLadderTo;
import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.to.SpuBoundTo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimail-coupon")
@Component
public interface CouponFeignService {

    @PostMapping("coupon/spubounds/save")
    public R saveBounds(@RequestBody SpuBoundTo spuBounds);

    @PostMapping("coupon/skuladder/save")
    //@RequiresPermissions("coupon:skuladder:save")
    public R saveLadder(@RequestBody SkuLadderTo skuLadder);
    @PostMapping("coupon/skufullreduction/save")
    //@RequiresPermissions("coupon:skufullreduction:save")
    public R saveReduction(@RequestBody SkuReductionTo skuFullReduction);

    @PostMapping("coupon/memberprice/saveinfo")
    void saveMemberPrice(MemberPriceTo memberPriceTo);
}
