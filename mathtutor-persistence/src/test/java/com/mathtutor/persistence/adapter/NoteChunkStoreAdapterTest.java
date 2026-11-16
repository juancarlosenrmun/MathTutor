package com.mathtutor.persistence.adapter;

import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.persistence.mapper.NoteChunkMapper;
import com.mathtutor.persistence.repository.NoteChunkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NoteChunkStoreAdapterTest {
    private final NoteChunkRepository repository = mock(NoteChunkRepository.class);
    private final NoteChunkMapper mapper = new NoteChunkMapper();
    private final NoteChunkStoreAdapter store = new NoteChunkStoreAdapter(repository, mapper);
    private final NoteChunk chunk = new NoteChunk(7L, "Derivada", new float[]{-1.5f, 0, 2.25f},
        "DEFINICION", "derivadas.tex", "CALCULO", "DERIVADAS", "v1");

    @Test
    void persistenciaConservaContenidoMetadatosYVector() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        NoteChunk saved = store.save(chunk);
        assertThat(saved).usingRecursiveComparison().isEqualTo(chunk);
        verify(repository).save(argThat(entity -> entity.getId().equals(7L)
            && entity.getModuleCode().equals("CALCULO")
            && entity.getTopicCode().equals("DERIVADAS")));
    }

    @Test
    void filtrosLleganAlRepositorioSinPerderElModuloOTema() {
        var entity = mapper.toEntity(chunk);
        when(repository.findAll()).thenReturn(List.of(entity));
        when(repository.findByModuleCode("CALCULO")).thenReturn(List.of(entity));
        when(repository.findByTopicCode("DERIVADAS")).thenReturn(List.of(entity));
        when(repository.findByModuleCodeAndTopicCode("CALCULO", "DERIVADAS"))
            .thenReturn(List.of(entity));
        assertThat(store.findAll()).usingRecursiveComparison().isEqualTo(List.of(chunk));
        assertThat(store.findByModuleCode("CALCULO")).usingRecursiveComparison().isEqualTo(List.of(chunk));
        assertThat(store.findByTopicCode("DERIVADAS")).usingRecursiveComparison().isEqualTo(List.of(chunk));
        assertThat(store.findByModuleCodeAndTopicCode("CALCULO", "DERIVADAS"))
            .usingRecursiveComparison().isEqualTo(List.of(chunk));
        assertThat(store.findByModuleCode("OTRO")).isEmpty();
    }

    @Test
    void paginaRespetaElLimiteYElDesplazamiento() {
        var page = PageRequest.of(2, 5);
        when(repository.findByModuleCodeAndTopicCode("CALCULO", "DERIVADAS", page))
            .thenReturn(new PageImpl<>(List.of(mapper.toEntity(chunk))));
        assertThat(store.findByModuleCodeAndTopicCode("CALCULO", "DERIVADAS", 10, 5))
            .usingRecursiveComparison().isEqualTo(List.of(chunk));
        verify(repository).findByModuleCodeAndTopicCode("CALCULO", "DERIVADAS", page);
    }

    @Test
    void mapperAdmiteAusenciaDeVectorYDeResultado() {
        assertThat(mapper.toDto(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
        var noVector = new NoteChunk(null, "Texto", null, null, null, null, null, null);
        assertThat(mapper.toDto(mapper.toEntity(noVector)).embedding()).isEmpty();
        var emptyVector = new NoteChunk(null, "Texto", new float[0], null, null, null, null, null);
        assertThat(mapper.toDto(mapper.toEntity(emptyVector)).embedding()).isEmpty();
    }

    @Test
    void desplazamientoNoMultiploDelLimiteNoRepiteElementos() {
        when(repository.findByModuleCodeAndTopicCode(eq("CALCULO"), eq("DERIVADAS"), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        store.findByModuleCodeAndTopicCode("CALCULO", "DERIVADAS", 7, 5);
        verify(repository).findByModuleCodeAndTopicCode(eq("CALCULO"), eq("DERIVADAS"),
            argThat(page -> page.getOffset() == 7 && page.getPageSize() == 5));
    }
}
