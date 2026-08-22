package com.SocialPairly_Workflow_Manager.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IdentityPerfTest {

    @Test
    void timedAddsDurationHeadersAndPreservesBody() {
        ResponseEntity<Map<String, String>> result = IdentityPerf.timed(
                "getIdentity",
                42L,
                () -> ResponseEntity.ok(Map.of("ok", "true"))
        );

        assertEquals("true", result.getBody().get("ok"));
        assertNotNull(result.getHeaders().getFirst("X-Identity-Duration-Ms"));
        assertEquals("getIdentity", result.getHeaders().getFirst("X-Identity-Op"));
        assertEquals("true", result.getHeaders().getFirst("X-Identity-Within-Target"));
        assertTrue(Long.parseLong(result.getHeaders().getFirst("X-Identity-Duration-Ms")) >= 0);
    }

    @Test
    void timingMetaIncludesTarget() {
        Map<String, Object> meta = IdentityPerf.timingMeta(100);
        assertEquals(100L, meta.get("durationMs"));
        assertEquals(IdentityPerf.TARGET_MS, meta.get("targetMs"));
        assertEquals(true, meta.get("withinTarget"));
    }

    @Test
    void timingMetaMarksOverTarget() {
        Map<String, Object> meta = IdentityPerf.timingMeta(IdentityPerf.TARGET_MS + 1);
        assertEquals(false, meta.get("withinTarget"));
    }

    @Test
    void timedMarksOutsideTargetWhenActionIsSlow() throws Exception {
        ResponseEntity<String> result = IdentityPerf.timed(
                "slowOp",
                1L,
                () -> {
                    try {
                        Thread.sleep(IdentityPerf.TARGET_MS + 50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return ResponseEntity.ok("done");
                }
        );
        assertEquals("false", result.getHeaders().getFirst("X-Identity-Within-Target"));
        assertEquals("done", result.getBody());
    }
}
