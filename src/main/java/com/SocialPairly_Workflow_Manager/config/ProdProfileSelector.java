package com.SocialPairly_Workflow_Manager.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads the environment flags before Spring starts. Spring always loads
 * {@code application.properties} as the base. {@code app.prod-flag=true} then
 * applies {@code application-prod.properties}. When that flag is off,
 * {@code app.preprod-flag=true} applies {@code application-preprod.properties}.
 * Prod wins if both flags are true, so an existing production switch is unchanged.
 *
 * <p>Precedence for each flag, highest first: {@code --app.prod-flag} /
 * {@code --app.preprod-flag}, the matching {@code -D} system property,
 * {@code APP_PROD_FLAG}/{@code PROD_FLAG} or {@code APP_PREPROD_FLAG}/{@code PREPROD_FLAG},
 * then the value in {@code application.properties}.
 */
public final class ProdProfileSelector {

    public static final String FLAG_KEY = "app.prod-flag";
    public static final String PREPROD_FLAG_KEY = "app.preprod-flag";
    public static final String PROD_PROFILE = "prod";
    public static final String PREPROD_PROFILE = "preprod";

    private ProdProfileSelector() {
    }

    /**
     * @return {@code prod}, {@code preprod}, or {@code null} for the dev file
     */
    public static String selectProfile(String[] args) {
        return selectProfile(isProdEnabled(args), isPreprodEnabled(args));
    }

    static String selectProfile(boolean prodEnabled, boolean preprodEnabled) {
        if (prodEnabled) {
            return PROD_PROFILE;
        }
        if (preprodEnabled) {
            return PREPROD_PROFILE;
        }
        return null;
    }

    public static boolean isProdEnabled(String[] args) {
        return resolve(
                argumentValue(args),
                System.getProperty(FLAG_KEY),
                firstNonBlank(System.getenv("APP_PROD_FLAG"), System.getenv("PROD_FLAG")),
                classpathFlag());
    }

    public static boolean isPreprodEnabled(String[] args) {
        return resolve(
                argumentValue(args, PREPROD_FLAG_KEY),
                System.getProperty(PREPROD_FLAG_KEY),
                firstNonBlank(System.getenv("APP_PREPROD_FLAG"), System.getenv("PREPROD_FLAG")),
                classpathFlag(PREPROD_FLAG_KEY));
    }

    static boolean resolve(String argument, String systemProperty, String environment, String fileValue) {
        if (hasText(argument)) {
            return truthy(argument);
        }
        if (hasText(systemProperty)) {
            return truthy(systemProperty);
        }
        if (hasText(environment)) {
            return truthy(environment);
        }
        if (hasText(fileValue)) {
            return truthy(expandPlaceholder(fileValue.trim()));
        }
        return false;
    }

    static String argumentValue(String[] args) {
        return argumentValue(args, FLAG_KEY);
    }

    static String argumentValue(String[] args, String flagKey) {
        if (args == null) {
            return null;
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null) {
                continue;
            }
            String prefix = "--" + flagKey + "=";
            if (arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
            if (arg.equals("--" + flagKey) && i + 1 < args.length) {
                return args[i + 1];
            }
        }
        return null;
    }

    static String classpathFlag() {
        return classpathFlag(FLAG_KEY);
    }

    static String classpathFlag(String flagKey) {
        try (InputStream in = ProdProfileSelector.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty(flagKey);
        } catch (IOException ex) {
            return null;
        }
    }

    static String expandPlaceholder(String raw) {
        if (!raw.startsWith("${") || !raw.endsWith("}")) {
            return raw;
        }
        String body = raw.substring(2, raw.length() - 1);
        int colon = body.indexOf(':');
        String name = colon >= 0 ? body.substring(0, colon) : body;
        String fallback = colon >= 0 ? body.substring(colon + 1) : "false";
        String fromEnv = System.getenv(name);
        return hasText(fromEnv) ? fromEnv.trim() : fallback;
    }

    static boolean truthy(String raw) {
        String value = raw == null ? "" : raw.trim();
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    private static String firstNonBlank(String first, String second) {
        return hasText(first) ? first : second;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
