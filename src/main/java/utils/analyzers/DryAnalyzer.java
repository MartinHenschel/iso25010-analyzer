package src.main.java.utils.analyzers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        // Lê o conteúdo de todos os arquivos
        List<String> fileNames = new ArrayList<>();
        List<List<String>> fileContents = new ArrayList<>();

        for (File file : files) {
            List<String> lines = readLines(file);
            fileNames.add(file.getName());
            fileContents.add(lines);
        }

        // Compara blocos entre todos os pares de arquivos
        for (int i = 0; i < fileContents.size(); i++) {
            for (int j = i + 1; j < fileContents.size(); j++) {
                List<String> linesA = fileContents.get(i);
                List<String> linesB = fileContents.get(j);

                findDuplicates(linesA, linesB,
                        fileNames.get(i), fileNames.get(j), result);
            }

            // Também verifica duplicatas dentro do mesmo arquivo
            findDuplicates(fileContents.get(i), fileContents.get(i),
                    fileNames.get(i), fileNames.get(i) + " (mesmo arquivo)", result);
        }

        return result;
    }

    private static void findDuplicates(List<String> linesA, List<String> linesB,
                                        String nameA, String nameB, Result result) {

        boolean sameFile = nameA.equals(nameB.replace(" (mesmo arquivo)", ""));

        for (int a = 0; a <= linesA.size() - MIN_LINES; a++) {
            for (int b = sameFile ? a + MIN_LINES : 0; b <= linesB.size() - MIN_LINES; b++) {

                // Tenta crescer o bloco duplicado
                int length = 0;
                while (a + length < linesA.size()
                        && b + length < linesB.size()
                        && normalize(linesA.get(a + length))
                           .equals(normalize(linesB.get(b + length)))
                        && !normalize(linesA.get(a + length)).isEmpty()) {
                    length++;
                }

                if (length >= MIN_LINES) {
                    String blockContent = String.join("\n",
                            linesA.subList(a, a + length));

                    // Verifica se esse bloco já foi registrado
                    if (!alreadyRegistered(result, blockContent)) {
                        DuplicateBlock block = new DuplicateBlock(blockContent);
                        block.foundInFiles.add(nameA + " (linha " + (a + 1) + ")");
                        block.foundInFiles.add(nameB + " (linha " + (b + 1) + ")");
                        result.duplicates.add(block);
                    }
                }
            }
        }
    }

    private static boolean alreadyRegistered(Result result, String content) {
        for (DuplicateBlock block : result.duplicates) {
            if (block.content.equals(content)) return true;
        }
        return false;
    }

    // Remove espaços extras para comparação justa
    private static String normalize(String line) {
        return line.trim().replaceAll("\\s+", " ");
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

// Ignora linhas que não representam lógica real
private static boolean isRelevantLine(String line) {
    if (line.isEmpty()) return false;
    if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) return false;
    if (line.startsWith("import ")) return false;
    if (line.startsWith("package ")) return false;
    if (line.equals("{") || line.equals("}") || line.equals("}}") || line.equals("},")) return false;
    if (line.equals("} catch (IOException e) {")) return false;
    return true;
}

    public static String getStatus(int points) {
        if (points == 0) return "[NENHUMA - OK]";
        if (points <= 2)  return "[BAIXA - ATENÇÃO]";
        if (points <= 5)  return "[ALTA - PROBLEMÁTICO]";
        return "[CRÍTICA - REFATORAR]";
    }
}