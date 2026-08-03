package com.storeiq.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storeiq.common.JwtUtil;
import com.storeiq.common.Result;
import com.storeiq.dto.LoginRequest;
import com.storeiq.dto.LoginResponse;
import com.storeiq.entity.User;
import com.storeiq.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 认证服务
 */
@Service
public class AuthService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    public Result<?> login(LoginRequest request) {
        // 查询用户（生产中密码应为 BCrypt 加密比对）
        User user = lambdaQuery()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getPassword, request.getPassword()) // TODO: 替换为 BCrypt
                .eq(User::getStatus, 1)
                .one();

        if (user == null) {
            return Result.fail("用户名或密码错误");
        }

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 构建响应
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setRole(user.getRole());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setPermissions(getPermissionsByRole(user.getRole()));
        resp.setUser(userInfo);

        return Result.success("登录成功", resp);
    }

    /**
     * 获取当前用户信息
     */
    public Result<?> getUserInfo(Long userId) {
        User user = getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setRole(user.getRole());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setPermissions(getPermissionsByRole(user.getRole()));

        return Result.success(userInfo);
    }

    /**
     * 根据角色获取权限列表
     */
    private java.util.List<String> getPermissionsByRole(String role) {
        switch (role) {
            case "superadmin":
                return java.util.Arrays.asList("all");
            case "admin":
                return java.util.Arrays.asList("store:view", "store:edit", "product:view", "product:edit",
                        "inventory:view", "purchase:view", "purchase:edit", "sale:view", "sale:edit",
                        "supplier:view", "supplier:edit", "stats:view", "ai:chat", "log:view");
            case "manager":
                return java.util.Arrays.asList("store:view", "product:view", "inventory:view",
                        "purchase:view", "sale:view", "sale:edit", "supplier:view", "stats:view", "ai:chat");
            case "viewer":
                return java.util.Arrays.asList("store:view", "product:view", "inventory:view",
                        "purchase:view", "sale:view", "stats:view");
            default:
                return Collections.emptyList();
        }
    }
}
