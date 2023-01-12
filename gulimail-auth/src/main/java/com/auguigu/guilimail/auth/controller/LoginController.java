package com.auguigu.guilimail.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.consts.AuthServerConst;
import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.to.session.MemberResponseVo;
import com.atguigu.common.utils.R;
import com.auguigu.guilimail.auth.feign.MemberFeignService;
import com.auguigu.guilimail.auth.feign.SmsFeignService;
import com.auguigu.guilimail.auth.vo.MemberLoginVo;
import com.auguigu.guilimail.auth.vo.MemberRegisterVo;
import com.auguigu.guilimail.auth.vo.UserLoginVo;
import com.auguigu.guilimail.auth.vo.UserRegisterVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController {
    @Autowired
    SmsFeignService smsFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    MemberFeignService memberFeignService;

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone) {
        //1、接口防刷:60s内如果再有请求发进来就直接返回不允许发送的响应，在值的后面拼接上时间戳
        if (!StringUtils.isEmpty(redisTemplate.opsForValue().get(AuthServerConst.SMS_CODE_CACHE_PREFIX + phone))){
            String val = redisTemplate.opsForValue().get(AuthServerConst.SMS_CODE_CACHE_PREFIX + phone);
            Long timeStempt = Long.parseLong(val.split("_")[1]);
            if ((System.currentTimeMillis() - timeStempt) < 60 * 1000) {
                //如果小于60s
                return  R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        //2、验证码的再次校验-放入redis sms:code:16622323032 -> 126539
        String code = UUID.randomUUID().toString().substring(0,5);
        String value = code + "_" +System.currentTimeMillis();
        //将生成的验证码在redis中保存10min
        redisTemplate.opsForValue().set(AuthServerConst.SMS_CODE_CACHE_PREFIX + phone, value,10, TimeUnit.MINUTES);
        smsFeignService.sendCode(phone,code);
        return R.ok();
    }

    @PostMapping("/register") // auth服务
    public String register(@Valid UserRegisterVo registerVo,  // 注册信息
                           BindingResult result,
                           RedirectAttributes attributes) {
        //1.判断校验是否通过
        Map<String, String> errors = new HashMap<>();
        if (result.hasErrors()){
            //1.1 如果校验不通过，则封装校验结果
            result.getFieldErrors().forEach(item->{
                // 获取错误的属性名和错误信息
                errors.put(item.getField(), item.getDefaultMessage());
                //1.2 将错误信息封装到session中
                attributes.addFlashAttribute("errors", errors);
            });
            //1.2 重定向到注册页
            //不能使用转发，刷新会出现问题
            return "redirect:http://auth.gulimail.com/reg.html";
        }
        else {//2.若JSR303校验通过

            //判断验证码是否正确
            String code = redisTemplate.opsForValue().get(AuthServerConst.SMS_CODE_CACHE_PREFIX + registerVo.getPhone());
            //2.1 如果对应手机的验证码不为空且与提交的相等-》验证码正确
            if (!StringUtils.isEmpty(code) && registerVo.getCode().equals(code.split("_")[0])) {
                //2.1.1 使得验证后的验证码失效
                redisTemplate.delete(AuthServerConst.SMS_CODE_CACHE_PREFIX + registerVo.getPhone());

                //2.1.2 远程调用会员服务注册
                MemberRegisterVo memberRegisterVo = new MemberRegisterVo();
                BeanUtils.copyProperties(registerVo,memberRegisterVo);
                R r = memberFeignService.regist(memberRegisterVo);
                if (r.getCode() == 0) {
                    //调用成功，重定向登录页
                    return "redirect:http://auth.gulimail.com/login.html";
                }else {
                    //调用失败，返回注册页并显示错误信息
                    String msg = (String) r.get("msg");
                    errors.put("msg", msg);
                    attributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimail.com/reg.html";
                }
            }else {
                //2.2 验证码错误
                errors.put("code", "验证码错误");
                attributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimail.com/reg.html";
            }
        }
//        return "redirect:http://auth.gulimail.com/reg.html";
    }

    @RequestMapping("/login") // auth
    public String login(UserLoginVo vo, RedirectAttributes attributes, HttpSession session){
        // 远程服务
        R r = memberFeignService.login(vo);
        //如果登陆成功，就跳转到商城的首页
        if (r.getCode() == 0) {
            //需要从r中获取到对象
            MemberResponseVo data = (MemberResponseVo) r.getData(new TypeReference<MemberResponseVo>() {});
            //通过包装类将session透明的添加到了redis中去，session保存的域名是gulimail.com的父域名地址
            session.setAttribute("loginUser",data);
            return "redirect:http://gulimail.com/";
        }else {// 登录失败重回登录页面，携带错误信息
            String msg = (String) r.get("msg");
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", msg);
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimail.com/login.html";
        }
    }
    @RequestMapping("/login.html")
    public String stepToLoginPage(HttpSession session) {
        if (session.getAttribute("loginUser") != null) {
            return "redirect:http://gulimail.com/";
        }
        return "login";
    }


}


