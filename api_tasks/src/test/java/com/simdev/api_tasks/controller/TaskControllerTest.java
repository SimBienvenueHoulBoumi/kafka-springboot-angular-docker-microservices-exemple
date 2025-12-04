package com.simdev.api_tasks.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simdev.api_tasks.task.controller.TaskController;
import com.simdev.api_tasks.task.domain.Task;
import com.simdev.api_tasks.task.dto.TaskRequest;
import com.simdev.api_tasks.task.dto.TaskResponse;
import com.simdev.api_tasks.task.service.TaskService;
import com.simdev.api_tasks.shared.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(controllers = TaskController.class)
@DisplayName("TaskController Tests")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;
    
    @MockitoBean
    private SecurityUtils securityUtils;

    @Test
    @DisplayName("Should create task successfully")
    void testCreateTask_Success() throws Exception {
        // Given
        TaskRequest request = new TaskRequest();
        request.setTitle("Test Task");
        request.setDescription("Test Description");
        request.setStatus(Task.TaskStatus.PENDING);

        TaskResponse response = TaskResponse.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(Task.TaskStatus.PENDING)
                .userId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(securityUtils.getUserIdFromRequest(any())).thenReturn(1L);
        when(taskService.createTask(any(TaskRequest.class), eq(1L))).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Task"));
    }

    @Test
    @DisplayName("Should get all tasks")
    void testGetAllTasks() throws Exception {
        // Given
        List<TaskResponse> tasks = Arrays.asList(
                TaskResponse.builder()
                        .id(1L)
                        .title("Task 1")
                        .status(Task.TaskStatus.PENDING)
                        .userId(1L)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                TaskResponse.builder()
                        .id(2L)
                        .title("Task 2")
                        .status(Task.TaskStatus.IN_PROGRESS)
                        .userId(1L)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        when(securityUtils.getUserIdFromRequest(any())).thenReturn(1L);
        when(securityUtils.isAdmin(any())).thenReturn(false);
        when(taskService.getTasksByUserId(eq(1L), eq(false))).thenReturn(tasks);

        // When/Then
        mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should get task by ID")
    void testGetTaskById() throws Exception {
        // Given
        TaskResponse response = TaskResponse.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(Task.TaskStatus.PENDING)
                .userId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(securityUtils.getUserIdFromRequest(any())).thenReturn(1L);
        when(securityUtils.isAdmin(any())).thenReturn(false);
        when(taskService.getTaskById(eq(1L), eq(1L), eq(false))).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/tasks/1")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Task"));
    }

    @Test
    @DisplayName("Should get tasks by user ID")
    void testGetTasksByUserId() throws Exception {
        // Given
        List<TaskResponse> tasks = Arrays.asList(
                TaskResponse.builder()
                        .id(1L)
                        .title("Task 1")
                        .userId(1L)
                        .status(Task.TaskStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        when(securityUtils.getUserIdFromRequest(any())).thenReturn(1L);
        when(securityUtils.isAdmin(any())).thenReturn(false);
        when(taskService.getTasksByUserId(eq(1L), eq(false))).thenReturn(tasks);

        // When/Then
        mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Should update task successfully")
    void testUpdateTask_Success() throws Exception {
        // Given
        TaskRequest request = new TaskRequest();
        request.setTitle("Updated Task");
        request.setDescription("Updated Description");
        request.setStatus(Task.TaskStatus.IN_PROGRESS);

        TaskResponse response = TaskResponse.builder()
                .id(1L)
                .title("Updated Task")
                .description("Updated Description")
                .status(Task.TaskStatus.IN_PROGRESS)
                .userId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(securityUtils.getUserIdFromRequest(any())).thenReturn(1L);
        when(securityUtils.isAdmin(any())).thenReturn(false);
        when(taskService.updateTask(eq(1L), any(TaskRequest.class), eq(1L), eq(false))).thenReturn(response);

        // When/Then
        mockMvc.perform(put("/tasks/1")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Task"));
    }

    @Test
    @DisplayName("Should delete task successfully")
    void testDeleteTask_Success() throws Exception {
        // Given
        when(securityUtils.getUserIdFromRequest(any())).thenReturn(1L);
        when(securityUtils.isAdmin(any())).thenReturn(false);
        doNothing().when(taskService).deleteTask(eq(1L), eq(1L), eq(false));

        // When/Then
        mockMvc.perform(delete("/tasks/1")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deleteTask(eq(1L), eq(1L), eq(false));
    }
}

