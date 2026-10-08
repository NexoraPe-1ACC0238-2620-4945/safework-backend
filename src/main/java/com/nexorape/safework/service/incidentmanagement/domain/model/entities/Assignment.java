package com.nexorape.safework.service.incidentmanagement.domain.model.entities;

import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.incidentmanagement.domain.model.aggregates.Incident;
import com.nexorape.safework.service.incidentmanagement.domain.model.commands.assignment.CreateAssignmentCommand;
import com.nexorape.safework.service.incidentmanagement.domain.model.valueobjects.assignment.AssignmentPriority;
import com.nexorape.safework.service.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.Date;

@Entity
public class Assignment extends AuditableModel {

    // ATRIBUTOS
    /**/
    @Getter
    @OneToOne
    @JoinColumn(name = "fk_incident_id", unique = true, nullable = false)
    private Incident incident;

    /**/
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id", nullable = false)
    private User user;

    /**/
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentPriority priority;

    /**/
    @Getter
    @Column
    private Date completionDate;

    // CONSTRUCTORES
    public Assignment() {
        this.priority = AssignmentPriority.MEDIUM;
    }

    public Assignment(Incident incident, User user) {
        this();
        this.incident = incident;
        this.user = user;
    }

    public Assignment(CreateAssignmentCommand command) {
        this();
    }

    // METODOS
    public Long getIncidentId() {
        return incident.getId();
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getPriority() {
        return priority.name();
    }

    public void complete() {
        if (completionDate != null) throw new IllegalStateException("Assignment already completed.");
        completionDate = new Date();
    }

    public void updatePriority(AssignmentPriority priority) {
        this.priority = priority;
    }

}
