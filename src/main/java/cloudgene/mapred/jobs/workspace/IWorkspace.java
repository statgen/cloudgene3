package cloudgene.mapred.jobs.workspace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import cloudgene.mapred.jobs.Download;

public interface IWorkspace {

	void setJob(String job);

	void setup() throws IOException;

	String upload(String id, File file) throws IOException;

	String uploadInput(String id, File file) throws IOException;

	String uploadLog(File file) throws IOException;

	InputStream download(String url) throws IOException;

	void delete(String job) throws IOException;

	String getName();

	String createPublicLink(String url);

	String getParent(String url);

	String createFolder(String id);

	String createFile(String name, String name2);

	String createLogFile(String name);

	String createTempFolder(String string);

	List<Download> getDownloads(String url) throws IOException;

	List<Download> getLogs() throws IOException;

	void cleanup(String job) throws IOException;

	boolean exists(String path) throws IOException;

	String downloadLog(String string) throws IOException;

}
