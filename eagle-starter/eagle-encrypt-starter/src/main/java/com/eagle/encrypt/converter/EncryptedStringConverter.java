package com.eagle.encrypt.converter;

import com.eagle.encrypt.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * JPA 字段级加密转换器。
 *
 * <p>在 JPA 实体字段上加 {@code @Convert} 注解即可自动加解密：
 * <pre>
 * &#64;Convert(converter = EncryptedStringConverter.class)
 * &#64;Column(name = "mobile")
 * private String mobile;
 * </pre>
 *
 * <p>未启用加密（{@code eagle.encrypt.enabled=false}）时，
 * {@link EncryptionService} 不注册，此转换器原样返回字符串（透明模式）。
 *
 * @author eagle
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Autowired(required = false)
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (encryptionService == null || attribute == null) {
            return attribute;
        }
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (encryptionService == null || dbData == null) {
            return dbData;
        }
        return encryptionService.decrypt(dbData);
    }
}
