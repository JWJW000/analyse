package com.ethicssra.service;

import com.ethicssra.domain.DocumentVersion;
import com.ethicssra.dto.CreateVersionRequest;
import com.ethicssra.dto.DocumentVersionDto;
import com.ethicssra.repository.DocumentVersionRepository;
import com.ethicssra.repository.UserRepository;
import com.ethicssra.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VersionService {

    private final DocumentVersionRepository versionRepository;
    private final UserRepository userRepository;

    public VersionService(DocumentVersionRepository versionRepository, UserRepository userRepository) {
        this.versionRepository = versionRepository;
        this.userRepository = userRepository;
    }

    public DocumentVersionDto createVersion(CreateVersionRequest request) {
        Long userId = SecurityUtils.currentUserId();
        
        Integer latestVersion = versionRepository
            .findMaxVersionNumber(request.projectId(), request.requirementId())
            .orElse(0);
        
        DocumentVersion version = new DocumentVersion();
        version.setProjectId(request.projectId());
        version.setRequirementId(request.requirementId());
        version.setVersionNumber(latestVersion + 1);
        version.setContent(request.content());
        version.setChangeSummary(request.changeSummary());
        version.setUserId(userId);
        
        DocumentVersion saved = versionRepository.save(version);
        return DocumentVersionDto.from(saved, getUserName(userId));
    }

    public List<DocumentVersionDto> getVersions(Long projectId, Long requirementId) {
        return versionRepository.findByProjectIdAndRequirementIdOrderByVersionNumberDesc(projectId, requirementId)
            .stream()
            .map(v -> DocumentVersionDto.from(v, getUserName(v.getUserId())))
            .toList();
    }

    public DocumentVersionDto getVersion(Long versionId) {
        DocumentVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new EntityNotFoundException("Version not found"));
        return DocumentVersionDto.from(version, getUserName(version.getUserId()));
    }

    private String getUserName(Long userId) {
        return userRepository.findById(userId)
            .map(u -> u.getDisplayName() != null ? u.getDisplayName() : u.getUsername())
            .orElse("Unknown");
    }
}