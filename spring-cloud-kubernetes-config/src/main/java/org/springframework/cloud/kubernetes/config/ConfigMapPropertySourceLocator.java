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

package org.springframework.cloud.kubernetes.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import static org.springframework.cloud.kubernetes.config.ConfigUtils.*;

@Order(0)
public class ConfigMapPropertySourceLocator implements PropertySourceLocator {
    private final KubernetesClient client;
    private final ConfigMapConfigProperties properties;

    public ConfigMapPropertySourceLocator(KubernetesClient client, ConfigMapConfigProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public MapPropertySource locate(Environment environment) {
        if (environment instanceof ConfigurableEnvironment) {
            ConfigurableEnvironment env = (ConfigurableEnvironment) environment;
            String name = getApplicationName(environment, properties);
            String namespace = getApplicationNamespace(client, env, properties);
            return new ConfigMapPropertySource(client, name, namespace, env.getActiveProfiles());
        }
        return null;
    }
}
