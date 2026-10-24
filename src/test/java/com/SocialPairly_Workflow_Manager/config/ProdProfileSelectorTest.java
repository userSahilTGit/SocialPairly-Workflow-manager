package com.SocialPairly_Workflow_Manager.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdProfileSelectorTest {

    @Test
    void argumentWinsOverEverythingElse() {
        assertTrue(ProdProfileSelector.resolve("true", "false", "false", "false"));
        assertFalse(ProdProfileSelector.resolve("false", "true", "true", "true"));
    }

    @Test
    void systemPropertyWinsOverEnvironmentAndFile() {
        assertTrue(ProdProfileSelector.resolve(null, "yes", "false", "false"));
        assertFalse(ProdProfileSelector.resolve("  ", "0", "true", "true"));
    }

    @Test
    void environmentWinsOverFile() {
        assertTrue(ProdProfileSelector.resolve(null, null, "1", "false"));
        assertFalse(ProdProfileSelector.resolve(null, " ", "no", "true"));
    }

    @Test
    void fileValueIsUsedWhenNothingElseIsSet() {
        assertTrue(ProdProfileSelector.resolve(null, null, null, "true"));
        assertFalse(ProdProfileSelector.resolve(null, null, null, "false"));
        assertFalse(ProdProfileSelector.resolve(null, null, null, null));
    }

    @Test
    void placeholderFileValueUsesItsDefaultWhenUnset() {
        String expanded = ProdProfileSelector.expandPlaceholder("${APP_PROD_FLAG:false}");
        String env = System.getenv("APP_PROD_FLAG");
        if (env == null || env.isBlank()) {
            assertEquals("false", expanded);
        } else {
            assertEquals(env.trim(), expanded);
        }
    }

    @Test
    void readsInlineAndFollowingFlagArguments() {
        assertEquals("true", ProdProfileSelector.argumentValue(new String[]{"--server.port=9010", "--app.prod-flag=true"}));
        assertEquals("false", ProdProfileSelector.argumentValue(new String[]{"--app.prod-flag", "false"}));
        assertEquals(null, ProdProfileSelector.argumentValue(new String[]{"--spring.profiles.active=local"}));
        assertEquals("true", ProdProfileSelector.argumentValue(
                new String[]{"--app.preprod-flag=true"}, ProdProfileSelector.PREPROD_FLAG_KEY));
    }

    @Test
    void prodFlagWinsAndPreprodIsUsedOnlyWhenProdIsOff() {
        assertEquals("prod", ProdProfileSelector.selectProfile(true, true));
        assertEquals("prod", ProdProfileSelector.selectProfile(true, false));
        assertEquals("preprod", ProdProfileSelector.selectProfile(false, true));
        assertEquals(null, ProdProfileSelector.selectProfile(false, false));
    }

    @org.junit.jupiter.api.AfterEach
    void clearFlagProperties() {
        System.clearProperty(ProdProfileSelector.FLAG_KEY);
        System.clearProperty(ProdProfileSelector.PREPROD_FLAG_KEY);
    }

    @Test
    void publicSelectorsHonorArgumentsSystemPropertiesAndTheClasspathFile() {
        assertEquals("prod", ProdProfileSelector.selectProfile(new String[]{"--app.prod-flag=true", "--app.preprod-flag=true"}));
        assertEquals("preprod", ProdProfileSelector.selectProfile(new String[]{"--app.preprod-flag", "1"}));
        assertFalse(ProdProfileSelector.isProdEnabled(new String[]{"--app.prod-flag=no"}));
        assertFalse(ProdProfileSelector.isPreprodEnabled(new String[]{"--app.preprod-flag=false"}));

        System.setProperty(ProdProfileSelector.FLAG_KEY, "yes");
        System.setProperty(ProdProfileSelector.PREPROD_FLAG_KEY, "1");
        try {
            assertTrue(ProdProfileSelector.isProdEnabled(new String[]{}));
            assertTrue(ProdProfileSelector.isPreprodEnabled(null));
            assertEquals("prod", ProdProfileSelector.selectProfile(new String[0]));
        } finally {
            System.clearProperty(ProdProfileSelector.FLAG_KEY);
            System.clearProperty(ProdProfileSelector.PREPROD_FLAG_KEY);
        }

        assertFalse(ProdProfileSelector.isProdEnabled(new String[]{"--unrelated"}));
        assertNull(ProdProfileSelector.classpathFlag());
    }

    @Test
    void argumentAndPlaceholderEdges() {
        assertNull(ProdProfileSelector.argumentValue(null));
        assertNull(ProdProfileSelector.argumentValue(new String[]{null, "--app.prod-flag"}));
        assertNull(ProdProfileSelector.argumentValue(new String[]{"--app.prod-flag"}, "app.preprod-flag"));

        assertEquals("plain", ProdProfileSelector.expandPlaceholder("plain"));
        assertEquals("false", ProdProfileSelector.expandPlaceholder("${THIS_ENV_SHOULD_NOT_EXIST_ZZZ:false}"));
        assertEquals("false", ProdProfileSelector.expandPlaceholder("${THIS_ENV_SHOULD_NOT_EXIST_ZZZ}"));
        String path = ProdProfileSelector.expandPlaceholder("${PATH:fallback}");
        assertFalse(path.isBlank());
        assertNotEquals("fallback", path);

        assertTrue(ProdProfileSelector.truthy(" YES "));
        assertTrue(ProdProfileSelector.truthy("1"));
        assertFalse(ProdProfileSelector.truthy("no"));
        assertFalse(ProdProfileSelector.truthy(null));
        assertFalse(ProdProfileSelector.truthy("  "));
    }
}
