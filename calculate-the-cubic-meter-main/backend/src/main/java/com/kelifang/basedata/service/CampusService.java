package com.kelifang.basedata.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.basedata.entity.Campus;
import com.kelifang.basedata.mapper.CampusMapper;
import com.kelifang.common.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CampusService extends ServiceImpl<CampusMapper, Campus> {

    /** 入参校验。HTTP 是信任边界，这里不能省。 */
    public void check(Campus campus) {
        if (!StringUtils.hasText(campus.getName())) {
            throw BizException.badRequest("校区名称不能为空");
        }
    }
}
