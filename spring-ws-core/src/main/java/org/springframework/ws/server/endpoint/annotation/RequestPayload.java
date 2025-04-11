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
 * Annotation which indicates that a method parameter should be bound to the
 * {@linkplain org.springframework.ws.WebServiceMessage#getPayloadSource() request
 * payload}. Supported for annotated endpoint methods.
 *
 * @author Arjen Poutsma
 * @author Lars Uffmann
 *
 * @since 2.0
 * @see ResponsePayload
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestPayload {
	/**
	 * Signifies the local part of the request payload root element expected by the annotated parameter.
	 * <p>
	 * If localPart starts with a plus sign (+), the remaining string is appended to the annotated
	 * methods name.
	 * <p>
	 * Defaults to "+Request".
	 *
	 * @see #namespace()
	 */
	String localPart() default "+Request";

	/**
	 * Signifies the namespace of the payload root element expected by the annotated parameter.
	 *
	 * @see #localPart()
	 */
	String namespace() default XMLConstants.NULL_NS_URI;


}
