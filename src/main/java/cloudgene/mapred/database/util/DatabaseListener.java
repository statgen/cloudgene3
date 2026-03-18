package cloudgene.mapred.database.util;

public interface DatabaseListener {

	public static final int AFTER_CONNECTION = 1;
	public static final int AFTER_DISCONNECTION = 2;
	public static final int BEFORE_DISCONNECTION = 3;
	public static final int ERROR = 4;

	public void onDatabaseEvent(int event);

}
