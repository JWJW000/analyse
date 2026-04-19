package com.ethicssra.service;

import com.ethicssra.domain.Requirement;
import com.ethicssra.dto.CollaborationSessionDto;
import com.ethicssra.repository.RequirementRepository;
import com.ethicssra.repository.UserRepository;
import com.ethicssra.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CollaborationService {

    private static final int LOCK_DURATION_MINUTES = 30;
    
    private final Map<Long, DocumentLock> activeLocks = new HashMap<>();
    
    private final RequirementRepository requirementRepository;
    private final UserRepository userRepository;

    public CollaborationService(RequirementRepository requirementRepository, UserRepository userRepository) {
        this.requirementRepository = requirementRepository;
        this.userRepository = userRepository;
    }

    public CollaborationSessionDto lockDocument(Long requirementId) {
        Long userId = SecurityUtils.currentUserId();
        
        if (activeLocks.containsKey(requirementId)) {
            DocumentLock existing = activeLocks.get(requirementId);
            if (!existing.userId().equals(userId) && existing.expiresAt().isAfter(Instant.now())) {
                throw new IllegalStateException("文档正在被其他用户编辑，请稍后再试");
            }
        }
        
        Requirement req = requirementRepository.findById(requirementId)
            .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        
        String userName = userRepository.findById(userId)
            .map(u -> u.getDisplayName() != null ? u.getDisplayName() : u.getUsername())
            .orElse("未知用户");
        
        Instant now = Instant.now();
        Instant expires = now.plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES);
        
        DocumentLock lock = new DocumentLock(requirementId, userId, userName, now, expires);
        activeLocks.put(requirementId, lock);
        
        return new CollaborationSessionDto(requirementId, userId, userName, now, expires, true);
    }

    public void unlockDocument(Long requirementId) {
        Long userId = SecurityUtils.currentUserId();
        
        DocumentLock lock = activeLocks.get(requirementId);
        if (lock == null) {
            return;
        }
        
        if (!lock.userId().equals(userId)) {
            throw new IllegalStateException("只能解锁自己锁定的文档");
        }
        
        activeLocks.remove(requirementId);
    }

    public Optional<CollaborationSessionDto> getLockStatus(Long requirementId) {
        DocumentLock lock = activeLocks.get(requirementId);
        if (lock == null) {
            return Optional.empty();
        }
        
        if (lock.expiresAt().isBefore(Instant.now())) {
            activeLocks.remove(requirementId);
            return Optional.empty();
        }
        
        return Optional.of(new CollaborationSessionDto(
            lock.requirementId(),
            lock.userId(),
            lock.userName(),
            lock.lockedAt(),
            lock.expiresAt(),
            true
        ));
    }

    public boolean isLocked(Long requirementId) {
        Optional<CollaborationSessionDto> status = getLockStatus(requirementId);
        if (status.isEmpty()) {
            return false;
        }
        CollaborationSessionDto lock = status.get();
        return !lock.userId().equals(SecurityUtils.currentUserId());
    }

    public void extendLock(Long requirementId) {
        Long userId = SecurityUtils.currentUserId();
        
        DocumentLock lock = activeLocks.get(requirementId);
        if (lock == null || !lock.userId().equals(userId)) {
            throw new IllegalStateException("无法延长锁");
        }
        
        Instant newExpiry = Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES);
        DocumentLock newLock = new DocumentLock(
            lock.requirementId(),
            lock.userId(),
            lock.userName(),
            lock.lockedAt(),
            newExpiry
        );
        activeLocks.put(requirementId, newLock);
    }

    public record DocumentLock(
        Long requirementId,
        Long userId,
        String userName,
        Instant lockedAt,
        Instant expiresAt
    ) {}
}
