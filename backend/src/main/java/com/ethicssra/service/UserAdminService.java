package com.ethicssra.service;

import com.ethicssra.domain.AuditLog;
import com.ethicssra.domain.Role;
import com.ethicssra.domain.User;
import com.ethicssra.dto.AuditLogDto;
import com.ethicssra.dto.ChangePasswordRequest;
import com.ethicssra.dto.UserOptionDto;
import com.ethicssra.dto.UserProfileDto;
import com.ethicssra.repository.AuditLogRepository;
import com.ethicssra.repository.UserRepository;
import com.ethicssra.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class UserAdminService {

    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;

    public UserAdminService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            AuditLogRepository auditLogRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.auditLogRepository = auditLogRepository;
    }

    public Page<AuditLogDto> myActivity(int page, int size) {
        Long uid = SecurityUtils.currentUserId();
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(uid, PageRequest.of(page, size))
                .map(this::toAuditDto);
    }

    private AuditLogDto toAuditDto(AuditLog a) {
        return new AuditLogDto(
                a.getId(),
                a.getAction(),
                a.getEntityType(),
                a.getEntityId(),
                a.getDetailJson(),
                a.getCreatedAt() != null ? ISO_INSTANT.format(a.getCreatedAt()) : null
        );
    }

    public UserProfileDto me() {
        User u = userRepository.findById(SecurityUtils.currentUserId()).orElseThrow();
        return new UserProfileDto(u.getId(), u.getUsername(), u.getRole(), u.getDisplayName());
    }

    @Transactional
    public UserProfileDto updateProfile(String displayName) {
        User u = userRepository.findById(SecurityUtils.currentUserId()).orElseThrow();
        u.setDisplayName(displayName);
        userRepository.save(u);
        return new UserProfileDto(u.getId(), u.getUsername(), u.getRole(), u.getDisplayName());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        User u = userRepository.findById(SecurityUtils.currentUserId()).orElseThrow();
        if (!passwordEncoder.matches(req.oldPassword(), u.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }
        u.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(u);
        auditService.log(u.getId(), "PASSWORD_CHANGE", "User", u.getId(), null);
    }

    public List<UserProfileDto> listAll() {
        return userRepository.findAll().stream()
                .map(u -> new UserProfileDto(u.getId(), u.getUsername(), u.getRole(), u.getDisplayName()))
                .toList();
    }

    public List<UserOptionDto> searchStudents(String q) {
        String term = q != null ? q.trim() : "";
        List<User> users = term.isBlank()
                ? userRepository.findByRole(Role.STUDENT)
                : userRepository.searchByRole(Role.STUDENT, term);
        return users.stream()
                .limit(50)
                .map(u -> new UserOptionDto(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole().name()))
                .toList();
    }

    @Transactional
    public UserProfileDto createUser(String username, String password, Role role, String displayName) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setRole(role);
        u.setDisplayName(displayName != null ? displayName : username);
        u = userRepository.save(u);
        auditService.log(SecurityUtils.currentUserId(), "USER_CREATE", "User", u.getId(), role.name());
        return new UserProfileDto(u.getId(), u.getUsername(), u.getRole(), u.getDisplayName());
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User u = userRepository.findById(userId).orElseThrow();
        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
        auditService.log(SecurityUtils.currentUserId(), "ADMIN_PASSWORD_RESET", "User", userId, null);
    }
}
