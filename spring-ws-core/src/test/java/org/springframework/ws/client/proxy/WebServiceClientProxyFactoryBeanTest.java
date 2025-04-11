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

package org.springframework.ws.client.proxy;

import java.util.Map;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import org.junit.jupiter.api.Test;

import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
class WebServiceClientProxyFactoryBeanTest {

	static final String REQUEST_XML_NS = """
			<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
				<SOAP-ENV:Header/>
				<SOAP-ENV:Body>
					<ns2:orderRequest xmlns:ns2="https://cachescrubber.org/api">
						<orderId>23</orderId>
					</ns2:orderRequest>
				</SOAP-ENV:Body>
			</SOAP-ENV:Envelope>
			""";

	static final String REQUEST_XML = """
			<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
				<SOAP-ENV:Header/>
				<SOAP-ENV:Body>
					<orderRequest>
						<orderId>23</orderId>
					</orderRequest>
				</SOAP-ENV:Body>
			</SOAP-ENV:Envelope>
			""";

	static final String RESPONSE_XML_NS = """
			<?xml version="1.0" encoding="UTF-8"?>
			<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
				<SOAP-ENV:Header/>
				<SOAP-ENV:Body>
					<api:orderResponse xmlns:api="https://cachescrubber.org/api">
						<referenceNo>42</referenceNo>
					</api:orderResponse>
				</SOAP-ENV:Body>
			</SOAP-ENV:Envelope>
			""";

	@Test
	void testJaxb2MarshallingMethodInvocationAdapter(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

		// The static DSL will be automatically configured for you
		WireMock.stubFor(WireMock.post("/api")
			.willReturn(WireMock.aResponse()
				.withStatus(200)
				.withHeader("Content-Type", "text/xml")
				.withBody(RESPONSE_XML_NS)));

		// Info such as port numbers is also available
		int port = wmRuntimeInfo.getHttpPort();

		WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
		webServiceTemplate.setDefaultUri("http://localhost:" + port + "/api");

		Jaxb2MarshallingMethodInvocationAdapter methodInvocationAdapter = new Jaxb2MarshallingMethodInvocationAdapter(
				MyEndpoint.class, webServiceTemplate);

		MyEndpoint myEndpoint = new WebServiceClientProxyFactoryBean<MyEndpoint>(MyEndpoint.class,
				methodInvocationAdapter)
			.getObject();

		OrderRequest orderRequest = new OrderRequest();
		orderRequest.setOrderId("23");
		OrderResponse orderResponse = myEndpoint.placeOrder(orderRequest);
		assertThat(orderResponse.getReferenceNo()).isEqualTo("42");
	}

	@Test
	void testOxmMarshallingMethodInvocationAdapter(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
		// The static DSL will be automatically configured for you
		WireMock.stubFor(WireMock.post("/api")
			.willReturn(WireMock.aResponse()
				.withStatus(200)
				.withHeader("Content-Type", "text/xml")
				.withBody(RESPONSE_XML_NS)));

		// Info such as port numbers is also available
		int port = wmRuntimeInfo.getHttpPort();

		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(OrderResponse.class, OrderRequest.class);
		marshaller.setMarshallerProperties(Map.of(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE));
		marshaller.afterPropertiesSet();

		WebServiceTemplate webServiceTemplate = new WebServiceTemplate(marshaller);
		webServiceTemplate.setDefaultUri("http://localhost:" + port + "/api");

		OxmMarshallingMethodInvocationAdapter methodInvocationAdapter = new OxmMarshallingMethodInvocationAdapter(
				webServiceTemplate);

		MyEndpoint myEndpoint = new WebServiceClientProxyFactoryBean<MyEndpoint>(MyEndpoint.class,
				methodInvocationAdapter)
			.getObject();

		OrderRequest orderRequest = new OrderRequest();
		orderRequest.setOrderId("23");
		OrderResponse orderResponse = myEndpoint.placeOrder(orderRequest);
		assertThat(orderResponse.getReferenceNo()).isEqualTo("42");

	}

	@XmlRootElement(namespace = "https://cachescrubber.org/api", name = "orderResponse")
	static class OrderResponse {

		private String referenceNo;

		public OrderResponse() {
		}

		public String getReferenceNo() {
			return this.referenceNo;
		}

		@XmlElement
		public void setReferenceNo(String referenceNo) {
			this.referenceNo = referenceNo;
		}

	}

	@XmlRootElement(namespace = "https://cachescrubber.org/api", name = "orderRequest")
	static class OrderRequest {

		public OrderRequest() {
		}

		private String orderId;

		public String getOrderId() {
			return this.orderId;
		}

		@XmlElement
		public void setOrderId(String orderId) {
			this.orderId = orderId;
		}

	}

	@Endpoint
	@XmlSeeAlso({ OrderRequest.class, OrderResponse.class })
	interface MyEndpoint {

		@PayloadRoot(namespace = "ns", localPart = "orderRequest")
		@ResponsePayload
		OrderResponse placeOrder(@RequestPayload OrderRequest orderRequest);

	}

	static class MyEndpointImpl implements MyEndpoint {

		@Override
		public OrderResponse placeOrder(OrderRequest orderRequest) {
			return new OrderResponse();
		}

	}

}