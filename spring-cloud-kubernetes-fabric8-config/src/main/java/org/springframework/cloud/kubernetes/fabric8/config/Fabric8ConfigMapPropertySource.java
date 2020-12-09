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

package org.springframework.cloud.kubernetes.fabric8.config;

import java.util.LinkedHashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.kubernetes.commons.config.ConfigMapPropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * A {@link MapPropertySource} that uses Kubernetes config maps.
 *
 * @author Ioannis Canellos
 * @author Ali Shahbour
 * @author Michael Moudatsos
 */
public class Fabric8ConfigMapPropertySource extends ConfigMapPropertySource {

	private static final Log LOG = LogFactory.getLog(Fabric8ConfigMapPropertySource.class);

	public Fabric8ConfigMapPropertySource(KubernetesClient client, String name) {
		this(client, name, null, (Environment) null);
	}

	public Fabric8ConfigMapPropertySource(KubernetesClient client, String name, String namespace,
			Environment environment) {
		super(getName(name, getNamespace(client, namespace)),
				asObjectMap(getData(client, name, getNamespace(client, namespace), environment)));
	}

	private static String getNamespace(KubernetesClient client, String namespace) {
		return StringUtils.isEmpty(namespace) ? client.getNamespace() : namespace;
	}

	private static Map<String, Object> getData(KubernetesClient client, String name, String namespace,
			Environment environment) {
		try {
			Map<String, Object> result = new LinkedHashMap<>();
			ConfigMap map = StringUtils.isEmpty(namespace) ? client.configMaps().withName(name).get()
					: client.configMaps().inNamespace(namespace).withName(name).get();

			if (map != null) {
				result.putAll(processAllEntries(map.getData(), environment));
			}

			if (environment != null) {
				for (String activeProfile : environment.getActiveProfiles()) {

					String mapNameWithProfile = name + "-" + activeProfile;

					ConfigMap mapWithProfile = StringUtils.isEmpty(namespace)
							? client.configMaps().withName(mapNameWithProfile).get()
							: client.configMaps().inNamespace(namespace).withName(mapNameWithProfile).get();

					if (mapWithProfile != null) {
						result.putAll(processAllEntries(mapWithProfile.getData(), environment));
					}

				}
			}

			return result;

		}
		catch (Exception e) {
			LOG.warn("Can't read configMap with name: [" + name + "] in namespace:[" + namespace + "]. Ignoring.", e);
		}

		return new LinkedHashMap<>();
	}

}
