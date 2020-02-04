package org.springframework.ws.maven;

import java.io.File;
import java.util.List;

import com.squareup.javapoet.JavaFile;
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
import org.springframework.xml.xsd.SimpleXsdSchema;

/**
 *
 */
@Mojo(name = "generate-endpoint-interface", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class EndpointInterfaceCodeGenMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project.build.directory}/generated-sources/sws")
	private File outputDirectory;

	@Parameter(required = true, defaultValue = "${project.basedir}/src/main/resources/schema.xsd")
	private File schema;

	@Parameter(required = true)
	private String portTypeName;

	@Parameter
	private String serviceName;

	@Parameter(required = true)
	private String packageName;

	@Parameter(defaultValue = "${project}")
	private MavenProject project;

	public void execute()
			throws MojoExecutionException {
		project.addCompileSourceRoot(outputDirectory.getPath());

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

		File f = outputDirectory;

		if (!f.exists()) {
			f.mkdirs();
		}

		try {
			List<JavaFile> javaFiles = definition.getDelegate().generateAnnotatedInterfaceAndSimpleImpl(packageName);
			for (JavaFile javaFile : javaFiles) {
				javaFile.writeTo(outputDirectory);
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException("error generating source code: " + e.getMessage(), e);
		}

	}

}
