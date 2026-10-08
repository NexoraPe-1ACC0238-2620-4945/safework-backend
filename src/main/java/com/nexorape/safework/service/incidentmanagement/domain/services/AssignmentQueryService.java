package com.nexorape.safework.service.incidentmanagement.domain.services;

import com.nexorape.safework.service.incidentmanagement.domain.model.entities.Assignment;
import com.nexorape.safework.service.incidentmanagement.domain.model.queries.assignment.GetAllAssignmentsQuery;
import com.nexorape.safework.service.incidentmanagement.domain.model.queries.assignment.GetAssignmentByIdQuery;

import java.util.List;
import java.util.Optional;

public interface AssignmentQueryService {

    /*
     * En cambio aquí podemos ser mas flexibles, ya que son solo queries para
     * lectura
     */
    /*
     * Podriamos hacerlo tambien todito en incident query service, pero esto lo hace
     * mas ordenado
     */

    Optional<Assignment> handle(GetAssignmentByIdQuery query);

    List<Assignment> handle(GetAllAssignmentsQuery query);

    List<Assignment> handle(
            com.nexorape.safework.service.incidentmanagement.domain.model.queries.assignment.GetAssignmentsByUserIdQuery query);

}
