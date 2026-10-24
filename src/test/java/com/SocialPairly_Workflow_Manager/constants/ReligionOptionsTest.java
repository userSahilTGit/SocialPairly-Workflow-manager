package com.SocialPairly_Workflow_Manager.constants;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReligionOptionsTest {

    @Test
    void referenceMapExposesReligionsAndPreferredReligions() throws Exception {
        Map<String, List<String>> reference = ReligionOptions.asReferenceMap();
        assertEquals(ReligionOptions.RELIGIONS, reference.get("religions"));
        assertEquals(ReligionOptions.PREFERRED_RELIGIONS, reference.get("preferredReligions"));
        assertTrue(ReligionOptions.PREFERRED_RELIGION_SET.contains(ReligionOptions.OPEN_TO_ALL));

        Constructor<ReligionOptions> constructor = ReligionOptions.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
