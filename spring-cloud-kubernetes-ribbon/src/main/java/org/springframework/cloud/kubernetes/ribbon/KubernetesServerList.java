/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.kubernetes.ribbon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractServerList;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Kubernetes {@link ServerList}. updated by wuzishu
 *
 * @author Ioannis Canellos
 */
public class KubernetesServerList extends AbstractServerList<Server>
		implements ServerList<Server> {

	private static final int FIRST = 0;

	private static final Log LOG = LogFactory.getLog(KubernetesServerList.class);

	private final KubernetesClient client;

	private String serviceId;

	private String namespace;

	private String portName;

	private KubernetesRibbonMode mode = KubernetesRibbonMode.POD;

	public KubernetesServerList(KubernetesClient client) {
		this.client = client;
	}

	public KubernetesServerList(KubernetesClient client, KubernetesRibbonMode mode) {
		this.client = client;
		this.mode = mode;
	}

	public void initWithNiwsConfig(IClientConfig clientConfig) {
		this.serviceId = clientConfig.getClientName();
		this.namespace = clientConfig.getPropertyAsString(KubernetesConfigKey.Namespace,
				this.client.getNamespace());
		this.portName = clientConfig.getPropertyAsString(KubernetesConfigKey.PortName,
				null);
	}

	public List<Server> getInitialListOfServers() {
		return Collections.emptyList();
	}

	public List<Server> getUpdatedListOfServers() {
		List<Server> result = new ArrayList<Server>();
		if (mode == KubernetesRibbonMode.SERVICE) {
			Service service = this.namespace != null
					? this.client.services().inNamespace(this.namespace)
							.withName(this.serviceId).get()
					: this.client.services().withName(this.serviceId).get();
			if (service != null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Found Service[" + service.getMetadata().getName() + "]");
				}
				if (service.getSpec().getPorts().size() == 1) {
					result.add(new Server(
							String.format("%s.%s.svc", service.getMetadata().getName(),
									service.getMetadata().getNamespace()),
							service.getSpec().getPorts().get(0).getPort()));
				}
				else {
					for (ServicePort servicePort : service.getSpec().getPorts()) {
						if (Utils.isNotNullOrEmpty(this.portName)
								|| this.portName.endsWith(servicePort.getName())) {
							result.add(new Server(
									String.format("%s.%s.svc",
											service.getMetadata().getName(),
											service.getMetadata().getNamespace()),
									servicePort.getPort()));
						}
					}

				}
			}
		}
		else {
			Endpoints endpoints = this.namespace != null
					? this.client.endpoints().inNamespace(this.namespace)
							.withName(this.serviceId).get()
					: this.client.endpoints().withName(this.serviceId).get();

			if (endpoints != null) {

				if (LOG.isDebugEnabled()) {
					LOG.debug("Found [" + endpoints.getSubsets().size()
							+ "] endpoints in l [" + this.namespace + "] for name ["
							+ this.serviceId + "] and portName [" + this.portName + "]");
				}
				for (EndpointSubset subset : endpoints.getSubsets()) {

					if (subset.getPorts().size() == 1) {
						EndpointPort port = subset.getPorts().get(FIRST);
						for (EndpointAddress address : subset.getAddresses()) {
							result.add(new Server(address.getIp(), port.getPort()));
						}
					}
					else {
						for (EndpointPort port : subset.getPorts()) {
							if (Utils.isNullOrEmpty(this.portName)
									|| this.portName.endsWith(port.getName())) {
								for (EndpointAddress address : subset.getAddresses()) {
									result.add(
											new Server(address.getIp(), port.getPort()));
								}
							}
						}
					}
				}
			}
			else {
				LOG.warn("Did not find any endpoints in ribbon in namespace ["
						+ this.namespace + "] for name [" + this.serviceId
						+ "] and portName [" + this.portName + "]");
			}

		}
		return result;
	}

}
