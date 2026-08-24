package com.roastlens.content;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:inventory;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class ContentItemRepositoryTest {
    @Autowired ContentItemRepository repository;

    @Test
    void persistsGeneratedItemAndCandidates() {
        ContentItem saved = repository.saveAndFlush(item("evt-1", ContentStatus.GENERATED,
                List.of(new ContentCandidate("joke", "dry", "low"))));

        ContentItem found = repository.findBySourceEventId("evt-1").orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getStatus()).isEqualTo(ContentStatus.GENERATED);
        assertThat(found.getCandidates()).singleElement().satisfies(candidate ->
                assertThat(candidate.getText()).isEqualTo("joke"));
    }

    @Test
    void sourceEventIdIsDatabaseUnique() {
        repository.saveAndFlush(item("same", ContentStatus.SKIPPED, List.of()));
        assertThatThrownBy(() -> repository.saveAndFlush(item("same", ContentStatus.GENERATED, List.of())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private ContentItem item(String eventId, ContentStatus status, List<ContentCandidate> candidates) {
        return new ContentItem(eventId, "FINSTREAM", "BTCUSDT", "ABNORMAL_VOLUME", null, null,
                .53, "zh-CN", status, candidates);
    }
}
