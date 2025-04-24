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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.server.endpoint.adapter.DefaultMethodEndpointAdapter;
import org.springframework.ws.server.endpoint.adapter.method.MethodArgumentResolver;
import org.springframework.ws.server.endpoint.adapter.method.MethodReturnValueHandler;
import org.springframework.ws.server.endpoint.interceptor.EndpointInterceptorAdapter;
import org.springframework.ws.server.endpoint.mapping.PayloadRootAnnotationMethodEndpointMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
@Deprecated
class WsConfigurerAdapterTests {

	private ApplicationContext applicationContext;

	@BeforeEach
	void setUp() {

		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(TestConfig.class);
		applicationContext.refresh();

		this.applicationContext = applicationContext;
	}

	@Test
	void interceptors() {

		PayloadRootAnnotationMethodEndpointMapping endpointMapping = this.applicationContext
			.getBean(PayloadRootAnnotationMethodEndpointMapping.class);

		assertThat(endpointMapping.getOrder()).isEqualTo(0);

		EndpointInterceptor[] interceptors = endpointMapping.getInterceptors();

		assertThat(interceptors).hasSize(1);
		assertThat(interceptors[0]).isInstanceOf(MyInterceptor.class);
	}

	@Test
	void argumentResolvers() {

		DefaultMethodEndpointAdapter endpointAdapter = this.applicationContext
			.getBean(DefaultMethodEndpointAdapter.class);

		List<MethodArgumentResolver> argumentResolvers = endpointAdapter.getMethodArgumentResolvers();

		assertThat(argumentResolvers).hasSizeGreaterThan(1);
		assertThat(argumentResolvers.get(0)).isInstanceOf(MyMethodArgumentResolver.class);

		argumentResolvers = endpointAdapter.getMethodArgumentResolvers();

		assertThat(argumentResolvers).isNotEmpty();
	}

	@Test
	void returnValueHandlers() {

		DefaultMethodEndpointAdapter endpointAdapter = this.applicationContext
			.getBean(DefaultMethodEndpointAdapter.class);

		List<MethodReturnValueHandler> returnValueHandlers = endpointAdapter.getMethodReturnValueHandlers();

		assertThat(returnValueHandlers).hasSizeGreaterThan(1);
		assertThat(returnValueHandlers.get(0)).isInstanceOf(MyReturnValueHandler.class);

		returnValueHandlers = endpointAdapter.getMethodReturnValueHandlers();

		assertThat(returnValueHandlers).isNotEmpty();
	}

	@Configuration
	@EnableWs
	public static class TestConfig implements WsConfigurer {

		@Override
		public void addInterceptors(List<EndpointInterceptor> interceptors) {
			interceptors.add(new MyInterceptor());
		}

		@Override
		public void addArgumentResolvers(List<MethodArgumentResolver> argumentResolvers) {
			argumentResolvers.add(0, new MyMethodArgumentResolver());
		}

		@Override
		public void addReturnValueHandlers(List<MethodReturnValueHandler> returnValueHandlers) {
			returnValueHandlers.add(0, new MyReturnValueHandler());
		}

	}

	public static class MyInterceptor extends EndpointInterceptorAdapter {

	}

	public static class MyMethodArgumentResolver implements MethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return false;
		}

		@Override
		public Object resolveArgument(MessageContext messageContext, MethodParameter parameter) {
			return null;
		}

	}

	public static class MyReturnValueHandler implements MethodReturnValueHandler {

		@Override
		public boolean supportsReturnType(MethodParameter returnType) {
			return false;
		}

		@Override
		public void handleReturnValue(MessageContext messageContext, MethodParameter returnType, Object returnValue) {
		}

	}

}
