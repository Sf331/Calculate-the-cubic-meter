package com.kelifang.basedata.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kelifang.basedata.entity.Campus;
import com.kelifang.basedata.service.CampusService;
import com.kelifang.common.BaseCrudController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basedata/campus")
@RequiredArgsConstructor
public class CampusController extends BaseCrudController<Campus> {

    private final CampusService campusService;

    @Override
    protected IService<Campus> service() {
        return campusService;
    }

    @Override
    protected void check(Campus entity) {
        campusService.check(entity);
    }
}
