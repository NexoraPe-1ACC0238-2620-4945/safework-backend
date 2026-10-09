package com.nexorape.safework.service.incidentmanagement.application.internal.queryservices;
import com.nexorape.safework.service.incidentmanagement.domain.model.entities.Assignment;
import com.nexorape.safework.service.incidentmanagement.domain.model.queries.assignment.*;
import com.nexorape.safework.service.incidentmanagement.domain.services.AssignmentQueryService;
import com.nexorape.safework.service.incidentmanagement.infrastructure.persistence.jpa.repositories.AssignmentRepository;
import com.nexorape.safework.service.iam.application.internal.security.AccessPolicy;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @Transactional(readOnly=true)
public class AssignmentQueryServiceImpl implements AssignmentQueryService {
    private final AssignmentRepository assignments; private final AccessPolicy access;
    public AssignmentQueryServiceImpl(AssignmentRepository assignments,AccessPolicy access) { this.assignments=assignments;this.access=access; }
    public Optional<Assignment> handle(GetAssignmentByIdQuery query) {
        var actor=access.mobile(); return Optional.of(assignments.findById(query.assignmentId()).filter(a -> a.getUserId().equals(actor.getId()) && a.getIncident().getCompanyId().equals(actor.getCompanyId())).orElseThrow(ApiException::notFound));
    }
    public List<Assignment> handle(GetAllAssignmentsQuery query) { return own(); }
    public List<Assignment> handle(GetAssignmentsByUserIdQuery query) { if(!access.mobile().getId().equals(query.userId())) throw ApiException.notFound(); return own(); }
    private List<Assignment> own() { var actor=access.mobile(); return assignments.findByUserId(actor.getId()).stream().filter(a -> a.getIncident().getCompanyId().equals(actor.getCompanyId())).toList(); }
}
