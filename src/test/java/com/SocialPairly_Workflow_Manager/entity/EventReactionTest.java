package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventReactionTest {

    @Test
    void shouldExposeAllFieldsAndLifecycleHooks() {
        EventDetail event = new EventDetail();
        event.setId(1L);

        EventReaction reaction = new EventReaction();
        reaction.setId(5L);
        reaction.setEvent(event);
        reaction.setFromUserId(1L);
        reaction.setToUserId(2L);
        reaction.setReactionType(ReactionType.Priority);
        reaction.setTokensSpent(5);

        LocalDateTime preset = LocalDateTime.of(2024, 5, 1, 12, 0);
        reaction.setCreatedAt(preset);
        ReflectionTestUtils.invokeMethod(reaction, "onCreate");
        assertEquals(preset, reaction.getCreatedAt());

        reaction.setCreatedAt(null);
        ReflectionTestUtils.invokeMethod(reaction, "onCreate");
        assertNotNull(reaction.getCreatedAt());

        assertEquals(5L, reaction.getId());
        assertSame(event, reaction.getEvent());
        assertEquals(1L, reaction.getFromUserId());
        assertEquals(2L, reaction.getToUserId());
        assertEquals(ReactionType.Priority, reaction.getReactionType());
        assertEquals(5, reaction.getTokensSpent());
    }
}
