package org.pt.test.tasks.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public TaskDto createTask(NewTaskRequest newTaskRequest) {
        log.info("Creating task with title: {}", newTaskRequest.getTitle());
        Task task = taskMapper.toTaskEntity(newTaskRequest);
        if (newTaskRequest.getUserId() != null) {
            User user = userRepository.findById(newTaskRequest.getUserId())
                    .orElseThrow(() -> new NoFoundException("User no found id: " + newTaskRequest.getUserId()));
            task.setUser(user);
        }
        task.setStatus(TaskStatus.CREATED);
        Task savedTask = taskRepository.save(task);
        log.info("Task created with id: {}", savedTask.getId());

        TaskCreatedEvent event = new TaskCreatedEvent(
                savedTask.getId(),
                savedTask.getTitle(),
                savedTask.getUser() != null ? savedTask.getUser().getId() : null
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send("task-created", event);
            }
        });

        return taskMapper.toTaskDto(savedTask);
    }

    @Transactional(readOnly = true)
    public Page<TaskDto> getTasks(Pageable pageable) {
        log.debug("Getting tasks with pageable: {}", pageable);
        return taskRepository.findAllWithUser(pageable).map(taskMapper::toTaskDto);
    }

    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long taskId) {
        log.debug("Getting task by id: {}", taskId);
        Task task = taskRepository.findTaskById(taskId)
                .orElseThrow(() -> new NoFoundException("Task no found: " + taskId));

        return taskMapper.toTaskDto(task);
    }

    @Transactional
    public void setTaskExecutor(Long taskId, Long userId) {
        log.info("Setting executor for task {} to user {}", taskId, userId);
        Task task = taskRepository.findTaskById(taskId)
                .orElseThrow(() -> new NoFoundException("Task no found: " + taskId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoFoundException("User no found id: " + userId));
        task.setUser(user);

        taskRepository.save(task);


        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send("executor-assigned", new ExecutorAssignedEvent(taskId, userId));
            }
        });
    }

    @Transactional
    public void setTaskStatus(Long taskId, TaskStatus status) {
        log.info("Setting task {} status to {}", taskId, status);

        Task task = taskRepository.findTaskById(taskId)
                .orElseThrow(() -> new NoFoundException("Task no found: " + taskId));
        task.setStatus(status);

        taskRepository.save(task);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send("status-changed", new StatusChangedEvent(taskId, status));
            }
        });
    }
}
