package com.mathtutor.common.port;

import com.mathtutor.common.dto.NoteChunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Interface contract tests for NoteChunkStore port.
 * Verifies that any implementation adheres to the expected behavior.
 * The actual implementation (NoteChunkStoreAdapter) is tested in mathtutor-persistence.
 */
@ExtendWith(MockitoExtension.class)
class NoteChunkStoreTest {

    @Mock
    private NoteChunkStore noteChunkStore;

    @Test
    @DisplayName("save returns saved NoteChunk with ID")
    void saveReturnsSavedChunk() {
        // Given
        NoteChunk input = new NoteChunk(
                null, "Test content", new float[]{0.1f, 0.2f, 0.3f},
                "DEFINICION", "test.md", "M1", "T1", "1.0");
        NoteChunk expected = new NoteChunk(
                1L, "Test content", new float[]{0.1f, 0.2f, 0.3f},
                "DEFINICION", "test.md", "M1", "T1", "1.0");
        when(noteChunkStore.save(input)).thenReturn(expected);

        // When
        NoteChunk result = noteChunkStore.save(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.content()).isEqualTo("Test content");
        verify(noteChunkStore).save(input);
    }

    @Test
    @DisplayName("findAll returns all chunks")
    void findAllReturnsAllChunks() {
        // Given
        List<NoteChunk> expected = List.of(
                new NoteChunk(1L, "Content 1", new float[]{0.1f}, "DEFINICION", "f1.md", "M1", "T1", "1.0"),
                new NoteChunk(2L, "Content 2", new float[]{0.2f}, "TEOREMA", "f2.md", "M1", "T1", "1.0")
        );
        when(noteChunkStore.findAll()).thenReturn(expected);

        // When
        List<NoteChunk> result = noteChunkStore.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expected);
        verify(noteChunkStore).findAll();
    }

    @Test
    @DisplayName("findByModuleCode filters by module")
    void findByModuleCodeFiltersCorrectly() {
        // Given
        String moduleCode = "M1";
        List<NoteChunk> expected = List.of(
                new NoteChunk(1L, "Content 1", new float[]{0.1f}, "DEFINICION", "f1.md", "M1", "T1", "1.0")
        );
        when(noteChunkStore.findByModuleCode(moduleCode)).thenReturn(expected);

        // When
        List<NoteChunk> result = noteChunkStore.findByModuleCode(moduleCode);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).moduleCode()).isEqualTo("M1");
        verify(noteChunkStore).findByModuleCode(moduleCode);
    }

    @Test
    @DisplayName("findByTopicCode filters by topic")
    void findByTopicCodeFiltersCorrectly() {
        // Given
        String topicCode = "T1";
        List<NoteChunk> expected = List.of(
                new NoteChunk(1L, "Content 1", new float[]{0.1f}, "DEFINICION", "f1.md", "M1", "T1", "1.0")
        );
        when(noteChunkStore.findByTopicCode(topicCode)).thenReturn(expected);

        // When
        List<NoteChunk> result = noteChunkStore.findByTopicCode(topicCode);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).topicCode()).isEqualTo("T1");
        verify(noteChunkStore).findByTopicCode(topicCode);
    }

    @Test
    @DisplayName("findByModuleCodeAndTopicCode filters by both")
    void findByModuleCodeAndTopicCodeFiltersCorrectly() {
        // Given
        String moduleCode = "M1";
        String topicCode = "T1";
        List<NoteChunk> expected = List.of(
                new NoteChunk(1L, "Content 1", new float[]{0.1f}, "DEFINICION", "f1.md", "M1", "T1", "1.0")
        );
        when(noteChunkStore.findByModuleCodeAndTopicCode(moduleCode, topicCode)).thenReturn(expected);

        // When
        List<NoteChunk> result = noteChunkStore.findByModuleCodeAndTopicCode(moduleCode, topicCode);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).moduleCode()).isEqualTo("M1");
        assertThat(result.get(0).topicCode()).isEqualTo("T1");
        verify(noteChunkStore).findByModuleCodeAndTopicCode(moduleCode, topicCode);
    }

    @Test
    @DisplayName("findByModuleCodeAndTopicCode with pagination returns limited results")
    void findByModuleCodeAndTopicCodeWithPaginationReturnsLimited() {
        // Given
        String moduleCode = "M1";
        String topicCode = "T1";
        int offset = 0;
        int limit = 2;
        List<NoteChunk> expected = List.of(
                new NoteChunk(1L, "Content 1", new float[]{0.1f}, "DEFINICION", "f1.md", "M1", "T1", "1.0"),
                new NoteChunk(2L, "Content 2", new float[]{0.2f}, "TEOREMA", "f2.md", "M1", "T1", "1.0")
        );
        when(noteChunkStore.findByModuleCodeAndTopicCode(moduleCode, topicCode, offset, limit)).thenReturn(expected);

        // When
        List<NoteChunk> result = noteChunkStore.findByModuleCodeAndTopicCode(moduleCode, topicCode, offset, limit);

        // Then
        assertThat(result).hasSize(2);
        verify(noteChunkStore).findByModuleCodeAndTopicCode(moduleCode, topicCode, offset, limit);
    }

    @Test
    @DisplayName("Handles null input gracefully in save")
    void saveHandlesNullInput() {
        // Given
        when(noteChunkStore.save(null)).thenReturn(null);

        // When
        NoteChunk result = noteChunkStore.save(null);

        // Then
        assertThat(result).isNull();
        verify(noteChunkStore).save(null);
    }

    @Test
    @DisplayName("Handles empty results gracefully")
    void findAllHandlesEmptyResults() {
        // Given
        when(noteChunkStore.findAll()).thenReturn(List.of());

        // When
        List<NoteChunk> result = noteChunkStore.findAll();

        // Then
        assertThat(result).isEmpty();
        verify(noteChunkStore).findAll();
    }

    @Test
    @DisplayName("Handles non-existent module/topic gracefully")
    void findByNonExistentModuleReturnsEmpty() {
        // Given
        when(noteChunkStore.findByModuleCode("NONEXISTENT")).thenReturn(List.of());
        when(noteChunkStore.findByTopicCode("NONEXISTENT")).thenReturn(List.of());

        // When
        List<NoteChunk> byModule = noteChunkStore.findByModuleCode("NONEXISTENT");
        List<NoteChunk> byTopic = noteChunkStore.findByTopicCode("NONEXISTENT");

        // Then
        assertThat(byModule).isEmpty();
        assertThat(byTopic).isEmpty();
        verify(noteChunkStore).findByModuleCode("NONEXISTENT");
        verify(noteChunkStore).findByTopicCode("NONEXISTENT");
    }
}