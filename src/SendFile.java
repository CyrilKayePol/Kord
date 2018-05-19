import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

public class SendFile {
	
	private String sourceFilePath = null;
	private String destinationPath = null;
	
	public SendFile(String sourceFilePath, String destinationPath) {
		this.sourceFilePath = sourceFilePath;
		this.destinationPath = destinationPath;
	}
	public FileEvent getFileEvent() {
		FileEvent fileEvent = new FileEvent();
		String fileName = sourceFilePath.substring(sourceFilePath.lastIndexOf("/") + 1, sourceFilePath.length());
		fileEvent.setDestinationDirectory(destinationPath);
		fileEvent.setFilename(fileName);
		fileEvent.setSourceDirectory(sourceFilePath);
		File file = new File(sourceFilePath);
		if (file.isFile()) {
			try {
				@SuppressWarnings("resource")
				DataInputStream diStream = new DataInputStream(new FileInputStream(file));
				long len = (int) file.length();
				byte[] fileBytes = new byte[(int) len];
				int read = 0;
				int numRead = 0;
				while (read < fileBytes.length && (numRead = diStream.read(fileBytes, read, fileBytes.length - read)) >= 0) {
					read = read + numRead;
				}
				fileEvent.setFileSize(len);
				fileEvent.setFileData(fileBytes);
				fileEvent.setStatus("Success");
			} catch (Exception e) {
				e.printStackTrace();
				fileEvent.setStatus("Error");
			}
		} else {
			System.out.println("\n[File not found!]");
			fileEvent.setStatus("Error");
		}
		return fileEvent;
	}
}
