package hudson.plugins.globalenvvar;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.DescribableList;

/**
 * This plugin is intended to update Hudson Global Properties -> Environment variables with variables defined by user in plugin configuration.
 * Plugin expects variables in java property file format, other environment variables can be used as values e.g.:<br/>
 * <code>
 * name1=value1<br/>
 * name2=$name1-value2
 * </code>
 */
public class GlobalEnvironmentVariablePublisher extends Recorder {

	private static final String LOG_PREFIX = "[GlobalEnvironmentVariableUpdate] ";
	private BuildListener listener;
	private String variablesText;


	@DataBoundConstructor
	public GlobalEnvironmentVariablePublisher(String variablesText) {
		this.variablesText = Util.fixEmpty(variablesText);
	}

	public String getVariablesText() {
		return variablesText;
	}

	public void setVariablesText(String variablesText) {
		this.variablesText = variablesText;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		this.listener = listener;
		printLog("Evaluating environment variables");
		Map<String, String> envProps = parsePropertiesString(variablesText);
		EnvVars currentEnvironment = build.getEnvironment(listener);
		// resolve variables against each other
		int maxDepth = 5;
		while (maxDepth > 0) {
			EnvVars.resolve(envProps);
			maxDepth--;
		}

		final Hudson instance = Hudson.getInstance();
		final DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = instance.getGlobalNodeProperties();
		List<EnvironmentVariablesNodeProperty> envVarsNodePropertyList = globalNodeProperties.getAll(EnvironmentVariablesNodeProperty.class);
		EnvVars envVars;

		if (envVarsNodePropertyList == null || envVarsNodePropertyList.size() == 0) {
			EnvironmentVariablesNodeProperty newEnvVarsNodeProperty = new EnvironmentVariablesNodeProperty();
			globalNodeProperties.add(newEnvVarsNodeProperty);
			envVars = newEnvVarsNodeProperty.getEnvVars();
		} else {
			envVars = envVarsNodePropertyList.get(0).getEnvVars();
		}

		for (Map.Entry<String, String> mapEntry : envProps.entrySet()) {
			String value = mapEntry.getValue();
			// resolve variable against current environment configuration
			value = Util.replaceMacro(value, currentEnvironment);
			envVars.put(mapEntry.getKey(), value);
			printLog(mapEntry.getKey() + "=" + value);
		}
		instance.save();
		printLog("Updated global environment variables");
		return true;
	}

	public Map<String, String> parsePropertiesString(String propertiesString) {
		final Properties properties = new Properties();
		try {
			properties.load(new StringReader(propertiesString));
		} catch (IOException e) {
			printLog("Could not parse environment variables");
			e.printStackTrace();
		}
		Map<String, String> propertiesMap = new HashMap<String, String>(properties.size());
		for (Entry<Object, Object> prop : properties.entrySet()) {
			String key = prop.getKey().toString();
			String value = prop.getValue().toString();
			if (!key.isEmpty() && !value.isEmpty()) {
				propertiesMap.put(key, value);
			}
		}
		return propertiesMap;
	}

	private void printLog(String message) {
		listener.getLogger().println(LOG_PREFIX + message);
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		@Override
		public String getDisplayName() {
			return Messages.GlobalEnvironmentVariablePublisher_DisplayName();
		}

		@Override
		public boolean isApplicable(Class jobType) {
			return true;
		}
	}

}
