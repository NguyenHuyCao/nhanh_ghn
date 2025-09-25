package com.app84soft.check_in.component.converter;

import com.app84soft.check_in.util.Aes;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Converter
public class DataSensitiveConverter implements AttributeConverter<String, String> {

    @Value("${app.data-sensitive.secret-key}")
    private String secretKey;

    @Override
    public String convertToDatabaseColumn(String s) {
        if (s == null) return null;
        try { return Aes.encrypt(s, secretKey); }
        catch (Exception e) { e.printStackTrace(); return s; }
    }

    @Override
    public String convertToEntityAttribute(String encrypt) {
        if (encrypt == null) return null;
        try { return Aes.decrypt(encrypt, secretKey); }
        catch (Exception e) { e.printStackTrace(); return encrypt; }
    }
}

