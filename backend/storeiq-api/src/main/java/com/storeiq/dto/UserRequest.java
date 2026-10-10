package com.storeiq.dto;

import javax.validation.constraints.NotBlank;

/**
 * 用户创建/编辑请求
 */
public class UserRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 创建时必填，编辑时可选（留空表示不修改密码） */
    private String password;

    private String nickname;
    private String phone;
    private String email;

    @NotBlank(message = "角色不能为空")
    private String role;

    /** 1=启用 0=停用 */
    private Integer status;

    // ===== Getter/Setter =====

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
