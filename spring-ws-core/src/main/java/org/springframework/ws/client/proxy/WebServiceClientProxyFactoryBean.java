package org.springframework.ws.client.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A FactoryBean that generates a dynamic web service proxy. The dynamic proxy could invoke the web service operations
 * defined by a service endpoint interface.
 *
 * @author Lars Uffmann
 * @since 3.0.9
 */
public class WebServiceClientProxyFactoryBean<SEI> implements FactoryBean<SEI>, InvocationHandler, InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Class<SEI> serviceEndpointInterface;

	private final MethodInvocationAdapter methodInvocationAdapter;

	private final List<Method> webServiceMethods;

	public WebServiceClientProxyFactoryBean(Class<SEI> serviceEndpointInterface, MethodInvocationAdapter methodInvocationAdapter) {
		this.serviceEndpointInterface = serviceEndpointInterface;
		this.methodInvocationAdapter = methodInvocationAdapter;
		this.webServiceMethods = initWebServiceMethods();
	}

	// FactoryBean

	@Override
	public SEI getObject() throws Exception {
		Class[] interfaces = new Class[] {serviceEndpointInterface};
		return (SEI) Proxy.newProxyInstance(serviceEndpointInterface.getClassLoader(), interfaces, this);
	}

	@Override
	public Class<?> getObjectType() {
		return serviceEndpointInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	// InitializingBean

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!methodInvocationAdapter.supportsInterface(serviceEndpointInterface)) {
			throw new BeanInitializationException("The invocation adapter does not support this service endpoint interface.");
		}
		List<String> invocations = getWebServiceMethods().stream().map(m -> m.getName()).collect(Collectors.toList());
		if (invocations.size() < 1) {
			throw new BeanInitializationException("no web service invocations found on interface " +
					serviceEndpointInterface);
		}
		logger.info("detected web service methods declared on SEI " +
				serviceEndpointInterface.getCanonicalName() + ": " +
				StringUtils.collectionToCommaDelimitedString(invocations));
	}

	// InvocationHandler

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (methodInvocationAdapter.isWebServiceInvocation(method)) {
			return methodInvocationAdapter.invoke(method, args);
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
		return 31 * System.identityHashCode(proxy) + serviceEndpointInterface.hashCode();
	}

	private String proxyToString(Object proxy) {
		return "WebServiceClientProxy[" + serviceEndpointInterface.getSimpleName() + "]@" + System.identityHashCode(proxy);
	}

	public List<Method> getWebServiceMethods() {
		return webServiceMethods;
	}

	private List<Method> initWebServiceMethods() {
		return Arrays.stream(serviceEndpointInterface.getMethods())
				.filter(methodInvocationAdapter::isWebServiceInvocation)
				.collect(Collectors.toList());
	}

}
