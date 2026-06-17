package com.example.travelassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.travelassistant.dto.AuthRequest;
import com.example.travelassistant.dto.UserProfileResponse;
import com.example.travelassistant.dto.UserProfileUpdateRequest;
import com.example.travelassistant.entity.Itinerary;
import com.example.travelassistant.entity.User;
import com.example.travelassistant.exception.BusinessException;
import com.example.travelassistant.mapper.ItineraryMapper;
import com.example.travelassistant.mapper.UserMapper;
import com.example.travelassistant.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务实现。
 * 主要负责：
 * 1. 注册
 * 2. 登录
 * 3. 查询资料
 * 4. 更新资料
 *
 * 当前项目没有引入 JWT 或 Spring Security，
 * 所以前后端是通过“返回用户信息 + 前端保存登录态”的轻量方式完成演示。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final ItineraryMapper itineraryMapper;

    @Override
    public UserProfileResponse register(AuthRequest request) {
        // 注册前先检查用户名是否已存在，避免违反数据库唯一约束。
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .last("limit 1"));
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }

        // 课程项目场景下直接保存明文密码，便于演示。
        // 如果是真实生产环境，这里必须进行密码加密。
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRole("USER");
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return buildProfile(user);
    }

    @Override
    public UserProfileResponse login(AuthRequest request) {
        // 通过用户名和密码精确匹配登录用户。
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getPassword, request.getPassword())
                .last("limit 1"));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        return buildProfile(user);
    }

    @Override
    public UserProfileResponse getProfile(Long id) {
        // 查询个人资料时只读取当前用户自身基础信息与行程数。
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return buildProfile(user);
    }

    @Override
    public UserProfileResponse updateProfile(UserProfileUpdateRequest request) {
        User user = userMapper.selectById(request.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 只有当用户名真的变化时才做唯一性检查。
        if (StringUtils.isNotBlank(request.getUsername()) && !StringUtils.equals(user.getUsername(), request.getUsername())) {
            User duplicate = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, request.getUsername())
                    .last("limit 1"));
            if (duplicate != null) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(request.getUsername());
        }
        if (StringUtils.isNotBlank(request.getPassword())) {
            user.setPassword(request.getPassword());
        }
        userMapper.updateById(user);
        return buildProfile(user);
    }

    /**
     * 把数据库实体组装成返回前端的资料对象。
     * 这里额外补充 itineraryCount，方便前端个人页直接显示统计信息。
     */
    private UserProfileResponse buildProfile(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(StringUtils.defaultIfBlank(user.getRole(), "USER"));
        response.setCreateTime(user.getCreateTime());
        Long itineraryCount = itineraryMapper.selectCount(new LambdaQueryWrapper<Itinerary>()
                .eq(Itinerary::getUserId, user.getId()));
        response.setItineraryCount(itineraryCount);
        return response;
    }
}
