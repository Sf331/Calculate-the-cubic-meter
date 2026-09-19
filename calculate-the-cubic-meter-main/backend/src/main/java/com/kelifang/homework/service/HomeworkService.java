package com.kelifang.homework.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kelifang.basedata.entity.ClassStudent;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.entity.Teacher;
import com.kelifang.basedata.service.StudentService;
import com.kelifang.basedata.mapper.ClassStudentMapper;
import com.kelifang.basedata.mapper.TeacherMapper;
import com.kelifang.basedata.mapper.ClazzMapper;
import com.kelifang.common.BizException;
import com.kelifang.common.UserContext;
import com.kelifang.homework.dto.*;
import com.kelifang.homework.entity.*;
import com.kelifang.homework.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HomeworkService extends ServiceImpl<HomeworkMapper, Homework> {
    private final QuestionMapper questionMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final SubmissionAnswerMapper answerMapper;
    private final WrongQuestionMapper wrongMapper;
    private final ClassStudentMapper classStudentMapper;
    private final ClazzMapper clazzMapper;
    private final TeacherMapper teacherMapper;
    private final StudentService studentService;
    private final ObjectMapper objectMapper;

    public List<Question> questions() {
        requireStaff();
        return questionMapper.selectList(Wrappers.<Question>lambdaQuery().orderByDesc(Question::getId));
    }
    public Question saveQuestion(QuestionRequest req, Long id) {
        requireStaff();
        if (!StringUtils.hasText(req.getStem()) || !StringUtils.hasText(req.getType()))
            throw BizException.badRequest("题干和题型不能为空");
        if (!Set.of("SINGLE","MULTI","BLANK","SUBJECTIVE").contains(req.getType()))
            throw BizException.badRequest("题型无效");
        Question q = new Question(); q.setId(id); q.setSubject(req.getSubject()); q.setGrade(req.getGrade());
        q.setKnowledgePoint(req.getKnowledgePoint()); q.setType(req.getType()); q.setStem(req.getStem());
        q.setOptions(req.getOptions()); q.setAnswer(req.getAnswer()); q.setScore(req.getScore() == null ? BigDecimal.ZERO : req.getScore());
        if (id == null) questionMapper.insert(q); else questionMapper.updateById(q); return q;
    }
    public List<Homework> list() {
        requireLogin();
        String role = UserContext.role();
        if ("PRINCIPAL".equals(role) || "ACADEMIC".equals(role)) return allHomeworks();
        if ("TEACHER".equals(role)) {
            Teacher t=teacherMapper.selectOne(Wrappers.<Teacher>lambdaQuery().eq(Teacher::getUserId,UserContext.userId()));
            if (t == null) return List.of();
            Set<Long> classes=clazzMapper.selectList(Wrappers.<com.kelifang.basedata.entity.Clazz>lambdaQuery().eq(com.kelifang.basedata.entity.Clazz::getTeacherId,t.getId())).stream().map(com.kelifang.basedata.entity.Clazz::getId).collect(java.util.stream.Collectors.toSet());
            return classes.isEmpty()?List.of():list(Wrappers.<Homework>lambdaQuery().in(Homework::getClassId,classes).orderByDesc(Homework::getId));
        }
        Set<Long> classes = visibleClassIds();
        if (classes.isEmpty()) return List.of();
        return list(Wrappers.<Homework>lambdaQuery().in(Homework::getClassId, classes).orderByDesc(Homework::getId));
    }
    private List<Homework> allHomeworks() { return super.list(Wrappers.<Homework>lambdaQuery().orderByDesc(Homework::getId)); }
    public Homework getVisible(Long id) {
        requireLogin(); Homework h = getById(id);
        if (h == null || ("TEACHER".equals(UserContext.role()) && !visibleTeacherClass(h.getClassId()))
                || (!isStaff(UserContext.role()) && !visibleClassIds().contains(h.getClassId())))
            throw BizException.notFound("作业不存在或无权访问");
        return h;
    }
    public Homework create(HomeworkCreateRequest req) {
        requireStaff();
        if (req.getClassId() == null || !StringUtils.hasText(req.getName()) || req.getQuestionIds() == null || req.getQuestionIds().isEmpty())
            throw BizException.badRequest("班级、作业名称和题目不能为空");
        if (clazzMapper.selectById(req.getClassId()) == null) throw BizException.notFound("班级不存在");
        assertTeacherClass(req.getClassId());
        List<Question> qs = questionMapper.selectBatchIds(req.getQuestionIds());
        if (qs.size() != req.getQuestionIds().stream().distinct().count()) throw BizException.badRequest("存在无效题目");
        Homework h = new Homework(); h.setClassId(req.getClassId()); h.setName(req.getName()); h.setDueTime(req.getDueTime());
        h.setPublisherId(UserContext.userId()); h.setQuestionIds(write(req.getQuestionIds()));
        h.setTotalScore(qs.stream().map(Question::getScore).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        save(h); return h;
    }
    public Map<String,Object> detail(Long id) {
        Homework h = getVisible(id); List<Long> ids = readIds(h.getQuestionIds());
        List<Question> qs = ids.isEmpty() ? List.of() : questionMapper.selectBatchIds(ids);
        Map<String,Object> out = new LinkedHashMap<>(); out.put("homework", h); out.put("questions", qs);
        if ("STUDENT".equals(UserContext.role())) {
            Student s = student(); HomeworkSubmission sub = submissionMapper.selectOne(Wrappers.<HomeworkSubmission>lambdaQuery()
                    .eq(HomeworkSubmission::getHomeworkId,id).eq(HomeworkSubmission::getStudentId,s.getId()));
            out.put("submission", sub);
            if (sub != null) out.put("answers", answerMapper.selectList(Wrappers.<SubmissionAnswer>lambdaQuery().eq(SubmissionAnswer::getSubmissionId,sub.getId())));
        }
        return out;
    }
    public HomeworkSubmission submit(Long id, HomeworkSubmitRequest req) {
        requireStudent(); Homework h = getVisible(id); Student s = student();
        HomeworkSubmission existing = submissionMapper.selectOne(Wrappers.<HomeworkSubmission>lambdaQuery().eq(HomeworkSubmission::getHomeworkId,id).eq(HomeworkSubmission::getStudentId,s.getId()));
        if (existing != null && "GRADED".equals(existing.getStatus())) throw BizException.badRequest("作业已批改，不能重复提交");
        HomeworkSubmission sub = existing == null ? new HomeworkSubmission() : existing;
        sub.setHomeworkId(id); sub.setStudentId(s.getId()); sub.setSubmitTime(LocalDateTime.now()); sub.setStatus("SUBMITTED");
        sub.setAttachmentPaths(write(req.getAttachmentPaths() == null ? List.of() : req.getAttachmentPaths())); sub.setScore(null);
        if (existing == null) submissionMapper.insert(sub); else submissionMapper.updateById(sub);
        if (existing != null) answerMapper.delete(Wrappers.<SubmissionAnswer>lambdaQuery().eq(SubmissionAnswer::getSubmissionId,sub.getId()));
        Map<Long,Question> qmap = questionMapper.selectBatchIds(readIds(h.getQuestionIds())).stream().collect(java.util.stream.Collectors.toMap(Question::getId,q->q));
        for (HomeworkSubmitRequest.Answer a : Optional.ofNullable(req.getAnswers()).orElse(List.of())) {
            Question q=qmap.get(a.getQuestionId()); if(q==null) throw BizException.badRequest("提交了不属于本作业的题目");
            SubmissionAnswer row=new SubmissionAnswer(); row.setSubmissionId(sub.getId()); row.setQuestionId(q.getId()); row.setAnswer(a.getAnswer());
            boolean objective=! "SUBJECTIVE".equals(q.getType()); row.setCorrect(objective && equalsAnswer(q.getAnswer(),a.getAnswer()));
            row.setScore(row.getCorrect() ? q.getScore() : (objective ? BigDecimal.ZERO : null)); answerMapper.insert(row);
            if (Boolean.FALSE.equals(row.getCorrect())) recordWrong(s.getId(),q);
        }
        return sub;
    }
    public HomeworkSubmission grade(Long submissionId, GradeRequest req) {
        requireStaff(); HomeworkSubmission sub=submissionMapper.selectById(submissionId);
        if(sub==null) throw BizException.notFound("提交不存在"); Homework h=getVisible(sub.getHomeworkId()); assertTeacherClass(h.getClassId());
        BigDecimal total=BigDecimal.ZERO;
        for(GradeRequest.Item i:Optional.ofNullable(req.getAnswers()).orElse(List.of())) {
            SubmissionAnswer a=answerMapper.selectOne(Wrappers.<SubmissionAnswer>lambdaQuery().eq(SubmissionAnswer::getSubmissionId,submissionId).eq(SubmissionAnswer::getQuestionId,i.getQuestionId()));
            if(a==null) throw BizException.badRequest("批改题目不属于提交"); a.setScore(i.getScore()); a.setComment(i.getComment()); answerMapper.updateById(a);
            if(i.getScore()!=null) total=total.add(i.getScore());
        }
        sub.setScore(total); sub.setStatus("GRADED"); submissionMapper.updateById(sub); return sub;
    }
    public List<WrongQuestion> wrongBook() { requireStudent(); return wrongMapper.selectList(Wrappers.<WrongQuestion>lambdaQuery().eq(WrongQuestion::getStudentId,student().getId()).orderByDesc(WrongQuestion::getWrongCount)); }
    public Map<String,Object> report() {
        requireLogin(); List<Long> studentIds;
        if ("STUDENT".equals(UserContext.role())) studentIds=List.of(student().getId());
        else if ("PARENT".equals(UserContext.role())) studentIds=studentService.studentIdsOfParent(UserContext.userId());
        else throw BizException.forbidden("只有学生或家长可以查看学情报告");
        List<HomeworkSubmission> subs=studentIds.isEmpty()?List.of():submissionMapper.selectList(Wrappers.<HomeworkSubmission>lambdaQuery().in(HomeworkSubmission::getStudentId,studentIds));
        long graded=subs.stream().filter(s->"GRADED".equals(s.getStatus())).count(); BigDecimal total=subs.stream().map(HomeworkSubmission::getScore).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        Map<String,Object> r=new LinkedHashMap<>(); r.put("submitted",subs.size()); r.put("graded",graded); r.put("score",total);
        r.put("wrongCount", "STUDENT".equals(UserContext.role()) ? wrongBook().size() : 0); return r;
    }
    private void recordWrong(Long sid,Question q) { WrongQuestion w=wrongMapper.selectOne(Wrappers.<WrongQuestion>lambdaQuery().eq(WrongQuestion::getStudentId,sid).eq(WrongQuestion::getQuestionId,q.getId())); if(w==null){w=new WrongQuestion();w.setStudentId(sid);w.setQuestionId(q.getId());w.setKnowledgePoint(q.getKnowledgePoint());w.setWrongCount(1);wrongMapper.insert(w);}else{w.setWrongCount(w.getWrongCount()+1);w.setLastWrongTime(LocalDateTime.now());wrongMapper.updateById(w);} }
    private boolean equalsAnswer(String expected,String actual){if(expected==null||actual==null)return false;try{JsonNode n=objectMapper.readTree(expected);if(n.isArray())for(JsonNode x:n)if(x.asText().equalsIgnoreCase(actual.trim()))return true;return n.asText().equalsIgnoreCase(actual.trim());}catch(Exception e){return expected.trim().equalsIgnoreCase(actual.trim());}}
    private Student student(){Student s=studentService.byUserId(UserContext.userId());if(s==null)throw BizException.forbidden("当前账号没有学生档案");return s;}
    private Set<Long> visibleClassIds(){
        String role=UserContext.role();
        if ("STUDENT".equals(role)) return classStudentMapper.selectList(Wrappers.<ClassStudent>lambdaQuery().eq(ClassStudent::getStudentId,student().getId())).stream().map(ClassStudent::getClassId).collect(java.util.stream.Collectors.toSet());
        if ("PARENT".equals(role)) {
            Set<Long> ids=new HashSet<>(); for(Long sid:studentService.studentIdsOfParent(UserContext.userId()))
                ids.addAll(classStudentMapper.selectList(Wrappers.<ClassStudent>lambdaQuery().eq(ClassStudent::getStudentId,sid)).stream().map(ClassStudent::getClassId).toList());
            return ids;
        }
        return Set.of();
    }
    private void assertTeacherClass(Long classId){
        if ("TEACHER".equals(UserContext.role())){
            Teacher t=teacherMapper.selectOne(Wrappers.<Teacher>lambdaQuery().eq(Teacher::getUserId,UserContext.userId()));
            if(t==null || clazzMapper.selectOne(Wrappers.<com.kelifang.basedata.entity.Clazz>lambdaQuery().eq(com.kelifang.basedata.entity.Clazz::getId,classId).eq(com.kelifang.basedata.entity.Clazz::getTeacherId,t.getId()))==null)
                throw BizException.forbidden("只能维护自己负责班级的作业");
        }
    }
    private boolean visibleTeacherClass(Long classId) {
        if (!"TEACHER".equals(UserContext.role())) return true;
        Teacher t=teacherMapper.selectOne(Wrappers.<Teacher>lambdaQuery().eq(Teacher::getUserId,UserContext.userId()));
        return t != null && clazzMapper.selectOne(Wrappers.<com.kelifang.basedata.entity.Clazz>lambdaQuery().eq(com.kelifang.basedata.entity.Clazz::getId,classId).eq(com.kelifang.basedata.entity.Clazz::getTeacherId,t.getId())) != null;
    }
    private void requireLogin(){if(UserContext.role()==null)throw BizException.forbidden("未登录");}
    private void requireStudent(){requireLogin();if(!"STUDENT".equals(UserContext.role()))throw BizException.forbidden("只有学生可以提交作业");}
    private void requireStaff(){requireLogin();if(!Set.of("PRINCIPAL","ACADEMIC","TEACHER").contains(UserContext.role()))throw BizException.forbidden("无权维护作业");}
    private boolean isStaff(String role){return Set.of("PRINCIPAL","ACADEMIC","TEACHER").contains(role);}
    private String write(Object value){try{return objectMapper.writeValueAsString(value);}catch(Exception e){throw BizException.badRequest("JSON 数据无效");}}
    private List<Long> readIds(String json){try{if(!StringUtils.hasText(json))return List.of();return objectMapper.readValue(json,objectMapper.getTypeFactory().constructCollectionType(List.class,Long.class));}catch(Exception e){throw BizException.badRequest("作业题目数据无效");}}
}
