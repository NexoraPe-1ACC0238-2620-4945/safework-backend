package com.nexorape.safework.service.incidentmanagement.domain.model.aggregates;

import com.nexorape.safework.service.iam.domain.model.aggregates.Company;
import com.nexorape.safework.service.iam.domain.model.aggregates.User;
import com.nexorape.safework.service.incidentmanagement.domain.model.commands.incident.CreateIncidentCommand;
import com.nexorape.safework.service.incidentmanagement.domain.model.entities.Assignment;
import com.nexorape.safework.service.incidentmanagement.domain.model.valueobjects.incident.IncidentStatus;
import com.nexorape.safework.service.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
public class Incident extends AuditableAbstractAggregateRoot<Incident> {

    // ATRIBUTOS
    /**/
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User user;

    /**/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_company_id", nullable = false)
    private Company company;

    /**/
    @Getter
    @Column(nullable = false, length = 120)
    private String title;

    /**/
    @Getter
    @Column(nullable = false, length = 4000)
    private String description;

    /**/
    @Getter
    @Column(nullable = false, length = 500)
    private String location;

    /**/
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    /**/
    @Getter
    @Column(length = 2048)
    private String documentUrl;

    @Version private long version;

    /*
     * mappedBy = "incident": Yo no tengo la FK, la tiene la clase Assignment en el
     * campo "incident"
     */
    @Getter
    @OneToOne(mappedBy = "incident", cascade = CascadeType.ALL)
    private Assignment assignment;

    // CONSTRUCTORES
    public Incident() {
        super();
        this.status = IncidentStatus.OPEN;
    }

    public Incident(CreateIncidentCommand command) {
        this();
        this.title = checkedText(command.title(), 120);
        this.description = checkedText(command.description(), 4000);
        this.location = checkedText(command.location(), 500);
    }

    public Incident(User user, Company company, CreateIncidentCommand command) {
        this();
        this.user = user;
        this.company = company;
        this.title = checkedText(command.title(), 120);
        this.description = checkedText(command.description(), 4000);
        this.location = checkedText(command.location(), 500);
    }

    // METODOS
    public String getStatus() {
        return status.name();
    }

    public Long getCompanyId() {
        return company.getId();
    }

    public Long getUserId() {
        return user.getId();
    }

    // METODOS
    public void assignTo(User user) {
        if (this.status != IncidentStatus.OPEN || this.assignment != null) {
            throw new IllegalStateException("Only OPEN incidents without assignments can be taken.");
        }

        this.assignment = new Assignment(this, user);

        this.status = IncidentStatus.ASSIGNED;
    }

    public void startProgress() {
        if (this.status != IncidentStatus.ASSIGNED) {
            throw new IllegalStateException("Incident must be ASSIGNED to start progress.");
        }
        this.status = IncidentStatus.IN_PROGRESS;
    }

    public void close() {
        if (this.status != IncidentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Incident must be IN_PROGRESS to close.");
        }
        this.assignment.complete();
        this.status = IncidentStatus.CLOSED;
    }

    private static String checkedText(String value, int maximum) {
        return com.nexorape.safework.service.shared.domain.model.valueobjects.TextRules.normalized(value,maximum);
    }

    public void updateDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

}
