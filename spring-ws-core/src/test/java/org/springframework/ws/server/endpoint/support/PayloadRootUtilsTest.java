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

package org.springframework.ws.server.endpoint.support;

import java.io.StringReader;
import java.lang.reflect.Method;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import org.springframework.core.MethodParameter;
import org.springframework.util.xml.StaxUtils;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.xml.DocumentBuilderFactoryUtils;
import org.springframework.xml.XMLInputFactoryUtils;
import org.springframework.xml.transform.TransformerFactoryUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PayloadRootUtilsTest {

	@Test
	void testGetQNameFromRequestPayloadParameter_IAE() throws NoSuchMethodException {
		Method underTest = PayloadRootUtilsTest.class.getMethod("testIAE", TestRequestType.class);
		assertThat(underTest).isNotNull();
		MethodParameter parameter = new MethodParameter(underTest, 0);

		assertThatCode(() -> PayloadRootUtils.getRequestPayloadQName(parameter))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testGetQNameFromResponsePayloadParameter_IAE() throws NoSuchMethodException {
		Method underTest = PayloadRootUtilsTest.class.getMethod("testIAE", TestRequestType.class);
		assertThat(underTest).isNotNull();
		MethodParameter returnType = new MethodParameter(underTest, -1);

		assertThatCode(() -> {
			QName responseQname = PayloadRootUtils.getResponsePayloadQName(returnType);
			System.out.println(responseQname);
		}).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testGetQNameFromRequestPayloadParameter() throws NoSuchMethodException {
		Method underTest = PayloadRootUtilsTest.class.getMethod("test", TestRequestType.class);
		assertThat(underTest).isNotNull();
		MethodParameter parameter = new MethodParameter(underTest, 0);
		MethodParameter returnType = new MethodParameter(underTest, -1);
		QName requestQname = PayloadRootUtils.getRequestPayloadQName(parameter);
		QName responseQname = PayloadRootUtils.getResponsePayloadQName(returnType);

		// localPart derived form method name
		assertThat(requestQname.getLocalPart()).isEqualTo("testRequest");
		// namespace uses default value from @PayloadRoot
		assertThat(requestQname.getNamespaceURI()).isEqualTo("http://foo.bar/");

		// localPart derived form method name
		assertThat(responseQname.getLocalPart()).isEqualTo("testResponse");
		// namespace uses default value from @PayloadRoot
		assertThat(responseQname.getNamespaceURI()).isEqualTo("http://foo.bar/");
	}

	@Test
	void testGetQNameFromRequestPayloadParameter_fun() throws NoSuchMethodException {
		Method underTest = PayloadRootUtilsTest.class.getMethod("fun", TestRequestType.class);
		assertThat(underTest).isNotNull();

		MethodParameter parameter = new MethodParameter(underTest, 0);
		MethodParameter returnType = new MethodParameter(underTest, -1);
		QName requestQname = PayloadRootUtils.getRequestPayloadQName(parameter);
		QName responseQname = PayloadRootUtils.getResponsePayloadQName(returnType);

		assertThat(requestQname.getLocalPart()).isEqualTo("testRequest");
		assertThat(requestQname.getNamespaceURI()).isEqualTo("http://foo.bar/");

		assertThat(responseQname.getLocalPart()).isEqualTo("testResponse");
		assertThat(responseQname.getNamespaceURI()).isEqualTo("http://foo.bar/");
	}

	@Test
	void testGetQNameFromRequestPayloadParameter_fun2() throws NoSuchMethodException {
		Method underTest = PayloadRootUtilsTest.class.getMethod("fun2", TestRequestType.class);
		assertThat(underTest).isNotNull();

		MethodParameter parameter = new MethodParameter(underTest, 0);
		MethodParameter returnType = new MethodParameter(underTest, -1);
		QName requestQname = PayloadRootUtils.getRequestPayloadQName(parameter);
		QName responseQname = PayloadRootUtils.getResponsePayloadQName(returnType);

		assertThat(requestQname.getLocalPart()).isEqualTo("funkyRequest");
		assertThat(requestQname.getNamespaceURI()).isEqualTo("http://foo.bar/");

		assertThat(responseQname.getLocalPart()).isEqualTo("funkyResponse");
		assertThat(responseQname.getNamespaceURI()).isEqualTo("http://foo.bar/");
	}

	public @ResponsePayload TestResponseType testIAE(@RequestPayload TestRequestType myRequestType) {
		return new TestResponseType();
	}

	@PayloadRoot(namespace = "http://foo.bar/", localPart = "testRequest")
	public @ResponsePayload TestResponseType test(@RequestPayload TestRequestType myRequestType) {
		return new TestResponseType();
	}

	public @ResponsePayload(namespace = "http://foo.bar/", localPart = "testResponse") TestResponseType fun(
			@RequestPayload(namespace = "http://foo.bar/", localPart = "testRequest") TestRequestType myRequestType) {
		return new TestResponseType();
	}

	public @ResponsePayload(namespace = "http://foo.bar/", localPart = "funkyResponse") TestResponseType fun2(
			@RequestPayload(namespace = "http://foo.bar/", localPart = "funkyRequest") TestRequestType myRequestType) {
		return new TestResponseType();
	}

	class TestResponseType {

	}

	class TestRequestType {

	}

	@Test
	public void testGetQNameForDomSource() throws Exception {

		DocumentBuilderFactory factory = DocumentBuilderFactoryUtils.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();
		Element element = document.createElementNS("namespace", "prefix:localname");
		document.appendChild(element);
		Source source = new DOMSource(document);
		QName qName = PayloadRootUtils.getPayloadRootQName(source, TransformerFactoryUtils.newInstance());

		assertThat(qName).isNotNull();
		assertThat(qName.getLocalPart()).isEqualTo("localname");
		assertThat(qName.getNamespaceURI()).isEqualTo("namespace");
		assertThat(qName.getPrefix()).isEqualTo("prefix");
	}

	@Test
	public void testGetQNameForStaxSourceStreamReader() throws Exception {

		String contents = "<prefix:localname xmlns:prefix='namespace'/>";
		XMLInputFactory inputFactory = XMLInputFactoryUtils.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(contents));
		Source source = StaxUtils.createStaxSource(streamReader);
		QName qName = PayloadRootUtils.getPayloadRootQName(source, TransformerFactoryUtils.newInstance());

		assertThat(qName).isNotNull();
		assertThat(qName.getLocalPart()).isEqualTo("localname");
		assertThat(qName.getNamespaceURI()).isEqualTo("namespace");
		assertThat(qName.getPrefix()).isEqualTo("prefix");
	}

	@Test
	public void testGetQNameForStaxSourceEventReader() throws Exception {

		String contents = "<prefix:localname xmlns:prefix='namespace'/>";
		XMLInputFactory inputFactory = XMLInputFactoryUtils.newInstance();
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(contents));
		Source source = StaxUtils.createStaxSource(eventReader);
		QName qName = PayloadRootUtils.getPayloadRootQName(source, TransformerFactoryUtils.newInstance());

		assertThat(qName).isNotNull();
		assertThat(qName.getLocalPart()).isEqualTo("localname");
		assertThat(qName.getNamespaceURI()).isEqualTo("namespace");
		assertThat(qName.getPrefix()).isEqualTo("prefix");
	}

	@Test
	public void testGetQNameForStreamSource() throws Exception {
		String contents = "<prefix:localname xmlns:prefix='namespace'/>";
		Source source = new StreamSource(new StringReader(contents));
		QName qName = PayloadRootUtils.getPayloadRootQName(source, TransformerFactoryUtils.newInstance());

		assertThat(qName).isNotNull();
		assertThat(qName.getLocalPart()).isEqualTo("localname");
		assertThat(qName.getNamespaceURI()).isEqualTo("namespace");
		assertThat(qName.getPrefix()).isEqualTo("prefix");
	}

	@Test
	public void testGetQNameForSaxSource() throws Exception {
		String contents = "<prefix:localname xmlns:prefix='namespace'/>";
		Source source = new SAXSource(new InputSource(new StringReader(contents)));
		QName qName = PayloadRootUtils.getPayloadRootQName(source, TransformerFactoryUtils.newInstance());

		assertThat(qName).isNotNull();
		assertThat(qName.getLocalPart()).isEqualTo("localname");
		assertThat(qName.getNamespaceURI()).isEqualTo("namespace");
		assertThat(qName.getPrefix()).isEqualTo("prefix");
	}

	@Test
	public void testGetQNameForNullSource() throws Exception {

		QName qName = PayloadRootUtils.getPayloadRootQName(null, TransformerFactoryUtils.newInstance());

		assertThat(qName).isNull();
	}

}
