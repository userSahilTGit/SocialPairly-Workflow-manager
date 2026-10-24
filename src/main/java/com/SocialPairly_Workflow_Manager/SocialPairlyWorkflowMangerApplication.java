package com.SocialPairly_Workflow_Manager;

import com.SocialPairly_Workflow_Manager.config.ProdProfileSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@SpringBootApplication
public class SocialPairlyWorkflowMangerApplication {

	private static final Logger log = LoggerFactory.getLogger(SocialPairlyWorkflowMangerApplication.class);

	/**
	 * Marker so a later startup in the same JVM (devtools restart) can undo a profile
	 * this class applied. An explicit {@code -Dspring.profiles.active} from the launch
	 * command is left alone when this marker is absent.
	 */
	private static final String APPLIED_PROFILE = "socialpairly.env-profile.applied";
	private static final String LEGACY_PROD_MARKER = "socialpairly.prod-flag.applied";

	public static void main(String[] args) {
		applyLaunchProfile(args);
		SpringApplication application = new SpringApplication(SocialPairlyWorkflowMangerApplication.class);
		application.addInitializers(context -> logActiveConfig(context.getEnvironment()));
		application.run(args);
	}

	/**
	 * Applies {@code prod} or {@code preprod} before Spring reads configuration, and
	 * clears a profile this process applied earlier when the flags are now off.
	 */
	static void applyLaunchProfile(String[] args) {
		String profile = ProdProfileSelector.selectProfile(args);
		if (profile != null) {
			// System properties outrank application.properties, so this replaces the dev
			// default profile. Restart after flipping a flag; nothing else reads it at runtime.
			System.setProperty("spring.profiles.active", profile);
			System.setProperty(APPLIED_PROFILE, profile);
			System.clearProperty(LEGACY_PROD_MARKER);
		} else if (System.getProperty(APPLIED_PROFILE) != null
				|| System.getProperty(LEGACY_PROD_MARKER) != null) {
			System.clearProperty("spring.profiles.active");
			System.clearProperty(APPLIED_PROFILE);
			System.clearProperty(LEGACY_PROD_MARKER);
		}
	}

	static void logActiveConfig(Environment environment) {
		String profiles = String.join(",", environment.getActiveProfiles());
		boolean prodFile = Arrays.asList(environment.getActiveProfiles()).contains(ProdProfileSelector.PROD_PROFILE);
		boolean preprodFile = Arrays.asList(environment.getActiveProfiles()).contains(ProdProfileSelector.PREPROD_PROFILE);
		if (prodFile) {
			log.info("Active profiles [{}]: application-prod.properties overrides application.properties", profiles);
		} else if (preprodFile) {
			log.info("Active profiles [{}]: application-preprod.properties overrides application.properties", profiles);
		} else {
			log.info("Active profiles [{}]: using application.properties (dev)", profiles);
		}
	}

}
