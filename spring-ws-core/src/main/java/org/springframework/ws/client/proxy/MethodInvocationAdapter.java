package org.springframework.ws.client.proxy;

import java.lang.reflect.Method;

/**
 * @author Lars Uffmann
 * @since 3.0.9
 */
public interface MethodInvocationAdapter {

	boolean supportsInterface(Class serviceEndpointInterface);

	boolean isWebServiceInvocation(Method method);

	Object invoke(Method method, Object[] args);
}
