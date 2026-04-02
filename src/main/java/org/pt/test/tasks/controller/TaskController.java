package org.pt.test.tasks.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.pt.test.tasks.dto.NewTaskRequest;
import org.pt.test.tasks.dto.TaskDto;
import org.pt.test.tasks.dto.TaskStatus;
import org.pt.test.tasks.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto createTask(@Valid @RequestBody NewTaskRequest newTaskRequest) {
        return taskService.createTask(newTaskRequest);
    }

    @GetMapping
    public Page<TaskDto> getTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return taskService.getTasks(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public TaskDto getTaskById(@Positive @PathVariable("id") Long taskId) {
        return taskService.getTaskById(taskId);
    }

    @PatchMapping("/{id}/executor/{userId}")
    public void setTaskExecutor(@Positive @PathVariable("id") Long taskId,
                                @Positive @PathVariable("userId") Long userId
    ) {
        taskService.setTaskExecutor(taskId, userId);
    }

    @PatchMapping("/{id}/status/{status}")
    public void setTaskStatus(@Positive @PathVariable("id") Long taskId,
                              @NotNull @PathVariable("status") TaskStatus status

    ) {
        taskService.setTaskStatus(taskId, status);
    }
}
