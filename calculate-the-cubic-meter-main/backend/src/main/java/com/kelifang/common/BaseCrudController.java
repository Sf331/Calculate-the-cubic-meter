package com.kelifang.common;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 基础数据 CRUD 的公用实现。六个实体的接口形状完全一样，抽在这里，
 * 子类只需要给出实体类型、路径和校验规则。
 *
 * 只做列表分页、新增、编辑、删除四件事。将来某个实体需要额外接口（比如班级要查学生名单），
 * 直接在子类里加方法即可，不用改这里。
 */
public abstract class BaseCrudController<T extends IdEntity> {

    protected abstract IService<T> service();

    /** 入参校验。HTTP 是信任边界，子类必须实现。 */
    protected abstract void check(T entity);

    @GetMapping
    public Result<Page<T>> page(@RequestParam(defaultValue = "1") long page,
                                @RequestParam(defaultValue = "20") long size) {
        return Result.ok(service().page(new Page<>(page, size),
                Wrappers.<T>query().orderByAsc("id")));
    }

    @PostMapping
    public Result<T> create(@RequestBody T entity) {
        entity.setId(null);
        check(entity);
        service().save(entity);
        return Result.ok(entity);
    }

    @PutMapping("/{id}")
    public Result<T> update(@PathVariable Long id, @RequestBody T entity) {
        entity.setId(id);
        check(entity);
        service().updateById(entity);
        return Result.ok(entity);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service().removeById(id);
        return Result.ok();
    }
}
