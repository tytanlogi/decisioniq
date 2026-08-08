package com.app.decisioniq.infrastructure.llm.openai;

import com.openai.models.responses.ResponseCreateParams;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class OpenAiStructuredOutputSchemaTest {

    @Test
    void interpretationContractProducesAValidOpenAiJsonSchema() {
        assertThatCode(() -> ResponseCreateParams.builder()
                .model("gpt-5-mini")
                .input("{}")
                .text(OpenAiInterpretationOutput.class)
                .build())
                .doesNotThrowAnyException();
    }
}
