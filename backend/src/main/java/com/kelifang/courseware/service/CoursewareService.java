package com.kelifang.courseware.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kelifang.common.BizException;
import com.kelifang.common.UserContext;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.service.StudentService;
import com.kelifang.courseware.dto.CoursewareAnswerRequest;
import com.kelifang.courseware.entity.Courseware;
import com.kelifang.courseware.entity.CoursewareRecord;
import com.kelifang.courseware.mapper.CoursewareMapper;
import com.kelifang.courseware.mapper.CoursewareRecordMapper;
import com.kelifang.courseware.vo.CoursewareStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CoursewareService extends ServiceImpl<CoursewareMapper, Courseware> {
    private final CoursewareRecordMapper recordMapper;
    private final StudentService studentService;
    private final ObjectMapper objectMapper;
    private static final Set<String> EDITORS = Set.of("PRINCIPAL", "ACADEMIC", "TEACHER");

    public List<Courseware> listVisible() {
        requireLoggedIn();
        return list(Wrappers.<Courseware>lambdaQuery().orderByDesc(Courseware::getId));
    }

    public Courseware getVisible(Long id) {
        requireLoggedIn();
        Courseware result = getById(id);
        if (result == null) throw BizException.notFound("课件不存在");
        return result;
    }

    public Courseware saveChecked(Courseware value) {
        requireEditor();
        if (!StringUtils.hasText(value.getName())) throw BizException.badRequest("课件名称不能为空");
        validateSchema(value.getSchemaJson());
        saveOrUpdate(value);
        return value;
    }

    public void removeChecked(Long id) {
        requireEditor();
        if (!removeById(id)) throw BizException.notFound("课件不存在");
    }

    public CoursewareRecord answer(Long id, CoursewareAnswerRequest request) {
        requireStudent();
        Courseware cw = getVisible(id);
        if (!StringUtils.hasText(request.getComponentId()) || request.getComponentId().length() > 50)
            throw BizException.badRequest("组件 id 不能为空且不能超过 50 个字符");
        if (!StringUtils.hasText(request.getAnswer())) throw BizException.badRequest("答案不能为空");
        JsonNode component;
        try {
            component = findComponent(objectMapper.readTree(cw.getSchemaJson()), request.getComponentId());
        } catch (Exception e) {
            throw BizException.badRequest("课件内容无效");
        }
        if (component == null) throw BizException.badRequest("组件不存在");
        Student student = studentService.byUserId(UserContext.userId());
        if (student == null) throw BizException.forbidden("当前账号没有学生档案");
        CoursewareRecord record = new CoursewareRecord();
        record.setCoursewareId(id); record.setScheduleId(request.getScheduleId());
        record.setStudentId(student.getId()); record.setComponentId(request.getComponentId());
        record.setAnswer(request.getAnswer());
        JsonNode expected = component.get("answer");
        record.setCorrect(expected != null && !expected.isNull() && answersEqual(expected, request.getAnswer()));
        recordMapper.insert(record);
        return record;
    }

    public CoursewareStats stats(Long id) {
        requireEditor();
        getVisible(id);
        List<CoursewareRecord> rows = recordMapper.selectList(Wrappers.<CoursewareRecord>lambdaQuery()
                .eq(CoursewareRecord::getCoursewareId, id));
        return new CoursewareStats(rows.size(), rows.stream().filter(r -> Boolean.TRUE.equals(r.getCorrect())).count(),
                rows.stream().map(CoursewareRecord::getStudentId).distinct().count());
    }

    private boolean answersEqual(JsonNode expected, String actual) {
        if (actual == null) return false;
        if (expected.isArray()) for (JsonNode item : expected) if (item.asText().equalsIgnoreCase(actual.trim())) return true;
        return expected.asText().equalsIgnoreCase(actual.trim());
    }

    private JsonNode findComponent(JsonNode root, String id) {
        JsonNode pages = root.get("pages");
        if (pages != null && pages.isArray()) for (JsonNode page : pages)
            for (JsonNode component : page.withArray("components"))
                if (id.equals(component.path("id").asText())) return component;
        return null;
    }

    private void validateSchema(String json) {
        if (!StringUtils.hasText(json)) throw BizException.badRequest("课件内容不能为空");
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject() || !root.path("version").canConvertToInt() || !root.path("pages").isArray())
                throw BizException.badRequest("课件必须包含 version 和 pages");
            Set<String> pageIds = new HashSet<>();
            Set<String> componentIds = new HashSet<>();
            int interactionCount = 0;
            for (JsonNode page : root.path("pages")) {
                if (!page.isObject() || !StringUtils.hasText(page.path("id").asText())
                        || !page.path("components").isArray()) throw BizException.badRequest("页面结构无效");
                if (!pageIds.add(page.path("id").asText())) throw BizException.badRequest("页面 id 不能重复");
                for (JsonNode component : page.path("components")) {
                    if (!component.isObject() || !StringUtils.hasText(component.path("id").asText())
                            || !StringUtils.hasText(component.path("type").asText()))
                        throw BizException.badRequest("组件必须包含 id 和 type");
                    if (!componentIds.add(component.path("id").asText())) throw BizException.badRequest("组件 id 不能重复");
                    String type = component.path("type").asText();
                    if (!Set.of("text", "blank", "single").contains(type)) throw BizException.badRequest("不支持的组件类型");
                    if (!"text".equals(type)) interactionCount++;
                    if ("text".equals(type) && !StringUtils.hasText(component.path("text").asText()))
                        throw BizException.badRequest("文本组件内容不能为空");
                    if (!"text".equals(type) && !StringUtils.hasText(component.path("prompt").asText()))
                        throw BizException.badRequest("互动组件题干不能为空");
                    if (!"text".equals(type) && !StringUtils.hasText(component.path("answer").asText()))
                        throw BizException.badRequest("互动组件正确答案不能为空");
                    if ("single".equals(type)) {
                        JsonNode options = component.get("options");
                        if (options == null || !options.isArray() || options.size() < 2)
                            throw BizException.badRequest("单选题至少需要两个选项");
                        boolean answerInOptions = false;
                        for (JsonNode option : options)
                            if (component.path("answer").asText().equals(option.asText())) answerInOptions = true;
                        if (!answerInOptions) throw BizException.badRequest("单选题答案必须属于选项");
                    }
                }
            }
            if (interactionCount == 0) throw BizException.badRequest("课件至少需要一个互动题");
        } catch (BizException e) { throw e; }
        catch (Exception e) { throw BizException.badRequest("课件 JSON 格式无效"); }
    }

    private void requireLoggedIn() { 
        if (UserContext.role() == null) throw BizException.forbidden("未登录"); }
    private void requireEditor() { 
        if (!EDITORS.contains(UserContext.role())) throw BizException.forbidden("只有教师及以上角色可以维护课件"); }
    private void requireStudent() { 
        if (!"STUDENT".equals(UserContext.role())) throw BizException.forbidden("只有学生可以提交互动答案"); }
}
