package hudson.plugins.globalenvvar;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jvnet.hudson.test.HudsonTestCase;

import hudson.EnvVars;
import hudson.model.BallColor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.Recorder;

public class GlobalEnvironmentVariablePublisherTest extends HudsonTestCase {

    public void testOnEmptyGlobalEnvironmentVariables() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject project = super.createFreeStyleProject();
		Recorder environmentVariablePublisher = new GlobalEnvironmentVariablePublisher("param1=value1\n" +
				"param2=$param1/val2\n" +
				"param3=$nonexistentVar");
		project.addPublisher(environmentVariablePublisher);
		// rub build
		FreeStyleBuild build = project.scheduleBuild2(0).get();

		// assert build status
		assertEquals(BallColor.BLUE, build.getResult().getColor());
		// assert global environment variables are created
		List<EnvironmentVariablesNodeProperty> environmentVariablesNodePropertyList = super.hudson.getGlobalNodeProperties().getAll(EnvironmentVariablesNodeProperty.class);

		assertFalse(environmentVariablesNodePropertyList.isEmpty());
		EnvVars envVars = environmentVariablesNodePropertyList.get(0).getEnvVars();
		assertEquals(3, envVars.size());

		String value = envVars.get("param1");
		assertNotNull(value, "missing param1 key");
		assertEquals("value1", value);

		value = envVars.get("param2");
		assertNotNull(value, "missing param2 key");
		assertEquals("value1/val2", value);

		value = envVars.get("param3");
		assertNotNull(value, "missing param3 key");
		assertEquals("$nonexistentVar", value);
    }

    public void testOnNotEmptyGlobalEnvironmentVariables() throws IOException, ExecutionException, InterruptedException {
		// add global variable
		EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
		EnvVars envVars = prop.getEnvVars();
		envVars.put("globalVar", "globalValue");
		super.hudson.getGlobalNodeProperties().add(prop);

        FreeStyleProject project = super.createFreeStyleProject();
		Recorder environmentVariablePublisher = new GlobalEnvironmentVariablePublisher("param1=$globalVar/$param2/val1\n" +
				"param2=$param3/val2\n" +
				"param3=value3");
		project.addPublisher(environmentVariablePublisher);

		// rub build
		FreeStyleBuild build = project.scheduleBuild2(0).get();

		// assert build status
		assertEquals(BallColor.BLUE, build.getResult().getColor());
		// assert global environment variables are created
		List<EnvironmentVariablesNodeProperty> environmentVariablesNodePropertyList = super.hudson.getGlobalNodeProperties().getAll(EnvironmentVariablesNodeProperty.class);

		assertFalse(environmentVariablesNodePropertyList.isEmpty());
		EnvVars updatedEnvVars = environmentVariablesNodePropertyList.get(0).getEnvVars();
		assertEquals(4, updatedEnvVars.size());

		String value = updatedEnvVars.get("param1");
		assertNotNull(value, "missing param1 key");
		assertEquals("globalValue/value3/val2/val1", value);

		value = updatedEnvVars.get("param2");
		assertNotNull(value, "missing param2 key");
		assertEquals("value3/val2", value);

		value = updatedEnvVars.get("param3");
		assertNotNull(value, "missing param3 key");
		assertEquals("value3", value);

		value = updatedEnvVars.get("globalVar");
		assertNotNull(value, "missing globalVar key");
		assertEquals("globalValue", value);
    }

	public void testGlobalEnvironmentVariablesUpdate() throws IOException, ExecutionException, InterruptedException {
		// add global variable
		EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
		EnvVars envVars = prop.getEnvVars();
		envVars.put("globalVar", "globalValue");
		super.hudson.getGlobalNodeProperties().add(prop);

		FreeStyleProject project = super.createFreeStyleProject();
		Recorder environmentVariablePublisher = new GlobalEnvironmentVariablePublisher("param1=test\n" +
				"globalVar=$param1/newVal");
		project.addPublisher(environmentVariablePublisher);

		// rub build
		FreeStyleBuild build = project.scheduleBuild2(0).get();

		// assert build status
		assertEquals(BallColor.BLUE, build.getResult().getColor());
		// assert global environment variables are created
		List<EnvironmentVariablesNodeProperty> environmentVariablesNodePropertyList = super.hudson.getGlobalNodeProperties().getAll(EnvironmentVariablesNodeProperty.class);

		assertFalse(environmentVariablesNodePropertyList.isEmpty());
		EnvVars updatedEnvVars = environmentVariablesNodePropertyList.get(0).getEnvVars();
		assertEquals(2, updatedEnvVars.size());

		String value = updatedEnvVars.get("param1");
		assertNotNull(value, "missing param1 key");
		assertEquals("test", value);

		value = updatedEnvVars.get("globalVar");
		assertNotNull(value, "missing globalVar key");
		assertEquals("test/newVal", value);
	}
}
