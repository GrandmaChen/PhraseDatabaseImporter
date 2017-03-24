package Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class FileFilter {

	public static String path = "D:\\test";

	public static void main(String[] args) {

		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();

		// Rename files
		for (File file : listOfFiles) {

			String newName = file.getName();

			if (newName.endsWith(".txt"))
				newName = newName.substring(0, newName.length() - 4) + ".xml";
			else if (newName.endsWith("dctx"))
				newName = newName.substring(0, newName.length() - 5) + ".xml";

			file.renameTo(new File(path + "/" + newName));

		}

		// Re-read files and clean files
		folder = new File(path);
		listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {

			String[] cmd = new String[] { "java", "-jar", "atlassian-xml-cleaner-0.1.jar",
					"cd " + path + " && " + file.getName(), ">", "cd " + path + "\\result && " + "新系统词库.xml" };

			for (String s : cmd)
				System.out.print(s + " ");
			System.out.println();

			try {
				ProcessBuilder pb = new ProcessBuilder(cmd);
				pb.redirectErrorStream(true);
				Process process = pb.start();

				BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;

				while (true) {
					line = r.readLine();
					if (line == null) {
						break;
					}
					System.out.println(line);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
