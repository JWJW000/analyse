package com.ethicssra.service;

import com.ethicssra.domain.EthicsModule;
import com.ethicssra.domain.EthicsModuleRevision;
import com.ethicssra.dto.EthicsModuleDto;
import com.ethicssra.dto.EthicsModuleUpsertRequest;
import com.ethicssra.repository.EthicsModuleRepository;
import com.ethicssra.repository.EthicsModuleRevisionRepository;
import com.ethicssra.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EthicsModuleService {

    private final EthicsModuleRepository moduleRepository;
    private final EthicsModuleRevisionRepository revisionRepository;
    private final AuditService auditService;

    public EthicsModuleService(
            EthicsModuleRepository moduleRepository,
            EthicsModuleRevisionRepository revisionRepository,
            AuditService auditService
    ) {
        this.moduleRepository = moduleRepository;
        this.revisionRepository = revisionRepository;
        this.auditService = auditService;
    }

    public List<EthicsModuleDto> listAll() {
        return moduleRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<EthicsModuleDto> search(String q) {
        if (q == null || q.isBlank()) {
            return listAll();
        }
        return moduleRepository.search(q.trim()).stream().map(this::toDto).toList();
    }

    public EthicsModuleDto get(Long id) {
        return moduleRepository.findById(id).map(this::toDto).orElseThrow();
    }

    @Transactional
    public EthicsModuleDto create(EthicsModuleUpsertRequest req) {
        EthicsModule m = new EthicsModule();
        m.setTitle(req.title());
        m.setCategory(req.category());
        m.setKeywords(req.keywords());
        m.setDescription(req.description());
        m.setCaseText(req.caseText());
        m.setReference(req.reference());
        applyMetadata(m, req);
        m.setCurrentVersion(1);
        m = moduleRepository.save(m);
        saveRevision(m, 1);
        auditService.log(SecurityUtils.currentUserId(), "ETHICS_MODULE_CREATE", "EthicsModule", m.getId(), null);
        return toDto(m);
    }

    @Transactional
    public EthicsModuleDto update(Long id, EthicsModuleUpsertRequest req) {
        EthicsModule m = moduleRepository.findById(id).orElseThrow();
        int next = m.getCurrentVersion() + 1;
        m.setTitle(req.title());
        m.setCategory(req.category());
        m.setKeywords(req.keywords());
        m.setDescription(req.description());
        m.setCaseText(req.caseText());
        m.setReference(req.reference());
        applyMetadata(m, req);
        m.setCurrentVersion(next);
        m = moduleRepository.save(m);
        saveRevision(m, next);
        auditService.log(SecurityUtils.currentUserId(), "ETHICS_MODULE_UPDATE", "EthicsModule", m.getId(), null);
        return toDto(m);
    }

    @Transactional
    public void delete(Long id) {
        moduleRepository.deleteById(id);
        auditService.log(SecurityUtils.currentUserId(), "ETHICS_MODULE_DELETE", "EthicsModule", id, null);
    }

    public List<EthicsModuleRevision> revisions(Long moduleId) {
        return revisionRepository.findByModuleIdOrderByVersionDesc(moduleId);
    }

    private void saveRevision(EthicsModule m, int version) {
        EthicsModuleRevision r = new EthicsModuleRevision();
        r.setModuleId(m.getId());
        r.setVersion(version);
        r.setTitle(m.getTitle());
        r.setCategory(m.getCategory());
        r.setKeywords(m.getKeywords());
        r.setDescription(m.getDescription());
        r.setCaseText(m.getCaseText());
        r.setReference(m.getReference());
        r.setApplicableScenario(m.getApplicableScenario());
        r.setTeachingObjective(m.getTeachingObjective());
        r.setValuePoint(m.getValuePoint());
        r.setDiscussionQuestions(m.getDiscussionQuestions());
        r.setRiskPoints(m.getRiskPoints());
        r.setIntegrationSuggestion(m.getIntegrationSuggestion());
        r.setApplicableMajor(m.getApplicableMajor());
        r.setDifficultyLevel(m.getDifficultyLevel());
        revisionRepository.save(r);
    }

    private void applyMetadata(EthicsModule m, EthicsModuleUpsertRequest req) {
        m.setApplicableScenario(req.applicableScenario());
        m.setTeachingObjective(req.teachingObjective());
        m.setValuePoint(req.valuePoint());
        m.setDiscussionQuestions(req.discussionQuestions());
        m.setRiskPoints(req.riskPoints());
        m.setIntegrationSuggestion(req.integrationSuggestion());
        m.setApplicableMajor(req.applicableMajor());
        m.setDifficultyLevel(req.difficultyLevel());
    }

    private EthicsModuleDto toDto(EthicsModule m) {
        return new EthicsModuleDto(
                m.getId(),
                m.getTitle(),
                m.getCategory(),
                m.getKeywords(),
                m.getDescription(),
                m.getCaseText(),
                m.getReference(),
                m.getCurrentVersion(),
                m.getApplicableScenario(),
                m.getTeachingObjective(),
                m.getValuePoint(),
                m.getDiscussionQuestions(),
                m.getRiskPoints(),
                m.getIntegrationSuggestion(),
                m.getApplicableMajor(),
                m.getDifficultyLevel()
        );
    }
}
