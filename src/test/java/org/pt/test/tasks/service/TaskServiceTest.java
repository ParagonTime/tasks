package org.pt.test.tasks.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pt.test.tasks.dto.NewTaskRequest;
import org.pt.test.tasks.dto.TaskDto;
import org.pt.test.tasks.dto.TaskStatus;
import org.pt.test.tasks.entity.Task;
import org.pt.test.tasks.entity.User;
import org.pt.test.tasks.event.ExecutorAssignedEvent;
import org.pt.test.tasks.event.StatusChangedEvent;
import org.pt.test.tasks.event.TaskCreatedEvent;
import org.pt.test.tasks.exception.NoFoundException;
import org.pt.test.tasks.mapper.TaskMapper;
import org.pt.test.tasks.repository.TaskRepository;
import org.pt.test.tasks.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks
    private TaskService taskService;

    private Task task;
    private TaskDto taskDto;
    private User user;
    private NewTaskRequest newTaskRequest;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();

        user = new User();
        user.setId(1L);
        user.setName("John");
        user.setEmail("john@example.com");

        task = new Task();
        task.setId(100L);
        task.setTitle("Test Task");
        task.setDescription("Description");
        task.setStatus(TaskStatus.CREATED);
        task.setUser(user);

        taskDto = new TaskDto();
        taskDto.setId(100L);
        taskDto.setTitle("Test Task");
        taskDto.setStatus(TaskStatus.CREATED);

        newTaskRequest = new NewTaskRequest();
        newTaskRequest.setTitle("Test Task");
        newTaskRequest.setDescription("Description");
        newTaskRequest.setUserId(1L);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void createTask_shouldCreateAndSendEvent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(taskMapper.toTaskEntity(newTaskRequest)).thenReturn(task);
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toTaskDto(task)).thenReturn(taskDto);

        TaskDto result = taskService.createTask(newTaskRequest);

        assertThat(result).isEqualTo(taskDto);
        verify(taskRepository).save(task);
        verify(taskMapper).toTaskDto(task);

        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertThat(syncs).hasSize(1);
        syncs.forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("task-created"), eventCaptor.capture());
        TaskCreatedEvent sentEvent = (TaskCreatedEvent) eventCaptor.getValue();
        assertThat(sentEvent.taskId()).isEqualTo(100L);
        assertThat(sentEvent.title()).isEqualTo("Test Task");
        assertThat(sentEvent.userId()).isEqualTo(1L);
    }

    @Test
    void createTask_whenUserNotFound_shouldThrowNoFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        newTaskRequest.setUserId(99L);
        when(taskMapper.toTaskEntity(newTaskRequest)).thenReturn(task);

        assertThatThrownBy(() -> taskService.createTask(newTaskRequest))
                .isInstanceOf(NoFoundException.class)
                .hasMessageContaining("User no found id: 99");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void getTasks_shouldReturnPageOfTaskDto() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Task> taskPage = new PageImpl<>(List.of(task));
        when(taskRepository.findAllWithUser(pageable)).thenReturn(taskPage);
        when(taskMapper.toTaskDto(task)).thenReturn(taskDto);

        Page<TaskDto> result = taskService.getTasks(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(taskDto);
        verify(taskRepository).findAllWithUser(pageable);
    }

    @Test
    void getTaskById_whenExists_shouldReturnTaskDto() {
        when(taskRepository.findTaskById(100L)).thenReturn(Optional.of(task));
        when(taskMapper.toTaskDto(task)).thenReturn(taskDto);

        TaskDto result = taskService.getTaskById(100L);

        assertThat(result).isEqualTo(taskDto);
        verify(taskRepository).findTaskById(100L);
    }

    @Test
    void getTaskById_whenNotExists_shouldThrowNoFoundException() {
        when(taskRepository.findTaskById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(999L))
                .isInstanceOf(NoFoundException.class)
                .hasMessageContaining("Task no found: 999");
    }

    @Test
    void setTaskExecutor_shouldAssignExecutorAndSendEvent() {
        when(taskRepository.findTaskById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        taskService.setTaskExecutor(100L, 2L);

        assertThat(task.getUser()).isEqualTo(user);
        verify(taskRepository).save(task);

        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertThat(syncs).hasSize(1);
        syncs.forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("executor-assigned"), eventCaptor.capture());
        ExecutorAssignedEvent sentEvent = (ExecutorAssignedEvent) eventCaptor.getValue();
        assertThat(sentEvent.taskId()).isEqualTo(100L);
        assertThat(sentEvent.executorId()).isEqualTo(2L);
    }

    @Test
    void setTaskExecutor_whenTaskNotFound_shouldThrowException() {
        when(taskRepository.findTaskById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.setTaskExecutor(100L, 1L))
                .isInstanceOf(NoFoundException.class)
                .hasMessageContaining("Task no found: 100");
        verify(userRepository, never()).findById(any());
    }

    @Test
    void setTaskExecutor_whenUserNotFound_shouldThrowException() {
        when(taskRepository.findTaskById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.setTaskExecutor(100L, 99L))
                .isInstanceOf(NoFoundException.class)
                .hasMessageContaining("User no found id: 99");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void setTaskStatus_shouldChangeStatusAndSendEvent() {
        when(taskRepository.findTaskById(100L)).thenReturn(Optional.of(task));

        taskService.setTaskStatus(100L, TaskStatus.IN_PROGRESS);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(taskRepository).save(task);

        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertThat(syncs).hasSize(1);
        syncs.forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("status-changed"), eventCaptor.capture());
        StatusChangedEvent sentEvent = (StatusChangedEvent) eventCaptor.getValue();
        assertThat(sentEvent.taskId()).isEqualTo(100L);
        assertThat(sentEvent.newStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void setTaskStatus_whenTaskNotFound_shouldThrowException() {
        when(taskRepository.findTaskById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.setTaskStatus(100L, TaskStatus.COMPLETED))
                .isInstanceOf(NoFoundException.class)
                .hasMessageContaining("Task no found: 100");
    }
}