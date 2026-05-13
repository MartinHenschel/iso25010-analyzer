package src.main.java.utils.analyzers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DryAnalyzer {

    public static class DuplicateBlock {
        public String content;
        public List<String> foundInFiles = new ArrayList<>();

        public DuplicateBlock(String content) {
            this.content = content;
        }
    }

    public static class Result {
        public List<DuplicateBlock> duplicates = new ArrayList<>();

        public int totalPoints() {
            return duplicates.size();
        }
    }

    private static final int MIN_LINES = 5;

    public static Result analyze(List<File> files) {
        Result result = new Result();

        Map<String, List<String>> blockMap = new HashMap<>();

        for (File file : files) {
            List<String> lines = readLines(file);

            for (int i = 0; i <= lines.size() - MIN_LINES; i++) {
                StringBuilder block = new StringBuilder();
                for (int j = i; j < i + MIN_LINES; j++) {
                    block.append(lines.get(j)).append("\n");
                }
                String key = block.toString();
                blockMap.computeIfAbsent(key, k -> new ArrayList<>())
                        .add(file.getName() + " (linha " + (i + 1) + ")");
            }
        }

        for (Map.Entry<String, List<String>> entry : blockMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                DuplicateBlock block = new DuplicateBlock(entry.getKey());
                block.foundInFiles = entry.getValue();
                result.duplicates.add(block);
            }
        }

        return result;
    }

    private static List<String> readLines(File file) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (isRelevantLine(trimmed)) {
                    lines.add(trimmed);
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao ler: " + file.getName());
        }
        return lines;
    }

    private static boolean isRelevantLine(String line) {
        if (line.isEmpty()) return false;
        if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) return false;
        if (line.startsWith("import ")) return false;
        if (line.startsWith("package ")) return false;
        if (line.equals("{") || line.equals("}")) return false;
        return true;
    }

    public static String getStatus(int points) {
        if (points == 0) return "[NENHUMA - OK]";
        if (points <= 2)  return "[BAIXA - ATENCAO]";
        if (points <= 5)  return "[ALTA - PROBLEMATICO]";
        return "[CRITICA - REFATORAR]";
    }
}