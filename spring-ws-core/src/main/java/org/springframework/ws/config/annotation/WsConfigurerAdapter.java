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
 * An default implementation of {@link WsConfigurer} with empty methods allowing
 * sub-classes to override only the methods they're interested in.
 *
 * @author Arjen Poutsma
 * @since 2.2
 * @deprecated as of 4.1.0 in favor of implementing {@link WsConfigurer} directly
 */
@Deprecated(since = "4.1.0", forRemoval = true)
public class WsConfigurerAdapter implements WsConfigurer {

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation is empty.
	 */
	@Override
	public void addInterceptors(List<EndpointInterceptor> interceptors) {
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation is empty.
	 */
	@Override
	public void addArgumentResolvers(List<MethodArgumentResolver> argumentResolvers) {
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation is empty.
	 */
	@Override
	public void addReturnValueHandlers(List<MethodReturnValueHandler> returnValueHandlers) {

	}

}
