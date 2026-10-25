package com.SocialPairly_Workflow_Manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class SocialPairlyWorkflowMangerApplicationTests {

	@Autowired
	private Environment environment;

	@Test
	void contextLoads() {
	}

	@Test
	void serverPortFollowsPortsProperties() throws Exception {
		String serverPort = Files.readAllLines(Path.of("src/main/resources/ports.properties")).stream()
				.filter(line -> line.startsWith("app.server.port="))
				.map(line -> line.substring("app.server.port=".length()).trim())
				.findFirst()
				.orElseThrow();
		String frontendPort = Files.readAllLines(Path.of("src/main/resources/ports.properties")).stream()
				.filter(line -> line.startsWith("app.frontend.port="))
				.map(line -> line.substring("app.frontend.port=".length()).trim())
				.findFirst()
				.orElseThrow();
		assertEquals(serverPort, environment.getProperty("app.server.port"));
		assertEquals(frontendPort, environment.getProperty("app.frontend.port"));
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
