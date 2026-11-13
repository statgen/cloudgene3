package cloudgene.mapred.plugins.nextflow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import cloudgene.mapred.util.BinaryFinder;
import cloudgene.mapred.util.config.Settings;
import cloudgene.mapred.util.command.Command;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NextflowBinary {
	private static final Logger log = LoggerFactory.getLogger(NextflowBinary.class);

	private String binary;
	private Path script;
	private String revision;
	private String profile;
	private Path paramsFile;
	private Path logFile;
	private String name;

	private final List<Path> configFiles = new ArrayList<>();
	private final List<Path> envScripts = new ArrayList<>();

	public static NextflowBinary build(Settings settings) {
		String binary = new BinaryFinder("nextflow")
				.settings(settings, "nextflow", "home")
				.env("NEXTFLOW_HOME")
				.envPath()
				.path("/usr/local/bin")
				.find();

		return new NextflowBinary(binary);
	}

	private NextflowBinary(@Nullable String binary) {
		this.binary = binary;
	}

	public @Nullable String getBinary() {
		return binary;
	}

	public boolean isInstalled() {
		if (binary != null) {
			return Files.exists(Path.of(binary));
		} else {
			return false;
		}
	}

	public @NonNull String getVersion() {
		if (isInstalled()) {
			Command command = new Command(binary, "info");
			StringBuffer stdout = new StringBuffer();
			command.writeStdout(stdout);
			command.setSilent(true);
			command.execute();
			return stdout.toString();
		} else {
			return "Nextflow not installed.";
		}
	}

	public void setBinary(String binary) {
		this.binary = binary;
	}

	public void setScript(Path script) {
		this.script = script;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	public void addConfig(Path configFile) {
		this.configFiles.add(configFile);
	}

	public void addConfig(String configFile) {
		this.configFiles.add(Path.of(configFile));
	}

	public void setParamsFile(Path paramsFile) {
		this.paramsFile = paramsFile;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public void setLogFile(Path logFile) {
		this.logFile = logFile;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addEnvScript(Path envScript) {
		this.envScripts.add(envScript);
	}

	public @NonNull List<String> buildCommand() {
		List<String> nextflow = new ArrayList<>();

		nextflow.add("PATH=$PATH:/usr/local/bin");

		for (Path envScript : envScripts) {
			if (envScript != null && Files.exists(envScript)) {
				nextflow.add("source " + envScript.toAbsolutePath() + ";");
			}
		}

		nextflow.add(getBinary());

		nextflow.add("-log");
		nextflow.add(logFile.toAbsolutePath().toString());

		nextflow.add("run");
		nextflow.add(script.toString());

		if (revision != null && !revision.isEmpty()) {
			nextflow.add("-r");
			nextflow.add(revision);
		}

		if (name != null) {
			nextflow.add("-name");
			nextflow.add(name);
		}

		// set profile
		if (profile != null && !profile.isEmpty()) {
			nextflow.add("-profile");
			nextflow.add(profile);
		}

		for (Path configFile : configFiles) {
			if (Files.exists(configFile)) {
				nextflow.add("-c");
				nextflow.add(configFile.toAbsolutePath().toString());
			}
		}

		nextflow.add("-params-file");
		nextflow.add(paramsFile.toAbsolutePath().toString());

		nextflow.add("-ansi-log");
		nextflow.add("false");

		String nextflowCommand = String.join(" ", nextflow);

		List<String> command = new ArrayList<>();
		command.add("/bin/bash");
		command.add("-c");
		command.add(nextflowCommand);

		return command;
	}
}
