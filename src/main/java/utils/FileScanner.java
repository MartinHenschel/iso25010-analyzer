package src.main.java.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileScanner {

    public static List<File> getJavaFiles(File folder) {

        List<File> files = new ArrayList<>();

        File[] allFiles = folder.listFiles();

        if (allFiles == null) {
            return files;
        }

        for (File file : allFiles) {

            if (file.isDirectory()) {
                files.addAll(getJavaFiles(file));
            }

            else if (file.getName().endsWith(".java")) {
                files.add(file);
            }
        }

        return files;
    }
}