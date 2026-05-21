package com.devflow.project.controller;

import com.devflow.common.dto.ApiResponse;
import com.devflow.project.dto.request.CommentRequest;
import com.devflow.project.dto.response.CommentResponse;
import com.devflow.project.security.UserPrincipal;
import com.devflow.project.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Comment management for tasks")
@SecurityRequirement(name = "Bearer Authentication")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Add a comment to a task")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CommentResponse response = commentService.addComment(taskId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @GetMapping
    @Operation(summary = "Get all comments for a task")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable UUID taskId) {
        List<CommentResponse> responses = commentService.getCommentsByTask(taskId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        commentService.deleteComment(commentId, principal.getId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
