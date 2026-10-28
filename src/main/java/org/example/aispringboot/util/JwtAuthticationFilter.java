package org.example.aispringboot.util;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.aispringboot.DTO.response.UserLoginResponseDTO;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.config.SecurityConfig;
import org.example.aispringboot.enumClass.UserStatus;
import org.example.aispringboot.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JwtAuthticationFilter extends OncePerRequestFilter {
    @Resource
    private UserService userService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (request.getDispatcherType() == DispatcherType.ERROR || request.getDispatcherType() == DispatcherType.ASYNC) {
            return true;
        }
        return SecurityConfig.isPublicRequest(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // 1. 提取 token
        String token = JwtTokenUtil.extractTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            System.out.println("[JWT Filter] " + method + " " + requestUri + " -> 无 token, 返回 401");
            clearSecurityContext();
            ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
            return;
        }

        // 2. 验证 token
        JwtTokenUtil.TokenVerificationResult validationResult = JwtTokenUtil.validateToken(token);
        if (validationResult == null || !validationResult.isValid()) {
            System.out.println("[JWT Filter] " + method + " " + requestUri + " -> token 无效, 返回 401");
            clearSecurityContext();
            ResponseUtil.writeError(response, ResultCode.TOKEN_INVALID);
            return;
        }

        // 3. 查询用户信息并验证状态
        UserLoginResponseDTO.UserDetailResponseDTO user =
                userService.getUserById(validationResult.getUserId());

        if (user == null || !UserStatus.NORMAL.getCode().equals(user.getStatus())) {
            System.out.println("[JWT Filter] " + method + " " + requestUri
                    + " -> userId=" + validationResult.getUserId()
                    + ", user=" + (user != null ? JSONUtil.parseObj(user) : "null")
                    + ", 返回 403");
            clearSecurityContext();
            ResponseUtil.writeError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
            return;
        }

        // 使用数据库中的当前角色，避免角色变更后旧 token 继续保留原权限。
        if (user.getUserType() == null) {
            clearSecurityContext();
            ResponseUtil.writeError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
            return;
        }

        // 4. 设置认证信息
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getUserType())
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        validationResult.getUsername(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute("jwtToken", token);

        System.out.println("[JWT Filter] " + method + " " + requestUri
                + " -> 认证通过, userId=" + validationResult.getUserId());
        chain.doFilter(request, response);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
