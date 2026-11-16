package com.mathtutor.llm;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.IllegalTransactionStateException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpringAiPgVectorRetrieverTest {
    private final VectorStore store = mock(VectorStore.class);
    private final RagProperties properties = new RagProperties();
    private SpringAiPgVectorRetriever retriever() {
        properties.setSimilarityThreshold(0.7);
        return new SpringAiPgVectorRetriever(store, properties);
    }

    @Test
    void blankQueriesDoNotCallProvider() {
        var retriever = retriever();
        assertThat(retriever.retrieve(null, null, null)).isEmpty();
        assertThat(retriever.retrieve(" ", null, null)).isEmpty();
        verifyNoInteractions(store);
    }

    @Test
    void mapsIdsMetadataAndUsesConfiguredFilterAndThreshold() {
        var capture = org.mockito.ArgumentCaptor.forClass(SearchRequest.class);
        when(store.similaritySearch(capture.capture())).thenReturn(List.of(new Document("texto", Map.of(
                "id", 42L, "type", "CONCEPTO", "module_code", "M", "topic_code", "T", "source_file", "a.tex", "version", "2"))));
        var result = retriever().retrieve("consulta", "M", "T");
        assertThat(result.get(0).id()).isEqualTo(42L);
        assertThat(result.get(0).content()).isEqualTo("texto");
        assertThat(result.get(0).moduleCode()).isEqualTo("M");
        assertThat(capture.getValue().getTopK()).isEqualTo(10);
        assertThat(capture.getValue().getSimilarityThreshold()).isEqualTo(0.7);
        assertThat(capture.getValue().getFilterExpression()).isNotNull();
    }

    @Test
    void supportsOptionalFiltersAndDefaultMetadata() {
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document("texto")));
        var retriever = retriever();
        for (String[] filters : List.of(new String[]{null, null}, new String[]{"M", null}, new String[]{null, "T"}, new String[]{" ", " "})) {
            var result = retriever.retrieve("consulta", filters[0], filters[1]);
            assertThat(result.get(0).id()).isZero();
            assertThat(result.get(0).type()).isEqualTo("UNKNOWN");
        }
    }

    @Test
    void indexesDocumentsAndIgnoresEmptyInput() {
        var retriever = retriever();
        retriever.indexAtStartup();
        retriever.indexDocuments(null);
        retriever.indexDocuments(List.of());
        verifyNoInteractions(store);
        var chunk = new com.mathtutor.common.dto.NoteChunk(9L, "texto", null, "CONCEPTO", "a.tex", "M", "T", "1");
        retriever.indexDocuments(List.of(chunk));
        verify(store).add(argThat(docs -> docs.size() == 1 && docs.get(0).getMetadata().get("id").equals(9L)));
    }

    @Test
    void rejectsCallerTransactionBeforeSearchOrIndexing() {
        var manager = new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected boolean isExistingTransaction(Object transaction) { return true; }
            protected void doBegin(Object tx, TransactionDefinition definition) { }
            protected void doCommit(DefaultTransactionStatus status) { }
            protected void doRollback(DefaultTransactionStatus status) { }
        };
        var proxyFactory = new ProxyFactory(retriever());
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        var proxy = (SpringAiPgVectorRetriever) proxyFactory.getProxy();
        assertThatThrownBy(() -> proxy.retrieve("consulta", null, null)).isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> proxy.indexDocuments(List.of())).isInstanceOf(IllegalTransactionStateException.class);
        verifyNoInteractions(store);
    }
}
