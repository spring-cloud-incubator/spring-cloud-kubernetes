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

package org.springframework.cloud.kubernetes;

import io.fabric8.kubernetes.api.model.Pod;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

/**
 * Kubernetes implementation of {@link AbstractHealthIndicator}.
 *
 * @author Ioannis Canellos
 * @author Eddú Meléndez
 */
public class KubernetesHealthIndicator extends AbstractHealthIndicator {

	private PodUtils utils;

	public KubernetesHealthIndicator(PodUtils utils) {
		this.utils = utils;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		try {
			Pod current = this.utils.currentPod().get();
			if (current != null) {
				builder.up().withDetail("inside", true).withDetail("namespace", current.getMetadata().getNamespace())
						.withDetail("podName", current.getMetadata().getName())
						.withDetail("podIp", current.getStatus().getPodIP())
						.withDetail("serviceAccount", current.getSpec().getServiceAccountName())
						.withDetail("nodeName", current.getSpec().getNodeName())
						.withDetail("hostIp", current.getStatus().getHostIP())
						.withDetail("labels", current.getMetadata().getLabels());
			}
			else {
				builder.up().withDetail("inside", false);
			}
		}
		catch (Exception e) {
			builder.down(e);
		}
	}

}
