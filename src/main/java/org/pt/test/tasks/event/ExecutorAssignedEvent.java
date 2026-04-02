package org.pt.test.tasks.event;

public record ExecutorAssignedEvent(Long taskId, Long executorId)  {
}
