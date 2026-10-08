package com.nexorape.safework.service.notificationmanagement.application.internal.eventhandlers;

import com.nexorape.safework.service.incidentmanagement.domain.model.events.IncidentAssignedEvent;
import com.nexorape.safework.service.incidentmanagement.domain.model.events.IncidentCreatedEvent;
import com.nexorape.safework.service.notificationmanagement.application.internal.transform.NotificationAssembler;
import com.nexorape.safework.service.notificationmanagement.infrastructure.persistence.jpa.repositories.NotificationRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens to Incident domain events and creates Notification records accordingly.
 * This component reacts to events from the Incident Management context and
 * persists notifications without containing business logic.
 */

@Component
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final NotificationAssembler notificationAssembler;

    public NotificationEventListener(NotificationRepository notificationRepository,
            NotificationAssembler notificationAssembler) {
        this.notificationRepository = notificationRepository;
        this.notificationAssembler = notificationAssembler;
    }

    @EventListener
    public void on(IncidentCreatedEvent event) {
        var notification = notificationAssembler.toEntityFrom(event);
        notificationRepository.save(notification);
    }

    @EventListener
    public void on(IncidentAssignedEvent event) {
        var notification = notificationAssembler.toEntityFrom(event);
        notificationRepository.save(notification);
    }

    @EventListener
    public void on(
            com.nexorape.safework.service.incidentmanagement.domain.model.events.IncidentStatusChangedEvent event) {
        var notification = notificationAssembler.toEntityFrom(event);
        notificationRepository.save(notification);
    }
}
