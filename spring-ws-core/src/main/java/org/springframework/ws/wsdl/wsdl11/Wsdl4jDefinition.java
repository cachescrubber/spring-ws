/*
 * Copyright 2005-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ws.wsdl.wsdl11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Input;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import org.w3c.dom.Document;

import org.springframework.core.NestedRuntimeException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.wsdl.WsdlDefinitionException;

//import com.sun.xml.internal.bind.api.impl.NameConverter;

/**
 * Implementation of the {@code Wsdl11Definition} based on WSDL4J. A {@link javax.wsdl.Definition} can be given as
 * as constructor argument, or set using a property.
 *
 * @author Arjen Poutsma
 * @author Greg Turnquist
 * @see #Wsdl4jDefinition(javax.wsdl.Definition)
 * @see #setDefinition(javax.wsdl.Definition)
 * @since 1.0.0
 */
public class Wsdl4jDefinition implements Wsdl11Definition {

	private Definition definition;

	/**
	 * WSDL4J is not thread safe, hence the need for a monitor.
	 */
	private final Object monitor = new Object();

	/**
	 * Constructs a new, empty {@code Wsdl4jDefinition}.
	 *
	 * @see #setDefinition(javax.wsdl.Definition)
	 */
	public Wsdl4jDefinition() {
	}

	/**
	 * Constructs a new {@code Wsdl4jDefinition} based on the given {@code Definition}.
	 *
	 * @param definition the WSDL4J definition
	 */
	public Wsdl4jDefinition(Definition definition) {
		setDefinition(definition);
	}

	/**
	 * Returns the WSDL4J {@code Definition}.
	 */
	public Definition getDefinition() {
		synchronized (monitor) {
			return definition;
		}
	}

	/**
	 * Set the WSDL4J {@code Definition}.
	 */
	public void setDefinition(Definition definition) {
		synchronized (monitor) {
			this.definition = definition;
		}
	}

	@Override
	public Source getSource() {
		synchronized (monitor) {
			Assert.notNull(definition, "definition must not be null");
			try {
				WSDLFactory wsdlFactory = WSDLFactory.newInstance();
				WSDLWriter wsdlWriter = wsdlFactory.newWSDLWriter();
				Document document = wsdlWriter.getDocument(definition);
				return new DOMSource(document);
			}
			catch (WSDLException ex) {
				throw new WsdlDefinitionException(ex.getMessage(), ex);
			}
		}
	}


	public static String namespace2package(String xmlNamespace, String defaultPackage) {
		// Ups - ancient internal jaxb api..
//		NameConverter nameConverter = new NameConverter.Standard();
//		return nameConverter.toPackageName(xmlNamespace);
		return defaultPackage;
	}

	public List<JavaFile> generateSimpleInterfaceAndAnnotatedImpl(String packageName) {
		Service service = null;
		Map services = definition.getServices();
		for (Object key : services.keySet()) {
			service = (Service) services.get(key);
			break;
		}

		Assert.notNull(service, "No service detected. Did you forgot afterPropertiesSet?");

		Map portTypes = definition.getPortTypes();
		PortType portType = null;
		for (Object key : portTypes.keySet()) {
			portType = (PortType) portTypes.get(key);
			break;
		}

		Assert.notNull(portType, "No portType detected. Did you forgot afterPropertiesSet?");

		List<MethodSpec> interfaceMethods = new ArrayList<>();
		List<MethodSpec> endpointMethods = new ArrayList<>();
		List<TypeSpec> exceptions = new ArrayList<>();
		String interfacePackage = packageName + ".wsdl";
		String endpointPackage = packageName + ".endpoint";

		List<Operation> operations = portType.getOperations();
		for (Operation operation : operations) {
			Input input = operation.getInput();
			Output output = operation.getOutput();
			Map faults = operation.getFaults();

			ClassName outputClass = ClassName.get(namespace2package(output.getMessage().getQName().getNamespaceURI(), packageName), output.getMessage().getQName().getLocalPart());
			ClassName inputClass = ClassName.get(namespace2package(input.getMessage().getQName().getNamespaceURI(), packageName), input.getMessage().getQName().getLocalPart());

			MethodSpec.Builder interfaceMethodBuilder = MethodSpec.methodBuilder(StringUtils.uncapitalize(operation.getName()))
					.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
					.returns(outputClass)
					.addParameter(ParameterSpec.builder(inputClass, StringUtils.uncapitalize(input.getMessage().getQName().getLocalPart()))
							.build());

			MethodSpec.Builder endpointMethodBuilder = MethodSpec.methodBuilder(StringUtils.uncapitalize(operation.getName()))
					.addModifiers(Modifier.PUBLIC)
					.addAnnotation(AnnotationSpec.builder(PayloadRoot.class)
							.addMember("namespace", "$S", input.getMessage().getQName().getNamespaceURI())
							.addMember("localPart", "$S", input.getMessage().getQName().getLocalPart())
							.build())
					.addAnnotation(AnnotationSpec.builder(ResponsePayload.class)
//							.addMember("namespace", "$S", output.getMessage().getQName().getNamespaceURI())
//							.addMember("localPart", "$S", output.getMessage().getQName().getLocalPart())
							.build())
					.returns(outputClass)
					.addParameter(ParameterSpec.builder(inputClass, StringUtils.uncapitalize(input.getMessage().getQName().getLocalPart()))
							.addAnnotation(AnnotationSpec.builder(RequestPayload.class)
//									.addMember("namespace", "$S", input.getMessage().getQName().getNamespaceURI())
//									.addMember("localPart", "$S", input.getMessage().getQName().getLocalPart())
									.build())
							.build())
					.addStatement("$T result = new $T()", outputClass, outputClass)
					.addComment("$L", "TODO: add implementation")
					.addStatement("return result");

			for (Object key : faults.keySet()) {
				Fault fault = (Fault) faults.get(key);
				ClassName exceptionClass = ClassName.get(interfacePackage, fault.getMessage().getQName().getLocalPart() + "Exception");
				ClassName faultType = ClassName.get(packageName, fault.getMessage().getQName().getLocalPart());
				TypeSpec.Builder excBuilder = TypeSpec.classBuilder(exceptionClass)
						.superclass(NestedRuntimeException.class)
						.addField(FieldSpec.builder(faultType, "faultInfo").build())
						.addMethod(MethodSpec.constructorBuilder()
								.addModifiers(Modifier.PUBLIC)
								.addParameter(String.class, "msg")
								.addParameter(faultType, "faultInfo")
								.addStatement("super(msg)")
								.addStatement("this.faultInfo = faultInfo")
								.build())
						.addMethod(MethodSpec.methodBuilder("getFaultInfo")
								.returns(faultType)
								.addStatement("return faultInfo")
								.build());
				interfaceMethodBuilder.addException(exceptionClass);
				endpointMethodBuilder.addException(exceptionClass);
				exceptions.add(excBuilder.build());
			}
			interfaceMethods.add(interfaceMethodBuilder.build());
			endpointMethods.add(endpointMethodBuilder.build());
		}

		ClassName interfaceClassName = ClassName.get(interfacePackage, service.getQName().getLocalPart() + "Operations");
		TypeSpec serviceInterface = TypeSpec.interfaceBuilder(interfaceClassName)
				.addModifiers(Modifier.PUBLIC)
				.addMethods(interfaceMethods)
				.build();

		ClassName endpointClassName = ClassName.get(endpointPackage, service.getQName().getLocalPart() + "Endpoint");
		TypeSpec serviceEndpoint = TypeSpec.classBuilder(endpointClassName)
				.addSuperinterface(interfaceClassName)
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Endpoint.class)
				.addMethods(endpointMethods)
				.build();

		List<JavaFile> result = new ArrayList<>();
		for (TypeSpec exceptionSpec : exceptions) {
			result.add(JavaFile.builder(interfacePackage, exceptionSpec)
					.indent("\t")
					.build());
		}
		result.add(JavaFile.builder(interfacePackage, serviceInterface)
				.indent("\t")
				.build());
		result.add(JavaFile.builder(endpointPackage, serviceEndpoint)
				.indent("\t")
				.build());

		return result;

	}

	public List<JavaFile> generateAnnotatedInterfaceAndSimpleImpl(String packageName) {
		Service service = null;
		Map services = definition.getServices();
		for (Object key : services.keySet()) {
			service = (Service) services.get(key);
			break;
		}

		Assert.notNull(service, "No service detected. Did you forgot afterPropertiesSet?");

		Map portTypes = definition.getPortTypes();
		PortType portType = null;
		for (Object key : portTypes.keySet()) {
			portType = (PortType) portTypes.get(key);
			break;
		}

		Assert.notNull(portType, "No portType detected. Did you forgot afterPropertiesSet?");

		List<MethodSpec> interfaceMethods = new ArrayList<>();
		List<MethodSpec> endpointMethods = new ArrayList<>();
		String endpointPackage = packageName + ".endpoint";
		String interfacePackage = packageName + ".wsdl";
		List<TypeSpec> exceptions = new ArrayList<>();

		List<Operation> operations = portType.getOperations();
		for (Operation operation : operations) {
			Input input = operation.getInput();
			Output output = operation.getOutput();
			Map faults = operation.getFaults();

			ClassName outputClass = ClassName.get(namespace2package(output.getMessage().getQName().getNamespaceURI(), packageName), output.getMessage().getQName().getLocalPart());
			ClassName inputClass = ClassName.get(namespace2package(input.getMessage().getQName().getNamespaceURI(), packageName), input.getMessage().getQName().getLocalPart());

			MethodSpec.Builder interfaceMethodBuilder = MethodSpec.methodBuilder(StringUtils.uncapitalize(operation.getName()))
					.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
					.addAnnotation(AnnotationSpec.builder(PayloadRoot.class)
							.addMember("namespace", "$S", input.getMessage().getQName().getNamespaceURI())
							.addMember("localPart", "$S", input.getMessage().getQName().getLocalPart())
							.build())
					.addAnnotation(AnnotationSpec.builder(ResponsePayload.class)
//							.addMember("namespace", "$S", output.getMessage().getQName().getNamespaceURI())
//							.addMember("localPart", "$S", output.getMessage().getQName().getLocalPart())
							.build())
					.returns(outputClass)
					.addParameter(ParameterSpec.builder(inputClass, StringUtils.uncapitalize(input.getMessage().getQName().getLocalPart()))
							.addAnnotation(AnnotationSpec.builder(RequestPayload.class)
//									.addMember("namespace", "$S", input.getMessage().getQName().getNamespaceURI())
//									.addMember("localPart", "$S", input.getMessage().getQName().getLocalPart())
									.build())
							.build());

			MethodSpec.Builder endpointMethodBuilder = MethodSpec.methodBuilder(StringUtils.uncapitalize(operation.getName()))
					.addModifiers(Modifier.PUBLIC)
					.returns(outputClass)
					.addParameter(ParameterSpec.builder(inputClass, StringUtils.uncapitalize(input.getMessage().getQName().getLocalPart()))
							.build())
					.addStatement("$T result = new $T()", outputClass, outputClass)
					.addComment("$L", "TODO: add implementation")
					.addStatement("return result");

			for (Object key : faults.keySet()) {
				Fault fault = (Fault) faults.get(key);
				ClassName exceptionClass = ClassName.get(interfacePackage, fault.getMessage().getQName().getLocalPart() + "Exception");
				ClassName faultType = ClassName.get(packageName, fault.getMessage().getQName().getLocalPart());
				TypeSpec.Builder excBuilder = TypeSpec.classBuilder(exceptionClass)
						.superclass(NestedRuntimeException.class)
						.addField(FieldSpec.builder(faultType, "faultInfo")
								.addModifiers(Modifier.PRIVATE)
								.build())
						.addMethod(MethodSpec.constructorBuilder()
								.addModifiers(Modifier.PUBLIC)
								.addParameter(String.class, "msg")
								.addParameter(faultType, "faultInfo")
								.addStatement("super(msg)")
								.addStatement("this.faultInfo = faultInfo")
								.build())
						.addMethod(MethodSpec.methodBuilder("getFaultInfo")
								.addModifiers(Modifier.PUBLIC)
								.returns(faultType)
								.addStatement("return faultInfo")
								.build());
				interfaceMethodBuilder.addException(exceptionClass);
				endpointMethodBuilder.addException(exceptionClass);
				exceptions.add(excBuilder.build());
			}
			interfaceMethods.add(interfaceMethodBuilder.build());
			endpointMethods.add(endpointMethodBuilder.build());
		}

		ClassName interfaceClassName = ClassName.get(interfacePackage, service.getQName().getLocalPart() + "Operations");
		ClassName objectFactory = ClassName.get(packageName, "ObjectFactory");
		TypeSpec serviceInterface = TypeSpec.interfaceBuilder(interfaceClassName)
				.addAnnotation(AnnotationSpec.builder(XmlSeeAlso.class)
						.addMember("value", "$T.class", objectFactory)
						.build())
				.addModifiers(Modifier.PUBLIC)
				.addMethods(interfaceMethods)
				.build();

		ClassName endpointClassName = ClassName.get(endpointPackage, service.getQName().getLocalPart() + "Endpoint");
		TypeSpec serviceEndpoint = TypeSpec.classBuilder(endpointClassName)
				.addSuperinterface(interfaceClassName)
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Endpoint.class)
				.addMethods(endpointMethods)
				.build();

		List<JavaFile> result = new ArrayList<>();
		for (TypeSpec exceptionSpec : exceptions) {
			result.add(JavaFile.builder(interfacePackage, exceptionSpec)
					.indent("\t")
					.build());
		}
		result.add(JavaFile.builder(interfacePackage, serviceInterface)
				.indent("\t")
				.build());
//		result.add(JavaFile.builder(endpointPackage, serviceEndpoint)
//				.indent("\t")
//				.build());
		return result;
	}


	public String toString() {
		StringBuilder builder = new StringBuilder("Wsdl4jDefinition");
		if (definition != null && StringUtils.hasLength(definition.getTargetNamespace())) {
			builder.append('{');
			builder.append(definition.getTargetNamespace());
			builder.append('}');
		}
		return builder.toString();
	}
}
