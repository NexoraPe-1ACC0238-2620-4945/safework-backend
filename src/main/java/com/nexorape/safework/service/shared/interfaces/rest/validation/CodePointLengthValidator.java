package com.nexorape.safework.service.shared.interfaces.rest.validation;
import jakarta.validation.*;
import com.nexorape.safework.service.shared.domain.model.valueobjects.TextRules;
public class CodePointLengthValidator implements ConstraintValidator<CodePointLength,String> {
    private int maximum;
    public void initialize(CodePointLength rule){ maximum=rule.max(); }
    public boolean isValid(String value,ConstraintValidatorContext context){return value==null || TextRules.valid(value,maximum);}
}
