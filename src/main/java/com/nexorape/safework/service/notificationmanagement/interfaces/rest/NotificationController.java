package com.nexorape.safework.service.notificationmanagement.interfaces.rest;
import com.nexorape.safework.service.notificationmanagement.application.internal.queryservices.NotificationResponse;
import com.nexorape.safework.service.notificationmanagement.application.internal.transform.NotificationAssembler;
import com.nexorape.safework.service.notificationmanagement.infrastructure.persistence.jpa.repositories.NotificationRepository;
import com.nexorape.safework.service.iam.application.internal.security.AccessPolicy;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@RestController @RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationRepository notifications; private final NotificationAssembler assembler; private final AccessPolicy access;
    public NotificationController(NotificationRepository notifications,NotificationAssembler assembler,AccessPolicy access) { this.notifications=notifications;this.assembler=assembler;this.access=access; }
    @GetMapping("/my-notifications") @Transactional(readOnly=true)
    public List<NotificationResponse> own() {
        var actor=access.mobile();
        return notifications.findByRecipientIdAndCompanyIdOrderByCreatedAtDesc(actor.getId().toString(),actor.getCompanyId()).stream().map(assembler::toResponseFrom).toList();
    }
}
