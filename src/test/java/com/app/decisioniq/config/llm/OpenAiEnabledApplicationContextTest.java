package com.app.decisioniq.config.llm;

import com.app.decisioniq.application.interpretation.RequestInterpretationService;
import com.app.decisioniq.infrastructure.llm.openai.OpenAiRequestInterpretationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "decisioniq.llm.openai.enabled=true",
        "decisioniq.llm.openai.api-key=test-key-not-used",
        "decisioniq.llm.openai.allow-external-content=true"
})
class OpenAiEnabledApplicationContextTest {

    @Autowired
    private RequestInterpretationService interpretationService;

    @Test
    void selectsOpenAiGatewayWhenExplicitlyEnabled() {
        assertThat(interpretationService)
                .isInstanceOf(OpenAiRequestInterpretationService.class);
    }
}
