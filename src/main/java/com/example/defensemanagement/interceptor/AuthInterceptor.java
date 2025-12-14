package com.example.defensemanagement.interceptor;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        // 排除不需要权限验证的路径
        if (requestURI.equals("/login") || requestURI.startsWith("/css/") || 
            requestURI.startsWith("/js/") || requestURI.startsWith("/images/")) {
            return true;
        }

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");

        // 检查是否已登录
        if (currentUser == null && currentTeacher == null) {
            response.sendRedirect("/login");
            return false;
        }

        // 检查特定权限
        if (requestURI.startsWith("/admin/")) {
            if (currentUser == null || !authService.hasPermission(currentUser, "ADMIN")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "权限不足");
                return false;
            }
        }

        if (requestURI.startsWith("/department/")) {
            if (currentUser == null || !authService.hasPermission(currentUser, "MANAGE_DEPARTMENT")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "权限不足");
                return false;
            }
        }

        if (requestURI.startsWith("/defense/")) {
            boolean hasPermission = false;
            if (currentUser != null && authService.hasPermission(currentUser, "MANAGE_DEFENSE")) {
                hasPermission = true;
            } else if (currentTeacher != null && authService.isDefenseLeader(currentTeacher.getId(), null)) {
                hasPermission = true;
            }
            
            if (!hasPermission) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "权限不足");
                return false;
            }
        }

        return true;
    }
}