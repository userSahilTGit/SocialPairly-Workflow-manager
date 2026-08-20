package com.SocialPairly_Workflow_Manager.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Lightweight timing helper for Identity &amp; Background endpoints (US-025).
 * Emits structured logs and X-Identity-Duration-Ms response headers for telemetry.
 */
public final class IdentityPerf {

    private static final Logger log = LoggerFactory.getLogger(IdentityPerf.class);

    /** Soft Release-1 target for identity API responses under normal load (ms). */
    public static final long TARGET_MS = 2000L;

    private IdentityPerf() {}

    public static <T> ResponseEntity<T> timed(String operation, Long userId, Supplier<ResponseEntity<T>> action) {
        long startNs = System.nanoTime();
        ResponseEntity<T> response = action.get();
        long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
        boolean withinTarget = durationMs <= TARGET_MS;
        log.info(
                "identity_perf op={} userId={} durationMs={} targetMs={} withinTarget={}",
                operation,
                userId,
                durationMs,
                TARGET_MS,
                withinTarget
        );
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .header("X-Identity-Duration-Ms", String.valueOf(durationMs))
                .header("X-Identity-Op", operation)
                .header("X-Identity-Within-Target", String.valueOf(withinTarget))
                .body(response.getBody());
    }

    public static Map<String, Object> timingMeta(long durationMs) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("durationMs", durationMs);
        meta.put("targetMs", TARGET_MS);
        meta.put("withinTarget", durationMs <= TARGET_MS);
        return meta;
    }
}
