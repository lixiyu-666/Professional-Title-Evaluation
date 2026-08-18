package org.dromara.title.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TitleFieldCatalogService {
    public record FieldDefinition(
        String code, String name, Integer step, String object, String control, String format,
        boolean required, String requiredRule, String source, String editRule, String visibility,
        String mapping, String scope, boolean visible, boolean readOnly, boolean repeatable
    ) { }

    public record PrecheckItem(String fieldCode, String fieldName, Integer step, String status,
                               String message, String basis) { }

    public record PrecheckResult(boolean passed, List<PrecheckItem> items, int passedCount,
                                 int blockCount, int manualCount) { }

    private static final Pattern MOBILE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD = Pattern.compile("^\\d{17}[0-9Xx]$");

    private final ObjectMapper json;
    private List<FieldDefinition> definitions = List.of();

    @PostConstruct
    void init() {
        try (InputStream input = new ClassPathResource("title-fields-v1.json").getInputStream()) {
            definitions = List.copyOf(json.readValue(input, new TypeReference<List<FieldDefinition>>() { }));
        } catch (Exception exception) {
            throw new IllegalStateException("职称字段目录加载失败", exception);
        }
        if (definitions.size() != 149 || definitions.stream().map(FieldDefinition::code).distinct().count() != 149) {
            throw new IllegalStateException("职称字段目录必须包含149个唯一字段");
        }
    }

    public List<FieldDefinition> definitions() {
        return definitions;
    }

    @SuppressWarnings("unchecked")
    public PrecheckResult precheck(Map<String, Object> form) {
        Object valuesObject = form.get("values");
        Map<String, Object> values = valuesObject instanceof Map<?, ?> map
            ? (Map<String, Object>) map : form;
        List<PrecheckItem> items = new ArrayList<>();
        for (FieldDefinition field : definitions) {
            if (!field.visible() || !field.required()) {
                continue;
            }
            Object value = values.get(field.code());
            if (blank(value)) {
                String status = field.readOnly() ? "MANUAL" : "BLOCK";
                String message = field.readOnly() ? "等待员工库或系统数据核验" : "必填项不能为空";
                items.add(item(field, status, message));
                continue;
            }
            if ("APP-CONTACT-001".equals(field.code()) && !MOBILE.matcher(String.valueOf(value)).matches()) {
                items.add(item(field, "BLOCK", "手机号码格式不正确"));
            } else if ("ADV-REVIEW-002".equals(field.code()) && !ID_CARD.matcher(String.valueOf(value)).matches()) {
                items.add(item(field, "BLOCK", "身份证号码格式不正确"));
            } else {
                items.add(item(field, "PASS", "校验通过"));
            }
        }
        int blocks = (int) items.stream().filter(item -> "BLOCK".equals(item.status())).count();
        int manual = (int) items.stream().filter(item -> "MANUAL".equals(item.status())).count();
        int passed = (int) items.stream().filter(item -> "PASS".equals(item.status())).count();
        return new PrecheckResult(blocks == 0, List.copyOf(items), passed, blocks, manual);
    }

    private static PrecheckItem item(FieldDefinition field, String status, String message) {
        return new PrecheckItem(field.code(), field.name(), field.step(), status, message, field.requiredRule());
    }

    private static boolean blank(Object value) {
        if (value == null) return true;
        if (value instanceof String string) return string.isBlank();
        if (value instanceof Collection<?> collection) return collection.isEmpty();
        return false;
    }
}
