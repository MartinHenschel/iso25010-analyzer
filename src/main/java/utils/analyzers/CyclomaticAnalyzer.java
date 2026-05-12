package src.main.java.utils.analyzers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CyclomaticAnalyzer {

    // Representa o resultado de um método analisado
    public static class MethodResult {
        public String methodName;
        public int complexity;

        public MethodResult(String methodName, int complexity) {
            this.methodName = methodName;
            this.complexity = complexity;
        }
    }

    // Representa o resultado de um arquivo analisado
    public static class FileResult {
        public String fileName;
        public List<MethodResult> methods = new ArrayList<>();

        public FileResult(String fileName) {
            this.fileName = fileName;
        }

        public int totalComplexity() {
            int total = 0;
            for (MethodResult m : methods) {
                total += m.complexity;
            }
            return total;
        }
    }

    // Analisa um arquivo .java e retorna os resultados
    public static FileResult analyze(File file) {
        FileResult result = new FileResult(file.getName());

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;
            String currentMethod = null;
            int complexity = 1; // começa em 1 (ponto base)
            int braceDepth = 0;
            int methodBraceStart = -1;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                // Detecta início de método (linha com parenteses e chave ou só parenteses)
                if (isMethodSignature(trimmed) && currentMethod == null) {
                    currentMethod = extractMethodName(trimmed);
                    complexity = 1;
                    methodBraceStart = braceDepth;
                }

                // Conta abertura e fechamento de chaves
                for (char c : trimmed.toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') braceDepth--;
                }

                // Conta ramificações lógicas dentro de um método
                if (currentMethod != null) {
                    if (trimmed.startsWith("if ") || trimmed.startsWith("if(")) {
                        complexity++;
                    } else if (trimmed.startsWith("else if ") || trimmed.startsWith("else if(")) {
                        complexity++;
                    } else if (trimmed.startsWith("else")) {
                        complexity++;
                    }
                }

                // Detecta fim do método quando volta ao nível de chave original
                if (currentMethod != null && braceDepth <= methodBraceStart && trimmed.contains("}")) {
                    result.methods.add(new MethodResult(currentMethod, complexity));
                    currentMethod = null;
                    complexity = 1;
                }
            }

        } catch (IOException e) {
            System.out.println("Erro ao ler arquivo: " + file.getName());
        }

        return result;
    }

    // Verifica se a linha parece uma assinatura de método
    private static boolean isMethodSignature(String line) {
        return (line.contains("(") && line.contains(")")) &&
               (line.startsWith("public") || line.startsWith("private") ||
                line.startsWith("protected") || line.startsWith("static")) &&
               !line.startsWith("//") &&
               !line.contains("new ") &&
               !line.contains(";");
    }

    // Extrai o nome do método da assinatura
    private static String extractMethodName(String line) {
        try {
            int parenIndex = line.indexOf("(");
            String beforeParen = line.substring(0, parenIndex).trim();
            String[] parts = beforeParen.split("\\s+");
            return parts[parts.length - 1];
        } catch (Exception e) {
            return "desconhecido";
        }
    }
}