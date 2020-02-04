package org.springframework.ws.wsdl.wsdl11;

import java.nio.file.Paths;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import com.squareup.javapoet.JavaFile;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.xml.transform.TransformerFactoryUtils;
import org.springframework.xml.xsd.SimpleXsdSchema;

/**
 * @author Lars Uffmann lars.uffmann@vcsys.de
 * @since 2020-01-02
 */
public class CodeGenTest {

	@Test
	public void testCodeGen() throws Exception {
		ClassPathResource classPathResource = new ClassPathResource("org/springframework/ws/wsdl/wsdl11/single-xmltype.xsd");
		org.junit.Assert.assertTrue(classPathResource.exists());

		DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
		definition.setPortTypeName("Order");
		SimpleXsdSchema schema = new SimpleXsdSchema(classPathResource);
		schema.afterPropertiesSet();

		definition.setSchema(schema);
		definition.afterPropertiesSet();

		ProviderBasedWsdl4jDefinition delegate = definition.getDelegate();

		List<JavaFile> javaFiles = delegate.generateAnnotatedInterfaceAndSimpleImpl("com.example.service");
		for (JavaFile javaFile : javaFiles) {
			javaFile.writeTo(System.out);
			javaFile.writeTo(Paths.get("spring-ws-core/target/generated-sources"));
		}

		TransformerFactory transformerFactory = TransformerFactoryUtils.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.transform(definition.getSource(), new StreamResult(System.out));
	}
}
