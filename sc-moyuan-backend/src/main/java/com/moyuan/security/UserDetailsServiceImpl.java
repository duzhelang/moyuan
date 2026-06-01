package com.moyuan.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.entity.User;
import com.moyuan.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        String role = user.getRole() != null ? user.getRole().toUpperCase() : "USER";
        return new LoginUser(user, role);
    }
}
