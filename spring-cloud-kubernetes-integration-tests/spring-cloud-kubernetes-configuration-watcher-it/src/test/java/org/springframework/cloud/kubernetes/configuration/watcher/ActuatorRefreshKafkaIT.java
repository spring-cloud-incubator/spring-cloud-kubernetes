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

package org.springframework.cloud.kubernetes.configuration.watcher;

import java.time.Duration;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1beta1Api;
import io.kubernetes.client.openapi.models.NetworkingV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapBuilder;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.kubernetes.integration.tests.commons.K8SUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.kubernetes.integration.tests.commons.K8SUtils.createApiClient;

/**
 * @author Kris Iyer
 */
@RunWith(MockitoJUnitRunner.class)
public class ActuatorRefreshKafkaIT {

	private Log log = LogFactory.getLog(getClass());

	private static final String KIND_REPO_HOST_PORT = "localhost:5000";

	private static final String KIND_REPO_URL = "http://" + KIND_REPO_HOST_PORT;

	private static final String CONFIG_WATCHER_IMAGE = "spring-cloud-kubernetes-configuration-watcher";

	private static final String CONFIG_WATCHER_IT_IMAGE = "spring-cloud-kubernetes-configuration-watcher-it";

	private static final String IMAGE_TAG = "2.0.0-SNAPSHOT";

	private static final String LOCAL_REPO = "docker.io/springcloud";

	private static final String CONFIG_WATCHER_LOCAL_IMAGE = LOCAL_REPO + "/" + CONFIG_WATCHER_IMAGE + ":" + IMAGE_TAG;

	private static final String CONFIG_WATCHER_IT_LOCAL_IMAGE = LOCAL_REPO + "/" + CONFIG_WATCHER_IT_IMAGE + ":"
			+ IMAGE_TAG;

	private static final String CONFIG_WATCHER_KIND_IMAGE = KIND_REPO_HOST_PORT + "/" + CONFIG_WATCHER_IMAGE;

	private static final String CONFIG_WATCHER_IT_KIND_IMAGE = KIND_REPO_HOST_PORT + "/" + CONFIG_WATCHER_IT_IMAGE;

	private static final String CONFIG_WATCHER_KIND_IMAGE_WITH_TAG = CONFIG_WATCHER_KIND_IMAGE + ":" + IMAGE_TAG;

	private static final String CONFIG_WATCHER_IT_KIND_IMAGE_WITH_TAG = CONFIG_WATCHER_IT_KIND_IMAGE + ":" + IMAGE_TAG;

	private static final String SPRING_CLOUD_K8S_CONFIG_WATCHER_DEPLOYMENT_NAME = "spring-cloud-kubernetes-configuration-watcher-deployment";

	private static final String SPRING_CLOUD_K8S_CONFIG_WATCHER_IT_DEPLOYMENT_NAME = "spring-cloud-kubernetes-configuration-watcher-it-deployment";

	private static final String SPRING_CLOUD_K8S_CONFIG_WATCHER_APP_NAME = "spring-cloud-kubernetes-configuration-watcher";

	private static final String NAMESPACE = "default";

	private static final String KAFKA_BROKER = "kafka-broker";

	private static final String KAFKA_SERVICE = "kafka";

	private static final String ZOOKEEPER_SERVICE = "zookeeper";

	private static final String ZOOKEEPER_DEPLOYMENT = "zookeeper";

	private ApiClient client;

	private CoreV1Api api;

	private AppsV1Api appsApi;

	private NetworkingV1beta1Api networkingApi;

	private K8SUtils k8SUtils;

	@Before
	public void setup() throws Exception {
		this.client = createApiClient();
		this.api = new CoreV1Api();
		this.appsApi = new AppsV1Api();
		this.networkingApi = new NetworkingV1beta1Api();
		this.k8SUtils = new K8SUtils(api, appsApi);

		deployZookeeper();

		k8SUtils.waitForDeployment(ZOOKEEPER_DEPLOYMENT, NAMESPACE);

		deployKafka();

		k8SUtils.waitForDeployment(KAFKA_BROKER, NAMESPACE);

		deployTestApp();

		k8SUtils.waitForDeployment(SPRING_CLOUD_K8S_CONFIG_WATCHER_IT_DEPLOYMENT_NAME, NAMESPACE);

		deployConfigWatcher();

		// Check to make sure the controller deployment is ready
		k8SUtils.waitForDeployment(SPRING_CLOUD_K8S_CONFIG_WATCHER_DEPLOYMENT_NAME, NAMESPACE);
	}

	@Test
	public void testRefresh() throws Exception {
		// Create new configmap to trigger controller to signal app to refresh
		V1ConfigMap configMap = new V1ConfigMapBuilder().editOrNewMetadata().withName(CONFIG_WATCHER_IT_IMAGE)
				.addToLabels("spring.cloud.kubernetes.config", "true").endMetadata().addToData("foo", "hello world")
				.build();
		api.createNamespacedConfigMap(NAMESPACE, configMap, null, null, null);
		RestTemplate rest = new RestTemplateBuilder().build();
		// Wait a bit before we verify
		await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(90)).until(() -> {
			Boolean value = rest.getForObject("http://localhost:80/it", Boolean.class);
			log.info("Returned " + value + " from http://localhost:80/it");
			return value;
		});

		assertThat(rest.getForObject("http://localhost:80/it", Boolean.class)).isTrue();
	}

	@After
	public void after() throws Exception {
		appsApi.deleteNamespacedDeployment(KAFKA_BROKER, NAMESPACE, null, null, null, null, null, null);
		api.deleteNamespacedService(KAFKA_SERVICE, NAMESPACE, null, null, null, null, null, null);
		appsApi.deleteNamespacedDeployment(ZOOKEEPER_DEPLOYMENT, NAMESPACE, null, null, null, null, null, null);
		api.deleteNamespacedService(ZOOKEEPER_SERVICE, NAMESPACE, null, null, null, null, null, null);

		api.deleteNamespacedService(CONFIG_WATCHER_IT_IMAGE, NAMESPACE, null, null, null, null, null, null);
		api.deleteNamespacedService(SPRING_CLOUD_K8S_CONFIG_WATCHER_APP_NAME, NAMESPACE, null, null, null, null, null,
				null);

		appsApi.deleteNamespacedDeployment(SPRING_CLOUD_K8S_CONFIG_WATCHER_DEPLOYMENT_NAME, NAMESPACE, null, null, null,
				null, null, null);
		appsApi.deleteNamespacedDeployment(SPRING_CLOUD_K8S_CONFIG_WATCHER_IT_DEPLOYMENT_NAME, NAMESPACE, null, null,
				null, null, null, null);

		networkingApi.deleteNamespacedIngress("it-ingress", NAMESPACE, null, null, null, null, null, null);

		api.deleteNamespacedConfigMap(SPRING_CLOUD_K8S_CONFIG_WATCHER_APP_NAME, NAMESPACE, null, null, null, null, null,
				null);
		api.deleteNamespacedConfigMap(CONFIG_WATCHER_IT_IMAGE, NAMESPACE, null, null, null, null, null, null);

	}

	private void deployTestApp() throws Exception {
		appsApi.createNamespacedDeployment(NAMESPACE, getItDeployment(), null, null, null);
		api.createNamespacedService(NAMESPACE, getItAppService(), null, null, null);
		networkingApi.createNamespacedIngress(NAMESPACE, getItIngress(), null, null, null);
	}

	private void deployConfigWatcher() throws Exception {
		api.createNamespacedConfigMap(NAMESPACE, getConfigWatcherConfigMap(), null, null, null);
		appsApi.createNamespacedDeployment(NAMESPACE, getConfigWatcherDeployment(), null, null, null);
		api.createNamespacedService(NAMESPACE, getConfigWatcherService(), null, null, null);
	}

	private void deployZookeeper() throws Exception {
		System.out.println("deploy deployZookeeper");
		api.createNamespacedService(NAMESPACE, getZookeeperService(), null, null, null);
		System.out.println("created  getZookeeperService");
		appsApi.createNamespacedDeployment(NAMESPACE, getZookeeperDeployment(), null, null, null);
		System.out.println("created  getZookeeperDeployment");
	}

	private void deployKafka() throws Exception {
		System.out.println("deploy kafka");
		api.createNamespacedService(NAMESPACE, getKafkaService(), null, null, null);
		System.out.println("created  getKafkaService");
		appsApi.createNamespacedDeployment(NAMESPACE, getKafkaDeployment(), null, null, null);
		System.out.println("created  getKafkaDeployment");
	}

	private V1Service getConfigWatcherService() throws Exception {
		V1Service service = (V1Service) k8SUtils
				.readYamlFromClasspath("spring-cloud-kubernetes-configuration-watcher-service.yaml");
		return service;
	}

	private V1ConfigMap getConfigWatcherConfigMap() throws Exception {
		V1ConfigMap configMap = (V1ConfigMap) k8SUtils
				.readYamlFromClasspath("spring-cloud-kubernetes-configuration-watcher-configmap.yaml");
		return configMap;
	}

	private V1Deployment getConfigWatcherDeployment() throws Exception {
		V1Deployment deployment = (V1Deployment) k8SUtils
				.readYamlFromClasspath("spring-cloud-kubernetes-configuration-watcher-bus-kafka-deployment.yaml");
		return deployment;
	}

	private V1Service getItAppService() throws Exception {
		String urlString = "spring-cloud-kubernetes-configuration-watcher-it-service.yaml";
		V1Service service = (V1Service) k8SUtils.readYamlFromClasspath(urlString);
		return service;
	}

	private V1Deployment getItDeployment() throws Exception {
		String urlString = "spring-cloud-kubernetes-configuration-watcher-it-bus-kafka-deployment.yaml";
		V1Deployment deployment = (V1Deployment) k8SUtils.readYamlFromClasspath(urlString);
		return deployment;
	}

	private NetworkingV1beta1Ingress getItIngress() throws Exception {
		String urlString = "spring-cloud-kubernetes-configuration-watcher-it-ingress.yaml";
		NetworkingV1beta1Ingress ingress = (NetworkingV1beta1Ingress) k8SUtils.readYamlFromClasspath(urlString);
		return ingress;
	}

	private V1Deployment getKafkaDeployment() throws Exception {
		String urlString = "kafka-deployment.yaml";
		V1Deployment deployment = (V1Deployment) k8SUtils.readYamlFromClasspath(urlString);
		return deployment;
	}

	private V1Service getKafkaService() throws Exception {
		String urlString = "kafka-service.yaml";
		V1Service service = (V1Service) k8SUtils.readYamlFromClasspath(urlString);
		return service;
	}

	private V1Deployment getZookeeperDeployment() throws Exception {
		String urlString = "zookeeper-deployment.yaml";
		V1Deployment deployment = (V1Deployment) k8SUtils.readYamlFromClasspath(urlString);
		return deployment;
	}

	private V1Service getZookeeperService() throws Exception {
		String urlString = "zookeeper-service.yaml";
		V1Service service = (V1Service) k8SUtils.readYamlFromClasspath(urlString);
		return service;
	}

}
