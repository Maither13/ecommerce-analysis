package com.example.ecommerce.module.user.controller;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.module.user.entity.SysUser;
import com.example.ecommerce.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "用户登录、登出等认证相关接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private UserService userService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "使用用户名和密码登录系统")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest,
                                             HttpServletRequest request) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        // 获取真实客户端IP（兼容反向代理）
        String loginIp = getClientIp(request);

        // 验证用户并获取token
        SysUser user = userService.login(username, password, loginIp);

        // 构建返回数据（SysUser 的 password 字段已标注 WRITE_ONLY，不会出现在响应中）
        Map<String, Object> data = new HashMap<>();
        data.put("token", "Bearer_" + user.getId() + "_" + System.currentTimeMillis());
        data.put("userInfo", user);
        data.put("roles", userService.getUserRoles(user.getId()));

        return Result.success(data);
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "根据token获取当前登录用户信息")
    @GetMapping("/info")
    public Result<SysUser> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return Result.error(401, "未授权");
        }

        // 从token中提取用户ID，格式: Bearer_{userId}_{timestamp}
        try {
            String token = authorization.replace("Bearer_", "");
            String[] parts = token.split("_");
            if (parts.length < 2) {
                return Result.error(401, "token格式无效");
            }
            Long userId = Long.parseLong(parts[0]);
            SysUser user = userService.getById(userId);
            if (user == null) {
                return Result.error(404, "用户不存在");
            }
            return Result.success(user);
        } catch (NumberFormatException e) {
            return Result.error(401, "token格式无效");
        }
    }

    /**
     * 获取客户端真实IP（兼容 X-Forwarded-For / Nginx 反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能包含多个IP，取第一个
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出", description = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT无状态,前端清除token即可
        return Result.success(null);
    }
}
