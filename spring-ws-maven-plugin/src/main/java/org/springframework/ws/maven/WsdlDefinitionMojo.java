package org.springframework.ws.maven;

import java.io.File;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.transform.TransformerFactoryUtils;
import org.springframework.xml.xsd.SimpleXsdSchema;

/**
 * Goal which touches a timestamp file.
 **/
@Mojo(name = "generate-wsdl", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class WsdlDefinitionMojo extends AbstractMojo {
	/**
	 * Location of the file.
	 *
	 * @parameter expression=
	 * @required
	 */
	@Parameter(defaultValue = "${project.build.directory}/generated-resources/sws")
	private File outputDirectory;

	@Parameter(required = true, defaultValue = "${project.basedir}/src/main/resources/schema.xsd")
	private File schema;

	@Parameter(required = true)
	private String portTypeName;

	@Parameter
	private String serviceName;

	@Parameter(defaultValue = "${project}")
	private MavenProject project;

	public void execute()
			throws MojoExecutionException {


		org.apache.maven.model.Resource generated = new org.apache.maven.model.Resource();
		generated.setFiltering(true);
		generated.setDirectory(outputDirectory.getPath());
		project.addResource(generated);

		DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
		Resource xsdResource = new FileSystemResource(schema);

//		XsdSchemaCollection schemaCollection = new CommonsXsdSchemaCollection()

		SimpleXsdSchema schema = new SimpleXsdSchema(xsdResource);
		definition.setSchema(schema);
		definition.setPortTypeName(portTypeName);
		if (StringUtils.hasText(serviceName)) {
			definition.setServiceName(serviceName);
		}

		try {
			schema.afterPropertiesSet();
			definition.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new MojoExecutionException("error initializing schema definition", e);
		}

		File f = new File(outputDirectory, "wsdl");
		if (!f.exists()) {
			f.mkdirs();
		}

		File generatedWsdl = new File(f, "generated.wsdl");

		try {
			TransformerFactory transformerFactory = TransformerFactoryUtils.newInstance();
			Transformer transformer = null;
			transformer = transformerFactory.newTransformer();
			transformer.transform(definition.getSource(), new StreamResult(generatedWsdl));
		}
		catch (TransformerException e) {
			e.printStackTrace();
		}

	}
}
