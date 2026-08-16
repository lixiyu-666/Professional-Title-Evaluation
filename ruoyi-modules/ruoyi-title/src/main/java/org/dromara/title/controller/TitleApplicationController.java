package org.dromara.title.controller;

import org.dromara.common.core.domain.R;
import org.dromara.title.service.TitleEvaluationService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/title/applications")
public class TitleApplicationController {
    private final TitleEvaluationService service;
    public TitleApplicationController(TitleEvaluationService service) { this.service = service; }
    @PostMapping public R<?> create(@RequestBody Map<String,Object> body) { return R.ok(service.createDraft(Long.valueOf(body.get("batchId").toString()), body)); }
    @PutMapping("/{id}/draft") public R<?> save(@PathVariable Long id, @RequestBody Map<String,Object> form) { return R.ok(service.saveDraft(id, form)); }
    @PostMapping("/{id}/submit") public R<?> submit(@PathVariable Long id) { return R.ok(service.submit(id)); }
    @PostMapping("/{id}/review") public R<?> review(@PathVariable Long id, @RequestBody Map<String,Object> body) { return R.ok(service.review(id, String.valueOf(body.get("action")), (String) body.get("reason"), body.get("correctionHours") == null ? null : Long.valueOf(body.get("correctionHours").toString()))); }
    @GetMapping("/{id}") public R<?> get(@PathVariable Long id) { return R.ok(service.get(id)); }
    @GetMapping("/{id}/audit-events") public R<?> audit(@PathVariable Long id) { return R.ok(service.auditEvents(id)); }
}
