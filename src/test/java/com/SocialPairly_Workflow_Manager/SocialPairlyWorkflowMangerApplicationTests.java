package com.SocialPairly_Workflow_Manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SocialPairlyWorkflowMangerApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void shouldStartApplicationMainWithoutWebServer() {
		String previousProfile = System.getProperty("spring.profiles.active");
		String previousApplied = System.getProperty("socialpairly.env-profile.applied");
		String previousLegacy = System.getProperty("socialpairly.prod-flag.applied");
		try {
			SocialPairlyWorkflowMangerApplication.main(new String[]{
					"--spring.main.web-application-type=none",
					"--spring.main.banner-mode=off",
					"--app.prod-flag=false",
					"--app.preprod-flag=false"
			});
		} finally {
			restore("spring.profiles.active", previousProfile);
			restore("socialpairly.env-profile.applied", previousApplied);
			restore("socialpairly.prod-flag.applied", previousLegacy);
		}
	}

	private static void restore(String key, String value) {
		if (value == null) {
			System.clearProperty(key);
		} else {
			System.setProperty(key, value);
		}
	}

}
