package com.example.ecommerce.module.user.controller;

import com.example.ecommerce.common.PageResult;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.module.user.entity.SysUser;
import com.example.ecommerce.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理控制器
 */
@Tag(name = "用户管理", description = "用户的增删改查等管理接口")
@RestController
@RequestMapping("/users")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 获取用户列表
     */
    @Operation(summary = "获取用户列表", description = "分页查询用户列表,支持关键词搜索")
    @GetMapping
    public Result<PageResult<SysUser>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        PageResult<SysUser> result = userService.getUserList(pageNum, pageSize, keyword, status);
        return Result.success(result);
    }

    /**
     * 获取用户详情
     */
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    @GetMapping("/{id}")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        // 隐藏密码
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 创建用户
     */
    @Operation(summary = "创建用户", description = "新增系统用户")
    @PostMapping
    public Result<Void> createUser(@RequestBody SysUser user) {
        userService.createUser(user);
        return Result.success(null);
    }

    /**
     * 更新用户
     */
    @Operation(summary = "更新用户", description = "修改用户信息")
    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        userService.updateUser(id, user);
        return Result.success(null);
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "删除指定用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success(null);
    }

    /**
     * 切换用户状态
     */
    @Operation(summary = "切换用户状态", description = "启用或禁用用户账号")
    @PostMapping("/{id}/toggle-status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        userService.toggleStatus(id);
        return Result.success(null);
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码", description = "重置指定用户的密码")
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String newPassword = request.get("newPassword");
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.error(400, "新密码不能为空");
        }
        userService.resetPassword(id, newPassword);
        return Result.success(null);
    }
}
