package cloudgene.mapred.util.command;

import java.io.*;

public class CommandStreamHandler implements Runnable {

	private final BufferedReader is;

	private boolean silent = false;

	private String filename;

	private StringBuffer memory;

	public CommandStreamHandler(InputStream is, String filename) {
		this.is = new BufferedReader(new InputStreamReader(is));
		this.filename = filename;
	}

	public void setSilent(boolean silent) {
		this.silent = silent;
	}

	public void setStringBuffer(StringBuffer memory) {
		this.memory = memory;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Override
	public void run() {
		try {
			boolean save = (filename != null && !filename.isEmpty());
			BufferedWriter writer = null;

			if (save) {
				writer = new BufferedWriter(new FileWriter(filename));
			}

			String line;
			while ((line = is.readLine()) != null) {
				if (memory != null) {
					memory.append(line).append("\n");
				}
				if (!silent) {
					System.out.println("    > " + line);
				}
				if (save) {
					writer.write(line);
					writer.newLine();
				}
			}

			if (save) {
				writer.close();
			}

			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}