package org.dromara.title.controller;

import org.dromara.common.core.domain.R;
import org.dromara.title.rag.PolicyRagService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/title/policies")
public class PolicyAssistantController {
    private final PolicyRagService rag;
    public PolicyAssistantController(PolicyRagService rag) { this.rag = rag; }
    @PostMapping("/index") public R<Void> index(@RequestBody Map<String,Object> body) { rag.index(String.valueOf(body.get("documentName")), String.valueOf(body.get("text")), (Map<String,String>) body.getOrDefault("tags", Map.of())); return R.ok(); }
    @PostMapping("/ask") public R<PolicyRagService.Answer> ask(@RequestBody Map<String,Object> body) { return R.ok(rag.ask(String.valueOf(body.get("question")), (Map<String,String>) body.getOrDefault("context", Map.of()))); }
}
