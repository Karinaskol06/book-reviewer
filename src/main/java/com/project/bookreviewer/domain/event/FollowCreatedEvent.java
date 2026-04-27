package com.project.bookreviewer.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FollowCreatedEvent extends ApplicationEvent {
    private final Long followerId;
    private final Long followingId;

    public FollowCreatedEvent(Object source, Long followerId, Long followingId) {
        super(source);
        this.followerId = followerId;
        this.followingId = followingId;
    }
}
