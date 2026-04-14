package cloudgene.mapred.core;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import io.micronaut.core.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonClassDescription
public class Group {

	private @NonNull String name;

	private @NonNull List<String> apps = new ArrayList<>();

	public Group(@NonNull String name) {
		this.name = name;
	}

	public void setName(@NonNull String name) {
		this.name = name;
	}

	public @NonNull String getName() {
		return name;
	}

	public void setApps(@NonNull List<String> apps) {
		this.apps = apps;
	}

	public @NonNull List<String> getApps() {
		return apps;
	}

	public void addApp(String app) {
		apps.add(app);
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Group group)) return false;
		return name.equalsIgnoreCase(group.name);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name.toLowerCase());
	}
}
