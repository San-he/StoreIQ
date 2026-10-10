package com.storeiq.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storeiq.common.JwtUtil;
import com.storeiq.common.Result;
import com.storeiq.dto.LoginRequest;
import com.storeiq.dto.LoginResponse;
import com.storeiq.entity.User;
import com.storeiq.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 认证服务
 */
@Service
public class AuthService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /** 角色列表（按权限从高到低） */
    public static final List<String> ROLES = Arrays.asList("superadmin", "admin", "manager");

    /** 角色中文名称映射 */
    public static String getRoleLabel(String role) {
        switch (role) {
            case "superadmin": return "总经理";
            case "admin": return "管理账号";
            case "manager": return "普通账号";
            default: return role;
        }
    }

    /**
     * 用户登录
     */
    public Result<?> login(LoginRequest request) {
        // 先按用户名 + 启用状态查询
        User user = lambdaQuery()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getStatus, 1)
                .one();

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
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
        userInfo.setRoles(Collections.singletonList(user.getRole()));
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
        userInfo.setRoles(Collections.singletonList(user.getRole()));
        userInfo.setAvatar(user.getAvatar());
        userInfo.setPermissions(getPermissionsByRole(user.getRole()));

        return Result.success(userInfo);
    }

    /**
     * 根据角色获取权限列表
     * superadmin(总经理): 全部权限，含用户管理
     * admin(管理账号):   数据增删改查 + 查看 + 下载
     * manager(普通账号):  仅查看 + 下载
     */
    private List<String> getPermissionsByRole(String role) {
        switch (role) {
            case "superadmin":
                return Arrays.asList("all", "user:manage");
            case "admin":
                return Arrays.asList("store:edit", "store:view", "product:edit", "product:view",
                        "inventory:view", "purchase:edit", "purchase:view", "sale:edit", "sale:view",
                        "supplier:edit", "supplier:view", "stats:view", "ai:chat", "log:view");
            case "manager":
                return Arrays.asList("store:view", "product:view", "inventory:view",
                        "purchase:view", "sale:view", "supplier:view", "stats:view");
            default:
                return Collections.emptyList();
        }
    }
}
