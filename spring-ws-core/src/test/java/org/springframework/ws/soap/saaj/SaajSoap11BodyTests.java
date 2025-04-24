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

package org.springframework.ws.soap.saaj;

import java.util.Locale;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPMessage;
import org.junit.jupiter.api.Test;

import org.springframework.ws.soap.SoapBody;
import org.springframework.ws.soap.soap11.AbstractSoap11BodyTests;

import static org.assertj.core.api.Assertions.assertThat;

class SaajSoap11BodyTests extends AbstractSoap11BodyTests {

	@Override
	protected SoapBody createSoapBody() throws Exception {

		MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
		SOAPMessage saajMessage = messageFactory.createMessage();

		return new SaajSoap11Body(saajMessage.getSOAPPart().getEnvelope().getBody(), true);
	}

	@Test
	void testLangAttributeOnSoap11FaultString() throws Exception {

		MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
		SOAPMessage saajMessage = messageFactory.createMessage();

		SOAPBody saajSoapBody = saajMessage.getSOAPPart().getEnvelope().getBody();
		SaajSoap11Body soapBody = new SaajSoap11Body(saajSoapBody, true);

		soapBody.addClientOrSenderFault("Foo", Locale.ENGLISH);

		assertThat(saajSoapBody.getFault().getFaultStringLocale()).isNotNull();

		saajSoapBody = saajMessage.getSOAPPart().getEnvelope().getBody();
		soapBody = new SaajSoap11Body(saajSoapBody, false);

		soapBody.addClientOrSenderFault("Foo", Locale.ENGLISH);

		assertThat(saajSoapBody.getFault().getFaultStringLocale()).isNull();
	}

}
