package com.mathtutor.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathtutor.common.dto.Prompt;
import com.mathtutor.common.port.LlmGateway;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProviderUsageTest {
    @Test void preservesActualGeminiMetadataEvenWithoutFinalText() throws Exception {
        var model=new SpringAiGoogleAiStudioChatModel("test","test",0.2,100,0,5,new ObjectMapper(),WebClient.builder().build());
        var parse=SpringAiGoogleAiStudioChatModel.class.getDeclaredMethod("parseResponse",String.class,boolean.class); parse.setAccessible(true);
        var response=(ChatResponse)parse.invoke(model,"{\"candidates\":[{\"content\":{\"parts\":[]},\"finishReason\":\"STOP\"}],\"usageMetadata\":{\"promptTokenCount\":12,\"candidatesTokenCount\":4,\"totalTokenCount\":16}}",true);
        assertThat(response.getMetadata().getUsage().getPromptTokens()).isEqualTo(12);
        var chat=mock(org.springframework.ai.chat.model.ChatModel.class);
        when(chat.stream(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(Flux.just(response));
        var usage=new AtomicReference<LlmGateway.Usage>();
        assertThat(new GoogleAiStudioLlmGateway(chat,"test").stream(new Prompt("s","u",null,null,10,0.2),usage::set).collectList().block()).isEmpty();
        assertThat(usage.get()).isEqualTo(new LlmGateway.Usage(12,4));
    }
}
