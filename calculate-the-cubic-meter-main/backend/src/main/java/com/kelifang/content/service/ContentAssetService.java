package com.kelifang.content.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.common.BizException;
import com.kelifang.common.UserContext;
import com.kelifang.content.entity.ContentAsset;
import com.kelifang.content.mapper.ContentAssetMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Set;

@Service
public class ContentAssetService extends ServiceImpl<ContentAssetMapper, ContentAsset> {

    public static final Set<String> TYPES = Set.of("COURSEWARE", "HANDOUT", "PAPER", "MEDIA");

    /** ALL = 所有人可见 / PRIVATE = 只有 owner 和校长教务可见 */
    public static final Set<String> SCOPES = Set.of("ALL", "PRIVATE");

    /** 能维护内容的角色。菜单也是这么配的，但边界在后端 —— 学生账号直接敲 URL 不该建得出内容。 */
    private static final Set<String> MANAGER_ROLES = Set.of("PRINCIPAL", "ACADEMIC", "TEACHER");

    /**
     * 入参校验 + 服务端兜底。
     * 新增和编辑都会先经过这里，所以归属和版本只在这一处处理，不用去覆写控制器的 create/update。
     * `id == null` 是基类给出的新增/编辑分界信号（create 先 setId(null)，update 先 setId(id)）。
     */
    public void check(ContentAsset asset) {
        requireCanManage();
        if (!StringUtils.hasText(asset.getName())) {
            throw BizException.badRequest("内容名称不能为空");
        }
        if (!TYPES.contains(asset.getType())) {
            throw BizException.badRequest("类型只能是 COURSEWARE / HANDOUT / PAPER / MEDIA");
        }
        if (!SCOPES.contains(asset.getScope())) {
            throw BizException.badRequest("可见范围只能是 ALL / PRIVATE");
        }

        if (asset.getId() == null) {
            // 归属由登录态决定，不认请求体 —— 否则谁都能把自己伪装成别人
            asset.setOwnerId(UserContext.userId());
            asset.setVersionNo(1);
        } else {
            ContentAsset old = getById(asset.getId());
            if (old == null) {
                throw BizException.notFound("内容不存在");
            }
            requireOwnership(old, "只能修改本人上传的内容");
            asset.setOwnerId(old.getOwnerId());
            // 版本读库自算：PUT 走 updateById 全字段覆盖，前端回传的是上一次的旧值
            asset.setVersionNo(old.getVersionNo() + 1);
        }
    }

    /**
     * 列表。校长和教务看全部，其余角色只看「所有人可见」或者自己传的。
     * courseId 是"按课表推送"用的筛选，可以不传。
     */
    public Page<ContentAsset> pageVisible(long page, long size, Long courseId) {
        var query = Wrappers.<ContentAsset>lambdaQuery().orderByAsc(ContentAsset::getId);

        if (courseId != null) {
            query.eq(ContentAsset::getCourseId, courseId);
        }
        if (!canManageAll()) {
            // or 必须用 and(...) 括起来。直接跟在 courseId 后面会拼成
            // `course_id = ? AND scope = 'ALL' OR owner_id = ?`，
            // 按课次筛选时别人 PRIVATE 的内容会漏出来
            query.and(w -> w.eq(ContentAsset::getScope, "ALL")
                    .or().eq(ContentAsset::getOwnerId, UserContext.userId()));
        }
        return page(new Page<>(page, size), query);
    }

    /** 基类的删除没有校验钩子，归属只能在这里挡。 */
    public void removeOwned(Long id) {
        requireCanManage();
        ContentAsset old = getById(id);
        if (old == null) {
            return;
        }
        requireOwnership(old, "只能删除本人上传的内容");
        removeById(id);
    }

    /** 上传接口也要挡 —— 否则学生账号能往服务器磁盘上丢任意文件。控制器直接调它。 */
    public static void requireCanManage() {
        if (!MANAGER_ROLES.contains(UserContext.role())) {
            throw BizException.forbidden("只有校长、教务和教师能维护内容");
        }
    }

    /** 校长和教务不受可见范围限制。和菜单的角色一致。 */
    private static boolean canManageAll() {
        String role = UserContext.role();
        return "PRINCIPAL".equals(role) || "ACADEMIC".equals(role);
    }

    private static void requireOwnership(ContentAsset asset, String message) {
        if (!canManageAll() && !Objects.equals(asset.getOwnerId(), UserContext.userId())) {
            throw BizException.forbidden(message);
        }
    }
}
