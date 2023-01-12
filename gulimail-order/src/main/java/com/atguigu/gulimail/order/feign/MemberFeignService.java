package com.atguigu.gulimail.order.feign;

import com.atguigu.gulimail.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("gulimail-member")
public interface MemberFeignService {
    @GetMapping("/member/memberreceiveaddress/{memberId}/address")
    public List<MemberAddressVo> getAddresses(@PathVariable Long memberId);
}
