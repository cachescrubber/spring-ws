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

package org.springframework.ws.server.endpoint.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.ws.MockWebServiceMessageFactory;
import org.springframework.ws.context.DefaultMessageContext;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.server.EndpointInvocationChain;
import org.springframework.ws.server.SmartEndpointInterceptor;
import org.springframework.ws.server.endpoint.interceptor.DelegatingSmartEndpointInterceptor;
import org.springframework.ws.server.endpoint.interceptor.EndpointInterceptorAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test case for {@link AbstractEndpointMapping}.
 */
class EndpointMappingTests {

	private MessageContext messageContext;

	@BeforeEach
	void setUp() {
		this.messageContext = new DefaultMessageContext(new MockWebServiceMessageFactory());
	}

	@Test
	void defaultEndpoint() throws Exception {

		Object defaultEndpoint = new Object();
		AbstractEndpointMapping mapping = new AbstractEndpointMapping() {
			@Override
			protected Object getEndpointInternal(MessageContext givenRequest) {
				assertThat(givenRequest).isEqualTo(EndpointMappingTests.this.messageContext);
				return null;
			}
		};
		mapping.setDefaultEndpoint(defaultEndpoint);

		EndpointInvocationChain result = mapping.getEndpoint(this.messageContext);

		assertThat(result).isNotNull();
		assertThat(result.getEndpoint()).isEqualTo(defaultEndpoint);
	}

	@Test
	void endpoint() throws Exception {

		final Object endpoint = new Object();
		AbstractEndpointMapping mapping = new AbstractEndpointMapping() {
			@Override
			protected Object getEndpointInternal(MessageContext givenRequest) {
				assertThat(givenRequest).isEqualTo(EndpointMappingTests.this.messageContext);
				return endpoint;
			}
		};

		EndpointInvocationChain result = mapping.getEndpoint(this.messageContext);

		assertThat(result).isNotNull();
		assertThat(result.getEndpoint()).isEqualTo(endpoint);
	}

	@Test
	void endpointInterceptors() throws Exception {

		final Object endpoint = new Object();
		EndpointInterceptor interceptor = new EndpointInterceptorAdapter();
		AbstractEndpointMapping mapping = new AbstractEndpointMapping() {
			@Override
			protected Object getEndpointInternal(MessageContext givenRequest) {
				assertThat(givenRequest).isEqualTo(EndpointMappingTests.this.messageContext);
				return endpoint;
			}
		};

		mapping.setInterceptors(new EndpointInterceptor[] { interceptor });
		EndpointInvocationChain result = mapping.getEndpoint(this.messageContext);

		assertThat(result.getInterceptors()).hasSize(1);
		assertThat(result.getInterceptors()[0]).isEqualTo(interceptor);
	}

	@Test
	void smartEndpointInterceptors() throws Exception {

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("smartInterceptor", MySmartEndpointInterceptor.class);

		final Object endpoint = new Object();
		EndpointInterceptor interceptor = new EndpointInterceptorAdapter();
		AbstractEndpointMapping mapping = new AbstractEndpointMapping() {
			@Override
			protected Object getEndpointInternal(MessageContext givenRequest) {
				assertThat(givenRequest).isEqualTo(EndpointMappingTests.this.messageContext);
				return endpoint;
			}
		};
		mapping.setApplicationContext(applicationContext);
		mapping.setInterceptors(new EndpointInterceptor[] { interceptor });

		EndpointInvocationChain result = mapping.getEndpoint(this.messageContext);

		assertThat(result.getInterceptors()).hasSize(2);
		assertThat(result.getInterceptors()[0]).isEqualTo(interceptor);
		assertThat(result.getInterceptors()[1]).isInstanceOf(MySmartEndpointInterceptor.class);
	}

	@Test
	void smartEndpointInterceptorAddedOnlyIfNecessary() throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		Object endpoint = new Object();
		SmartEndpointInterceptor firstInterceptor = mock(SmartEndpointInterceptor.class);
		given(firstInterceptor.shouldIntercept(this.messageContext, endpoint)).willReturn(false);
		applicationContext.registerBean("first", SmartEndpointInterceptor.class, () -> firstInterceptor);
		SmartEndpointInterceptor secondInterceptor = mock(SmartEndpointInterceptor.class);
		given(secondInterceptor.shouldIntercept(this.messageContext, endpoint)).willReturn(true);
		applicationContext.registerBean("second", SmartEndpointInterceptor.class, () -> secondInterceptor);

		AbstractEndpointMapping mapping = new AbstractEndpointMapping() {
			@Override
			protected Object getEndpointInternal(MessageContext givenRequest) {
				assertThat(givenRequest).isEqualTo(EndpointMappingTests.this.messageContext);
				return endpoint;
			}
		};
		mapping.setApplicationContext(applicationContext);
		EndpointInvocationChain result = mapping.getEndpoint(this.messageContext);
		assertThat(result).isNotNull();
		assertThat(result.getInterceptors()).singleElement().isSameAs(secondInterceptor);
		verify(firstInterceptor).shouldIntercept(this.messageContext, endpoint);
		verify(secondInterceptor).shouldIntercept(this.messageContext, endpoint);
	}

	@Test
	void smartEndpointInterceptorSetAsInterceptorAreHandled() throws Exception {
		Object endpoint = new Object();
		SmartEndpointInterceptor firstInterceptor = mock(SmartEndpointInterceptor.class);
		given(firstInterceptor.shouldIntercept(this.messageContext, endpoint)).willReturn(false);
		SmartEndpointInterceptor secondInterceptor = mock(SmartEndpointInterceptor.class);
		given(secondInterceptor.shouldIntercept(this.messageContext, endpoint)).willReturn(true);

		AbstractEndpointMapping mapping = new AbstractEndpointMapping() {
			@Override
			protected Object getEndpointInternal(MessageContext givenRequest) {
				assertThat(givenRequest).isEqualTo(EndpointMappingTests.this.messageContext);
				return endpoint;
			}
		};
		mapping.setInterceptors(new EndpointInterceptor[] { firstInterceptor, secondInterceptor });
		EndpointInvocationChain result = mapping.getEndpoint(this.messageContext);
		assertThat(result).isNotNull();
		assertThat(result.getInterceptors()).singleElement().isSameAs(secondInterceptor);
		verify(firstInterceptor).shouldIntercept(this.messageContext, endpoint);
		verify(secondInterceptor).shouldIntercept(this.messageContext, endpoint);
	}

	@Test
	void endpointBeanName() throws Exception {

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("endpoint", Object.class);

		AbstractEndpointMapping mapping = new AbstractEndpointMapping() {

			@Override
			protected Object getEndpointInternal(MessageContext message) {
				assertThat(message).isEqualTo(EndpointMappingTests.this.messageContext);
				return "endpoint";
			}
		};
		mapping.setApplicationContext(applicationContext);

		EndpointInvocationChain result = mapping.getEndpoint(this.messageContext);

		assertThat(result).isNotNull();
	}

	@Test
	void endpointInvalidBeanName() throws Exception {

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("endpoint", Object.class);

		AbstractEndpointMapping mapping = new AbstractEndpointMapping() {

			@Override
			protected Object getEndpointInternal(MessageContext message) {
				assertThat(message).isEqualTo(EndpointMappingTests.this.messageContext);
				return "noSuchBean";
			}
		};
		mapping.setApplicationContext(applicationContext);

		EndpointInvocationChain result = mapping.getEndpoint(this.messageContext);

		assertThat(result).isNull();
	}

	@Test
	void endpointPrototype() throws Exception {

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerPrototype("endpoint", MyEndpoint.class);

		AbstractEndpointMapping mapping = new AbstractEndpointMapping() {

			@Override
			protected Object getEndpointInternal(MessageContext message) {
				assertThat(message).isEqualTo(EndpointMappingTests.this.messageContext);
				return "endpoint";
			}
		};
		mapping.setApplicationContext(applicationContext);

		EndpointInvocationChain result = mapping.getEndpoint(this.messageContext);

		assertThat(result).isNotNull();

		result = mapping.getEndpoint(this.messageContext);

		assertThat(result).isNotNull();
		assertThat(MyEndpoint.constructorCount).isEqualTo(2);
	}

	private static final class MyEndpoint {

		private static int constructorCount;

		private MyEndpoint() {
			constructorCount++;
		}

	}

	private static final class MySmartEndpointInterceptor extends DelegatingSmartEndpointInterceptor {

		private MySmartEndpointInterceptor() {
			super(new EndpointInterceptorAdapter());
		}

	}

}
