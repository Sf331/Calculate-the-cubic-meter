package com.kelifang.homework.controller;

import com.kelifang.common.Result;
import com.kelifang.homework.dto.*;
import com.kelifang.homework.entity.*;
import com.kelifang.homework.service.HomeworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/homework")
@RequiredArgsConstructor
public class HomeworkController {
    private final HomeworkService service;
    @GetMapping("/list") public Result<List<Homework>> list(){return Result.ok(service.list());}
    @GetMapping("/{id}") public Result<Map<String,Object>> detail(@PathVariable Long id){return Result.ok(service.detail(id));}
    @PostMapping public Result<Homework> create(@RequestBody HomeworkCreateRequest req){return Result.ok(service.create(req));}
    @GetMapping("/questions") public Result<List<Question>> questions(){return Result.ok(service.questions());}
    @PostMapping("/questions") public Result<Question> createQuestion(@RequestBody QuestionRequest req){return Result.ok(service.saveQuestion(req,null));}
    @PutMapping("/questions/{id}") public Result<Question> updateQuestion(@PathVariable Long id,@RequestBody QuestionRequest req){return Result.ok(service.saveQuestion(req,id));}
    @PostMapping("/{id}/submit") public Result<HomeworkSubmission> submit(@PathVariable Long id,@RequestBody HomeworkSubmitRequest req){return Result.ok(service.submit(id,req));}
    @PostMapping("/submissions/{id}/grade") public Result<HomeworkSubmission> grade(@PathVariable Long id,@RequestBody GradeRequest req){return Result.ok(service.grade(id,req));}
    @GetMapping("/wrong-book") public Result<List<WrongQuestion>> wrongBook(){return Result.ok(service.wrongBook());}
    @GetMapping("/report") public Result<Map<String,Object>> report(){return Result.ok(service.report());}
}
