package org.dromara.title.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.title.domain.TitleBatch;
import org.dromara.title.service.TitleEvaluationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/title/batches")
public class TitleBatchController {
    private final TitleEvaluationService service;

    @SaCheckPermission("title:batch:list")
    @GetMapping
    public R<List<TitleBatch>> list() {
        return R.ok(service.listPublishedBatches());
    }

    @SaCheckPermission("title:batch:list")
    @GetMapping("/{id}")
    public R<TitleBatch> get(@PathVariable Long id) {
        return R.ok(service.getPublishedBatch(id));
    }

    @SaCheckPermission("title:batch:manage")
    @GetMapping("/manage")
    public R<List<TitleBatch>> manageList() {
        return R.ok(service.listManageableBatches());
    }

    @SaCheckPermission("title:batch:manage")
    @PostMapping
    public R<TitleBatch> create(@RequestBody Map<String, Object> body) {
        return R.ok(service.createBatch(body));
    }

    @SaCheckPermission("title:batch:manage")
    @PutMapping("/{id}")
    public R<TitleBatch> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(service.updateBatch(id, body));
    }

    @SaCheckPermission("title:batch:publish")
    @PostMapping("/{id}/publish")
    public R<TitleBatch> publish(@PathVariable Long id) {
        return R.ok(service.publishBatch(id));
    }
}
