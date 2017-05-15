/*
 * Copyright (C) 2016 to the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.springframework.cloud.kubernetes.ribbon;

import java.io.IOException;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.server.mock.KubernetesServer;
import io.fabric8.mockwebserver.DefaultMockServer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.fail;

;

/**
 * @author <a href="mailto:cmoullia@redhat.com">Charles Moulliard</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class,
                properties = {
	"spring.application.name=testapp",
	"spring.cloud.kubernetes.client.namespace=testns",
	"spring.cloud.kubernetes.client.trustCerts=true",
	"spring.cloud.kubernetes.config.namespace=testns"})
@EnableAutoConfiguration
@EnableDiscoveryClient
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RibbonFallbackTest {

	@ClassRule
	public static KubernetesServer mockServer = new KubernetesServer();

	public static DefaultMockServer mockEndpoint;

	public static KubernetesClient mockClient;

	@Autowired
	RestTemplate restTemplate;

	@BeforeClass
	public static void setUpBefore() throws Exception {
		mockClient = mockServer.getClient();

		//Configure the kubernetes master url to point to the mock server
		System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, mockClient.getConfiguration().getMasterUrl());
		System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
		System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
		System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, "false");

		mockEndpoint = new DefaultMockServer(false);
		mockEndpoint.start();
	}

	@Test
	public void test1DiscoverCallGreetingEndpoint() throws IOException {
		// Register an endpoint
		mockServer.expect().get()
			      .withPath("/api/v1/namespaces/testns/endpoints/testapp")
			      .andReturn(200, newEndpoint("testapp-a","testns", mockEndpoint)).once();

		mockEndpoint.expect().get().withPath("/greeting").andReturn(200, "Hello from A").once();
		String response = restTemplate.getForObject("http://testapp/greeting", String.class);
		Assert.assertEquals("Hello from A",response);
	}

	@Test
	public void test2DontDiscoverEndpointGreeting() {
		try {
			Thread.sleep(1000);
			restTemplate.getForObject("http://testapp/greeting", String.class);
			fail("My method didn't throw when I expected it to");
		}
		catch (Exception e) {
			// No endpoint is available anymore and Ribbon list is empty
			Assert.assertEquals("No instances available for testapp",e.getMessage());
		}
	}

	@Test
	public void test3RediscoverGreetingEndpoint() throws Exception {
		mockServer.expect().get()
			.withPath("/api/v1/namespaces/testns/endpoints/testapp")
			.andReturn(200, newEndpoint("testapp-a","testns", mockEndpoint)).always();
		// Sleep thread to let Ribbon to discover the endpoint after RefreshTime is passed (= 500ms)
		Thread.sleep(1000);
		mockEndpoint.expect().get().withPath("/greeting").andReturn(200, "Hello from A").once();
		String response = restTemplate.getForObject("http://testapp/greeting", String.class);
		Assert.assertEquals("Hello from A",response);
	}

	public static Endpoints newEndpoint(String name, String namespace, DefaultMockServer mockServer) {
		return new EndpointsBuilder()
			        .withNewMetadata()
			          .withName(name)
			          .withNamespace(namespace)
			          .endMetadata()
			        .addNewSubset()
			          .addNewAddress().withIp(mockServer.getHostName()).endAddress()
			          .addNewPort("http",mockServer.getPort(),"http")
			        .endSubset()
			        .build();
	}

}
