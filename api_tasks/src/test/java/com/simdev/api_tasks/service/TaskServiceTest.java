package com.simdev.api_tasks.service;

import com.simdev.api_tasks.task.domain.Task;
import com.simdev.api_tasks.task.dto.TaskRequest;
import com.simdev.api_tasks.task.dto.TaskResponse;
import com.simdev.api_tasks.task.exception.ResourceNotFoundException;
import com.simdev.api_tasks.task.repository.TaskRepository;
import com.simdev.api_tasks.task.service.TaskService;
import com.simdev.api_tasks.shared.service.EventService;
import com.simdev.api_tasks.shared.service.UserCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventService eventService;

    @Mock
    private UserCacheService userCacheService;

    @InjectMocks
    private TaskService taskService;

    private Task testTask;
    private TaskRequest taskRequest;

    @BeforeEach
    void setUp() {
        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(Task.TaskStatus.PENDING)
                .userId(1L)
                .build();

        taskRequest = new TaskRequest();
        taskRequest.setTitle("New Task");
        taskRequest.setDescription("New Description");
        taskRequest.setStatus(Task.TaskStatus.PENDING);
    }

    @Test
    @DisplayName("Should create task successfully")
    void testCreateTask_Success() {
        // Given
        doNothing().when(userCacheService).userExists(1L);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        doNothing().when(eventService).publishTaskEvent(any());

        // When
        TaskResponse response = taskService.createTask(taskRequest, 1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo(testTask.getTitle());
        verify(userCacheService, times(1)).userExists(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(eventService, times(1)).publishTaskEvent(any());
    }

    @Test
    @DisplayName("Should get all tasks")
    void testGetAllTasks() {
        // Given
        Task task2 = Task.builder().id(2L).title("Task 2").description("Desc 2").status(Task.TaskStatus.IN_PROGRESS).userId(2L).build();
        when(taskRepository.findAll()).thenReturn(Arrays.asList(testTask, task2));

        // When - getTasksByUserId avec isAdmin=true retourne toutes les tâches
        List<TaskResponse> tasks = taskService.getTasksByUserId(1L, true);

        // Then
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).getTitle()).isEqualTo(testTask.getTitle());
    }

    @Test
    @DisplayName("Should get task by ID")
    void testGetTaskById_Success() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        // When
        TaskResponse response = taskService.getTaskById(1L, 1L, false);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo(testTask.getTitle());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when task not found")
    void testGetTaskById_NotFound() {
        // Given
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> taskService.getTaskById(999L, 1L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get tasks by user ID")
    void testGetTasksByUserId() {
        // Given
        Task task2 = Task.builder().id(2L).title("Task 2").userId(1L).status(Task.TaskStatus.PENDING).build();
        when(taskRepository.findByUserId(1L)).thenReturn(Arrays.asList(testTask, task2));

        // When
        List<TaskResponse> tasks = taskService.getTasksByUserId(1L);

        // Then
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).getUserId()).isEqualTo(1L);
        assertThat(tasks.get(1).getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should update task successfully")
    void testUpdateTask_Success() {
        // Given
        TaskRequest updateRequest = new TaskRequest();
        updateRequest.setTitle("Updated Task");
        updateRequest.setDescription("Updated Description");
        updateRequest.setStatus(Task.TaskStatus.IN_PROGRESS);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        doNothing().when(eventService).publishTaskEvent(any());

        // When
        TaskResponse response = taskService.updateTask(1L, updateRequest, 1L, false);

        // Then
        assertThat(response).isNotNull();
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(eventService, times(1)).publishTaskEvent(any());
    }

    @Test
    @DisplayName("Should delete task successfully")
    void testDeleteTask_Success() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        doNothing().when(taskRepository).delete(testTask);
        doNothing().when(eventService).publishTaskEvent(any());

        // When
        taskService.deleteTask(1L, 1L, false);

        // Then
        verify(taskRepository, times(1)).delete(testTask);
        verify(eventService, times(1)).publishTaskEvent(any());
    }
}

