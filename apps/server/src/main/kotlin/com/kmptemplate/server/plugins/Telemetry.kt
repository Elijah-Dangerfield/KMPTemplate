package com.kmptemplate.server.plugins

import com.kmptemplate.server.config.ObservabilityConfig
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.logging.LoggingMetricExporter
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.metrics.export.MetricReader
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.slf4j.LoggerFactory
import java.time.Duration as JavaDuration

/**
 * Initialises the OpenTelemetry SDK and registers it as [GlobalOpenTelemetry],
 * so feature code reads the tracer/meter via the global without threading the
 * SDK through constructors. Returns the live SDK.
 *
 * **Env-gated by a single switch.** `OTEL_EXPORTER_OTLP_ENDPOINT` flips traces,
 * logs, and metrics together:
 *  - unset → stdout exporters (spans/logs/metrics appear in your normal log
 *    output, so dev + unconfigured deploys stay debuggable, no external sink).
 *  - set   → OTLP/HTTP for all three, with headers from
 *    `OTEL_EXPORTER_OTLP_HEADERS` (`Key=Value,Key=Value`, percent-decoded for
 *    `Authorization=Basic%20…`-style vendor headers).
 *
 * The global slot is set-once. We do **not** probe with `GlobalOpenTelemetry.get()`
 * first — the probe locks the slot to noop and makes our `set()` throw. Try-set-
 * catch is the only safe order.
 */
fun Application.installOpenTelemetry(config: ObservabilityConfig): OpenTelemetry {
    val log = LoggerFactory.getLogger("OpenTelemetry")

    val sdk = buildOpenTelemetrySdk(config)
    val installed: OpenTelemetry = try {
        GlobalOpenTelemetry.set(sdk)
        sdk
    } catch (e: IllegalStateException) {
        log.warn("OpenTelemetry global already set; reusing the live SDK", e)
        GlobalOpenTelemetry.get()
    }
    OpenTelemetryAppender.install(installed)
    ServerMetrics.register()

    monitor.subscribe(ApplicationStopPreparing) {
        try {
            sdk.close()
        } catch (e: Throwable) {
            log.warn("OpenTelemetry SDK shutdown failed", e)
        }
    }

    log.info(
        "OpenTelemetry initialised (service={}, env={}, exporter={})",
        config.serviceName,
        config.environment,
        if (config.otlpEndpoint.isNullOrBlank()) "stdout" else "otlp-http",
    )
    return installed
}

private fun buildOpenTelemetrySdk(config: ObservabilityConfig): OpenTelemetrySdk {
    val resource = Resource.getDefault().merge(
        Resource.create(
            Attributes.builder()
                .put(AttributeKey.stringKey("service.name"), config.serviceName)
                .put(AttributeKey.stringKey("deployment.environment"), config.environment)
                .apply { config.release?.let { put(AttributeKey.stringKey("service.version"), it) } }
                .build(),
        ),
    )
    val otlp = !config.otlpEndpoint.isNullOrBlank()

    val spanExporter = defaultSpanExporter(config)
    val tracerProvider = SdkTracerProvider.builder()
        .setResource(resource)
        .addSpanProcessor(
            if (otlp) BatchSpanProcessor.builder(spanExporter).build()
            else SimpleSpanProcessor.create(spanExporter),
        )
        .build()

    val logExporter = defaultLogRecordExporter(config)
    val loggerProvider = SdkLoggerProvider.builder()
        .setResource(resource)
        .addLogRecordProcessor(
            if (otlp) BatchLogRecordProcessor.builder(logExporter).build()
            else SimpleLogRecordProcessor.create(logExporter),
        )
        .build()

    val meterProvider = SdkMeterProvider.builder()
        .setResource(resource)
        .registerMetricReader(defaultMetricReader(config))
        .build()

    return OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .setLoggerProvider(loggerProvider)
        .setMeterProvider(meterProvider)
        .build()
}

private fun defaultSpanExporter(config: ObservabilityConfig) =
    config.otlpEndpoint?.takeUnless { it.isBlank() }?.let { endpoint ->
        OtlpHttpSpanExporter.builder()
            .setEndpoint(endpoint.trimEnd('/') + "/v1/traces")
            .apply { parseOtlpHeaders(config.otlpHeaders).forEach { (n, v) -> addHeader(n, v) } }
            .build()
    } ?: LoggingSpanExporter.create()

private fun defaultLogRecordExporter(config: ObservabilityConfig) =
    config.otlpEndpoint?.takeUnless { it.isBlank() }?.let { endpoint ->
        OtlpHttpLogRecordExporter.builder()
            .setEndpoint(endpoint.trimEnd('/') + "/v1/logs")
            .apply { parseOtlpHeaders(config.otlpHeaders).forEach { (n, v) -> addHeader(n, v) } }
            .build()
    } ?: SystemOutLogRecordExporter.create()

private fun defaultMetricExporter(config: ObservabilityConfig): MetricExporter =
    config.otlpEndpoint?.takeUnless { it.isBlank() }?.let { endpoint ->
        OtlpHttpMetricExporter.builder()
            .setEndpoint(endpoint.trimEnd('/') + "/v1/metrics")
            .apply { parseOtlpHeaders(config.otlpHeaders).forEach { (n, v) -> addHeader(n, v) } }
            .build()
    } ?: LoggingMetricExporter.create()

private fun defaultMetricReader(config: ObservabilityConfig): MetricReader =
    PeriodicMetricReader.builder(defaultMetricExporter(config))
        .setInterval(JavaDuration.ofSeconds(60))
        .build()

/**
 * Parses an `OTEL_EXPORTER_OTLP_HEADERS`-shaped string (`Key=Value,Key=Value`).
 * Values are percent-decoded per the OTel spec — vendors hand you headers like
 * `Authorization=Basic%20<base64>` with the space encoded.
 */
internal fun parseOtlpHeaders(raw: String?): List<Pair<String, String>> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(',').mapNotNull { pair ->
        val eq = pair.indexOf('=')
        if (eq <= 0) return@mapNotNull null
        pair.substring(0, eq).trim() to java.net.URLDecoder.decode(pair.substring(eq + 1).trim(), Charsets.UTF_8)
    }
}
