package com.nexorape.safework.service.incidentmanagement.interfaces.rest;
import com.nexorape.safework.service.incidentmanagement.domain.model.commands.assignment.*;
import com.nexorape.safework.service.incidentmanagement.domain.model.queries.assignment.*;
import com.nexorape.safework.service.incidentmanagement.domain.services.*;
import com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.assignment.*;
import com.nexorape.safework.service.incidentmanagement.interfaces.rest.transform.assignment.AssignmentResourceFromEntityAssembler;
import com.nexorape.safework.service.iam.application.internal.security.AccessPolicy;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import jakarta.validation.Valid;
import java.util.List;
@RestController @RequestMapping(value="/api/v1/assignments",produces=MediaType.APPLICATION_JSON_VALUE)
public class AssignmentsController {
    private final IncidentCommandService commands; private final AssignmentQueryService queries; private final AccessPolicy access;
    public AssignmentsController(IncidentCommandService commands,AssignmentQueryService queries,AccessPolicy access) { this.commands=commands;this.queries=queries;this.access=access; }
    @PostMapping public ResponseEntity<AssignmentResource> take(@Valid @RequestBody CreateAssignmentResource resource) {
        var assignment=commands.handle(new CreateAssignmentCommand(resource.incidentId(),access.mobile().getId())).orElseThrow(ApiException::notFound);
        return ResponseEntity.status(201).body(AssignmentResourceFromEntityAssembler.toResourceFromEntity(assignment));
    }
    @GetMapping public List<AssignmentResource> list() {
        return queries.handle(new GetAssignmentsByUserIdQuery(access.mobile().getId())).stream().map(AssignmentResourceFromEntityAssembler::toResourceFromEntity).toList();
    }
    @GetMapping("/{assignmentId}") public AssignmentResource detail(@PathVariable Long assignmentId) {
        return AssignmentResourceFromEntityAssembler.toResourceFromEntity(queries.handle(new GetAssignmentByIdQuery(assignmentId)).orElseThrow(ApiException::notFound));
    }
    @PatchMapping("/{assignmentId}/priority") public AssignmentResource priority(@PathVariable Long assignmentId,@Valid @RequestBody UpdateAssignmentPriorityResource resource) {
        return AssignmentResourceFromEntityAssembler.toResourceFromEntity(commands.handle(new UpdateAssignmentPriorityCommand(assignmentId,resource.priority())).orElseThrow(ApiException::notFound));
    }
}
