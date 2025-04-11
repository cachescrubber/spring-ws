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

import java.lang.reflect.Method;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlType;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.ws.client.WebServiceClientProxyException;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.support.PayloadRootUtils;
import org.springframework.ws.soap.server.endpoint.annotation.SoapAction;

/**
 * AbstractMarshallingMethodInvocationAdapter - base class for marshalling method adapter
 * implementations.
 *
 * @author Lars Uffmann
 * @since 3.0.9
 */
public abstract class AbstractMarshallingMethodInvocationAdapter implements MethodInvocationAdapter {

	abstract Object invokeInternal(Object requestPayload, @Nullable SoapAction soapAction);

	protected abstract boolean supportsRequestPayloadParameter(MethodParameter parameter);

	protected abstract boolean supportsResponsePayloadReturnType(MethodParameter returnType);

	@Override
	public boolean supportsInterface(Class serviceEndpointInterface) {
		return true;
	}

	@Override
	public boolean isWebServiceInvocation(Method method) {
		return (method.getAnnotation(PayloadRoot.class) != null || method.getAnnotation(SoapAction.class) != null)
				&& method.getParameterCount() > 0;
	}

	public Object invoke(Method method, Object[] args) {
		MethodParameter returnType = new MethodParameter(method, -1);

		if (!supportsResponsePayloadReturnType(returnType)) {
			throw new WebServiceClientProxyException("Method return type is not supported.");
		}

		MethodParameter requestParameter = resolveRequestPayload(method, args);
		if (null == requestParameter) {
			throw new WebServiceClientProxyException("Could not resolve a parameter to use as request payload.");
		}

		if (null == args[requestParameter.getParameterIndex()]) {
			throw new IllegalArgumentException("The request payload argument must not be null");
		}

		if (!supportsRequestPayloadParameter(requestParameter)) {
			throw new WebServiceClientProxyException("The request payload parameter is not supported.");
		}

		Object requestPayload = extractRequestPayload(requestParameter, args);

		Object returnValue = invokeInternal(requestPayload, method.getAnnotation(SoapAction.class));

		if (returnType.getParameterType().isAssignableFrom(JAXBElement.class)) {
			return returnValue;
		}
		else {
			return unwrapJaxbElement(returnValue);
		}
	}

	/**
	 * Return the first method parameter annotated with {@link RequestPayload}.
	 * @param method the method to be invoked
	 * @param args array of method arguments
	 * @return the resolved request payload method parameter, null if no suitable
	 * parameter could be resolved.
	 * @see #supportsRequestPayloadParameter
	 * @see #extractRequestPayload
	 */
	protected MethodParameter resolveRequestPayload(Method method, Object[] args) {
		for (int i = 0; i < method.getParameterCount(); i++) {
			MethodParameter parameter = new MethodParameter(method, i);
			if (null != parameter.getParameterAnnotation(RequestPayload.class)) {
				parameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
				return parameter;
			}
		}
		return null;
	}

	/**
	 * Extract the request payload argument value, performing JAXB specific argument
	 * processing if necessary.
	 * <p>
	 * If the given method parameter type is annotated with {@link XmlType}, the value at
	 * the parameter index is wrapped inside a {@link JAXBElement}. Otherwise it is
	 * returned as-is. The {@link QName} of the JAXBElement is resolved using the metadata
	 * available via the method parameter.
	 * @param parameter the parameter to extract
	 * @param args array of method arguments
	 * @return the extracted argument value - possibly wrapped inside a JAXBElement
	 * @see PayloadRootUtils
	 */
	protected Object extractRequestPayload(MethodParameter parameter, Object[] args) {
		Object payload = args[parameter.getParameterIndex()];
		if (payload.getClass().getAnnotation(XmlType.class) != null) {
			QName requestQname = PayloadRootUtils.getRequestPayloadQName(parameter);
			JAXBElement<?> element = new JAXBElement(requestQname, payload.getClass(), payload);
			return element;
		}
		else {
			return payload;
		}
	}

	protected Object unwrapJaxbElement(Object returnValue) {
		if (returnValue instanceof JAXBElement) {
			return ((JAXBElement<?>) returnValue).getValue();
		}
		else {
			return returnValue;
		}
	}

}
