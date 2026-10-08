package com.nexorape.safework.service.incidentmanagement.interfaces.rest.resources.incident;

import java.util.Map;

public record IncidentAnalyticsResponse(Map<String, IncidentStatusAnalytics> analytics) {
    public record IncidentStatusAnalytics(Long count, Double percentage) {
    }
}
