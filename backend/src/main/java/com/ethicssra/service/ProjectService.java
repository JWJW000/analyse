package com.ethicssra.service;

import com.ethicssra.domain.*;
import com.ethicssra.dto.*;
import com.ethicssra.repository.*;
import com.ethicssra.security.SecurityUserDetails;
import com.ethicssra.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectLiteratureRepository literatureRepository;
    private final ProjectRequirementRepository requirementRepository;
    private final ProjectEthicsModuleRepository ethicsModuleRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository memberRepository,
            ProjectLiteratureRepository literatureRepository,
            ProjectRequirementRepository requirementRepository,
            ProjectEthicsModuleRepository ethicsModuleRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.literatureRepository = literatureRepository;
        this.requirementRepository = requirementRepository;
        this.ethicsModuleRepository = ethicsModuleRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public ProjectDto createProject(CreateProjectRequest request) {
        Long userId = SecurityUtils.currentUserId();
        User owner = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setCourseId(request.courseId());
        project.setOwnerId(userId);
        project = projectRepository.save(project);

        ProjectMember ownerMember = new ProjectMember();
        ownerMember.setProject(project);
        ownerMember.setUser(owner);
        ownerMember.setRole(ProjectRole.OWNER);
        memberRepository.save(ownerMember);

        if (request.memberIds() != null) {
            for (Long memberId : request.memberIds()) {
                if (memberId.equals(userId)) continue;
                User member = userRepository.findById(memberId).orElse(null);
                if (member != null) {
                    ProjectMember pm = new ProjectMember();
                    pm.setProject(project);
                    pm.setUser(member);
                    pm.setRole(ProjectRole.MEMBER);
                    memberRepository.save(pm);
                }
            }
        }

        return ProjectDto.from(project, owner.getDisplayName() != null ? owner.getDisplayName() : owner.getUsername());
    }

    public ProjectDto updateProject(Long id, UpdateProjectRequest request) {
        Long userId = SecurityUtils.currentUserId();
        Project project = projectRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Project not found"));

        if (!hasEditPermission(project, userId)) {
            throw new AccessDeniedException("No permission to edit this project");
        }

        if (request.name() != null) project.setName(request.name());
        if (request.description() != null) project.setDescription(request.description());
        // 阶段仅允许通过 advance-phase / rollback-phase 变更，避免绕过门禁
        if (request.status() != null) project.setStatus(request.status());

        return ProjectDto.from(projectRepository.save(project), getOwnerName(project.getOwnerId()));
    }

    public List<ProjectDto> getUserProjects() {
        Long userId = SecurityUtils.currentUserId();
        List<Project> owned = projectRepository.findByOwnerId(userId);
        List<Project> asMember = projectRepository.findByMemberUserId(userId);

        Set<Project> all = new HashSet<>(owned);
        all.addAll(asMember);

        return all.stream()
                .map(p -> ProjectDto.from(p, getOwnerName(p.getOwnerId())))
                .toList();
    }

    public ProjectDto getProject(Long id) {
        Long userId = SecurityUtils.currentUserId();
        Project project = projectRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Project not found"));

        if (!hasViewPermission(project, userId)) {
            throw new AccessDeniedException("No permission to view this project");
        }

        return ProjectDto.from(project, getOwnerName(project.getOwnerId()));
    }

    public void deleteProject(Long id) {
        Long userId = SecurityUtils.currentUserId();
        Project project = projectRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Project not found"));

        if (!project.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("Only owner can delete the project");
        }

        projectRepository.delete(project);
    }

    public ProjectDto addLiterature(Long projectId, Long literatureId) {
        Long userId = SecurityUtils.currentUserId();
        Project project = projectRepository.findById(projectId).orElseThrow();
        if (!hasEditPermission(project, userId)) throw new AccessDeniedException("No permission");

        if (!literatureRepository.existsByProjectIdAndLiteratureId(projectId, literatureId)) {
            Literature literature = new Literature();
            literature.setId(literatureId);
            ProjectLiterature pl = new ProjectLiterature();
            pl.setProject(project);
            pl.setLiterature(literature);
            literatureRepository.save(pl);
        }

        return getProject(projectId);
    }

    public ProjectDto removeLiterature(Long projectId, Long literatureId) {
        Long userId = SecurityUtils.currentUserId();
        Project project = projectRepository.findById(projectId).orElseThrow();
        if (!hasEditPermission(project, userId)) throw new AccessDeniedException("No permission");

        literatureRepository.deleteByProjectIdAndLiteratureId(projectId, literatureId);
        return getProject(projectId);
    }

    public ProjectDto addRequirement(Long projectId, Long requirementId) {
        Long userId = SecurityUtils.currentUserId();
        Project project = projectRepository.findById(projectId).orElseThrow();
        if (!hasEditPermission(project, userId)) throw new AccessDeniedException("No permission");

        if (!requirementRepository.existsByProjectIdAndRequirementId(projectId, requirementId)) {
            Requirement requirement = new Requirement();
            requirement.setId(requirementId);
            ProjectRequirement pr = new ProjectRequirement();
            pr.setProject(project);
            pr.setRequirement(requirement);
            requirementRepository.save(pr);
        }

        return getProject(projectId);
    }

    public ProjectDto removeRequirement(Long projectId, Long requirementId) {
        Long userId = SecurityUtils.currentUserId();
        Project project = projectRepository.findById(projectId).orElseThrow();
        if (!hasEditPermission(project, userId)) throw new AccessDeniedException("No permission");

        requirementRepository.deleteByProjectIdAndRequirementId(projectId, requirementId);
        return getProject(projectId);
    }

    public ProjectDto addEthicsModule(Long projectId, Long ethicsModuleId) {
        Long userId = SecurityUtils.currentUserId();
        Project project = projectRepository.findById(projectId).orElseThrow();
        if (!hasEditPermission(project, userId)) throw new AccessDeniedException("No permission");

        if (!ethicsModuleRepository.existsByProjectIdAndEthicsModuleId(projectId, ethicsModuleId)) {
            EthicsModule ethicsModule = new EthicsModule();
            ethicsModule.setId(ethicsModuleId);
            ProjectEthicsModule pem = new ProjectEthicsModule();
            pem.setProject(project);
            pem.setEthicsModule(ethicsModule);
            ethicsModuleRepository.save(pem);
        }

        return getProject(projectId);
    }

    public ProjectDto removeEthicsModule(Long projectId, Long ethicsModuleId) {
        Long userId = SecurityUtils.currentUserId();
        Project project = projectRepository.findById(projectId).orElseThrow();
        if (!hasEditPermission(project, userId)) throw new AccessDeniedException("No permission");

        ethicsModuleRepository.deleteByProjectIdAndEthicsModuleId(projectId, ethicsModuleId);
        return getProject(projectId);
    }

    public List<ProjectPhaseChecklistItemDto> phaseChecklist(Long projectId) {
        Long userId = SecurityUtils.currentUserId();
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("Project not found"));
        if (!hasViewPermission(project, userId)) {
            throw new IllegalArgumentException("无权查看该项目");
        }
        return buildChecklistForCurrentPhase(project);
    }

    public List<ProjectContentDto> getProjectLiteratures(Long projectId) {
        Long userId = SecurityUtils.currentUserId();
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("Project not found"));
        if (!hasViewPermission(project, userId)) {
            throw new AccessDeniedException("No permission to view this project");
        }
        return project.getLiteratures().stream()
                .map(pl -> ProjectContentDto.from(pl.getLiterature()))
                .toList();
    }

    public List<RequirementContentDto> getProjectRequirements(Long projectId) {
        Long userId = SecurityUtils.currentUserId();
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("Project not found"));
        if (!hasViewPermission(project, userId)) {
            throw new AccessDeniedException("No permission to view this project");
        }
        return project.getRequirements().stream()
                .map(pr -> RequirementContentDto.from(pr.getRequirement()))
                .toList();
    }

    public List<EthicsModuleContentDto> getProjectEthicsModules(Long projectId) {
        Long userId = SecurityUtils.currentUserId();
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("Project not found"));
        if (!hasViewPermission(project, userId)) {
            throw new AccessDeniedException("No permission to view this project");
        }
        return project.getEthicsModules().stream()
                .map(pe -> EthicsModuleContentDto.from(pe.getEthicsModule()))
                .toList();
    }

    public ProjectDto advancePhase(Long projectId) {
        Long userId = SecurityUtils.currentUserId();
        SecurityUserDetails user = SecurityUtils.currentUser();
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("Project not found"));
        if (!hasEditPermission(project, userId)) {
            throw new IllegalArgumentException("无权限推进阶段");
        }
        ProjectPhase current = project.getCurrentPhase() != null ? project.getCurrentPhase() : ProjectPhase.LITERATURE;
        if (current == ProjectPhase.REVIEW) {
            throw new IllegalArgumentException("已进入最终阶段，无法继续推进");
        }
        assertAdvanceGates(project, current);
        ProjectPhase next = ProjectPhase.values()[current.ordinal() + 1];
        project.setCurrentPhase(next);
        projectRepository.save(project);
        auditService.log(userId, "PROJECT_PHASE_ADVANCE", "Project", projectId,
                Map.of("fromPhase", current.name(), "toPhase", next.name(), "actor", user.getUsername()));
        return getProject(projectId);
    }

    public ProjectDto rollbackPhase(Long projectId, String reason) {
        Long userId = SecurityUtils.currentUserId();
        SecurityUserDetails user = SecurityUtils.currentUser();
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("Project not found"));
        Role role = user.role();
        if (!canRollbackPhase(project, userId, role)) {
            throw new IllegalArgumentException("仅项目负责人或教师/管理员可回退阶段");
        }
        ProjectPhase current = project.getCurrentPhase() != null ? project.getCurrentPhase() : ProjectPhase.LITERATURE;
        if (current == ProjectPhase.LITERATURE) {
            throw new IllegalArgumentException("已在首阶段，无法回退");
        }
        ProjectPhase prev = ProjectPhase.values()[current.ordinal() - 1];
        project.setCurrentPhase(prev);
        projectRepository.save(project);
        auditService.log(userId, "PROJECT_PHASE_ROLLBACK", "Project", projectId,
                Map.of("fromPhase", current.name(), "toPhase", prev.name(), "reason", reason, "actor", user.getUsername()));
        return getProject(projectId);
    }

    private boolean canRollbackPhase(Project project, Long userId, Role role) {
        if (project.getOwnerId().equals(userId)) {
            return true;
        }
        return role == Role.TEACHER || role == Role.ADMIN || role == Role.TA;
    }

    private List<ProjectPhaseChecklistItemDto> buildChecklistForCurrentPhase(Project project) {
        ProjectPhase current = project.getCurrentPhase() != null ? project.getCurrentPhase() : ProjectPhase.LITERATURE;
        List<ProjectPhaseChecklistItemDto> items = new ArrayList<>();
        int lit = project.getLiteratures().size();
        int reqN = project.getRequirements().size();
        int eth = project.getEthicsModules().size();
        boolean anySubmitted = anyRequirementSubmittedOrApproved(project);
        boolean allSubmitted = allLinkedRequirementsSubmittedOrApproved(project);

        switch (current) {
            case LITERATURE -> {
                items.add(new ProjectPhaseChecklistItemDto(
                        "min_literature",
                        "至少关联 1 篇文献",
                        lit >= 1,
                        lit < 1 ? "请在「文献调研」中关联文献后再推进" : null));
            }
            case REQUIREMENTS -> {
                items.add(new ProjectPhaseChecklistItemDto(
                        "min_requirement",
                        "至少关联 1 个需求",
                        reqN >= 1,
                        reqN < 1 ? "请在「需求分析」中关联需求文档" : null));
                items.add(new ProjectPhaseChecklistItemDto(
                        "requirement_submitted",
                        "至少 1 个关联需求已提交或已通过",
                        anySubmitted,
                        !anySubmitted ? "请将需求提交审核（提交后）或获得通过后再推进" : null));
            }
            case ETHICS -> {
                items.add(new ProjectPhaseChecklistItemDto(
                        "min_ethics",
                        "至少关联 1 个思政模块",
                        eth >= 1,
                        eth < 1 ? "请在「思政融合」中关联模块后再推进" : null));
            }
            case SUBMISSION -> {
                items.add(new ProjectPhaseChecklistItemDto(
                        "all_requirements_final",
                        "所有关联需求均已提交或已通过",
                        allSubmitted,
                        !allSubmitted ? "请确保每个关联需求处于「已提交」或「已通过」状态" : null));
            }
            case REVIEW -> {
                items.add(new ProjectPhaseChecklistItemDto(
                        "terminal",
                        "当前为审核反馈阶段（终点）",
                        false,
                        "无需推进；可与教师沟通后由教师决定是否回退阶段"));
            }
        }
        return items;
    }

    private void assertAdvanceGates(Project project, ProjectPhase current) {
        List<ProjectPhaseChecklistItemDto> checklist = buildChecklistForCurrentPhase(project);
        for (ProjectPhaseChecklistItemDto item : checklist) {
            if (!item.satisfied()) {
                throw new IllegalArgumentException(item.hint() != null ? item.hint() : ("未满足：" + item.label()));
            }
        }
    }

    private boolean anyRequirementSubmittedOrApproved(Project project) {
        for (ProjectRequirement pr : project.getRequirements()) {
            RequirementStatus s = pr.getRequirement().getStatus();
            if (s == RequirementStatus.SUBMITTED || s == RequirementStatus.APPROVED) {
                return true;
            }
        }
        return false;
    }

    private boolean allLinkedRequirementsSubmittedOrApproved(Project project) {
        if (project.getRequirements().isEmpty()) {
            return false;
        }
        for (ProjectRequirement pr : project.getRequirements()) {
            RequirementStatus s = pr.getRequirement().getStatus();
            if (s != RequirementStatus.SUBMITTED && s != RequirementStatus.APPROVED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasViewPermission(Project project, Long userId) {
        if (project.getOwnerId().equals(userId)) return true;
        return memberRepository.existsByProjectIdAndUserId(project.getId(), userId);
    }

    private boolean hasEditPermission(Project project, Long userId) {
        if (project.getOwnerId().equals(userId)) return true;
        var member = memberRepository.findByProjectIdAndUserId(project.getId(), userId).orElse(null);
        if (member == null) return false;
        return member.getRole() == ProjectRole.OWNER || member.getRole() == ProjectRole.EDITOR;
    }

    private String getOwnerName(Long ownerId) {
        return userRepository.findById(ownerId)
                .map(u -> u.getDisplayName() != null ? u.getDisplayName() : u.getUsername())
                .orElse("Unknown");
    }
}