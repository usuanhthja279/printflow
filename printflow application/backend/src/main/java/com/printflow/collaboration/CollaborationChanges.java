package com.printflow.collaboration;

import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Emits intent only; the broker broadcasts after the enclosing database transaction commits. */
@Component
public final class CollaborationChanges {
    private final ApplicationEventPublisher events;
    public CollaborationChanges(ApplicationEventPublisher events){this.events=events;}
    public void changed(UUID projectId,String type,UUID proofRevisionId){events.publishEvent(new CollaborationBroker.ProjectChange(projectId,type,proofRevisionId));}
}
