package org.dromara.title.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.title.domain.TitleBatch;
import org.dromara.title.service.TitleEvaluationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
