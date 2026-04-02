package org.pt.test.tasks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NewTaskRequest {
    @NotBlank(message = "Наименование не может быть пустым")
    private String title;
    @Positive(message = "Не может быть отрицательным числом")
    private Long userId;
    @NotBlank(message = "Описание не может быть пустым")
    private String description;
}
