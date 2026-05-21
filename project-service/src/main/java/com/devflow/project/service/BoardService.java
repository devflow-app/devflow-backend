package com.devflow.project.service;

import com.devflow.common.exception.DevFlowException;
import com.devflow.project.dto.request.BoardRequest;
import com.devflow.project.dto.response.BoardResponse;
import com.devflow.project.entity.Board;
import com.devflow.project.entity.Project;
import com.devflow.project.mapper.ProjectMapper;
import com.devflow.project.repository.BoardRepository;
import com.devflow.project.repository.ProjectRepository;
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
public class BoardService {

    private final BoardRepository boardRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final AuditTrailService auditTrailService;

    @Transactional
    public BoardResponse createBoard(BoardRequest request, UUID actorId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(request.getProjectId())
                .orElseThrow(() -> new DevFlowException("Project not found", HttpStatus.NOT_FOUND));

        Board board = Board.builder()
                .project(project)
                .name(request.getName().trim())
                .build();

        board = boardRepository.save(board);
        auditTrailService.logChange("Board", board.getId(), "CREATE", null, null, board.getName(), actorId);

        log.info("Board created: {} for project {}", board.getName(), project.getName());
        return projectMapper.toResponse(board);
    }

    public List<BoardResponse> getBoardsByProject(UUID projectId) {
        return boardRepository.findAllByProjectId(projectId).stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }
}
