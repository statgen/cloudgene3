package cloudgene.mapred.core;

import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonClassDescription
public class Group {

	private String name;

	private List<String> apps = new ArrayList<>();

	public Group(String name) {
		this.name = name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setApps(List<String> apps) {
		this.apps = apps;
	}

	public List<String> getApps() {
		return apps;
	}

	public void addApp(String app) {
		apps.add(app);
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Group group)) return false;
		return Objects.equals(name, group.name);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}
}
