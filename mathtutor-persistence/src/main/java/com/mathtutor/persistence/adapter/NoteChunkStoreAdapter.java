package com.mathtutor.persistence.adapter;

import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.port.NoteChunkStore;
import com.mathtutor.persistence.mapper.NoteChunkMapper;
import com.mathtutor.persistence.repository.NoteChunkRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adaptador de NoteChunkStore que usa NoteChunkRepository (JPA).
 * Implementa el puerto definido en mathtutor-common.
 */
@Component
public class NoteChunkStoreAdapter implements NoteChunkStore {

    private final NoteChunkRepository repository;
    private final NoteChunkMapper mapper;

    public NoteChunkStoreAdapter(NoteChunkRepository repository, NoteChunkMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public com.mathtutor.common.dto.NoteChunk save(com.mathtutor.common.dto.NoteChunk chunk) {
        com.mathtutor.persistence.entity.NoteChunk entity = mapper.toEntity(chunk);
        return mapper.toDto(repository.save(entity));
    }

    @Override
    public List<com.mathtutor.common.dto.NoteChunk> findAll() {
        return repository.findAll().stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<com.mathtutor.common.dto.NoteChunk> findByModuleCode(String moduleCode) {
        return repository.findByModuleCode(moduleCode).stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<com.mathtutor.common.dto.NoteChunk> findByTopicCode(String topicCode) {
        return repository.findByTopicCode(topicCode).stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<com.mathtutor.common.dto.NoteChunk> findByModuleCodeAndTopicCode(String moduleCode, String topicCode) {
        return repository.findByModuleCodeAndTopicCode(moduleCode, topicCode).stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<com.mathtutor.common.dto.NoteChunk> findByModuleCodeAndTopicCode(String moduleCode, String topicCode, int offset, int limit) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        return repository.findByModuleCodeAndTopicCode(moduleCode, topicCode, pageable)
            .stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());
    }
}