package com.dutir.guilimail.cart.interceptor;

import com.atguigu.common.to.session.MemberResponseVo;
import com.dutir.guilimail.cart.constant.CartConstant;
import com.dutir.guilimail.cart.vo.LoginConfirmVo;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 *  配置登陆验证的拦截器，在进入该模块的任何操作之前进行登陆验证
 */
public class GulimailInterceptor implements HandlerInterceptor {

    public static ThreadLocal<LoginConfirmVo> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        LoginConfirmVo confirmVo = new LoginConfirmVo();
        if (request.getSession().getAttribute("loginUser") != null) {
            //说明有登陆信息
            MemberResponseVo loginUser = (MemberResponseVo) request.getSession().getAttribute("loginUser");
            confirmVo.setUserId(loginUser.getId());
//            confirmVo.setTempt(false);
        } else {
            //说明没有找到session中的登陆信息,查看cookie中有没有临时用户信息
            Cookie[] cookies = request.getCookies();
            if (cookies != null && cookies.length > 0) {
                for (Cookie cookie : cookies) {
                    String name = cookie.getName();
                    if (name.equals(CartConstant.TEMP_USER_COOKIE_NAME)) {
                        confirmVo.setUserKey(cookie.getValue());
//                        confirmVo.setTempt(true);
                    }
                }
            }
            //从cookie中获取但是没有获取到内容
            if (StringUtils.isEmpty(confirmVo.getUserKey())) {
                String userKey = UUID.randomUUID().toString();
                confirmVo.setUserKey(userKey);
                confirmVo.setFirstTempt(true);
            }
        }
        //让同一个线程可以共享这个confirmVo的数据
        threadLocal.set(confirmVo);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        LoginConfirmVo confirmVo = threadLocal.get();
        if (confirmVo.isFirstTempt()) {
            //如果是第一次创建的临时用户
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME,confirmVo.getUserKey());
            cookie.setDomain("gulimail.com");
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIME_OUT);
            response.addCookie(cookie);
        }
    }
}
