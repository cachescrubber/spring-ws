/*
 * Copyright 2005-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ws.server.endpoint;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

public class MethodEndpointInterfaceTest {

	private MethodEndpoint endpoint;

	private MyEndpointImpl endpointImpl;

	private boolean myMethodInvoked;

	private Method method;

	@Before
	public void setUp() throws Exception {
		myMethodInvoked = false;
		endpointImpl = new MyEndpointImpl();
		method = MyEndpointImpl.class.getMethod("order", OrderRequest.class);
		endpoint = new MethodEndpoint(endpointImpl, method);
	}

	@Test
	public void testGetters() throws Exception {
		Assert.assertEquals("Invalid bean", endpointImpl, endpoint.getBean());
		Assert.assertEquals("Invalid bean", method, endpoint.getMethod());
	}

	@Test
	public void testInvoke() throws Exception {
		Assert.assertFalse("Method invoked before invocation", myMethodInvoked);
		endpoint.invoke(new OrderRequest("arg"));
		Assert.assertTrue("Method invoked before invocation", myMethodInvoked);
	}

	@Test
	public void testEquals() throws Exception {
		Assert.assertEquals("Not equal", endpoint, endpoint);
		Assert.assertEquals("Not equal", new MethodEndpoint(endpointImpl, method), endpoint);
		Method otherMethod = getClass().getMethod("testEquals");
		Assert.assertFalse("Equal", new MethodEndpoint(this, otherMethod).equals(endpoint));
	}

	@Test
	public void testHashCode() throws Exception {
		Assert.assertEquals("Not equal", new MethodEndpoint(endpointImpl, method).hashCode(), endpoint.hashCode());
		Method otherMethod = getClass().getMethod("testEquals");
		Assert.assertFalse("Equal", new MethodEndpoint(this, otherMethod).hashCode() == endpoint.hashCode());
	}

	@Test
	public void testToString() throws Exception {
		Assert.assertNotNull("No valid toString", endpoint.toString());
	}

	@Test
	public void testReturnMethod() {
		MethodParameter returnType = endpoint.getReturnType();
		Assert.assertEquals(OrderResponse.class, returnType.getParameterType());

		ResponsePayload responsePayload = returnType.getMethodAnnotation(ResponsePayload.class);
		Assert.assertNotNull("annoation should resolve", responsePayload);

		Endpoint endpointAnn = returnType.getMethodAnnotation(Endpoint.class);
		Assert.assertNull("annoation should not resolve", endpointAnn);
	}

	@Test
	public void testInterfaceAnnotations() {

		PayloadRoot payloadRoot = endpoint.getMethodAnnotation(PayloadRoot.class);
		Assert.assertNotNull(payloadRoot);
		Assert.assertEquals(payloadRoot.namespace(), "ns");
		Assert.assertEquals(payloadRoot.localPart(), "orderRequest");

		MethodParameter[] methodParameters = endpoint.getMethodParameters();
		Assert.assertNotNull(methodParameters);
		Assert.assertEquals("not exactly one method", 1, methodParameters.length);

		MethodParameter param0 = methodParameters[0];
		Assert.assertNotNull(param0);
		Assert.assertEquals("param0 class not OrderRequest", OrderRequest.class, param0.getParameterType());

		RequestPayload requestPayload = param0.getParameterAnnotation(RequestPayload.class);
		Assert.assertNotNull(requestPayload);

		// test parameter name discovery
		String parameterName = param0.getParameterName();
		Assert.assertEquals("parameter name could not be discovered", "orderRequest", parameterName);

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
			Assert.assertEquals("Invalid argument", "arg", orderRequest.getOrderId());
			myMethodInvoked = true;
			return null;
		}
	}
}
