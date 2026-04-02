package org.pt.test.tasks.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NewUserRequest {
    @NotBlank(message = "Имя не может быть пустым")
    private String name;
    @NotBlank(message = "Почта не может быть пустым")
    @Email(message = "Неверный формат почты")
    private String email;
}
