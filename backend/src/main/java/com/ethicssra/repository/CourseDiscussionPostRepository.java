package com.ethicssra.repository;

import com.ethicssra.domain.CourseDiscussionPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseDiscussionPostRepository extends JpaRepository<CourseDiscussionPost, Long> {

    List<CourseDiscussionPost> findByCourseIdAndVisibleTrueOrderByCreatedAtDesc(Long courseId);

    List<CourseDiscussionPost> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    List<CourseDiscussionPost> findByCourseIdAndVisibleTrueAndCategoryOrderByCreatedAtDesc(Long courseId, String category);

    List<CourseDiscussionPost> findByCourseIdAndCategoryOrderByCreatedAtDesc(Long courseId, String category);

    Optional<CourseDiscussionPost> findByIdAndCourseId(Long id, Long courseId);
}
