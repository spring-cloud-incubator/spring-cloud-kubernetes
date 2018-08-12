/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.springframework.cloud.kubernetes.discovery;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Oleg Vyukov
 */
@RunWith(MockitoJUnitRunner.class)
public class KubernetesCatalogWatchTest {

	@Mock
	private KubernetesClient kubernetesClient;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	@Mock
	private MixedOperation<Endpoints, EndpointsList, DoneableEndpoints, Resource<Endpoints, DoneableEndpoints>> endpointsOperation;

	@Captor
	private ArgumentCaptor<HeartbeatEvent> heartbeatEventArgumentCaptor;

	@InjectMocks
	private KubernetesCatalogWatch underTest;

	@Before
	public void setUp() throws Exception {
		underTest.setApplicationEventPublisher(applicationEventPublisher);
	}

	@Test
	public void testRandomOrderChangePods() throws Exception {
		when(endpointsOperation.list())
			.thenReturn(createSingleEndpointEndpointListByPodName("api-pod", "other-pod"))
			.thenReturn(createSingleEndpointEndpointListByPodName("other-pod", "api-pod"));
		when(kubernetesClient.endpoints()).thenReturn(endpointsOperation);

		underTest.catalogServicesWatch();
		// second execution on shuffleServices
		underTest.catalogServicesWatch();

		verify(applicationEventPublisher).publishEvent(any(HeartbeatEvent.class));
	}

	@Test
	public void testRandomOrderChangeServices() throws Exception {
		when(endpointsOperation.list())
			.thenReturn(createEndpointsListByServiceName("api-service", "other-service"))
			.thenReturn(createEndpointsListByServiceName("other-service", "api-service"));
		when(kubernetesClient.endpoints()).thenReturn(endpointsOperation);

		underTest.catalogServicesWatch();
		// second execution on shuffleServices
		underTest.catalogServicesWatch();

		verify(applicationEventPublisher).publishEvent(any(HeartbeatEvent.class));
	}

	@Test
	public void testEventBody() throws Exception {
		when(endpointsOperation.list())
			.thenReturn(createSingleEndpointEndpointListByPodName("api-pod", "other-pod"));
		when(kubernetesClient.endpoints()).thenReturn(endpointsOperation);

		underTest.catalogServicesWatch();

		verify(applicationEventPublisher).publishEvent(heartbeatEventArgumentCaptor.capture());

		HeartbeatEvent event = heartbeatEventArgumentCaptor.getValue();
		assertThat(event.getValue(), instanceOf(List.class));

		List<String> expectedPodsList = Arrays.asList("api-pod", "other-pod");
		assertEquals(expectedPodsList, event.getValue());
	}

	@Test
	public void testEndpointsWithoutAddresses() {

		EndpointsList endpoints = createSingleEndpointEndpointListByPodName("api-pod");
		endpoints.getItems().get(0).getSubsets().get(0).setAddresses(null);

		when(endpointsOperation.list()).thenReturn(endpoints);
		when(kubernetesClient.endpoints()).thenReturn(endpointsOperation);

		underTest.catalogServicesWatch();
		// second execution on shuffleServices
		underTest.catalogServicesWatch();

		verify(applicationEventPublisher).publishEvent(any(HeartbeatEvent.class));
	}

	@Test
	public void testEndpointsWithoutTargetRefs() {

		EndpointsList endpoints = createSingleEndpointEndpointListByPodName("api-pod");
		endpoints.getItems().get(0).getSubsets().get(0).getAddresses().get(0).setTargetRef(null);

		when(endpointsOperation.list()).thenReturn(endpoints);
		when(kubernetesClient.endpoints()).thenReturn(endpointsOperation);

		underTest.catalogServicesWatch();
		// second execution on shuffleServices
		underTest.catalogServicesWatch();

		verify(applicationEventPublisher).publishEvent(any(HeartbeatEvent.class));
	}


	private EndpointsList createEndpointsListByServiceName(String... serviceNames) {
		List<Endpoints> endpoints = stream(serviceNames)
			.map(s -> createEndpointsByPodName(s + "-singlePodUniqueId"))
			.collect(Collectors.toList());

		EndpointsList endpointsList = new EndpointsList();
		endpointsList.setItems(endpoints);
		return endpointsList;
	}

	private EndpointsList createSingleEndpointEndpointListByPodName(String... podNames) {
		Endpoints endpoints = new Endpoints();
		endpoints.setSubsets(createSubsetsByPodName(podNames));

		EndpointsList endpointsList = new EndpointsList();
		endpointsList.setItems(Collections.singletonList(endpoints));
		return endpointsList;
	}


	private Endpoints createEndpointsByPodName(String podName) {
		Endpoints endpoints = new Endpoints();
		endpoints.setSubsets(createSubsetsByPodName(podName));
		return endpoints;
	}


	private List<EndpointSubset> createSubsetsByPodName(String... names) {
		EndpointSubset endpointSubset = new EndpointSubset();
		endpointSubset.setAddresses(createEndpointAddressByPodNames(names));
		return Collections.singletonList(endpointSubset);
	}

	private List<EndpointAddress> createEndpointAddressByPodNames(String[] names) {
		return stream(names).map(name -> {
			ObjectReference podRef = new ObjectReference();
			podRef.setName(name);
			EndpointAddress endpointAddress = new EndpointAddress();
			endpointAddress.setTargetRef(podRef);
			return endpointAddress;
		}).collect(Collectors.toList());
	}


}
