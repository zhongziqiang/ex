package com.org.kline.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.org.kline.mapper.UserMapper;
import com.org.kline.model.User;
import com.org.kline.service.UserService;
import org.springframework.stereotype.Service;

/**
* @author Apple
* @description 针对表【user】的数据库操作Service实现
* @createDate 2025-06-06 15:36:28
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

}




