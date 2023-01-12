package com.atguigu.gulimail.seckill.interceptor;

import com.atguigu.common.to.session.MemberResponseVo;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SeckillInterceptor implements HandlerInterceptor {
    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //只有秒杀的请求需要进行拦截
        boolean match = new AntPathMatcher().match("/kill/**", request.getRequestURI());

        if (match) {
            HttpSession session = request.getSession();
            Object obj = session.getAttribute("loginUser");
            if (obj != null) {
                MemberResponseVo memberResponseVo = (MemberResponseVo) obj;
                loginUser.set(memberResponseVo);
                return true;
            } else
                session.setAttribute("msg","请先进行登陆");
            response.sendRedirect("http://auth.gulimail.com/login.html");
            return false;
        }
        return true;

    }
}
