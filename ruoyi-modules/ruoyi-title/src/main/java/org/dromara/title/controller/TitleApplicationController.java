package org.dromara.title.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.title.domain.TitleApplication;
import org.dromara.title.domain.TitleAuditEvent;
import org.dromara.title.service.TitleEvaluationService;
import org.dromara.title.service.TitleFieldCatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/title/applications")
public class TitleApplicationController {
    private final TitleEvaluationService service;

    @SaCheckPermission("title:application:create")
    @PostMapping
    public R<TitleApplication> create(@RequestBody Map<String, Object> body) {
        Object batchId = body.get("batchId");
        if (batchId == null) {
            throw new IllegalArgumentException("batchId不能为空");
        }
        Map<String, Object> form = new LinkedHashMap<>(body);
        form.remove("batchId");
        return R.ok(service.createDraft(Long.valueOf(batchId.toString()), form));
    }

    @SaCheckPermission("title:application:query")
    @GetMapping("/mine")
    public R<List<TitleApplication>> mine() {
        return R.ok(service.listMine());
    }

    @SaCheckPermission("title:application:review")
    @GetMapping("/review-tasks")
    public R<List<TitleApplication>> reviewTasks() {
        return R.ok(service.listReviewTasks());
    }

    @SaCheckPermission("title:application:update")
    @PutMapping("/{id}/draft")
    public R<TitleApplication> save(@PathVariable Long id, @RequestBody Map<String, Object> form) {
        return R.ok(service.saveDraft(id, form));
    }

    @SaCheckPermission("title:application:submit")
    @PostMapping("/{id}/submit")
    public R<TitleApplication> submit(@PathVariable Long id) {
        return R.ok(service.submit(id));
    }

    @SaCheckPermission("title:application:update")
    @PostMapping("/{id}/precheck")
    public R<TitleFieldCatalogService.PrecheckResult> precheck(@PathVariable Long id) {
        return R.ok(service.precheck(id));
    }

    @SaCheckPermission("title:application:query")
    @GetMapping("/field-definitions")
    public R<List<TitleFieldCatalogService.FieldDefinition>> fieldDefinitions() {
        return R.ok(service.fieldDefinitions());
    }

    @SaCheckPermission("title:application:review")
    @PostMapping("/{id}/review")
    public R<TitleApplication> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(service.review(
            id,
            String.valueOf(body.get("action")),
            (String) body.get("reason"),
            body.get("correctionHours") == null ? null : Long.valueOf(body.get("correctionHours").toString())
        ));
    }

    @SaCheckPermission("title:application:query")
    @GetMapping("/{id}")
    public R<TitleApplication> get(@PathVariable Long id) {
        return R.ok(service.getAccessible(id));
    }

    @SaCheckPermission("title:audit:list")
    @GetMapping("/{id}/audit-events")
    public R<List<TitleAuditEvent>> audit(@PathVariable Long id) {
        return R.ok(service.auditEvents(id));
    }
}
