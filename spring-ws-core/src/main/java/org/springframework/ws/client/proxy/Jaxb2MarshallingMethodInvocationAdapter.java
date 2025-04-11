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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.util.JAXBResult;
import jakarta.xml.bind.util.JAXBSource;

import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.ws.client.WebServiceMarshallingException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.soap.server.endpoint.annotation.SoapAction;

/**
 * Jaxb2MarshallingMethodInvocationAdapter - marshalling adapter using Jakarta JaxB.
 *
 * @author Lars Uffmann
 * @since 3.0.9
 */
public class Jaxb2MarshallingMethodInvocationAdapter extends AbstractMarshallingMethodInvocationAdapter {

	private final JAXBContext jaxbContext;

	private final Class serviceEndpointInterface;

	private final WebServiceTemplate webServiceTemplate;

	public Jaxb2MarshallingMethodInvocationAdapter(Class serviceEndpointInterface,
			WebServiceTemplate webServiceTemplate) {
		XmlSeeAlso xmlSeeAlso = (XmlSeeAlso) serviceEndpointInterface.getAnnotation(XmlSeeAlso.class);
		Assert.notNull(xmlSeeAlso, "The SEI is not annotated with @XmlSeeAlso.");
		Assert.notNull(webServiceTemplate, "A webServiceTemplate is required");
		this.serviceEndpointInterface = serviceEndpointInterface;
		this.webServiceTemplate = webServiceTemplate;
		try {
			this.jaxbContext = JAXBContext.newInstance(xmlSeeAlso.value());
		}
		catch (JAXBException ex) {
			throw new WebServiceMarshallingException("error instantiating JAXBContext: " + ex.getMessage(), ex);
		}
	}

	public WebServiceTemplate getWebServiceTemplate() {
		return this.webServiceTemplate;
	}

	@Override
	public boolean supportsInterface(Class serviceEndpointInterface) {
		return serviceEndpointInterface.getAnnotation(XmlSeeAlso.class) != null;
	}

	@Override
	protected boolean supportsRequestPayloadParameter(MethodParameter parameter) {
		return supportsParameter(parameter);
	}

	@Override
	protected boolean supportsResponsePayloadReturnType(MethodParameter returnType) {
		return supportsParameter(returnType);
	}

	private boolean supportsParameter(MethodParameter parameter) {
		Class<?> parameterType = parameter.getParameterType();
		return parameterType.isAnnotationPresent(XmlRootElement.class)
				|| parameterType.isAnnotationPresent(XmlType.class)
				|| parameterType.isAssignableFrom(JAXBElement.class);
	}

	@Override
	Object invokeInternal(Object requestPayload, SoapAction soapAction) {
		try {
			JAXBSource source = new JAXBSource(this.jaxbContext, requestPayload);
			JAXBResult result = new JAXBResult(this.jaxbContext);
			if (null != soapAction) {
				SoapActionCallback actionCallback = new SoapActionCallback(soapAction.value());
				getWebServiceTemplate().sendSourceAndReceiveToResult(source, actionCallback, result);
			}
			else {
				getWebServiceTemplate().sendSourceAndReceiveToResult(source, result);
			}
			return result.getResult();
		}
		catch (JAXBException ex) {
			throw new WebServiceMarshallingException("jaxb error: " + ex.getMessage(), ex);
		}
	}

}
