package org.springframework.ws.client.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.util.JAXBResult;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.oxm.GenericMarshaller;
import org.springframework.oxm.GenericUnmarshaller;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.ws.client.WebServiceClientProxyException;
import org.springframework.ws.client.WebServiceMarshallingException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.soap.server.endpoint.annotation.SoapAction;

/**
 * A FactoryBean that generates a dynamic web service proxy. The dynamic proxy could invoke the web service operations
 * defined by a service endpoint interface.
 *
 * @author Lars Uffmann
 * @since 3.0.9
 */
public class WebServiceClientProxyFactoryBean<SEI> implements FactoryBean<SEI>, InvocationHandler, InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private final WebServiceTemplate webServiceTemplate;

	private final Class<SEI> serviceEndpointInterface;

	private MethodInvocationAdapter methodInvocationAdapter;

	private final List<Method> webServiceMethods;

	public WebServiceClientProxyFactoryBean(Class<SEI> serviceEndpointInterface, WebServiceTemplate webServiceTemplate) {
		this.webServiceTemplate = webServiceTemplate;
		this.serviceEndpointInterface = serviceEndpointInterface;
		this.webServiceMethods = initWebServiceMethods();
	}

	public WebServiceTemplate getWebServiceTemplate() {
		return webServiceTemplate;
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

		List<String> invocations = getWebServiceMethods().stream().map(m -> m.getName()).collect(Collectors.toList());
		if (invocations.size() < 1) {
			throw new BeanInitializationException("no web service invocations found on interface " +
					serviceEndpointInterface);
		}

		logger.info("detected web service methods declared on SEI " +
				serviceEndpointInterface.getCanonicalName() + ": " +
				StringUtils.collectionToCommaDelimitedString(invocations));

		if (webServiceTemplate.getUnmarshaller() != null && webServiceTemplate.getMarshaller() != null) {
			this.methodInvocationAdapter = new OxmMarshallingMethodInvocationAdapter();
		}
		else {
			this.methodInvocationAdapter = new Jaxb2MarshallingMethodInvocationAdapter();
		}
	}

	// InvocationHandler

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (isWebServiceInvocation(method)) {
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
				.filter(this::isWebServiceInvocation)
				.collect(Collectors.toList());
	}

	private boolean isWebServiceInvocation(Method method) {
		return (method.getAnnotation(PayloadRoot.class) != null || method.getAnnotation(SoapAction.class) != null)
				&& method.getParameterCount() > 0;
	}

	// Impl

	private class Jaxb2MarshallingMethodInvocationAdapter extends AbstractMarshallingMethodInvocationAdapter {

		private final JAXBContext jaxbContext;

		Jaxb2MarshallingMethodInvocationAdapter() throws JAXBException {
			XmlSeeAlso xmlSeeAlso = serviceEndpointInterface.getAnnotation(XmlSeeAlso.class);
			Assert.notNull(xmlSeeAlso, "The SEI is not annotated with @XmlSeeAlso.");
			Assert.notNull(webServiceTemplate, "A webServiceTemplate is required");
			this.jaxbContext = JAXBContext.newInstance(xmlSeeAlso.value());
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
			return parameterType.isAnnotationPresent(XmlRootElement.class) ||
					parameterType.isAnnotationPresent(XmlType.class) ||
					parameterType.isAssignableFrom(JAXBElement.class);
		}

		@Override
		Object invokeInternal(Object requestPayload, SoapAction soapAction) {
			try {
				JAXBSource source = new JAXBSource(jaxbContext, requestPayload);
				JAXBResult result = new JAXBResult(jaxbContext);
				if (null != soapAction) {
					SoapActionCallback actionCallback = new SoapActionCallback(soapAction.value());
					getWebServiceTemplate().sendSourceAndReceiveToResult(source, actionCallback, result);
				}
				else {
					getWebServiceTemplate().sendSourceAndReceiveToResult(source, result);
				}
				return result.getResult();
			}
			catch (JAXBException e) {
				throw new WebServiceMarshallingException("jaxb error: " + e.getMessage(), e);
			}
		}
	}

	private class OxmMarshallingMethodInvocationAdapter extends AbstractMarshallingMethodInvocationAdapter {

		OxmMarshallingMethodInvocationAdapter() {
			Assert.notNull(webServiceTemplate, "A webServiceTemplate is required");
			Assert.notNull(webServiceTemplate.getMarshaller(), "The given webServiceTemplate must have a marshaller");
			Assert.notNull(webServiceTemplate.getUnmarshaller(), "The given webServiceTemplate must have an unmarshaller");
		}

		@Override
		protected boolean supportsRequestPayloadParameter(MethodParameter parameter) {
			Marshaller marshaller = webServiceTemplate.getMarshaller();
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
			Unmarshaller unmarshaller = webServiceTemplate.getUnmarshaller();
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

	interface MethodInvocationAdapter {
		Object invoke(Method method, Object[] args);
	}

	abstract class AbstractMarshallingMethodInvocationAdapter implements MethodInvocationAdapter {

		abstract Object invokeInternal(Object requestPayload, @Nullable SoapAction soapAction);

		abstract protected boolean supportsRequestPayloadParameter(MethodParameter parameter);

		abstract protected boolean supportsResponsePayloadReturnType(MethodParameter returnType);

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
		 * Resolve the Method parameter which is used to marshal the request payload by iterating over the array of method
		 * parameters until a parameter annotated with {@link RequestPayload} is found.
		 *
		 * @param method the method to be invoked
		 * @param args   array of method arguments
		 * @return the resolved request payload method parameter
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
		 * Performs JAXB specific argument value handling.
		 * <p>
		 * Check if the given method parameter is annotated with {@link XmlType}. If it is, the value at the
		 * parameter index is wrapped inside a {@link JAXBElement}. Otherwise it is returned as-is. The {@link QName} of
		 * the JAXBElement is resolved using the metadata available via the method parameter.
		 *
		 * @param parameter the parameter to extract
		 * @param args      array of method arguments
		 * @return the extracted argument value - possibly wrapped inside a JAXBElement
		 * @see #requestQname(MethodParameter)
		 */
		protected Object extractRequestPayload(MethodParameter parameter, Object[] args) {
			Object payload = args[parameter.getParameterIndex()];
			if (payload.getClass().getAnnotation(XmlType.class) != null) {
				QName requestQname = requestQname(parameter);
				JAXBElement<?> element = new JAXBElement(requestQname, payload.getClass(), payload);
				return element;
			}
			else {
				return payload;
			}
		}

		/**
		 * Use metadata available via the given method parameter to construct a {@link QName}. To construct the QName,
		 * the methods name, the parameter name and parameter annotations are used.
		 *
		 * @param parameter the method parameter used to construct the QName
		 * @return the resolved QName
		 * @see RequestPayload
		 */
		protected QName requestQname(MethodParameter parameter) {
			RequestPayload requestPayload = parameter.getParameterAnnotation(RequestPayload.class);
			Assert.notNull(requestPayload, "could not resolve @RequestPayload annotation from parameter");
			if (!StringUtils.hasText(requestPayload.namespace())) {
				logger.warn("@RequestPayload annotation with empty namespace attribute detected");
			}
			final String localPart;
			if (!StringUtils.hasText(requestPayload.localPart())) {
				localPart = parameter.getParameterName();
			}
			else if (requestPayload.localPart().startsWith("+")) {
				localPart = parameter.getMethod().getName() + requestPayload.localPart().substring(1);
			}
			else {
				localPart = requestPayload.localPart();
			}
			return new QName(requestPayload.namespace(), localPart);
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

}
