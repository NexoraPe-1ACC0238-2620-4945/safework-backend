package com.nexorape.safework.service.incidentmanagement.application.internal.commandservices;
import com.nexorape.safework.service.iam.application.internal.security.AccessPolicy;
import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.incidentmanagement.domain.model.aggregates.Incident;
import com.nexorape.safework.service.incidentmanagement.domain.model.entities.Assignment;
import com.nexorape.safework.service.incidentmanagement.domain.model.commands.assignment.*;
import com.nexorape.safework.service.incidentmanagement.domain.model.commands.incident.*;
import com.nexorape.safework.service.incidentmanagement.domain.model.events.*;
import com.nexorape.safework.service.incidentmanagement.domain.model.valueobjects.assignment.AssignmentPriority;
import com.nexorape.safework.service.incidentmanagement.domain.services.IncidentCommandService;
import com.nexorape.safework.service.incidentmanagement.infrastructure.persistence.jpa.repositories.*;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
@Service @Transactional
public class IncidentCommandServiceImpl implements IncidentCommandService {
    private final IncidentRepository incidents; private final AssignmentRepository assignments; private final AccessPolicy access; private final ApplicationEventPublisher events;
    public IncidentCommandServiceImpl(IncidentRepository incidents,AssignmentRepository assignments,AccessPolicy access,ApplicationEventPublisher events) { this.incidents=incidents;this.assignments=assignments;this.access=access;this.events=events; }
    private Incident lock(Long id,User actor) { if(id==null || id<=0) throw ApiException.validation(); var item=incidents.lockByIdAndCompany(id,actor.getCompanyId()).orElseThrow(ApiException::notFound); item.getUser().getFullName(); if(item.getAssignment()!=null) item.getAssignment().getUser().getFullName(); return item; }
    private void responsible(Incident item,User actor) {
        if(!access.has(actor,"EMPLOYER")) throw ApiException.forbidden();
        if(item.getAssignment()==null) throw ApiException.state();
        if(!item.getAssignment().getUserId().equals(actor.getId())) throw new ApiException(403,"NOT_RESPONSIBLE","Only the responsible user may perform this operation.");
    }
    public Optional<Incident> handle(CreateIncidentCommand command) {
        var actor=access.mobile();
        if(!actor.getId().equals(command.userId()) || !actor.getCompanyId().equals(command.companyId())) throw ApiException.validation();
        var item=incidents.saveAndFlush(new Incident(actor,actor.getCompany(),command));
        events.publishEvent(new IncidentCreatedEvent(item.getId(),item.getTitle(),item.getDescription(),item.getCompanyId(),actor.getId())); return Optional.of(item);
    }
    public Optional<Assignment> handle(CreateAssignmentCommand command) {
        var actor=access.employer(); if(!actor.getId().equals(command.userId())) throw ApiException.validation();
        var item=lock(command.incidentId(),actor); item.assignTo(actor); incidents.saveAndFlush(item);
        events.publishEvent(new IncidentAssignedEvent(item.getId(),actor.getId())); return Optional.of(item.getAssignment());
    }
    public Optional<Incident> handle(StartIncidentProgressCommand command) {
        var actor=access.mobile(); var item=lock(command.incidentId(),actor); responsible(item,actor);
        if(!actor.getId().equals(command.userId())) throw ApiException.validation();
        item.startProgress(); incidents.saveAndFlush(item); events.publishEvent(new IncidentStatusChangedEvent(item.getId(),item.getStatus(),actor.getId())); return Optional.of(item);
    }
    public Optional<Incident> handle(CloseIncidentCommand command) {
        var actor=access.mobile(); var item=lock(command.incidentId(),actor); responsible(item,actor);
        if(!actor.getId().equals(command.userId())) throw ApiException.validation();
        item.close(); incidents.saveAndFlush(item); events.publishEvent(new IncidentStatusChangedEvent(item.getId(),item.getStatus(),actor.getId())); return Optional.of(item);
    }
    public Optional<Assignment> handle(UpdateAssignmentPriorityCommand command) {
        var actor=access.mobile(); var assignment=assignments.findById(command.assignmentId()).orElseThrow(ApiException::notFound);
        var item=lock(assignment.getIncidentId(),actor); responsible(item,actor); assignment.updatePriority(AssignmentPriority.valueOf(command.priority())); return Optional.of(assignments.saveAndFlush(assignment));
    }
    public Optional<Incident> handle(UpdateIncidentDocumentCommand command) {
        var actor=access.mobile(); var item=lock(command.incidentId(),actor); if(!item.getUserId().equals(actor.getId())) responsible(item,actor);
        item.updateDocumentUrl(command.documentUrl()); return Optional.of(incidents.saveAndFlush(item));
    }
}
