package com.devflow.project.service;

import com.devflow.common.exception.DevFlowException;
import com.devflow.project.dto.request.CommentRequest;
import com.devflow.project.dto.response.CommentResponse;
import com.devflow.project.entity.Comment;
import com.devflow.project.entity.Task;
import com.devflow.project.mapper.ProjectMapper;
import com.devflow.project.repository.CommentRepository;
import com.devflow.project.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final ProjectMapper projectMapper;
    private final AuditTrailService auditTrailService;

    @Transactional
    public CommentResponse addComment(UUID taskId, CommentRequest request, UUID authorId) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new DevFlowException("Task not found", HttpStatus.NOT_FOUND));

        Comment comment = Comment.builder()
                .task(task)
                .authorId(authorId)
                .content(request.getContent().trim())
                .build();

        comment = commentRepository.save(comment);
        
        // Log comment creation in task history
        auditTrailService.logChange("Task", task.getId(), "COMMENT", "comment", null, "New comment by " + authorId, authorId);

        log.info("Comment added to task {} by user {}", task.getKey(), authorId);
        return projectMapper.toResponse(comment);
    }

    public List<CommentResponse> getCommentsByTask(UUID taskId) {
        return commentRepository.findAllByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(UUID commentId, UUID actorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new DevFlowException("Comment not found", HttpStatus.NOT_FOUND));

        if (!comment.getAuthorId().equals(actorId)) {
            throw new DevFlowException("Only the author can delete this comment", HttpStatus.FORBIDDEN);
        }

        commentRepository.delete(comment);
        log.info("Comment {} deleted by author {}", commentId, actorId);
    }
}
