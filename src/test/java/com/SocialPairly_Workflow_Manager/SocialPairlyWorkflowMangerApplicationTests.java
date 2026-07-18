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
		SocialPairlyWorkflowMangerApplication.main(new String[]{"--spring.main.web-application-type=none"});
	}

}
