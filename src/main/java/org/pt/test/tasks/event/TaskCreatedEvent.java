package org.pt.test.tasks.event;

public record TaskCreatedEvent(Long taskId, String title, Long userId) {
}
