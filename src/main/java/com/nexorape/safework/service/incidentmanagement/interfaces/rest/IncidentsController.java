package com.nexorape.safework.service.incidentmanagement.interfaces.rest;
import com.nexorape.safework.service.incidentmanagement.domain.model.commands.incident.*;
import com.nexorape.safework.service.incidentmanagement.domain.model.queries.incident.*;
import com.nexorape.safework.service.incidentmanagement.domain.services.*;
import com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.incident.*;
import com.nexorape.safework.service.incidentmanagement.interfaces.rest.transform.incidents.IncidentResourceFromEntityAssembler;
import com.nexorape.safework.service.iam.application.internal.security.AccessPolicy;
import com.nexorape.safework.service.shared.application.errors.ApiException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import jakarta.validation.Valid;
import java.util.List;
@RestController @RequestMapping(value="/api/v1/incidents",produces=MediaType.APPLICATION_JSON_VALUE)
public class IncidentsController {
    private final IncidentCommandService commands; private final IncidentQueryService queries; private final AccessPolicy access;
    public IncidentsController(IncidentCommandService commands,IncidentQueryService queries,AccessPolicy access) { this.commands=commands;this.queries=queries;this.access=access; }
    @PostMapping public ResponseEntity<IncidentResource> create(@Valid @RequestBody CreateIncidentResource resource) {
        var actor=access.mobile();
        var item=commands.handle(new CreateIncidentCommand(actor.getId(),actor.getCompanyId(),resource.title(),resource.description(),resource.location())).orElseThrow(ApiException::notFound);
        return ResponseEntity.status(201).body(IncidentResourceFromEntityAssembler.toResourceFromEntity(item));
    }
    @GetMapping public List<IncidentResource> list() { return queries.handle(new GetAllIncidentsQuery()).stream().map(IncidentResourceFromEntityAssembler::toResourceFromEntity).toList(); }
    @GetMapping("/{incidentId}") public IncidentResource detail(@PathVariable Long incidentId) {
        return IncidentResourceFromEntityAssembler.toResourceFromEntity(queries.handle(new GetIncidentByIdQuery(incidentId)).orElseThrow(ApiException::notFound));
    }
    @PostMapping("/{incidentId}/start") public IncidentResource start(@PathVariable Long incidentId) {
        return IncidentResourceFromEntityAssembler.toResourceFromEntity(commands.handle(new StartIncidentProgressCommand(incidentId,access.mobile().getId())).orElseThrow(ApiException::notFound));
    }
    @PostMapping("/{incidentId}/close") public IncidentResource close(@PathVariable Long incidentId) {
        return IncidentResourceFromEntityAssembler.toResourceFromEntity(commands.handle(new CloseIncidentCommand(incidentId,access.mobile().getId())).orElseThrow(ApiException::notFound));
    }
    @PatchMapping("/{incidentId}/document") public IncidentResource document(@PathVariable Long incidentId,@Valid @RequestBody UpdateIncidentDocumentResource resource) {
        return IncidentResourceFromEntityAssembler.toResourceFromEntity(commands.handle(new UpdateIncidentDocumentCommand(incidentId,resource.documentUrl())).orElseThrow(ApiException::notFound));
    }
    @GetMapping("/analytics") public IncidentAnalyticsResponse analytics() { return queries.getAnalytics(access.mobile().getCompanyId()); }
}
