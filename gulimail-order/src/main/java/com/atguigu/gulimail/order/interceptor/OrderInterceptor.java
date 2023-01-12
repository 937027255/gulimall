package com.atguigu.gulimail.order.interceptor;

import com.atguigu.common.to.session.MemberResponseVo;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class OrderInterceptor implements HandlerInterceptor {
    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //进行调用order的远程调用无需登陆就可以进行调用
        boolean match = new AntPathMatcher().match("/order/order/status/**", request.getRequestURI());
        boolean match1 = new AntPathMatcher().match("/payed/**", request.getRequestURI());
        if (match || match1) {
            return true;
        }

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
}
