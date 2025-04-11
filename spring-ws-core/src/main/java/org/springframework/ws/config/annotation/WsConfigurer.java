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

package org.springframework.ws.config.annotation;

import java.util.List;

import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.server.endpoint.adapter.method.MethodArgumentResolver;
import org.springframework.ws.server.endpoint.adapter.method.MethodReturnValueHandler;

/**
 * Defines callback methods to customize the Java-based configuration for Spring Web
 * Services enabled via {@link EnableWs @EnableWs}.
 * <p>
 * {@code @EnableWs}-annotated configuration classes may implement this interface to be
 * called back and given a chance to customize the default configuration.
 *
 * @author Arjen Poutsma
 * @since 2.2
 */
public interface WsConfigurer {

	/**
	 * Add {@link EndpointInterceptor}s for pre- and post-processing of endpoint method
	 * invocations.
	 */
	default void addInterceptors(List<EndpointInterceptor> interceptors) {

	}

	/**
	 * Configure the {@link MethodArgumentResolver}s to use in addition to the ones
	 * registered by default.
	 * @param argumentResolvers the list of resolvers; initially the default resolvers
	 */
	default void addArgumentResolvers(List<MethodArgumentResolver> argumentResolvers) {

	}

	/**
	 * Configure the {@link MethodReturnValueHandler}s to use in addition to the ones
	 * registered by default.
	 * @param returnValueHandlers the list of handlers; initially the default handlers
	 */
	default void addReturnValueHandlers(List<MethodReturnValueHandler> returnValueHandlers) {

	}

}
