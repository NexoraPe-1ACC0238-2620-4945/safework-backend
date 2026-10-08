package com.nexorape.safework.service.shared.domain.model.valueobjects;
/** Unicode code-point length and well-formed text, independent of UTF-16 storage units. */
public final class TextRules {
    private TextRules() {}
    public static boolean valid(String value,int maximum) {
        if(value==null || value.isBlank() || value.codePointCount(0,value.length())>maximum) return false;
        for(int i=0;i<value.length();i++) {
            char unit=value.charAt(i);
            if(Character.isHighSurrogate(unit)) {
                if(i+1>=value.length() || !Character.isLowSurrogate(value.charAt(++i))) return false;
            } else if(Character.isLowSurrogate(unit)) return false;
        }
        return true;
    }
    public static String normalized(String value,int maximum) {
        String text=value==null?null:value.strip();
        if(!valid(text,maximum)) throw new IllegalArgumentException("Text does not satisfy published limits.");
        return text;
    }
}
