package com.simdev.api_tasks.task.dto;

import com.simdev.api_tasks.task.domain.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskRequest {
    
    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 3, max = 200, message = "Le titre doit contenir entre 3 et 200 caractères")
    private String title;
    
    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    private String description;
    
    private Task.TaskStatus status;
}

