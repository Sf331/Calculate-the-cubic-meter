package com.kelifang.courseware.controller;
import com.kelifang.common.Result;
import com.kelifang.courseware.dto.CoursewareAnswerRequest;
import com.kelifang.courseware.entity.Courseware;
import com.kelifang.courseware.service.CoursewareService;
import com.kelifang.courseware.vo.CoursewareStats;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/courseware")
@RequiredArgsConstructor
public class CoursewareController {
    private final CoursewareService service;
    @GetMapping public Result<List<Courseware>> rootList() { return Result.ok(service.listVisible()); }
    @GetMapping("/list") public Result<List<Courseware>> list() { return Result.ok(service.listVisible()); }
    @GetMapping("/{id}") public Result<Courseware> get(@PathVariable Long id) { return Result.ok(service.getVisible(id)); }
    @PostMapping public Result<Courseware> create(@RequestBody Courseware value) { value.setId(null); return Result.ok(service.saveChecked(value)); }
    @PutMapping("/{id}") public Result<Courseware> update(@PathVariable Long id, @RequestBody Courseware value) { value.setId(id); return Result.ok(service.saveChecked(value)); }
    @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id) { service.removeChecked(id); return Result.ok(); }
    @PostMapping("/{id}/answers") public Result<?> answer(@PathVariable Long id, @RequestBody CoursewareAnswerRequest request) { return Result.ok(service.answer(id, request)); }
    @GetMapping("/{id}/stats") public Result<CoursewareStats> stats(@PathVariable Long id) { return Result.ok(service.stats(id)); }
}
