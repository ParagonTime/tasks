package org.pt.test.tasks.event;

import org.pt.test.tasks.dto.TaskStatus;

public record StatusChangedEvent(Long taskId, TaskStatus newStatus) {
}
