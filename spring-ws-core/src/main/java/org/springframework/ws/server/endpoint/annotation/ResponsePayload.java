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

package org.springframework.ws.server.endpoint.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.xml.XMLConstants;

/**
 * Annotation which indicates that a method return value should be bound to the
 * {@linkplain org.springframework.ws.WebServiceMessage#getPayloadSource() response
 * payload}. Supported for annotated endpoint methods.
 * Annotation which indicates that a method return value should be bound to the {@linkplain
 * org.springframework.ws.WebServiceMessage#getPayloadSource() response payload}. Supported for annotated endpoint
 * methods. The namespace and localPart properties are only relevant when the return value is a JAXB2 object annotated
 * with @XmlType.
 *
 * @author Arjen Poutsma
 * @author Lars Uffmann
 * @since 2.0
 * @see RequestPayload
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponsePayload {
	/**
	 * Signifies the local part of the response payload root element returned by the annotated method.
	 * <p>
	 * If localPart starts with a plus sign (+), the remaining string is appended to the annotated
	 * methods name.
	 * <p>
	 * Defaults to "+Response".
	 *
	 * @see #namespace()
	 */
	String localPart() default "+Response";

	/**
	 * Signifies the namespace of the payload root element handled by the annotated method.
	 *
	 * @see #localPart()
	 */
	String namespace() default XMLConstants.NULL_NS_URI;

}
