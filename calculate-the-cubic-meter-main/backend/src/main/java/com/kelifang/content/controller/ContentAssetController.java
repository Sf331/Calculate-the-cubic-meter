package com.kelifang.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kelifang.common.BaseCrudController;
import com.kelifang.common.BizException;
import com.kelifang.common.Result;
import com.kelifang.common.StorageUtil;
import com.kelifang.content.entity.ContentAsset;
import com.kelifang.content.service.ContentAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentAssetController extends BaseCrudController<ContentAsset> {

    private final ContentAssetService assetService;
    private final StorageUtil storageUtil;

    @Override
    protected IService<ContentAsset> service() {
        return assetService;
    }

    @Override
    protected void check(ContentAsset entity) {
        assetService.check(entity);
    }

    /**
     * 覆写基类的列表，把可见范围过滤加上。
     * 不覆写的话教师能直接请求 /api/content 绕过过滤看到所有人的 PRIVATE 内容。
     *
     * 签名必须和基类一模一样 —— 加个 courseId 参数就变成重载而不是覆写，
     * 两个方法映射同一个 GET /api/content，Spring 启动直接报 Ambiguous mapping。
     * 要按课程筛选走下面的 /list。
     */
    @Override
    @GetMapping
    public Result<Page<ContentAsset>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long size) {
        return Result.ok(assetService.pageVisible(page, size, null));
    }

    /** 表格用的列表。多一个 courseId，就是"按课表推送本节内容"：选完课次拿它的课程来筛。 */
    @GetMapping("/list")
    public Result<Page<ContentAsset>> list(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long size,
                                           @RequestParam(required = false) Long courseId) {
        return Result.ok(assetService.pageVisible(page, size, courseId));
    }

    @Override
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        assetService.removeOwned(id);
        return Result.ok();
    }

    /**
     * 上传文件，返回落盘后的相对文件名；前端把它填进 filePath 再走 POST /api/content 建资产。
     * 分两步是因为一步到位没法复用基类的 create，得手写 @RequestParam 拼对象，代码更多。
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        ContentAssetService.requireCanManage();
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("文件为空");
        }
        return Result.ok(storageUtil.store(file));
    }
}
