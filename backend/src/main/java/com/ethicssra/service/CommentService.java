package com.ethicssra.service;

import com.ethicssra.domain.Comment;
import com.ethicssra.dto.AddCommentRequest;
import com.ethicssra.dto.CommentDto;
import com.ethicssra.repository.CommentRepository;
import com.ethicssra.repository.UserRepository;
import com.ethicssra.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public CommentDto addComment(AddCommentRequest request) {
        Long userId = SecurityUtils.currentUserId();
        
        Comment comment = new Comment();
        comment.setProjectId(request.projectId());
        comment.setRequirementId(request.requirementId());
        comment.setContent(request.content());
        comment.setUserId(userId);
        comment.setParentId(request.parentId());
        
        Comment saved = commentRepository.save(comment);
        String userName = getUserName(userId);
        return CommentDto.from(saved, userName);
    }

    public List<CommentDto> getComments(Long projectId, Long requirementId) {
        return commentRepository.findByProjectAndRequirement(projectId, requirementId)
            .stream()
            .map(c -> CommentDto.from(c, getUserName(c.getUserId())))
            .toList();
    }

    public List<CommentDto> getProjectComments(Long projectId) {
        return commentRepository.findByProjectOrderByCreatedAtDesc(projectId)
            .stream()
            .map(c -> CommentDto.from(c, getUserName(c.getUserId())))
            .toList();
    }

    public void deleteComment(Long commentId) {
        Long userId = SecurityUtils.currentUserId();
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
        
        if (!comment.getUserId().equals(userId)) {
            throw new AccessDeniedException("Cannot delete others' comment");
        }
        
        commentRepository.delete(comment);
    }

    private String getUserName(Long userId) {
        return userRepository.findById(userId)
            .map(u -> u.getDisplayName() != null ? u.getDisplayName() : u.getUsername())
            .orElse("Unknown");
    }
}