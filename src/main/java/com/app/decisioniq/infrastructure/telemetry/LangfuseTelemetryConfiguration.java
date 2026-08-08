package com.app.decisioniq.infrastructure.telemetry;

import com.app.decisioniq.config.llm.LangfuseProperties;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class LangfuseTelemetryConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "decisioniq.llm.langfuse", name = "enabled", havingValue = "true")
    SdkTracerProvider langfuseTracerProvider(LangfuseProperties properties) {
        String credentials = properties.publicKey() + ":" + properties.secretKey();
        String authorization = "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
        );
        OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(properties.endpoint())
                .addHeader("Authorization", authorization)
                .addHeader("x-langfuse-ingestion-version", "4")
                .build();
        Resource resource = Resource.getDefault().merge(Resource.create(Attributes.of(
                AttributeKey.stringKey("service.name"), "decisioniq"
        )));
        return SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .build();
    }

    @Bean
    OpenTelemetry decisionIqOpenTelemetry(ObjectProvider<SdkTracerProvider> provider) {
        SdkTracerProvider tracerProvider = provider.getIfAvailable();
        return tracerProvider == null
                ? OpenTelemetry.noop()
                : OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
    }

    @Bean
    Tracer decisionIqTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("com.app.decisioniq.llm", "1.0");
    }
}
