package com.anjing.aigc.model.entity;

import com.anjing.aigc.model.response.AgentAnalysis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter
public class AgentAnalysisConverter implements AttributeConverter<AgentAnalysis, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public String convertToDatabaseColumn(AgentAnalysis attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("序列化 AgentAnalysis 失败", e);
            return null;
        }
    }

    @Override
    public AgentAnalysis convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, AgentAnalysis.class);
        } catch (JsonProcessingException e) {
            log.error("反序列化 AgentAnalysis 失败: {}", dbData, e);
            return null;
        }
    }
}
