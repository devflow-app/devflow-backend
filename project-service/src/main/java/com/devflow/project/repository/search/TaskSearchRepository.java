package com.devflow.project.repository.search;

import com.devflow.project.document.TaskDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskSearchRepository extends ElasticsearchRepository<TaskDocument, String> {
    List<TaskDocument> findByTitleContainingOrDescriptionContainingAndProjectId(String titleQuery, String descriptionQuery, String projectId);
}
