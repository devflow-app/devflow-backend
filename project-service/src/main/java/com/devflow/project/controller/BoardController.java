package com.devflow.project.controller;

import com.devflow.common.dto.ApiResponse;
import com.devflow.project.dto.request.BoardRequest;
import com.devflow.project.dto.response.BoardResponse;
import com.devflow.project.security.UserPrincipal;
import com.devflow.project.service.BoardService;
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
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
@Tag(name = "Boards", description = "Kanban/Sprint Board management")
@SecurityRequirement(name = "Bearer Authentication")
public class BoardController {

    private final BoardService boardService;

    @PostMapping
    @Operation(summary = "Create a new board")
    public ResponseEntity<ApiResponse<BoardResponse>> create(
            @Valid @RequestBody BoardRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(boardService.createBoard(request, principal.getId())));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get all boards in a project")
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.getBoardsByProject(projectId)));
    }
}
