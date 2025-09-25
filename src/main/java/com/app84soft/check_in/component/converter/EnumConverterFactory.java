package com.app84soft.check_in.component.converter;

import com.app84soft.check_in.dto.constant.BaseEnum;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

@Component
public final class EnumConverterFactory implements ConverterFactory<String, BaseEnum> {

    @NotNull
    @Override
    public <T extends BaseEnum> Converter<String, T> getConverter(@NotNull Class<T> targetType) {
        return new EnumConverter<>(targetType);
    }

    private static class EnumConverter<T extends BaseEnum> implements Converter<String, T> {

        private final Class<T> enumType;

        public EnumConverter(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public T convert(@NotNull String source) {
            for (T enumConstant : enumType.getEnumConstants()) {
                if (source.equals(String.valueOf(enumConstant.toValue()))) {
                    return enumConstant;
                }
            }
            return null;
        }
    }
}