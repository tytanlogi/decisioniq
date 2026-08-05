package com.app.decisioniq.config.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StrictJsonConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer strictJsonTypes() {
        return builder -> builder.postConfigurer(this::disableScalarToTextCoercion);
    }

    private void disableScalarToTextCoercion(ObjectMapper objectMapper) {
        var textualCoercion = objectMapper.coercionConfigFor(LogicalType.Textual);
        textualCoercion.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
        textualCoercion.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
        textualCoercion.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
    }
}
