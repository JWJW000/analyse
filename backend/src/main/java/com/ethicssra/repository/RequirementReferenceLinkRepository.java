package com.ethicssra.repository;

import com.ethicssra.dto.RequirementReferenceLinkDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Repository
public class RequirementReferenceLinkRepository {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT;

    private static final RowMapper<RequirementReferenceLinkDto> ROW_MAPPER = (rs, rowNum) -> {
        Timestamp ts = rs.getTimestamp("created_at");
        String createdAt = ts != null ? FMT.format(ts.toInstant()) : null;
        return new RequirementReferenceLinkDto(
                rs.getLong("id"),
                rs.getLong("requirement_id"),
                rs.getLong("reference_id"),
                rs.getString("evidence_note"),
                rs.getObject("confidence") != null ? rs.getDouble("confidence") : null,
                createdAt
        );
    };

    private final NamedParameterJdbcTemplate jdbc;

    public RequirementReferenceLinkRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public RequirementReferenceLinkDto create(Long requirementId, Long referenceId, String evidenceNote, Double confidence) {
        String sql = """
                INSERT INTO requirement_reference_link(requirement_id, reference_id, evidence_note, confidence)
                VALUES (:requirementId, :referenceId, :evidenceNote, :confidence)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("requirementId", requirementId)
                .addValue("referenceId", referenceId)
                .addValue("evidenceNote", evidenceNote)
                .addValue("confidence", confidence);
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建引用映射失败");
        }
        return getById(key.longValue());
    }

    public RequirementReferenceLinkDto getById(Long id) {
        String sql = """
                SELECT id, requirement_id, reference_id, evidence_note, confidence, created_at
                FROM requirement_reference_link
                WHERE id = :id
                """;
        List<RequirementReferenceLinkDto> rows = jdbc.query(sql, new MapSqlParameterSource("id", id), ROW_MAPPER);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("引用映射不存在");
        }
        return rows.get(0);
    }

    public List<RequirementReferenceLinkDto> findByRequirementId(Long requirementId) {
        String sql = """
                SELECT id, requirement_id, reference_id, evidence_note, confidence, created_at
                FROM requirement_reference_link
                WHERE requirement_id = :requirementId
                ORDER BY id DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("requirementId", requirementId), ROW_MAPPER);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM requirement_reference_link WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    public int countByRequirementId(Long requirementId) {
        String sql = "SELECT COUNT(*) FROM requirement_reference_link WHERE requirement_id = :requirementId";
        Integer val = jdbc.queryForObject(sql, new MapSqlParameterSource("requirementId", requirementId), Integer.class);
        return val != null ? val : 0;
    }

    public int countByRequirementIds(Set<Long> requirementIds) {
        if (requirementIds == null || requirementIds.isEmpty()) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM requirement_reference_link WHERE requirement_id IN (:ids)";
        Integer val = jdbc.queryForObject(sql, new MapSqlParameterSource("ids", requirementIds), Integer.class);
        return val != null ? val : 0;
    }
}
