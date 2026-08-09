package com.SocialPairly_Workflow_Manager.constants;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class ReligionOptions {

    private ReligionOptions() {}

    public static final String OPEN_TO_ALL = "Open to all";

    public static final List<String> RELIGIONS = List.of(
            "Christianity",
            "Catholicism",
            "Protestantism",
            "Orthodox Christianity",
            "Islam",
            "Hinduism",
            "Buddhism",
            "Sikhism",
            "Judaism",
            "Jainism",
            "Bahai",
            "Spiritual but not religious",
            "Agnostic",
            "Atheist",
            "Prefer not to say",
            "Other"
    );

    public static final List<String> PREFERRED_RELIGIONS = Stream
            .concat(Stream.of(OPEN_TO_ALL), RELIGIONS.stream())
            .toList();

    public static final Set<String> RELIGION_SET = Set.copyOf(RELIGIONS);
    public static final Set<String> PREFERRED_RELIGION_SET = Set.copyOf(PREFERRED_RELIGIONS);

    public static Map<String, List<String>> asReferenceMap() {
        return Map.of(
                "religions", RELIGIONS,
                "preferredReligions", PREFERRED_RELIGIONS
        );
    }
}
