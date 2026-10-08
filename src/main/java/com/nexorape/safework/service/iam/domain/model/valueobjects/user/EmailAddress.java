package com.nexorape.safework.service.iam.domain.model.valueobjects.user;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import java.util.Locale;
@Embeddable
public record EmailAddress(@Email String address) {
    public EmailAddress { if(address!=null) address=address.trim().toLowerCase(Locale.ROOT); }
    public EmailAddress() { this(null); }
}
