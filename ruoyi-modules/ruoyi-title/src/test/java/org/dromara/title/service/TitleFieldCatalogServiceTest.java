package org.dromara.title.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class TitleFieldCatalogServiceTest {

    @Test
    void catalogContainsExactly149UniqueFields() {
        TitleFieldCatalogService service = service();
        assertEquals(149, service.definitions().size());
        assertEquals(149, service.definitions().stream().map(TitleFieldCatalogService.FieldDefinition::code).distinct().count());
        assertTrue(service.definitions().stream().allMatch(field -> field.step() >= 1 && field.step() <= 5));
    }

    @Test
    void missingEditableRequiredFieldsBlockSubmission() {
        TitleFieldCatalogService.PrecheckResult result = service().precheck(Map.of("values", new HashMap<>()));
        assertFalse(result.passed());
        assertTrue(result.blockCount() > 0);
        assertTrue(result.manualCount() > 0);
    }

    private static TitleFieldCatalogService service() {
        TitleFieldCatalogService service = new TitleFieldCatalogService(new ObjectMapper());
        service.init();
        return service;
    }
}
