/*
 * Copyright 2005-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	   http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Assert;
import org.junit.Test;
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

public class PayloadRootUtilsTest {

	@Test(expected = IllegalArgumentException.class)
	public void testGetQNameFromRequestPayloadParameter_IAE() throws NoSuchMethodException {
		Method underTest = PayloadRootUtilsTest.class.getMethod("testIAE", TestRequestType.class);
		Assert.assertNotNull(underTest);
		MethodParameter parameter = new MethodParameter(underTest, 0);
		QName requestQname = PayloadRootUtils.getRequestPayloadQName(parameter);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetQNameFromResponsePayloadParameter_IAE() throws NoSuchMethodException {
		Method underTest = PayloadRootUtilsTest.class.getMethod("testIAE", TestRequestType.class);
		Assert.assertNotNull(underTest);
		MethodParameter returnType = new MethodParameter(underTest, -1);
		QName responseQname = PayloadRootUtils.getResponsePayloadQName(returnType);
		System.out.println(responseQname);
	}

	@Test
	public void testGetQNameFromRequestPayloadParameter() throws NoSuchMethodException {
		Method underTest = PayloadRootUtilsTest.class.getMethod("test", TestRequestType.class);
		Assert.assertNotNull(underTest);
		MethodParameter parameter = new MethodParameter(underTest, 0);
		MethodParameter returnType = new MethodParameter(underTest, -1);
		QName requestQname = PayloadRootUtils.getRequestPayloadQName(parameter);
		QName responseQname = PayloadRootUtils.getResponsePayloadQName(returnType);
		// localPart derived form method name
		Assert.assertEquals( "testRequest", requestQname.getLocalPart());
		// namespace uses default value from @PayloadRoot
		Assert.assertEquals( "http://foo.bar/", requestQname.getNamespaceURI());

		// localPart derived form method name
		Assert.assertEquals("testResponse", responseQname.getLocalPart());
		// namespace uses default value from @PayloadRoot
		Assert.assertEquals( "http://foo.bar/", responseQname.getNamespaceURI());
	}

	@Test
	public void testGetQNameFromRequestPayloadParameter_fun() throws NoSuchMethodException {
		Method underTest = PayloadRootUtilsTest.class.getMethod("fun", TestRequestType.class);
		Assert.assertNotNull(underTest);
		MethodParameter parameter = new MethodParameter(underTest, 0);
		MethodParameter returnType = new MethodParameter(underTest, -1);
		QName requestQname = PayloadRootUtils.getRequestPayloadQName(parameter);
		QName responseQname = PayloadRootUtils.getResponsePayloadQName(returnType);
		Assert.assertEquals( "testRequest", requestQname.getLocalPart());
		Assert.assertEquals( "http://foo.bar/", requestQname.getNamespaceURI());

		Assert.assertEquals("testResponse", responseQname.getLocalPart());
		Assert.assertEquals( "http://foo.bar/", responseQname.getNamespaceURI());
	}

	@Test
	public void testGetQNameFromRequestPayloadParameter_fun2() throws NoSuchMethodException {
		Method underTest = PayloadRootUtilsTest.class.getMethod("fun2", TestRequestType.class);
		Assert.assertNotNull(underTest);
		MethodParameter parameter = new MethodParameter(underTest, 0);
		MethodParameter returnType = new MethodParameter(underTest, -1);
		QName requestQname = PayloadRootUtils.getRequestPayloadQName(parameter);
		QName responseQname = PayloadRootUtils.getResponsePayloadQName(returnType);
		Assert.assertEquals( "funkyRequest", requestQname.getLocalPart());
		Assert.assertEquals( "http://foo.bar/", requestQname.getNamespaceURI());

		Assert.assertEquals("funkyResponse", responseQname.getLocalPart());
		Assert.assertEquals( "http://foo.bar/", responseQname.getNamespaceURI());
	}

	public @ResponsePayload
	TestResponseType testIAE(@RequestPayload TestRequestType myRequestType) {
		return new TestResponseType();
	}

	@PayloadRoot(namespace = "http://foo.bar/", localPart = "testRequest")
	public @ResponsePayload
	TestResponseType test(@RequestPayload TestRequestType myRequestType) {
		return new TestResponseType();
	}

	public @ResponsePayload(namespace = "http://foo.bar/", localPart = "testResponse")
	TestResponseType fun(@RequestPayload(namespace = "http://foo.bar/", localPart = "testRequest") TestRequestType myRequestType) {
		return new TestResponseType();
	}

	public @ResponsePayload(namespace = "http://foo.bar/", localPart = "funkyResponse")
	TestResponseType fun2(@RequestPayload(namespace = "http://foo.bar/", localPart = "funkyRequest") TestRequestType myRequestType) {
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
		Assert.assertNotNull("getQNameForNode returns null", qName);
		Assert.assertEquals("QName has invalid localname", "localname", qName.getLocalPart());
		Assert.assertEquals("Qname has invalid namespace", "namespace", qName.getNamespaceURI());
		Assert.assertEquals("Qname has invalid prefix", "prefix", qName.getPrefix());
	}

	@Test
	public void testGetQNameForStaxSourceStreamReader() throws Exception {
		String contents = "<prefix:localname xmlns:prefix='namespace'/>";
		XMLInputFactory inputFactory = XMLInputFactoryUtils.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(contents));
		Source source = StaxUtils.createStaxSource(streamReader);
		QName qName = PayloadRootUtils.getPayloadRootQName(source, TransformerFactoryUtils.newInstance());
		Assert.assertNotNull("getQNameForNode returns null", qName);
		Assert.assertEquals("QName has invalid localname", "localname", qName.getLocalPart());
		Assert.assertEquals("Qname has invalid namespace", "namespace", qName.getNamespaceURI());
		Assert.assertEquals("Qname has invalid prefix", "prefix", qName.getPrefix());
	}

	@Test
	public void testGetQNameForStaxSourceEventReader() throws Exception {
		String contents = "<prefix:localname xmlns:prefix='namespace'/>";
		XMLInputFactory inputFactory = XMLInputFactoryUtils.newInstance();
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(contents));
		Source source = StaxUtils.createStaxSource(eventReader);
		QName qName = PayloadRootUtils.getPayloadRootQName(source, TransformerFactoryUtils.newInstance());
		Assert.assertNotNull("getQNameForNode returns null", qName);
		Assert.assertEquals("QName has invalid localname", "localname", qName.getLocalPart());
		Assert.assertEquals("Qname has invalid namespace", "namespace", qName.getNamespaceURI());
		Assert.assertEquals("Qname has invalid prefix", "prefix", qName.getPrefix());
	}

	@Test
	public void testGetQNameForStreamSource() throws Exception {
		String contents = "<prefix:localname xmlns:prefix='namespace'/>";
		Source source = new StreamSource(new StringReader(contents));
		QName qName = PayloadRootUtils.getPayloadRootQName(source, TransformerFactoryUtils.newInstance());
		Assert.assertNotNull("getQNameForNode returns null", qName);
		Assert.assertEquals("QName has invalid localname", "localname", qName.getLocalPart());
		Assert.assertEquals("Qname has invalid namespace", "namespace", qName.getNamespaceURI());
		Assert.assertEquals("Qname has invalid prefix", "prefix", qName.getPrefix());
	}

	@Test
	public void testGetQNameForSaxSource() throws Exception {
		String contents = "<prefix:localname xmlns:prefix='namespace'/>";
		Source source = new SAXSource(new InputSource(new StringReader(contents)));
		QName qName = PayloadRootUtils.getPayloadRootQName(source, TransformerFactoryUtils.newInstance());
		Assert.assertNotNull("getQNameForNode returns null", qName);
		Assert.assertEquals("QName has invalid localname", "localname", qName.getLocalPart());
		Assert.assertEquals("Qname has invalid namespace", "namespace", qName.getNamespaceURI());
		Assert.assertEquals("Qname has invalid prefix", "prefix", qName.getPrefix());
	}

	@Test
	public void testGetQNameForNullSource() throws Exception {
		QName qName = PayloadRootUtils.getPayloadRootQName(null, TransformerFactoryUtils.newInstance());
		Assert.assertNull("Qname returned", qName);
	}
}
