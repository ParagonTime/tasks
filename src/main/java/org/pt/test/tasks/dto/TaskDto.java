package org.pt.test.tasks.dto;

import lombok.Data;

@Data
public class TaskDto {
    private Long id;
    private String title;
    private UserDto user;
    private String description;
    private TaskStatus status;
}
