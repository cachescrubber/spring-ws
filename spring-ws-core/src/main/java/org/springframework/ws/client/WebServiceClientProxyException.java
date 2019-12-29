package org.springframework.ws.client;

/**
 * Exception thrown whenever a web service client proxy could not handle a method on the service endpoint interface.
 *
 * @author Lars Uffmann
 * @since 3.0.9
 */
public class WebServiceClientProxyException extends WebServiceClientException {
	/**
	 * Create a new instance of the {@code WebServiceClientProxyException} class.
	 *
	 * @param msg the detail message
	 */
	public WebServiceClientProxyException(String msg) {
		super(msg);
	}

	/**
	 * Create a new instance of the {@code WebServiceClientProxyException} class.
	 *  @param msg the detail message
	 * @param ex  the root {@link Throwable exception}
	 */
	public WebServiceClientProxyException(String msg, Throwable ex) {
		super(msg, ex);
	}
}
