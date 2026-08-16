package org.dromara.title.rag;

import org.springframework.stereotype.Service;
import java.util.*;

/** MVP retrieval facade. Model providers are intentionally optional. */
@Service
public class PolicyRagService {
    public record Citation(String documentName, String locator, String excerpt) { }
    public record Answer(String answer, List<Citation> citations, boolean modelConfigured) { }
    public interface DocumentParser { List<Chunk> parse(String name, String text); }
    public interface Retriever { List<Chunk> retrieve(String query, Map<String,String> context); }
    public interface EmbeddingProvider { float[] embed(String text); }
    public interface ChatModelProvider { String answer(String query, List<Citation> citations); }
    public interface CitationBuilder { Citation build(Chunk chunk); }
    public record Chunk(String documentName, String locator, String text, Map<String,String> tags) { }
    private final List<Chunk> chunks = new ArrayList<>();
    public synchronized void index(String documentName, String text, Map<String,String> tags) { int n=1; for (String paragraph : text.split("\\n\\s*\\n")) if (!paragraph.isBlank()) chunks.add(new Chunk(documentName, "段落 " + n++, paragraph.trim(), tags)); }
    public synchronized Answer ask(String query, Map<String,String> context) {
        List<Citation> citations = chunks.stream().filter(c -> matches(c, query, context)).limit(5).map(c -> new Citation(c.documentName(), c.locator(), c.text())).toList();
        if (citations.isEmpty()) return new Answer("未找到可作为依据的已发布政策，请咨询人事职称管理员。", citations, false);
        return new Answer("已找到以下政策依据。MVP 未配置生成模型，因此仅展示可核验出处。", citations, false);
    }
    private boolean matches(Chunk c, String query, Map<String,String> context) { return Arrays.stream(query.split("\\s+|，|。|、")).filter(s -> s.length()>1).anyMatch(c.text()::contains) && context.entrySet().stream().allMatch(e -> e.getValue().isBlank() || e.getValue().equals(c.tags().get(e.getKey()))); }
}
