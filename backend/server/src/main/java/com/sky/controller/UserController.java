package com.sky.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.UserLoginDTO;
import com.sky.dto.UserRegisterDTO;
import com.sky.dto.UserStatusDTO;
import com.sky.entity.User;
import com.sky.service.UserService;
import com.sky.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器
 * <p>
 * 处理用户注册、登录、信息管理、状态管理和密码修改请求
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     *
     * @param dto 包含手机号和密码的注册信息
     * @return 注册成功的用户信息（不含密码）
     */
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody UserRegisterDTO dto) {
        User user = userService.register(dto);
        return Result.success(user);
    }

    /**
     * 用户登录
     *
     * @param dto 包含手机号和密码的登录信息
     * @return 登录成功的用户信息（不含密码）
     */
    @PostMapping("/login")
    public Result<User> login(@Valid @RequestBody UserLoginDTO dto) {
        User user = userService.login(dto);
        return Result.success(user);
    }

    /**
     * 分页查询用户列表（管理后台）
     *
     * @param page  当前页码，默认 1
     * @param size  每页条数，默认 10
     * @param phone 手机号搜索关键字（可选）
     * @return 分页用户数据
     */
    @GetMapping("/list")
    public Result<Page<User>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "phone", required = false) String phone) {
        Page<User> result = userService.list(page, size, phone);
        return Result.success(result);
    }

    /**
     * 修改用户启用/禁用状态（管理后台）
     *
     * @param dto 包含用户 ID 和目标状态值（1=启用 0=禁用）
     */
    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestBody UserStatusDTO dto) {
        userService.updateStatus(dto.getId(), dto.getStatus());
        return Result.success();
    }

    /**
     * 修改个人信息（昵称、头像）
     *
     * @param user 包含 id、nickname、avatar 的用户对象
     * @return 更新后的用户信息
     */
    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody User user) {
        User updated = userService.updateProfile(user);
        return Result.success(updated);
    }

    /**
     * 修改登录密码
     * <p>
     * 需要提供原密码进行验证，新密码将通过 BCrypt 加密存储
     *
     * @param body 包含 userId、oldPassword、newPassword
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        Long userId = Long.valueOf(body.get("userId"));
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        userService.changePassword(userId, oldPassword, newPassword);
        return Result.success();
    }
}
