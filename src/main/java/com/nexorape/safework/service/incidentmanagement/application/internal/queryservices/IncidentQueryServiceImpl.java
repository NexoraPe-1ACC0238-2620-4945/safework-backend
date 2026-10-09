package com.nexorape.safework.service.incidentmanagement.application.internal.queryservices;

import com.nexorape.safework.service.incidentmanagement.domain.model.aggregates.Incident;
import com.nexorape.safework.service.incidentmanagement.domain.model.queries.incident.GetAllIncidentsByCompanyQuery;
import com.nexorape.safework.service.incidentmanagement.domain.model.queries.incident.GetAllIncidentsQuery;
import com.nexorape.safework.service.incidentmanagement.domain.model.queries.incident.GetIncidentByIdQuery;
import com.nexorape.safework.service.incidentmanagement.domain.services.IncidentQueryService;
import com.nexorape.safework.service.incidentmanagement.infrastructure.persistence.jpa.repositories.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nexorape.safework.service.iam.application.internal.security.AccessPolicy;
import com.nexorape.safework.service.shared.application.errors.ApiException;

import java.util.List;
import java.util.Optional;

@Service @Transactional(readOnly=true)
public class IncidentQueryServiceImpl implements IncidentQueryService {

    private final IncidentRepository incidentRepository;

    private final AccessPolicy access;
    public IncidentQueryServiceImpl(IncidentRepository incidentRepository, AccessPolicy access) {
        this.access=access;
        this.incidentRepository = incidentRepository;
    }

    // QUERIES

    @Override
    public Optional<Incident> handle(GetIncidentByIdQuery query) {
        if(query.incidentId()==null || query.incidentId()<=0) throw ApiException.validation();
        return java.util.Optional.of(incidentRepository.findVisible(query.incidentId(),access.mobile().getCompanyId()).orElseThrow(ApiException::notFound));
    }

    @Override
    public List<Incident> handle(GetAllIncidentsQuery query) {
        return incidentRepository.findByCompanyId(access.mobile().getCompanyId());
    }

    @Override
    public List<Incident> handle(GetAllIncidentsByCompanyQuery query) {

        access.ownCompany(query.company().getId());
        return incidentRepository.findByCompanyId(query.company().getId());
    }

    @Override
    public com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.incident.IncidentAnalyticsResponse getAnalytics(
            Long companyId) {
        access.ownCompany(companyId);
        var rawCounts = incidentRepository.countTotalIncidentsByStatusAndCompanyId(companyId);

        long total = 0;
        for (Object[] row : rawCounts) {
            total += (Long) row[1];
        }

        java.util.Map<String, com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.incident.IncidentAnalyticsResponse.IncidentStatusAnalytics> analyticsMap = new java.util.HashMap<>();

        // Initialize with 0 for all statuses to ensure complete response
        for (com.nexorape.safework.service.incidentmanagement.domain.model.valueobjects.incident.IncidentStatus status : com.nexorape.safework.service.incidentmanagement.domain.model.valueobjects.incident.IncidentStatus
                .values()) {
            analyticsMap.put(status.name().toLowerCase(),
                    new com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.incident.IncidentAnalyticsResponse.IncidentStatusAnalytics(
                            0L, 0.0));
        }

        for (Object[] row : rawCounts) {
            var status = (com.nexorape.safework.service.incidentmanagement.domain.model.valueobjects.incident.IncidentStatus) row[0];
            var count = (Long) row[1];
            double percentage = total > 0 ? (double) count / total * 100 : 0.0;
            analyticsMap.put(status.name().toLowerCase(),
                    new com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.incident.IncidentAnalyticsResponse.IncidentStatusAnalytics(
                            count, percentage));
        }

        return new com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.incident.IncidentAnalyticsResponse(
                analyticsMap);
    }

}
