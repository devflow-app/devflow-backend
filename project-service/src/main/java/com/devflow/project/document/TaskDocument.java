package com.devflow.project.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.*;

@Document(indexName = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "project_id")
    private String projectId;

    @Field(type = FieldType.Keyword, name = "project_key")
    private String projectKey;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String priority;

    @Field(type = FieldType.Keyword, name = "assignee_id")
    private String assigneeId;

    @Field(type = FieldType.Keyword)
    private String key;
}
