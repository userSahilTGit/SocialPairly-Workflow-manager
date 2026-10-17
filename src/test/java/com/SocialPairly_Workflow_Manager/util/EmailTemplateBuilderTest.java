package com.SocialPairly_Workflow_Manager.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTemplateBuilderTest {

    @Test
    void refundSummaryTableOmitsTokenRowWhenBlankAndEscapesNullReason() {
        String html = EmailTemplateBuilder.refundSummaryTable("$10", "$1", "  ", "$9", null);
        assertFalse(html.contains("Plan Token Usage Deduction"));
        assertTrue(html.contains("$10"));

        String withToken = EmailTemplateBuilder.refundSummaryTable("$10", "$1", "$2", "$7", "note");
        assertTrue(withToken.contains("Plan Token Usage Deduction"));
        assertTrue(withToken.contains("-$2"));
    }

    @Test
    void refundSummaryTableFourArgDelegates() {
        String html = EmailTemplateBuilder.refundSummaryTable("$10", "$1", "$9", "reason");
        assertTrue(html.contains("$9"));
    }

    @Test
    void buttonAndInfoBoxHandleNulls() {
        assertDoesNotThrow(() -> EmailTemplateBuilder.button("Click", null));
        assertDoesNotThrow(() -> EmailTemplateBuilder.paragraph(null));
        assertDoesNotThrow(() -> EmailTemplateBuilder.infoBox("Title", java.util.Arrays.asList("a", null)));
    }

    @Test
    void buildFooterDirectsQueriesToSupportWithoutReplyPrompt() {
        String html = EmailTemplateBuilder.build("SocialPairly", "customercare@socialpairly.com", "<p>Hi</p>");
        assertTrue(html.contains("For any more queries, write us at"));
        assertTrue(html.contains("customercare@socialpairly.com"));
        assertFalse(html.contains("Reply to this email"));
        assertFalse(html.contains("Questions?"));
    }
}
