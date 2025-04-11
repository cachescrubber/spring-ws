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

package org.springframework.ws.server.endpoint;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import static org.assertj.core.api.Assertions.assertThat;

class MethodEndpointInterfaceTest {

	private MethodEndpoint endpoint;

	private MyEndpointImpl endpointImpl;

	private boolean myMethodInvoked;

	private Method method;

	@BeforeEach
	public void setUp() throws Exception {
		myMethodInvoked = false;
		endpointImpl = new MyEndpointImpl();
		method = MyEndpointImpl.class.getMethod("order", OrderRequest.class);
		endpoint = new MethodEndpoint(endpointImpl, method);
	}

	@Test
	void testGetters() throws Exception {
		assertThat(endpoint.getBean()).isSameAs(endpointImpl);
		assertThat(endpoint.getMethod()).isSameAs(method);
	}

	@Test
	void testInvoke() throws Exception {
		assertThat(myMethodInvoked).isFalse();
		endpoint.invoke(new OrderRequest("arg"));
		assertThat(myMethodInvoked).isTrue();
	}

	@Test
	void testEquals() throws Exception {
		assertThat(endpoint).isEqualTo(new MethodEndpoint(endpointImpl, method));
		Method otherMethod = getClass().getMethod("methodRef");
		assertThat(endpoint).isNotEqualTo(new MethodEndpoint(this, otherMethod));
	}

	public void methodRef() {
		// unused
	}

	@Test
	void testHashCode() throws Exception {
		assertThat(new MethodEndpoint(endpointImpl, method)).hasSameHashCodeAs(endpoint);
		Method otherMethod = getClass().getMethod("methodRef");
		assertThat(otherMethod).doesNotHaveSameHashCodeAs(endpoint);
	}

	@Test
	void testToString() throws Exception {
		assertThat(endpointImpl.toString()).isNotNull();
	}

	@Test
	void testReturnMethod() {
		MethodParameter returnType = endpoint.getReturnType();
		assertThat(returnType.getParameterType()).isAssignableFrom(OrderResponse.class);

		ResponsePayload responsePayload = returnType.getMethodAnnotation(ResponsePayload.class);
		assertThat(responsePayload).isNotNull();

		Endpoint endpointAnn = returnType.getMethodAnnotation(Endpoint.class);
		assertThat(endpointAnn).isNull();
	}

	@Test
	void testInterfaceAnnotations() {

		PayloadRoot payloadRoot = endpoint.getMethodAnnotation(PayloadRoot.class);
		assertThat(payloadRoot).isNotNull();
		assertThat(payloadRoot.namespace()).isEqualTo("ns");
		assertThat(payloadRoot.localPart()).isEqualTo("orderRequest");

		MethodParameter[] methodParameters = endpoint.getMethodParameters();
		assertThat(methodParameters).isNotNull().hasSize(1);

		MethodParameter param0 = methodParameters[0];
		assertThat(param0).isNotNull();
		assertThat(param0.getParameterType()).isAssignableFrom(OrderRequest.class);

		RequestPayload requestPayload = param0.getParameterAnnotation(RequestPayload.class);
		assertThat(requestPayload).isNotNull();

		// FIXME: test parameter name discovery
		// String parameterName = param0.getParameterName();
		// assertThat(parameterName).isEqualTo("orderRequest");
	}

	@Service
	class OrderResponse {

	}

	class OrderRequest {

		private final String orderId;

		public OrderRequest(String orderId) {
			this.orderId = orderId;
		}

		public String getOrderId() {
			return orderId;
		}

	}

	@Endpoint
	interface MyEndpoit {

		@PayloadRoot(namespace = "ns", localPart = "orderRequest")
		@ResponsePayload
		OrderResponse order(@RequestPayload OrderRequest orderRequest);

	}

	class MyEndpointImpl implements MyEndpoit {

		@Override
		public OrderResponse order(OrderRequest orderRequest) {
			assertThat(orderRequest.getOrderId()).isEqualTo("arg");
			myMethodInvoked = true;
			return null;
		}

	}

}
