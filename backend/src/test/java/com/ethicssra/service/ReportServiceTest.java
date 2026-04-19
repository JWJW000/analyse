package com.ethicssra.service;

import com.ethicssra.domain.Requirement;
import com.ethicssra.domain.Role;
import com.ethicssra.dto.ReportRequest;
import com.ethicssra.dto.ReportResponse;
import com.ethicssra.repository.EthicsModuleRepository;
import com.ethicssra.repository.LiteratureRepository;
import com.ethicssra.repository.ProjectRepository;
import com.ethicssra.repository.RequirementRepository;
import com.ethicssra.repository.UserRepository;
import com.ethicssra.security.SecurityUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateReportSupportsRequirementOnlyModeWhenUserHasNoProject() {
        RequirementRepository requirementRepository = mock(RequirementRepository.class);
        Requirement requirement = new Requirement();
        requirement.setId(10L);
        requirement.setUserId(7L);
        requirement.setTitle("课程任务需求");
        requirement.setTextContent("系统需要支持课程任务、文献证据和工程伦理映射。");
        when(requirementRepository.findById(10L)).thenReturn(Optional.of(requirement));

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new SecurityUserDetails(7L, "student", "pwd", Role.STUDENT),
                null
        ));

        ReportService service = new ReportService(
                mock(ProjectRepository.class),
                mock(LiteratureRepository.class),
                requirementRepository,
                mock(EthicsModuleRepository.class),
                mock(UserRepository.class)
        );

        ReportResponse response = service.generateReport(new ReportRequest(
                0L,
                ReportRequest.ReportFormat.WORD,
                new ReportRequest.ReportContent(false, true, false, false, true),
                List.of(),
                List.of(10L),
                List.of()
        ));

        assertThat(response.fileName()).contains("我的需求报告");
        assertThat(response.downloadUrl()).startsWith("/api/reports/download/");
        assertThat(response.downloadUrl()).doesNotContain("我的需求报告");
        assertThat(response.fileSize()).isGreaterThan(0);

        ReportService.ReportDownload download = service.getDownload(response.downloadUrl().substring("/api/reports/download/".length()));
        assertThat(download.fileName()).isEqualTo(response.fileName());
        assertThat(new String(download.bytes())).contains("课程任务需求");
    }
}
