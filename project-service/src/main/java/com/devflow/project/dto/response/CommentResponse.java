package com.devflow.project.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    private UUID id;
    private UUID taskId;
    private UUID authorId;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
