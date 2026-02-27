package cloudgene.mapred.core;

import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.ArrayList;
import java.util.List;

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
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		Group g = (Group) obj;
		return g.getName().equalsIgnoreCase(getName());
	}
}
