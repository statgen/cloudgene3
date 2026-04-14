package cloudgene.mapred.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.*;

public final class S3Util {

	private S3Util() {
	}

	/**
	 * Separates an S3 URI into its bucket and key parts.
	 *
	 * @param bucket The S3 bucket this URI points at.
	 * @param key    The key / path inside the bucket pointing at this item.
	 */
	public record UriParts(String bucket, String key) {}

	private static S3AsyncClient s3;
	private static S3TransferManager tm;
	private static S3Presigner presigner;

	private static S3AsyncClient getS3Client() {
		if (s3 == null) {
			s3 = S3AsyncClient.create();
		}
		return s3;
	}

	private static S3TransferManager getTransferManager() {
		if (tm == null) {
			s3 = getS3Client();
			tm = S3TransferManager.builder().s3Client(s3).build();
		}
		return tm;
	}

	private static S3Presigner getPresigner() {
		if (presigner == null) {
			presigner = S3Presigner.create();
		}
		return presigner;
	}

	/**
	 * Separates the given S3 URI of form {@code s3://<bucket>/<key>} into the
	 * bucket and key, returned as a {@link UriParts} instance.
	 *
	 * @param uri S3 URI of form {@code s3://<bucket>/<key>}.
	 * @return The bucket and key as fields in a {@link UriParts} instance.
	 */
	public static UriParts getParts(String uri) {
		if (!uri.startsWith("s3://")) {
			throw new IllegalArgumentException("S3 URLs must start with 's3://'; found: '" + uri + "'");
		}
		uri = uri.replaceAll("s3://", "");

		String[] rawParts = uri.split("/", 2);

		if (rawParts.length != 2) {
			throw new IllegalArgumentException(
					"S3 URLs must contain at least one forward slash (/) separating the bucket from the key; found: '"
							+ uri + "'");
		}

		return new UriParts(rawParts[0], rawParts[1]);
	}

	/**
	 * Fetches an object from S3 as an input stream.
	 * <p>
	 * Returns a blocking {@link InputStream} that produces the object contents.
	 *
	 * @param uriParts Bucket and key indicating the S3 path to the desired object.
	 * @return A data stream producing the object contents.
	 */
	public static InputStream getObject(UriParts uriParts) throws IOException {
		return getObject(uriParts.bucket(), uriParts.key());
	}

	/**
	 * Fetches an object from S3 as an input stream.
	 * <p>
	 * Returns a blocking {@link InputStream} that produces the object contents.
	 *
	 * @param bucket S3 bucket the object will be downloaded from.
	 * @param key    Path within the S3 bucket the object will be downloaded from.
	 * @return A data stream producing the object contents.
	 */
	public static InputStream getObject(String bucket, String key) throws IOException {
		GetObjectRequest request = GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();

		S3AsyncClient s3 = getS3Client();
		CompletableFuture<ResponseInputStream<GetObjectResponse>> response = s3.getObject(
				request,
				AsyncResponseTransformer.toBlockingInputStream());

		try {
			return response.join();
		} catch (CancellationException | CompletionException | SdkException e) {
			throw new IOException("Failed to get object: " + bucket + "/" + key, e);
		}
	}

	/**
	 * Returns the requested S3 object's metadata without downloading the object.
	 * <p>
	 * If any errors are raised, including a missing {@code key}, returns
	 * {@code null}. Does not throw.
	 *
	 * @param uriParts Bucket and key indicating the S3 path to the desired object.
	 * @return The queried object's metadata, if present. Otherwise, {@code null}.
	 */
	public static HeadObjectResponse getObjectHead(UriParts uriParts) {
		return getObjectHead(uriParts.bucket(), uriParts.key());
	}

	/**
	 * Returns the requested S3 object's metadata without downloading the object.
	 * <p>
	 * If any errors are raised, including a missing {@code key}, returns
	 * {@code null}. Does not throw.
	 *
	 * @param bucket S3 bucket containing the queried object.
	 * @param key    Path within the S3 bucket identifying the queried object.
	 * @return The queried object's metadata, if present. Otherwise, {@code null}.
	 */
	public static HeadObjectResponse getObjectHead(String bucket, String key) {
		HeadObjectRequest request = HeadObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();

		try {
			S3AsyncClient s3 = getS3Client();
			CompletableFuture<HeadObjectResponse> future = s3.headObject(request);
			return future.join();
		} catch (CancellationException | CompletionException | SdkException e) {
			return null;
		}
	}

	/**
	 * Returns whether an object exists or not at the given S3 location.
	 *
	 * @param uriParts Bucket and key indicating the S3 path to the queried object.
	 * @return {@code true} if an object is found at the given S3 location;
	 *         {@code false} otherwise.
	 */
	public static boolean doesObjectExist(UriParts uriParts) {
		return doesObjectExist(uriParts.bucket(), uriParts.key());
	}

	/**
	 * Returns whether an object exists or not at the given S3 location.
	 *
	 * @param bucket S3 bucket containing the queried object.
	 * @param key    Path within the S3 bucket identifying the queried object.
	 * @return {@code true} if an object is found at the given S3 location;
	 *         {@code false} otherwise.
	 */
	public static boolean doesObjectExist(String bucket, String key) {
		return getObjectHead(bucket, key) != null;
	}

	public static URL generatePresignedLink(UriParts uriParts, Duration duration) {
		return generatePresignedLink(uriParts.bucket(), uriParts.key(), duration);
	}

	public static URL generatePresignedLink(String bucket, String key, Duration duration) {
		GetObjectRequest getRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.getObjectRequest(getRequest)
				.signatureDuration(duration)
				.build();

		S3Presigner presigner = getPresigner();
		PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);

		return presigned.url();
	}

	/**
	 * Returns {@code true} if {@code uri} is a valid S3 URI of form
	 * {@code s3://<bucket>/<key>};
	 * returns {@code false} otherwise.
	 */
	public static boolean isValidS3Uri(String uri) {
		try {
			getParts(uri);
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}

	/**
	 * Downloads the S3 object in {@code uri} into the given local {@code file}
	 * path. Blocking operation.
	 *
	 * @param uri  S3 URI pointing to a downloadable object.
	 * @param file Local path that the file should be downloaded into.
	 * @throws IOException If the download fails.ss
	 */
	public static void copyToFile(String uri, File file) throws IOException {
		UriParts uriParts = getParts(uri);
		copyToFile(uriParts.bucket(), uriParts.key(), file);
	}

	/**
	 * Downloads the S3 object in {@code s3://<bucket>/<key>} into the given local
	 * {@code file} path. Blocking operation.
	 *
	 * @param bucket S3 bucket to download from.
	 * @param key    Path within the S3 bucket identifying the object to download.
	 * @param file   Local path that the file should be downloaded into.
	 * @throws IOException If the download fails.
	 */
	public static void copyToFile(String bucket, String key, File file) throws IOException {
		S3TransferManager tm = getTransferManager();

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();

		DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
				.getObjectRequest(getObjectRequest)
				.destination(file)
				.build();

		try {
			FileDownload download = tm.downloadFile(downloadFileRequest);
			download.completionFuture().join();
		} catch (CancellationException | CompletionException | SdkException e) {
			throw new IOException("Failed to download file from S3: " + bucket + "/" + key, e);
		}
	}

	/**
	 * Uploads the contents of the provided {@code file} to the S3 bucket and key
	 * specified by the provided {@code uri}, of form {@code s3://<bucket>/<key>}.
	 * Blocking operation.
	 *
	 * @param file Local file to upload into S3.
	 * @param uri  Destination within S3 to upload the file to.
	 * @throws IOException If the upload fails.
	 */
	public static void copyToS3(File file, String uri) throws IOException {
		UriParts uriParts = getParts(uri);
		copyToS3(file, uriParts.bucket(), uriParts.key());
	}

	/**
	 * Uploads the provided {@code content} to a file in the S3 bucket and key
	 * specified by the provided {@code uri}, of form {@code s3://<bucket>/<key>}.
	 * Blocking operation.
	 *
	 * @param content Data to upload into S3.
	 * @param uri     Destination within S3 to upload the contents to.
	 * @throws IOException If the upload fails.
	 */
	public static void copyToS3(String content, String uri) throws IOException {
		UriParts uriParts = getParts(uri);
		copyToS3(content, uriParts.bucket(), uriParts.key());
	}

	/**
	 * Uploads the contents of the provided {@code file} to the provided S3
	 * {@code bucket}, at the path specified by the given {@code key}.
	 * Blocking operation.
	 *
	 * @param file   Local file to upload into S3.
	 * @param bucket S3 bucket the file will be uploaded to.
	 * @param key    Path within the S3 bucket the file will be uploaded to.
	 * @throws IOException If the upload fails.
	 */
	public static void copyToS3(File file, String bucket, String key) throws IOException {
		S3TransferManager tm = getTransferManager();

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();

		UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
				.source(file)
				.putObjectRequest(putObjectRequest)
				.build();

		try {
			FileUpload upload = tm.uploadFile(uploadFileRequest);
			upload.completionFuture().join();
		} catch (CancellationException | CompletionException | SdkException e) {
			throw new IOException("Failed to upload file '" + file.getPath() + "' to " + bucket + "/" + key, e);
		}
	}

	/**
	 * Uploads the provided {@code contents} to the provided S3
	 * {@code bucket}, at the path specified by the given {@code key}.
	 * Blocking operation.
	 *
	 * @param content Data to upload into S3.
	 * @param bucket  S3 bucket the file will be uploaded to.
	 * @param key     Path within the S3 bucket the file will be uploaded to.
	 * @throws IOException If the upload fails.
	 */
	public static void copyToS3(String content, String bucket, String key) throws IOException {
		S3AsyncClient s3 = getS3Client();

		PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();

		AsyncRequestBody body = AsyncRequestBody.fromString(content);

		try {
			CompletableFuture<PutObjectResponse> future = s3.putObject(request, body);
			future.join();
		} catch (CancellationException | CompletionException | SdkException e) {
			throw new IOException("Failed to upload content to " + bucket + "/" + key, e);
		}
	}

	public static List<S3Object> listObjects(UriParts uriParts) {
		return listObjects(uriParts.bucket(), uriParts.key());
	}

	public static List<S3Object> listObjects(String bucket, String prefix) {
		S3AsyncClient s3 = getS3Client();

		ListObjectsRequest request = ListObjectsRequest.builder()
				.bucket(bucket)
				.prefix(prefix)
				.build();

		CompletableFuture<ListObjectsResponse> future = s3.listObjects(request);
		ListObjectsResponse response = future.join();

		if (response.hasContents()) {
			return response.contents();
		} else {
			return List.of();
		}
	}

	public static void deleteFolder(String uri) throws IOException {
		UriParts uriParts = getParts(uri);
		S3AsyncClient s3 = S3Util.getS3Client();
		String continuationToken = null;

		try { // There's some kind of pagination going on with the listObjects response.
			do {
				ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
						.bucket(uriParts.bucket())
						.prefix(uriParts.key())
						.continuationToken(continuationToken)
						.build();

				CompletableFuture<ListObjectsV2Response> future = s3.listObjectsV2(listRequest);
				ListObjectsV2Response response = future.join();

				// TODO(Marc): It would be more efficient to use DeleteObjects* (note the
				//             plural) to batch delte, but it has a max of 1,000 deletes
				//             per call, so we'd have to control for that.
				for (S3Object head : response.contents()) {
					DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
							.bucket(uriParts.bucket())
							.key(head.key())
							.build();

					CompletableFuture<DeleteObjectResponse> deleteFuture = s3.deleteObject(deleteRequest);
					deleteFuture.join();
				}

				continuationToken = response.nextContinuationToken();
			} while (continuationToken != null);
		} catch (CancellationException | CompletionException | SdkException e) {
			throw new IOException("Failed to delete S3 dir: " + uriParts.bucket() + "/" + uriParts.key(), e);
		}
	}
}
