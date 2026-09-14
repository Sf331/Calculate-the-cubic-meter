package com.kelifang.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kelifang.auth.dto.LoginRequest;
import com.kelifang.auth.entity.SysUser;
import com.kelifang.auth.mapper.SysUserMapper;
import com.kelifang.common.BizException;
import com.kelifang.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;

    public CurrentUser login(LoginRequest req) {
        if (req.getUsername() == null || req.getPassword() == null) {
            throw BizException.badRequest("账号和密码不能为空");
        }

        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, req.getUsername()));

        // demo 阶段明文比对。上线前这里必须改为 BCrypt 之类的哈希校验。
        if (user == null || !user.getPassword().equals(req.getPassword())) {
            throw BizException.badRequest("账号或密码错误");
        }

        return new CurrentUser(user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }
}
