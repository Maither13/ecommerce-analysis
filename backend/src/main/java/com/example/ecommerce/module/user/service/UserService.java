package com.example.ecommerce.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.PageResult;
import com.example.ecommerce.module.user.entity.SysUser;
import com.example.ecommerce.module.user.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务类
 */
@Service
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 查询用户角色列表 */
    public List<String> getUserRoles(Long userId) {
        String sql = "SELECT r.role_code FROM sys_role r " +
                     "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
                     "WHERE ur.user_id = ?";
        return jdbcTemplate.queryForList(sql, String.class, userId);
    }

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 明文密码
     * @param loginIp  登录IP（可为 null）
     */
    public SysUser login(String username, String password, String loginIp) {
        // 入参校验
        if (username == null || username.isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessException("密码不能为空");
        }

        // 查询用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = this.getOne(wrapper);

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 验证密码(BCrypt)
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 检查用户状态（先校验密码再检查状态，避免泄露账号是否存在）
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用,请联系管理员");
        }

        // 更新最后登录时间和IP
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(loginIp);
        this.updateById(user);

        return user;
    }

    /**
     * 分页查询用户列表
     */
    public PageResult<SysUser> getUserList(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getEmail, keyword));
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }

        wrapper.orderByAsc(SysUser::getId);
        Page<SysUser> result = this.page(page, wrapper);

        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /**
     * 创建用户
     */
    public void createUser(SysUser user) {
        // 检查用户名是否已存在（含软删除记录，避免唯一索引冲突）
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ?",
                Long.class, user.getUsername());
        if (count != null && count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 加密密码(BCrypt)
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1); // 默认启用

        this.save(user);
    }

    /**
     * 更新用户
     */
    public void updateUser(Long id, SysUser user) {
        user.setId(id);
        // 如果提供了新密码,则加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null); // 不更新密码
        }

        this.updateById(user);
    }

    /**
     * 删除用户
     */
    public void deleteUser(Long id) {
        // 不能删除自己
        if (id == 1L) {
            throw new BusinessException("不能删除超级管理员账号");
        }
        this.removeById(id);
    }

    /**
     * 切换用户状态
     */
    public void toggleStatus(Long id) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 不能禁用超级管理员
        if (id == 1L) {
            throw new BusinessException("不能禁用超级管理员账号");
        }

        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        this.updateById(user);
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long id, String newPassword) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
    }
}
