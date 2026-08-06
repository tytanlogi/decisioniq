package com.app.decisioniq.config.guardrail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class GuardrailConfigurationTest {

    @Autowired
    private GuardrailProperties properties;

    @Test
    void bindsVersionedYamlPolicy() {
        assertThat(properties.version()).isEqualTo("1.3");
        assertThat(properties.limits().maxQuestionCharacters()).isEqualTo(4000);
        assertThat(properties.detectors().enabled()).containsExactly(
                GuardrailProperties.DetectorId.RAW_SQL,
                GuardrailProperties.DetectorId.SCRIPT_ATTACK,
                GuardrailProperties.DetectorId.INSTRUCTION_BYPASS
        );
        assertThat(properties.responses().blockedUnsupportedContent().message())
                .contains("read-only decision assistance")
                .contains("existing data");
        assertThat(properties.patterns().detectors().rawSql())
                .hasSize(13)
                .extracting(GuardrailProperties.PatternDefinition::id)
                .contains(
                        "sql.select-from",
                        "sql.select-aggregate",
                        "sql.select-quoted-identifiers",
                        "sql.copy",
                        "sql.call-execute",
                        "sql.privilege"
                );
        assertThat(properties.patterns().detectors().instructionBypass())
                .hasSize(8)
                .extracting(GuardrailProperties.PatternDefinition::id)
                .contains(
                        "bypass.ignore-instructions",
                        "bypass.forget-instructions",
                        "bypass.override-policy",
                        "bypass.no-restrictions-roleplay"
                );
    }

    @Test
    void rejectsConfigurationThatDisablesMandatoryDetector() {
        GuardrailProperties invalid = new GuardrailProperties(
                properties.version(),
                properties.limits(),
                properties.normalization(),
                properties.patterns(),
                new GuardrailProperties.Detectors(List.of(
                        GuardrailProperties.DetectorId.RAW_SQL,
                        GuardrailProperties.DetectorId.SCRIPT_ATTACK
                )),
                properties.responses()
        );

        assertThatThrownBy(() -> new GuardrailConfigurationValidator(invalid).validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mandatory guardrail security detectors");
    }
}
