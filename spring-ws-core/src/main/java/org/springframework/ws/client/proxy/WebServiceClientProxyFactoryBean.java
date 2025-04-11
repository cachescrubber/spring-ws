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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A FactoryBean that generates a dynamic web service proxy. The dynamic proxy could
 * invoke the web service operations defined by a service endpoint interface.
 *
 * @param <SEI> the service endpoint interface
 * @author Lars Uffmann
 * @since 3.0.9
 */
public class WebServiceClientProxyFactoryBean<SEI> implements FactoryBean<SEI>, InvocationHandler, InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Class<SEI> serviceEndpointInterface;

	private final MethodInvocationAdapter methodInvocationAdapter;

	private final List<Method> webServiceMethods;

	public WebServiceClientProxyFactoryBean(Class<SEI> serviceEndpointInterface,
			MethodInvocationAdapter methodInvocationAdapter) {
		this.serviceEndpointInterface = serviceEndpointInterface;
		this.methodInvocationAdapter = methodInvocationAdapter;
		this.webServiceMethods = initWebServiceMethods();
	}

	// FactoryBean

	@Override
	public SEI getObject() throws Exception {
		Class[] interfaces = new Class[] { this.serviceEndpointInterface };
		return (SEI) Proxy.newProxyInstance(this.serviceEndpointInterface.getClassLoader(), interfaces, this);
	}

	@Override
	public Class<?> getObjectType() {
		return this.serviceEndpointInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	// InitializingBean

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!this.methodInvocationAdapter.supportsInterface(this.serviceEndpointInterface)) {
			throw new BeanInitializationException(
					"The invocation adapter does not support this service endpoint interface.");
		}
		List<String> invocations = getWebServiceMethods().stream().map(Method::getName).toList();
		if (invocations.isEmpty()) {
			throw new BeanInitializationException(
					"no web service invocations found on interface " + this.serviceEndpointInterface);
		}
		this.logger
			.info("detected web service methods declared on SEI " + this.serviceEndpointInterface.getCanonicalName()
					+ ": " + StringUtils.collectionToCommaDelimitedString(invocations));
	}

	// InvocationHandler

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (this.methodInvocationAdapter.isWebServiceInvocation(method)) {
			return this.methodInvocationAdapter.invoke(method, args);
		}
		else if (ReflectionUtils.isEqualsMethod(method)) {
			return proxyEquals(proxy, args[0]);
		}
		else if (ReflectionUtils.isHashCodeMethod(method)) {
			return proxyHashCode(proxy);
		}
		else if (ReflectionUtils.isToStringMethod(method)) {
			return proxyToString(proxy);
		}
		else {
			throw new UnsupportedOperationException("Method is not supported: " + method);
		}
	}

	private Boolean proxyEquals(Object proxy, Object other) {
		return proxy == other;
	}

	private Integer proxyHashCode(Object proxy) {
		return 31 * System.identityHashCode(proxy) + this.serviceEndpointInterface.hashCode();
	}

	private String proxyToString(Object proxy) {
		return "WebServiceClientProxy[" + this.serviceEndpointInterface.getSimpleName() + "]@"
				+ System.identityHashCode(proxy);
	}

	public List<Method> getWebServiceMethods() {
		return this.webServiceMethods;
	}

	private List<Method> initWebServiceMethods() {
		return Arrays.stream(this.serviceEndpointInterface.getMethods())
			.filter(this.methodInvocationAdapter::isWebServiceInvocation)
			.toList();
	}

}
