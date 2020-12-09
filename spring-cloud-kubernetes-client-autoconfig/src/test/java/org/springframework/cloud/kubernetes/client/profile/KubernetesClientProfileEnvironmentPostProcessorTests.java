/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.kubernetes.client.profile;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;

import static io.kubernetes.client.util.Config.ENV_SERVICE_HOST;
import static io.kubernetes.client.util.Config.ENV_SERVICE_PORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.kubernetes.commons.profile.AbstractKubernetesProfileEnvironmentPostProcessor.KUBERNETES_PROFILE;

/**
 * @author Thomas Vitale
 */
@SpringBootTest(properties = { ENV_SERVICE_HOST + "=10.0.0.1", ENV_SERVICE_PORT + "=80" },
		classes = { KubernetesClientProfileEnvironmentPostProcessorTests.App.class })
class KubernetesClientProfileEnvironmentPostProcessorTests {

	@Autowired
	Environment environment;

	@MockBean
	CoreV1Api coreV1Api;

	@MockBean
	ApiClient apiClient;

	@Test
	void whenKubernetesEnvironmentAndNoApiAccessThenProfileEnabled() {
		assertThat(environment.getActiveProfiles()).contains(KUBERNETES_PROFILE);
	}

	@SpringBootApplication
	static class App {

	}

}
