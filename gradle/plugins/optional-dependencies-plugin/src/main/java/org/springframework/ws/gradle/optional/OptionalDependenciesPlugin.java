/*
 * Copyright 2005-2025 the original author or authors.
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

package org.springframework.ws.gradle.optional;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;

/**
 * Plugin that adds support for "optional" dependencies. An optional dependency is
 * available for compilation and at runtime when building and testing the project but is
 * not exposed to consumers.
 *
 * @author Andy Wilkinson
 */
public class OptionalDependenciesPlugin implements Plugin<Project> {

	@Override
	public void apply(Project target) {
		target.getPlugins().withType(JavaPlugin.class, (plugin) -> {
			ConfigurationContainer configurations = target.getConfigurations();
			configurations.create("optional", (optional) -> {
				optional.setCanBeConsumed(false);
				optional.setCanBeResolved(false);
				target.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().all((sourceSet) -> {
					configurations.getByName(sourceSet.getCompileClasspathConfigurationName()).extendsFrom(optional);
					configurations.getByName(sourceSet.getRuntimeClasspathConfigurationName()).extendsFrom(optional);
				});
			});
		});
	}

}
