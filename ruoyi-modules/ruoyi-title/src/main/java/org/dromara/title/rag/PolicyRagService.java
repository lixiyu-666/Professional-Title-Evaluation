package org.dromara.title.rag;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.title.domain.TitlePolicyChunk;
import org.dromara.title.domain.TitlePolicyDocument;
import org.dromara.title.mapper.TitlePolicyChunkMapper;
import org.dromara.title.mapper.TitlePolicyDocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/** Database-backed MVP retrieval facade. Model providers remain optional. */
@Service
@RequiredArgsConstructor
public class PolicyRagService {
    public record Citation(String documentName, String locator, String excerpt, String sourceUrl) { }
    public record Answer(String answer, List<Citation> citations, boolean modelConfigured) { }
    public interface DocumentParser { List<Chunk> parse(String name, String text); }
    public interface Retriever { List<Chunk> retrieve(String query, Map<String, String> context); }
    public interface EmbeddingProvider { float[] embed(String text); }
    public interface ChatModelProvider { String answer(String query, List<Citation> citations); }
    public interface CitationBuilder { Citation build(Chunk chunk); }
    public record Chunk(String documentName, String locator, String text, Map<String, String> tags) { }

    private final TitlePolicyDocumentMapper documents;
    private final TitlePolicyChunkMapper chunks;

    @Transactional
    public void index(String documentName, String text, Map<String, String> tags) {
        TitlePolicyDocument document = new TitlePolicyDocument();
        document.setName(documentName);
        document.setVersion(tags.getOrDefault("version", "MVP-1.0"));
        document.setPublisher(tags.getOrDefault("publisher", "人事职称管理部门"));
        document.setEffectiveAt(parseDate(tags.get("effectiveAt")));
        document.setExpiresAt(parseDate(tags.get("expiresAt")));
        document.setInformationLevel(tags.getOrDefault("informationLevel", "公开"));
        document.setSourceUrl(tags.get("sourceUrl"));
        document.setStatus("EFFECTIVE");
        documents.insert(document);

        int number = 1;
        for (String paragraph : text.split("\\n\\s*\\n")) {
            if (paragraph.isBlank()) {
                continue;
            }
            TitlePolicyChunk chunk = new TitlePolicyChunk();
            chunk.setDocumentId(document.getId());
            chunk.setLocator("段落 " + number++);
            chunk.setContent(paragraph.trim());
            chunk.setApplicableYear(parseInteger(tags.get("evaluationYear")));
            chunk.setTitleSeries(tags.get("titleSeries"));
            chunk.setTitleLevel(tags.get("titleLevel"));
            chunks.insert(chunk);
        }
    }

    public Answer ask(String query, Map<String, String> context) {
        List<TitlePolicyDocument> activeDocuments = documents.selectList(Wrappers.<TitlePolicyDocument>lambdaQuery()
            .eq(TitlePolicyDocument::getStatus, "EFFECTIVE"));
        if (activeDocuments.isEmpty()) {
            return noEvidence();
        }
        Map<Long, TitlePolicyDocument> byId = new HashMap<>();
        activeDocuments.forEach(document -> byId.put(document.getId(), document));
        List<String> terms = searchTerms(query);
        List<Citation> citations = chunks.selectList(Wrappers.<TitlePolicyChunk>lambdaQuery()
                .in(TitlePolicyChunk::getDocumentId, byId.keySet()))
            .stream()
            .filter(chunk -> matchesContext(chunk, context))
            .filter(chunk -> terms.stream().anyMatch(chunk.getContent()::contains))
            .limit(5)
            .map(chunk -> {
                TitlePolicyDocument document = byId.get(chunk.getDocumentId());
                return new Citation(document.getName(), chunk.getLocator(), chunk.getContent(), document.getSourceUrl());
            })
            .toList();
        if (citations.isEmpty()) {
            return noEvidence();
        }
        return new Answer("已找到以下政策依据。MVP 未配置生成模型，因此仅展示可核验出处。", citations, false);
    }

    private static boolean matchesContext(TitlePolicyChunk chunk, Map<String, String> context) {
        return matches(firstPresent(context, "evaluationYear", "year"), chunk.getApplicableYear() == null ? null : chunk.getApplicableYear().toString())
            && matches(firstPresent(context, "titleSeries", "series"), chunk.getTitleSeries())
            && matches(firstPresent(context, "titleLevel", "level"), chunk.getTitleLevel());
    }

    private static List<String> searchTerms(String query) {
        String normalized = query == null ? "" : query.replaceAll("[\\s，。、“”‘’；：！？?、,.!;:]", "");
        if (normalized.length() < 2) {
            return List.of();
        }
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (int length = Math.min(6, normalized.length()); length >= 2; length--) {
            for (int start = 0; start + length <= normalized.length(); start++) {
                terms.add(normalized.substring(start, start + length));
            }
        }
        return List.copyOf(terms);
    }

    private static String firstPresent(Map<String, String> context, String primary, String alias) {
        String value = context.get(primary);
        return value == null || value.isBlank() ? context.get(alias) : value;
    }

    private static boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || actual == null || expected.equals(actual);
    }

    private static Answer noEvidence() {
        return new Answer("未找到可作为依据的已发布政策，请咨询人事职称管理员。", List.of(), false);
    }

    private static LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private static Integer parseInteger(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }
}
