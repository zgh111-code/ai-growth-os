package com.al.aichat.controller;

import com.al.aichat.common.Result;
import com.al.aichat.entity.ProjectRecord;
import com.al.aichat.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/submit")
    public Result<ProjectRecord> submit(@RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String projectName = (String) body.get("projectName");
        String status = (String) body.get("status");
        String blocker = (String) body.get("blocker");
        String progress = (String) body.get("progress");
        ProjectRecord record = projectService.submit(userId, projectName, status, blocker, progress);
        return Result.success("分析完成", record);
    }

    @GetMapping("/list")
    public Result<List<ProjectRecord>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(projectService.list(userId));
    }

    @GetMapping("/{id}")
    public Result<ProjectRecord> getById(@PathVariable Long id,
                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(projectService.getById(id, userId));
    }
}
