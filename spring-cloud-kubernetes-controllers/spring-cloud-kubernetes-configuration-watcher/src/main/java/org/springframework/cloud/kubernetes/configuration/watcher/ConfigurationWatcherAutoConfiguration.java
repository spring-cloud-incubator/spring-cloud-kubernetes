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

import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.boot.actuate.autoconfigure.amqp.RabbitHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.cloud.kubernetes.commons.config.reload.ConfigReloadProperties;
import org.springframework.cloud.kubernetes.commons.config.reload.ConfigurationUpdateStrategy;
import org.springframework.cloud.kubernetes.fabric8.config.Fabric8ConfigMapPropertySourceLocator;
import org.springframework.cloud.kubernetes.fabric8.config.Fabric8SecretsPropertySourceLocator;
import org.springframework.cloud.kubernetes.fabric8.discovery.reactive.KubernetesReactiveDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Ryan Baxter
 * @author Kris Iyer
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ ConfigurationWatcherConfigurationProperties.class })
public class ConfigurationWatcherAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public WebClient webClient(WebClient.Builder webClientBuilder) {
		return webClientBuilder.build();
	}

	@Bean
	@ConditionalOnMissingBean(ConfigMapWatcherChangeDetector.class)
	@ConditionalOnBean(Fabric8ConfigMapPropertySourceLocator.class)
	public ConfigMapWatcherChangeDetector httpBasedConfigMapWatchChangeDetector(AbstractEnvironment environment,
			KubernetesClient kubernetesClient,
			Fabric8ConfigMapPropertySourceLocator fabric8ConfigMapPropertySourceLocator,
			Fabric8SecretsPropertySourceLocator fabric8SecretsPropertySourceLocator, ConfigReloadProperties properties,
			ConfigurationUpdateStrategy strategy,
			ConfigurationWatcherConfigurationProperties k8SConfigurationProperties,
			ThreadPoolTaskExecutor threadFactory, WebClient webClient,
			KubernetesReactiveDiscoveryClient kubernetesReactiveDiscoveryClient) {
		return new HttpBasedConfigMapWatchChangeDetector(environment, properties, kubernetesClient, strategy,
				fabric8ConfigMapPropertySourceLocator, k8SConfigurationProperties, threadFactory, webClient,
				kubernetesReactiveDiscoveryClient);
	}

	@Bean
	@ConditionalOnMissingBean(SecretsWatcherChangeDetector.class)
	@ConditionalOnBean(Fabric8SecretsPropertySourceLocator.class)
	public SecretsWatcherChangeDetector httpBasedSecretsWatchChangeDetector(AbstractEnvironment environment,
			KubernetesClient kubernetesClient, Fabric8SecretsPropertySourceLocator fabric8SecretsPropertySourceLocator,
			ConfigReloadProperties properties, ConfigurationUpdateStrategy strategy,
			ConfigurationWatcherConfigurationProperties k8SConfigurationProperties,
			ThreadPoolTaskExecutor threadFactory, WebClient webClient,
			KubernetesReactiveDiscoveryClient kubernetesReactiveDiscoveryClient) {
		return new HttpBasedSecretsWatchChangeDetector(environment, properties, kubernetesClient, strategy,
				fabric8SecretsPropertySourceLocator, k8SConfigurationProperties, threadFactory, webClient,
				kubernetesReactiveDiscoveryClient);
	}

	@Configuration
	@Profile("bus-amqp")
	@Import({ ContextFunctionCatalogAutoConfiguration.class, RabbitHealthContributorAutoConfiguration.class })
	static class BusRabbitConfiguration {

		@Bean
		@ConditionalOnMissingBean(ConfigMapWatcherChangeDetector.class)
		@ConditionalOnBean(Fabric8ConfigMapPropertySourceLocator.class)
		public ConfigMapWatcherChangeDetector busConfigMapChangeWatcher(BusProperties busProperties,
				AbstractEnvironment environment, KubernetesClient kubernetesClient,
				Fabric8ConfigMapPropertySourceLocator fabric8ConfigMapPropertySourceLocator,
				ConfigReloadProperties properties, ConfigurationUpdateStrategy strategy,
				ConfigurationWatcherConfigurationProperties k8SConfigurationProperties,
				ThreadPoolTaskExecutor threadFactory) {
			return new BusEventBasedConfigMapWatcherChangeDetector(environment, properties, kubernetesClient, strategy,
					fabric8ConfigMapPropertySourceLocator, busProperties, k8SConfigurationProperties, threadFactory);
		}

		@Bean
		@ConditionalOnMissingBean(SecretsWatcherChangeDetector.class)
		@ConditionalOnBean(Fabric8SecretsPropertySourceLocator.class)
		public SecretsWatcherChangeDetector busSecretsChangeWatcher(BusProperties busProperties,
				AbstractEnvironment environment, KubernetesClient kubernetesClient,
				Fabric8SecretsPropertySourceLocator secretsPropertySourceLocator, ConfigReloadProperties properties,
				ConfigurationUpdateStrategy strategy,
				ConfigurationWatcherConfigurationProperties k8SConfigurationProperties,
				ThreadPoolTaskExecutor threadFactory) {
			return new BusEventBasedSecretsWatcherChangeDetector(environment, properties, kubernetesClient, strategy,
					secretsPropertySourceLocator, busProperties, k8SConfigurationProperties, threadFactory);
		}

	}

	@Configuration
	@Profile("bus-kafka")
	@Import({ ContextFunctionCatalogAutoConfiguration.class })
	static class BusKafkaConfiguration {

		@Bean
		@ConditionalOnMissingBean(ConfigMapWatcherChangeDetector.class)
		@ConditionalOnBean(Fabric8ConfigMapPropertySourceLocator.class)
		public ConfigMapWatcherChangeDetector busConfigMapChangeWatcher(BusProperties busProperties,
				AbstractEnvironment environment, KubernetesClient kubernetesClient,
				Fabric8ConfigMapPropertySourceLocator configMapPropertySourceLocator, ConfigReloadProperties properties,
				ConfigurationUpdateStrategy strategy,
				ConfigurationWatcherConfigurationProperties k8SConfigurationProperties,
				ThreadPoolTaskExecutor threadFactory) {
			return new BusEventBasedConfigMapWatcherChangeDetector(environment, properties, kubernetesClient, strategy,
					configMapPropertySourceLocator, busProperties, k8SConfigurationProperties, threadFactory);
		}

		@Bean
		@ConditionalOnMissingBean(SecretsWatcherChangeDetector.class)
		@ConditionalOnBean(Fabric8SecretsPropertySourceLocator.class)
		public SecretsWatcherChangeDetector busSecretsChangeWatcher(BusProperties busProperties,
				AbstractEnvironment environment, KubernetesClient kubernetesClient,
				Fabric8SecretsPropertySourceLocator fabric8SecretsPropertySourceLocator,
				ConfigReloadProperties properties, ConfigurationUpdateStrategy strategy,
				ConfigurationWatcherConfigurationProperties k8SConfigurationProperties,
				ThreadPoolTaskExecutor threadFactory) {
			return new BusEventBasedSecretsWatcherChangeDetector(environment, properties, kubernetesClient, strategy,
					fabric8SecretsPropertySourceLocator, busProperties, k8SConfigurationProperties, threadFactory);
		}

	}

}
