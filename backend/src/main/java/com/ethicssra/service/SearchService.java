package com.ethicssra.service;

import com.ethicssra.domain.Course;
import com.ethicssra.domain.EthicsModule;
import com.ethicssra.domain.Literature;
import com.ethicssra.domain.Requirement;
import com.ethicssra.domain.Role;
import com.ethicssra.dto.SearchHitDto;
import com.ethicssra.dto.SearchResponseDto;
import com.ethicssra.repository.CourseRepository;
import com.ethicssra.repository.EthicsModuleRepository;
import com.ethicssra.repository.LiteratureRepository;
import com.ethicssra.repository.RequirementRepository;
import com.ethicssra.security.SecurityUserDetails;
import com.ethicssra.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private static final int SNIPPET_LEN = 160;

    private final EthicsModuleRepository ethicsModuleRepository;
    private final LiteratureRepository literatureRepository;
    private final RequirementRepository requirementRepository;
    private final CourseRepository courseRepository;

    public SearchService(
            EthicsModuleRepository ethicsModuleRepository,
            LiteratureRepository literatureRepository,
            RequirementRepository requirementRepository,
            CourseRepository courseRepository
    ) {
        this.ethicsModuleRepository = ethicsModuleRepository;
        this.literatureRepository = literatureRepository;
        this.requirementRepository = requirementRepository;
        this.courseRepository = courseRepository;
    }

    public SearchResponseDto search(String q, int limit) {
        if (q == null || q.isBlank()) {
            return new SearchResponseDto("", List.of());
        }
        String term = q.trim();
        if (term.length() > 200) {
            term = term.substring(0, 200);
        }
        int perType = Math.max(5, limit / 3);
        SecurityUserDetails u = SecurityUtils.currentUser();

        List<SearchHitDto> hits = new ArrayList<>();

        List<EthicsModule> em = ethicsModuleRepository.search(term);
        int emCount = 0;
        for (EthicsModule m : em) {
            if (hits.size() >= limit || emCount >= perType) {
                break;
            }
            String snip = snippet(m.getDescription() != null ? m.getDescription() : m.getKeywords());
            hits.add(new SearchHitDto("ETHICS_MODULE", m.getId(), m.getTitle(), snip));
            emCount++;
        }

        List<Literature> lit = literatureRepository.search(term);
        int litCount = 0;
        for (Literature l : lit) {
            if (hits.size() >= limit || litCount >= perType) {
                break;
            }
            String snip = snippet(l.getAbstractText() != null ? l.getAbstractText() : l.getKeywords());
            hits.add(new SearchHitDto("LITERATURE", l.getId(), l.getTitle(), snip));
            litCount++;
        }

        List<Requirement> reqs = requirementRepository.searchByText(term);
        int reqCount = 0;
        for (Requirement r : reqs) {
            if (hits.size() >= limit || reqCount >= perType) {
                break;
            }
            if (!canSeeRequirement(r, u)) {
                continue;
            }
            String snip = snippet(r.getTextContent() != null ? r.getTextContent() : "");
            hits.add(new SearchHitDto("REQUIREMENT", r.getId(), r.getTitle() != null ? r.getTitle() : "需求#" + r.getId(), snip));
            reqCount++;
        }

        return new SearchResponseDto(term, hits);
    }

    private boolean canSeeRequirement(Requirement r, SecurityUserDetails u) {
        if (u.role() == Role.ADMIN) {
            return true;
        }
        if (r.getUserId().equals(u.id())) {
            return true;
        }
        if (u.role() == Role.TEACHER && r.getCourseId() != null) {
            Course c = courseRepository.findById(r.getCourseId()).orElse(null);
            return c != null && c.getTeacherId().equals(u.id());
        }
        return false;
    }

    private static String snippet(String text) {
        if (text == null || text.isBlank()) {
            return "(无摘要)";
        }
        String t = text.replace("\n", " ").trim();
        if (t.length() <= SNIPPET_LEN) {
            return t;
        }
        return t.substring(0, SNIPPET_LEN) + "…";
    }
}
