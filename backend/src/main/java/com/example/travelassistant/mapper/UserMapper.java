package com.example.travelassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.travelassistant.entity.User;

public interface UserMapper extends BaseMapper<User> {

    Long selectTotalUserCount();
}
