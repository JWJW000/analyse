package com.ethicssra.service;

import com.ethicssra.domain.Literature;
import com.ethicssra.dto.LiteratureAnalysisDto;
import com.ethicssra.dto.LiteratureDto;
import com.ethicssra.dto.LiteratureUpsertRequest;
import com.ethicssra.repository.LiteratureRepository;
import com.ethicssra.storage.LiteratureFileStorage;
import com.ethicssra.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class LiteratureService {

    private final LiteratureRepository literatureRepository;
    private final AuditService auditService;
    private final LiteratureFileStorage literatureFileStorage;

    public LiteratureService(
            LiteratureRepository literatureRepository,
            AuditService auditService,
            LiteratureFileStorage literatureFileStorage) {
        this.literatureRepository = literatureRepository;
        this.auditService = auditService;
        this.literatureFileStorage = literatureFileStorage;
    }

    public List<LiteratureDto> list(String q) {
        if (q == null || q.isBlank()) {
            return literatureRepository.findAll().stream().map(this::toDto).toList();
        }
        return literatureRepository.search(q.trim()).stream().map(this::toDto).toList();
    }

    public LiteratureDto get(Long id) {
        return literatureRepository.findById(id).map(this::toDto).orElseThrow();
    }

    @Transactional
    public LiteratureDto create(LiteratureUpsertRequest req, String filePath) {
        Literature l = new Literature();
        l.setTitle(req.title());
        l.setAuthor(req.author());
        l.setSource(req.source());
        l.setAbstractText(req.abstractText());
        l.setKeywords(req.keywords());
        l.setFilePath(filePath);
        l.setCreatedBy(SecurityUtils.currentUserId());
        l = literatureRepository.save(l);
        auditService.log(SecurityUtils.currentUserId(), "LITERATURE_CREATE", "Literature", l.getId(), null);
        return toDto(l);
    }

    @Transactional
    public LiteratureDto update(Long id, LiteratureUpsertRequest req) {
        Literature l = literatureRepository.findById(id).orElseThrow();
        l.setTitle(req.title());
        l.setAuthor(req.author());
        l.setSource(req.source());
        l.setAbstractText(req.abstractText());
        l.setKeywords(req.keywords());
        l = literatureRepository.save(l);
        auditService.log(SecurityUtils.currentUserId(), "LITERATURE_UPDATE", "Literature", l.getId(), null);
        return toDto(l);
    }

    @Transactional
    public LiteratureDto updateFilePath(Long id, String filePath) {
        Literature l = literatureRepository.findById(id).orElseThrow();
        String previous = l.getFilePath();
        l.setFilePath(filePath);
        l = literatureRepository.save(l);
        if (previous != null && !previous.isBlank() && !previous.equals(filePath)) {
            literatureFileStorage.deleteIfExists(previous);
        }
        auditService.log(SecurityUtils.currentUserId(), "LITERATURE_FILE", "Literature", id, null);
        return toDto(l);
    }

    @Transactional
    public void delete(Long id) {
        Literature l = literatureRepository.findById(id).orElseThrow();
        String filePath = l.getFilePath();
        literatureRepository.deleteById(id);
        if (filePath != null && !filePath.isBlank()) {
            literatureFileStorage.deleteIfExists(filePath);
        }
        auditService.log(SecurityUtils.currentUserId(), "LITERATURE_DELETE", "Literature", id, null);
    }

    public LiteratureAnalysisDto analyzeLiterature(Long id) {
        Literature l = literatureRepository.findById(id).orElseThrow();

        String title = l.getTitle() != null ? l.getTitle() : "";
        String abstractText = l.getAbstractText() != null ? l.getAbstractText() : "";
        String content = title + " " + abstractText;

        List<String> keywords = extractKeywords(content);
        String researchDirection = classifyResearchDirection(content);
        String summary = generateSummary(abstractText);

        return new LiteratureAnalysisDto(
            l.getTitle(),
            l.getAuthor(),
            l.getAbstractText(),
            keywords,
            researchDirection,
            summary
        );
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return List.of("待分析");
        }

        String[] words = text.split("[\\s,.，。、；;()（）]+");

        return Arrays.stream(words)
            .filter(w -> w.length() > 2 && w.length() < 15)
            .filter(w -> !isStopWord(w))
            .limit(5)
            .toList();
    }

    private boolean isStopWord(String word) {
        List<String> stopWords = List.of("的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这", "that", "the", "and", "for", "are", "with", "this", "from");
        return stopWords.contains(word.toLowerCase());
    }

    private String classifyResearchDirection(String text) {
        if (text == null || text.isBlank()) {
            return "待分析";
        }

        String lower = text.toLowerCase();

        if (lower.contains("machine learning") || lower.contains("deep learning") || lower.contains("neural") || lower.contains("人工智能") || lower.contains("机器学习") || lower.contains("深度学习")) {
            return "人工智能/机器学习";
        }
        if (lower.contains("software") || lower.contains("requirement") || lower.contains("软件") || lower.contains("需求")) {
            return "软件工程";
        }
        if (lower.contains("network") || lower.contains("安全") || lower.contains("network security")) {
            return "网络安全";
        }
        if (lower.contains("data") || lower.contains("数据") || lower.contains("big data") || lower.contains("大数据")) {
            return "数据分析";
        }
        if (lower.contains("ethics") || lower.contains("思政") || lower.contains("伦理")) {
            return "工程伦理";
        }

        return "综合研究";
    }

    private String generateSummary(String abstractText) {
        if (abstractText == null || abstractText.isBlank()) {
            return "暂无摘要内容，无法生成总结。";
        }

        String[] sentences = abstractText.split("[.。;；!！?？]");
        if (sentences.length > 0 && sentences[0].length() > 10) {
            return "本文主要研究了" + sentences[0].trim() + "。";
        }

        return "本文对相关研究领域进行了探讨分析。";
    }

    private LiteratureDto toDto(Literature l) {
        return new LiteratureDto(
                l.getId(),
                l.getTitle(),
                l.getAuthor(),
                l.getSource(),
                l.getAbstractText(),
                l.getKeywords(),
                l.getFilePath(),
                l.getCreatedBy()
        );
    }
}