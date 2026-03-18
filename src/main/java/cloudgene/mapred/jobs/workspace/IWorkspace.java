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

	/**
	 * Returns the "parent folder" of the given {@code id}. May return {@code null}
	 * if a distinct parent cannot be computed.
	 *
	 * @param id Hierarchical file identifier (file path, S3 URI...)
	 * @return The parent ID, in the same style as the input {@code id} (file path,
	 *         S3 URI...)
	 */
	String getParent(String id);

	/**
	 * Returns an identifier (filepath, S3 URI...) for a persistent folder within
	 * the workspace, unique to the provided {@code id}. Guarantees that data can be
	 * inserted into the folder, but may not create anything.
	 *
	 * @param id Unique identifier for the created folder (essentially the
	 *           basename).
	 */
	String createFolder(String id);

	/**
	 * Returns an identifier (filepath, S3 URI...) for a persistent file within the
	 * workspace and {@code folder}, unique to the provided {@code id}. Guarantees
	 * that such a file can be created without additional steps, but may not create
	 * anything.
	 *
	 * @param folder Unique identifier for the parent folder.
	 * @param id     Unique identifier for the created file (essentially the
	 *               basename).
	 */
	String createFile(String folder, String id);

	/**
	 * Returns an identifier (filepath, S3 URI...) for a log file within the
	 * workspace, unique to the provided {@code name}. Guarantees that such a file
	 * can be created without additional steps, but may not create anything.
	 *
	 * @param name Unique identifier for the created log file.
	 */
	String createLogFile(String name);

	/**
	 * Returns an identifier (filepath, S3 URI...) for a temp folder within the
	 * workspace, unique to the provided {@code id}. Guarantees that data can be
	 * inserted into the folder, but may not create anything.
	 *
	 * @param id Unique identifier for the created temp folder (essentially the
	 *           basename).
	 */
	String createTempFolder(String id);

	List<Download> getDownloads(String url) throws IOException;

	List<Download> getLogs() throws IOException;

	void cleanup(String job) throws IOException;

	boolean exists(String path) throws IOException;

	String downloadLog(String string) throws IOException;

}
