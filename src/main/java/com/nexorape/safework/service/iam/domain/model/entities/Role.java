package com.nexorape.safework.service.iam.domain.model.entities;
import com.nexorape.safework.service.iam.domain.model.valueobjects.Roles;
import jakarta.persistence.*;
import lombok.*;
/** Shared persisted authority. Users reference seeded rows and never cascade role creation. */
@Entity @Data @NoArgsConstructor @AllArgsConstructor @With
public class Role {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(length=20,nullable=false,unique=true) private Roles name;
    public Role(Roles name) { this.name=name; }
    public String getStringName() { return name.name(); }
}
