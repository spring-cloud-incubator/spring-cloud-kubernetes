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

package org.springframework.cloud.kubernetes.commons.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.springframework.cloud.kubernetes.commons.config.PropertySourceUtils.KEY_VALUE_TO_PROPERTIES;
import static org.springframework.cloud.kubernetes.commons.config.PropertySourceUtils.PROPERTIES_TO_MAP;
import static org.springframework.cloud.kubernetes.commons.config.PropertySourceUtils.throwingMerger;
import static org.springframework.cloud.kubernetes.commons.config.PropertySourceUtils.yamlParserGenerator;

/**
 * A {@link MapPropertySource} that uses Kubernetes config maps.
 *
 * @author Ioannis Canellos
 * @author Ali Shahbour
 * @author Michael Moudatsos
 */
public abstract class ConfigMapPropertySource extends MapPropertySource {

	private static final Log LOG = LogFactory.getLog(ConfigMapPropertySource.class);

	protected static final String APPLICATION_YML = "application.yml";

	protected static final String APPLICATION_YAML = "application.yaml";

	protected static final String APPLICATION_PROPERTIES = "application.properties";

	protected static final String PREFIX = "configmap";

	public ConfigMapPropertySource(String name, Map<String, Object> source) {
		super(name, source);
	}

	protected static Environment createEnvironmentWithActiveProfiles(String[] activeProfiles) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles(activeProfiles);
		return environment;
	}

	protected static String getName(String name, String namespace) {
		return new StringBuilder().append(PREFIX).append(Constants.PROPERTY_SOURCE_NAME_SEPARATOR).append(name)
				.append(Constants.PROPERTY_SOURCE_NAME_SEPARATOR).append(namespace).toString();
	}

	protected static Map<String, Object> processAllEntries(Map<String, String> input, Environment environment) {

		Set<Entry<String, String>> entrySet = input.entrySet();
		if (entrySet.size() == 1) {
			// we handle the case where the configmap contains a single "file"
			// in this case we don't care what the name of t he file is
			Entry<String, String> singleEntry = entrySet.iterator().next();
			String propertyName = singleEntry.getKey();
			String propertyValue = singleEntry.getValue();
			if (propertyName.endsWith(".yml") || propertyName.endsWith(".yaml")) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("The single property with name: [" + propertyName + "] will be treated as a yaml file");
				}

				return yamlParserGenerator(environment).andThen(PROPERTIES_TO_MAP).apply(propertyValue);
			}
			else if (propertyName.endsWith(".properties")) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("The single property with name: [" + propertyName
							+ "] will be treated as a properties file");
				}

				return KEY_VALUE_TO_PROPERTIES.andThen(PROPERTIES_TO_MAP).apply(propertyValue);
			}
			else {
				return defaultProcessAllEntries(input, environment);
			}
		}

		return defaultProcessAllEntries(input, environment);
	}

	protected static Map<String, Object> defaultProcessAllEntries(Map<String, String> input, Environment environment) {

		return input.entrySet().stream().map(e -> extractProperties(e.getKey(), e.getValue(), environment))
				.filter(m -> !m.isEmpty()).flatMap(m -> m.entrySet().stream())
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, throwingMerger(), LinkedHashMap::new));
	}

	protected static Map<String, Object> extractProperties(String resourceName, String content,
			Environment environment) {

		if (resourceName.equals(APPLICATION_YAML) || resourceName.equals(APPLICATION_YML)) {
			return yamlParserGenerator(environment).andThen(PROPERTIES_TO_MAP).apply(content);
		}
		else if (resourceName.equals(APPLICATION_PROPERTIES)) {
			return KEY_VALUE_TO_PROPERTIES.andThen(PROPERTIES_TO_MAP).apply(content);
		}

		return new LinkedHashMap<String, Object>() {
			{
				put(resourceName, content);
			}
		};
	}

	protected static Map<String, Object> asObjectMap(Map<String, Object> source) {
		return source.entrySet().stream()
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, throwingMerger(), LinkedHashMap::new));
	}

}
