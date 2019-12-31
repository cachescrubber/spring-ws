package org.springframework.ws.client.proxy;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.util.JAXBResult;
import javax.xml.bind.util.JAXBSource;

import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.ws.client.WebServiceMarshallingException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.soap.server.endpoint.annotation.SoapAction;

/**
 * @author Lars Uffmann
 * @since 3.0.9
 */
public class Jaxb2MarshallingMethodInvocationAdapter extends AbstractMarshallingMethodInvocationAdapter {

	private final JAXBContext jaxbContext;

	private final Class serviceEndpointInterface;

	private final WebServiceTemplate webServiceTemplate;

	public Jaxb2MarshallingMethodInvocationAdapter(Class serviceEndpointInterface, WebServiceTemplate webServiceTemplate) {
		XmlSeeAlso xmlSeeAlso = (XmlSeeAlso) serviceEndpointInterface.getAnnotation(XmlSeeAlso.class);
		Assert.notNull(xmlSeeAlso, "The SEI is not annotated with @XmlSeeAlso.");
		Assert.notNull(webServiceTemplate, "A webServiceTemplate is required");
		this.serviceEndpointInterface = serviceEndpointInterface;
		this.webServiceTemplate = webServiceTemplate;
		try {
			this.jaxbContext = JAXBContext.newInstance(xmlSeeAlso.value());
		}
		catch (JAXBException e) {
			throw new WebServiceMarshallingException("error instantiating JAXBContext: " + e.getMessage(), e);
		}
	}

	public WebServiceTemplate getWebServiceTemplate() {
		return webServiceTemplate;
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
