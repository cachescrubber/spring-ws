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

import org.springframework.core.MethodParameter;
import org.springframework.oxm.GenericMarshaller;
import org.springframework.oxm.GenericUnmarshaller;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.soap.server.endpoint.annotation.SoapAction;

/**
 * OxmMarshallingMethodInvocationAdapter - marshalling adapter using Spring Oxm.
 *
 * @author Lars Uffmann
 * @since 3.0.9
 */
public class OxmMarshallingMethodInvocationAdapter extends AbstractMarshallingMethodInvocationAdapter {

	private final WebServiceTemplate webServiceTemplate;

	public OxmMarshallingMethodInvocationAdapter(WebServiceTemplate webServiceTemplate) {
		Assert.notNull(webServiceTemplate, "A webServiceTemplate is required");
		Assert.notNull(webServiceTemplate.getMarshaller(), "The given webServiceTemplate must have a marshaller");
		Assert.notNull(webServiceTemplate.getUnmarshaller(), "The given webServiceTemplate must have an unmarshaller");
		this.webServiceTemplate = webServiceTemplate;
	}

	public WebServiceTemplate getWebServiceTemplate() {
		return this.webServiceTemplate;
	}

	@Override
	protected boolean supportsRequestPayloadParameter(MethodParameter parameter) {
		Marshaller marshaller = getWebServiceTemplate().getMarshaller();
		if (marshaller == null) {
			return false;
		}
		else if (marshaller instanceof GenericMarshaller) {
			GenericMarshaller genericMarshaller = (GenericMarshaller) marshaller;
			return genericMarshaller.supports(parameter.getGenericParameterType());
		}
		else {
			return marshaller.supports(parameter.getParameterType());
		}
	}

	@Override
	protected boolean supportsResponsePayloadReturnType(MethodParameter returnType) {
		Unmarshaller unmarshaller = getWebServiceTemplate().getUnmarshaller();
		if (unmarshaller == null) {
			return false;
		}
		else if (unmarshaller instanceof GenericUnmarshaller) {
			return ((GenericUnmarshaller) unmarshaller).supports(returnType.getGenericParameterType());
		}
		else {
			return unmarshaller.supports(returnType.getParameterType());
		}
	}

	@Override
	Object invokeInternal(Object requestPayload, SoapAction soapAction) {
		if (null != soapAction) {
			SoapActionCallback actionCallback = new SoapActionCallback(soapAction.value());
			return getWebServiceTemplate().marshalSendAndReceive(requestPayload, actionCallback);
		}
		else {
			return getWebServiceTemplate().marshalSendAndReceive(requestPayload);
		}
	}

}
