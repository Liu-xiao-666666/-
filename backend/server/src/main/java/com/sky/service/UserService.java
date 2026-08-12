package com.sky.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.UserLoginDTO;
import com.sky.dto.UserRegisterDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户业务服务
 * <p>
 * 处理用户注册、登录、信息查询、状态管理、个人信息修改和密码修改，
 * 密码使用 BCrypt 加密存储和验证
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    /**
     * 用户注册
     * <p>
     * 校验手机号唯一性，密码 BCrypt 加密后存储，
     * 默认昵称为"用户+手机号后4位"，状态默认启用
     *
     * @param dto 包含 phone 和 password 的注册信息
     * @return 注册成功的用户（不含密码）
     * @throws RuntimeException 手机号已注册时抛出
     */
    public User register(UserRegisterDTO dto) {
        boolean exists = userMapper.exists(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (exists) {
            throw new RuntimeException("手机号已注册");
        }

        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname("用户" + dto.getPhone().substring(7));
        user.setStatus(1);

        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    /**
     * 用户登录
     * <p>
     * 校验手机号存在、账号未被禁用、密码匹配（BCrypt 验证）
     *
     * @param dto 包含 phone 和 password 的登录信息
     * @return 登录成功的用户（不含密码）
     * @throws RuntimeException 手机号未注册、账号禁用或密码错误时抛出
     */
    public User login(UserLoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user == null) {
            throw new RuntimeException("手机号未注册");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 分页查询用户列表（管理后台）
     *
     * @param page  当前页码
     * @param size  每页条数
     * @param phone 手机号筛选（可选，模糊匹配）
     * @return 分页用户数据（不含密码）
     */
    public Page<User> list(int page, int size, String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StringUtils.hasText(phone), User::getPhone, phone)
                .orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    /**
     * 修改用户状态（启用/禁用）
     *
     * @param id     用户 ID
     * @param status 目标状态（1=启用 0=禁用）
     */
    public void updateStatus(Long id, Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    /**
     * 修改个人信息（昵称、头像）
     *
     * @param dto 包含 id、nickname、avatar 的用户对象
     * @return 更新后的用户信息（不含密码）
     */
    public User updateProfile(User dto) {
        User user = new User();
        user.setId(dto.getId());
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        userMapper.updateById(user);
        User updated = userMapper.selectById(dto.getId());
        updated.setPassword(null);
        return updated;
    }

    /**
     * 修改登录密码
     * <p>
     * 先验证原密码是否正确，再使用 BCrypt 加密新密码并更新
     *
     * @param userId      用户 ID
     * @param oldPassword 原密码（明文）
     * @param newPassword 新密码（明文）
     * @throws RuntimeException 用户不存在或原密码错误时抛出
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(BCrypt.hashpw(newPassword));
        userMapper.updateById(update);
    }
}
