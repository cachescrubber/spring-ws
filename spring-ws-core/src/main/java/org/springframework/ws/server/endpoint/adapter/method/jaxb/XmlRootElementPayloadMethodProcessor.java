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

package org.springframework.ws.server.endpoint.adapter.method.jaxb;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.springframework.core.MethodParameter;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.server.endpoint.support.PayloadRootUtils;

/**
 * Implementation of {@link org.springframework.ws.server.endpoint.adapter.method.MethodArgumentResolver
 * MethodArgumentResolver} and {@link org.springframework.ws.server.endpoint.adapter.method.MethodReturnValueHandler
 * MethodReturnValueHandler} that supports parameters annotated with {@link XmlRootElement @XmlRootElement} or {@link
 * XmlType @XmlType}, and return values annotated with {@link XmlRootElement @XmlRootElement}.
 * <p>
 * Since version 3.0.9 return values annotated with {@link XmlType @XmlType} are supported. In order to marshall the
 * response message, the {@link ResponsePayload @ResponsePayload} annotation has been extended with the namespace and
 * localPart properties.
 *
 * @author Arjen Poutsma
 * @author Lars Uffmann
 * @since 2.0
 */
public class XmlRootElementPayloadMethodProcessor extends AbstractJaxb2PayloadMethodProcessor {

	@Override
	protected boolean supportsRequestPayloadParameter(MethodParameter parameter) {
		Class<?> parameterType = parameter.getParameterType();
		return parameterType.isAnnotationPresent(XmlRootElement.class)
				|| parameterType.isAnnotationPresent(XmlType.class);
	}

	@Override
	public Object resolveArgument(MessageContext messageContext, MethodParameter parameter) throws JAXBException {
		Class<?> parameterType = parameter.getParameterType();

		if (parameterType.isAnnotationPresent(XmlRootElement.class)) {
			return unmarshalFromRequestPayload(messageContext, parameterType);
		}
		else {
			JAXBElement<?> element = unmarshalElementFromRequestPayload(messageContext, parameterType);
			return (element != null) ? element.getValue() : null;
		}
	}

	@Override
	protected boolean supportsResponsePayloadReturnType(MethodParameter returnType) {
		Class<?> parameterType = returnType.getParameterType();
		return parameterType.isAnnotationPresent(XmlRootElement.class) ||
				parameterType.isAnnotationPresent(XmlType.class);
	}

	@Override
	protected void handleReturnValueInternal(MessageContext messageContext, MethodParameter returnType,
			Object returnValue) throws JAXBException {
		Class<?> parameterType = returnType.getParameterType();
		if (parameterType.isAnnotationPresent(XmlRootElement.class)) {
			marshalToResponsePayload(messageContext, parameterType, returnValue);
		}
		else {
			QName returnQname = PayloadRootUtils.getResponsePayloadQName(returnType);
			JAXBElement<?> element = new JAXBElement(returnQname, parameterType, returnValue);
			marshalToResponsePayload(messageContext, parameterType, element);
		}
	}

}
