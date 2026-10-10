package com.storeiq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storeiq.common.QueryPageParam;
import com.storeiq.common.Result;
import com.storeiq.dto.UserRequest;
import com.storeiq.entity.User;
import com.storeiq.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理控制器 — 仅 superadmin(总经理) 可访问
 */
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private AuthService authService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /** 分页列表 */
    @PostMapping("/list")
    public Result<?> list(@RequestBody QueryPageParam query) {
        HashMap<String, Object> param = query.getParam();
        Page<User> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (param != null) {
            String username = (String) param.get("username");
            String nickname = (String) param.get("nickname");
            String role = (String) param.get("role");
            String status = (String) param.get("status");
            if (StringUtils.isNotBlank(username)) wrapper.like(User::getUsername, username);
            if (StringUtils.isNotBlank(nickname)) wrapper.like(User::getNickname, nickname);
            if (StringUtils.isNotBlank(role)) wrapper.eq(User::getRole, role);
            if (StringUtils.isNotBlank(status)) wrapper.eq(User::getStatus, Integer.parseInt(status));
        }
        wrapper.orderByAsc(User::getCreateTime);

        IPage<User> result = authService.page(page, wrapper);

        // 脱敏：不返回密码
        List<Map<String, Object>> enriched = result.getRecords().stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("nickname", u.getNickname());
            map.put("phone", u.getPhone());
            map.put("email", u.getEmail());
            map.put("role", u.getRole());
            map.put("roleLabel", AuthService.getRoleLabel(u.getRole()));
            map.put("status", u.getStatus());
            map.put("createTime", u.getCreateTime());
            return map;
        }).collect(Collectors.toList());

        return Result.success(enriched, result.getTotal());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        User user = authService.getById(id);
        if (user == null) return Result.fail("用户不存在");
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("phone", user.getPhone());
        map.put("email", user.getEmail());
        map.put("role", user.getRole());
        map.put("roleLabel", AuthService.getRoleLabel(user.getRole()));
        map.put("status", user.getStatus());
        map.put("createTime", user.getCreateTime());
        return Result.success(map);
    }

    /** 创建用户 */
    @PostMapping
    public Result<?> create(@Valid @RequestBody UserRequest req) {
        // 校验角色合法性
        if (!AuthService.ROLES.contains(req.getRole())) {
            return Result.fail("无效的角色: " + req.getRole());
        }
        // 校验用户名唯一
        User exist = authService.lambdaQuery().eq(User::getUsername, req.getUsername()).one();
        if (exist != null) {
            return Result.fail("用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setRole(req.getRole());
        user.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        authService.save(user);

        return Result.success("创建成功");
    }

    /** 编辑用户 */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody UserRequest req) {
        User user = authService.getById(id);
        if (user == null) return Result.fail("用户不存在");

        // 校验角色合法性
        if (!AuthService.ROLES.contains(req.getRole())) {
            return Result.fail("无效的角色: " + req.getRole());
        }
        // 校验用户名唯一（排除自身）
        User exist = authService.lambdaQuery().eq(User::getUsername, req.getUsername()).ne(User::getId, id).one();
        if (exist != null) {
            return Result.fail("用户名已存在");
        }

        user.setUsername(req.getUsername());
        // 密码非空时才更新
        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        user.setNickname(req.getNickname());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setRole(req.getRole());
        user.setStatus(req.getStatus());
        authService.updateById(user);

        return Result.success("更新成功");
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        User user = authService.getById(id);
        if (user == null) return Result.fail("用户不存在");
        authService.removeById(id);
        return Result.success("删除成功");
    }

    /** 角色列表（供前端下拉使用） */
    @GetMapping("/roles")
    public Result<?> roles() {
        List<Map<String, String>> list = AuthService.ROLES.stream().map(r -> {
            Map<String, String> m = new HashMap<>();
            m.put("value", r);
            m.put("label", AuthService.getRoleLabel(r));
            return m;
        }).collect(Collectors.toList());
        return Result.success(list);
    }
}
