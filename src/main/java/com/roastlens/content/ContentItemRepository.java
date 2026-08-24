package com.roastlens.content;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentItemRepository extends JpaRepository<ContentItem, String> {
    boolean existsBySourceEventId(String sourceEventId);

    @EntityGraph(attributePaths = "candidates")
    Optional<ContentItem> findBySourceEventId(String sourceEventId);

    @Override
    @EntityGraph(attributePaths = "candidates")
    Optional<ContentItem> findById(String id);

    List<ContentItem> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
