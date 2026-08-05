package com.app.decisioniq.config.nlp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "decisioniq.operation-policy")
public record OperationPolicyProperties(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,32}") String version,
        @NotNull @Valid Actions actions,
        @NotNull @Valid Targets targets
) {

    public record Actions(
            @NotEmpty List<@NotBlank @Size(max = 40) String> read,
            @NotEmpty List<@NotBlank @Size(max = 40) String> presentation,
            @NotEmpty List<@NotBlank @Size(max = 40) String> construct,
            @NotEmpty List<@NotBlank @Size(max = 40) String> persist,
            @NotEmpty List<@NotBlank @Size(max = 40) String> modify,
            @NotEmpty List<@NotBlank @Size(max = 40) String> transfer,
            @NotEmpty List<@NotBlank @Size(max = 40) String> external
    ) {
        public Actions {
            read = copy(read);
            presentation = copy(presentation);
            construct = copy(construct);
            persist = copy(persist);
            modify = copy(modify);
            transfer = copy(transfer);
            external = copy(external);
        }
    }

    public record Targets(
            @NotEmpty List<@NotBlank @Size(max = 40) String> presentation,
            @NotEmpty List<@NotBlank @Size(max = 40) String> persistent,
            @NotEmpty List<@NotBlank @Size(max = 40) String> domainData
    ) {
        public Targets {
            presentation = copy(presentation);
            persistent = copy(persistent);
            domainData = copy(domainData);
        }
    }

    private static List<String> copy(List<String> values) {
        return values == null ? null : List.copyOf(values);
    }
}
