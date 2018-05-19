import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class StoreFile {
	
	
	public void createAndWriteFile(FileEvent fileEvent) {
		String outputFile = fileEvent.getDestinationDirectory() + fileEvent.getFilename();
		if (!new File(fileEvent.getDestinationDirectory()).exists()) {
		new File(fileEvent.getDestinationDirectory()).mkdirs();
		}
		File dstFile = new File(outputFile);
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(dstFile);
			fileOutputStream.write(fileEvent.getFileData());
			fileOutputStream.flush();
			fileOutputStream.close();
			System.out.println("\n[File successfully saved]");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
