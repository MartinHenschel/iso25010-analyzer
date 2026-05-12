package src.main.java.utils.analyzers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CboAnalyzer {

    public static class FileResult {
        public String fileName;
        public Set<String> usedClasses = new HashSet<>();

        public FileResult(String fileName) {
            this.fileName = fileName;
        }

        public int getCbo() {
            return usedClasses.size();
        }
    }

    private static final Set<String> IGNORED = Set.of(
        "String", "Integer", "List", "ArrayList", "Map", "HashMap",
        "Set", "HashSet", "File", "Scanner", "System", "Math",
        "Object", "Exception", "IOException", "BufferedReader",
        "FileReader", "PrintWriter", "FileWriter", "Collections",
        "Arrays", "Optional", "StringBuilder", "Override",
        "True", "False", "Unresolved", "Thread", "Class"
    );

    public static FileResult analyze(File file) {
        FileResult result = new FileResult(file.getName());

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                // Ignora comentários e linhas vazias
                if (trimmed.startsWith("//") || trimmed.startsWith("*")
                        || trimmed.startsWith("/*") || trimmed.isEmpty()) continue;

                // Ignora strings entre aspas (System.out.println("..."))
                trimmed = trimmed.replaceAll("\"[^\"]*\"", "");

                // Só analisa linhas de código real: declarações, imports, instanciações
                if (!isCodeLine(trimmed)) continue;

                // Extrai tokens que parecem nomes de classes (CamelCase)
                String[] tokens = trimmed.split("[^a-zA-Z0-9_]");
                for (String token : tokens) {
                    if (isClassName(token, file)) {
                        result.usedClasses.add(token);
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Erro ao ler arquivo: " + file.getName());
        }

        return result;
    }

    // Verifica se a linha é código real (não comentário, não print isolado)
    private static boolean isCodeLine(String line) {
        return line.startsWith("import ") ||
               line.contains(" new ") ||
               line.matches(".*\\b[A-Z][a-zA-Z0-9]+\\s+[a-z][a-zA-Z0-9]*\\s*[=;(].*") ||
               line.matches(".*[A-Z][a-zA-Z0-9]+\\..*");
    }

    // Verifica se o token parece um nome de classe válido (CamelCase real)
    private static boolean isClassName(String token, File file) {
        if (token.length() < 2) return false;
        if (!Character.isUpperCase(token.charAt(0))) return false;
        if (IGNORED.contains(token)) return false;
        if (token.equals(getClassName(file))) return false;

        // Deve ter pelo menos uma letra minúscula (evita siglas como "CBO", "OK")
        boolean hasLower = false;
        for (char c : token.toCharArray()) {
            if (Character.isLowerCase(c)) {
                hasLower = true;
                break;
            }
        }
        return hasLower;
    }

    private static String getClassName(File file) {
        return file.getName().replace(".java", "");
    }

    public static String getStatus(int cbo) {
        if (cbo <= 3)  return "[BAIXO - OK]";
        if (cbo <= 6)  return "[MÉDIO - ATENÇÃO]";
        if (cbo <= 10) return "[ALTO - PROBLEMÁTICO]";
        return "[CRÍTICO - REFATORAR]";
    }
}