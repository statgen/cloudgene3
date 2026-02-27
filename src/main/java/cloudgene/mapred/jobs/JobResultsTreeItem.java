package cloudgene.mapred.jobs;

import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.ArrayList;
import java.util.List;

@JsonClassDescription
public class JobResultsTreeItem {

	private String name = "";

	private String path = "";

	private String hash = "";

	private String size;

	private boolean folder = true;

	private List<JobResultsTreeItem> children = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public List<JobResultsTreeItem> getChildren() {
		return children;
	}

	public void setChildren(List<JobResultsTreeItem> children) {
		this.children = children;
	}

	public void setFolder(boolean folder) {
		this.folder = folder;
	}

	public boolean isFolder() {
		return folder;
	}
}
