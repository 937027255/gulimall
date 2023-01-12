package com.atguigu.gulimail.member.interceptor;

import com.atguigu.common.to.session.MemberResponseVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class MemberInterceptor implements HandlerInterceptor {
    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        boolean match = new AntPathMatcher().match("/member/**", request.getRequestURI());
        if (match) {
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
