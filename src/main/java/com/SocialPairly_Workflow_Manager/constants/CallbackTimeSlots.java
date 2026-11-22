package com.SocialPairly_Workflow_Manager.constants;

import com.SocialPairly_Workflow_Manager.exception.BadRequestException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Canonical 24/7 two-hour phone callback time slots (12 slots per day).
 */
public final class CallbackTimeSlots {

    private static final Map<String, String> SLOT_LABELS;
    private static final Map<String, String> LABEL_TO_CODE;
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    static {
        Map<String, String> slots = new LinkedHashMap<>();
        slots.put("00:00-02:00", "12:00 AM - 02:00 AM");
        slots.put("02:00-04:00", "02:00 AM - 04:00 AM");
        slots.put("04:00-06:00", "04:00 AM - 06:00 AM");
        slots.put("06:00-08:00", "06:00 AM - 08:00 AM");
        slots.put("08:00-10:00", "08:00 AM - 10:00 AM");
        slots.put("10:00-12:00", "10:00 AM - 12:00 PM");
        slots.put("12:00-14:00", "12:00 PM - 02:00 PM");
        slots.put("14:00-16:00", "02:00 PM - 04:00 PM");
        slots.put("16:00-18:00", "04:00 PM - 06:00 PM");
        slots.put("18:00-20:00", "06:00 PM - 08:00 PM");
        slots.put("20:00-22:00", "08:00 PM - 10:00 PM");
        slots.put("22:00-24:00", "10:00 PM - 12:00 AM");
        SLOT_LABELS = Collections.unmodifiableMap(slots);

        Map<String, String> reverse = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : slots.entrySet()) {
            reverse.put(entry.getValue().toUpperCase(Locale.ROOT), entry.getKey());
        }
        LABEL_TO_CODE = Collections.unmodifiableMap(reverse);
    }

    private CallbackTimeSlots() {
    }

    public static Map<String, String> allSlots() {
        return SLOT_LABELS;
    }

    public static List<Map<String, String>> catalog() {
        return SLOT_LABELS.entrySet().stream()
                .map(entry -> Map.of("code", entry.getKey(), "label", entry.getValue()))
                .toList();
    }

    public static String normalizeSlot(String rawSlot) {
        if (rawSlot == null || rawSlot.isBlank()) {
            return null;
        }
        String trimmed = rawSlot.trim().replace("–", "-");
        if (SLOT_LABELS.containsKey(trimmed)) {
            return trimmed;
        }
        String byLabel = LABEL_TO_CODE.get(trimmed.toUpperCase(Locale.ROOT));
        if (byLabel != null) {
            return byLabel;
        }
        throw new BadRequestException("Invalid 2-hour callback time slot: " + trimmed);
    }

    public static String labelFor(String slotCode) {
        if (slotCode == null || slotCode.isBlank()) {
            return null;
        }
        return SLOT_LABELS.getOrDefault(slotCode.trim(), slotCode.trim());
    }

    public static LocalDate parsePreferredDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        try {
            LocalDate parsed = LocalDate.parse(rawDate.trim());
            LocalDate earliestAllowed = LocalDate.now().minusDays(1);
            LocalDate latestAllowed = LocalDate.now().plusDays(14);
            if (parsed.isBefore(earliestAllowed) || parsed.isAfter(latestAllowed)) {
                throw new BadRequestException("Callback date must be within the upcoming schedule window.");
            }
            return parsed;
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid callback date format. Expected YYYY-MM-DD.");
        }
    }

    public static String dayNameFor(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    public static String formatScheduleSummary(LocalDate preferredDate, String preferredDay, String timeSlot) {
        String slotLabel = labelFor(timeSlot);
        String dayPart = preferredDay != null && !preferredDay.isBlank()
                ? preferredDay.trim()
                : dayNameFor(preferredDate);
        if (preferredDate == null && slotLabel == null && dayPart == null) {
            return null;
        }
        String datePart = preferredDate != null ? preferredDate.format(DISPLAY_DATE) : null;
        StringBuilder sb = new StringBuilder();
        if (dayPart != null) {
            sb.append(dayPart);
        }
        if (datePart != null) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(datePart);
        }
        if (slotLabel != null) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append(slotLabel);
        }
        return sb.toString();
    }
}
