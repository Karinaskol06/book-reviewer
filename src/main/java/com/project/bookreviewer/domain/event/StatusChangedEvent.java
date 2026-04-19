package com.project.bookreviewer.domain.event;

import com.project.bookreviewer.domain.model.ReadingStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StatusChangedEvent extends ApplicationEvent {
    private final Long userId;
    private final Long bookId;
    private final ReadingStatus newStatus;

    public StatusChangedEvent(Object source, Long userId, Long bookId, ReadingStatus newStatus) {
        super(source);
        this.userId = userId;
        this.bookId = bookId;
        this.newStatus = newStatus;
    }
}