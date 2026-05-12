package src.main.java.utils.analyzers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CoverageAnalyzer {

    public static class FileResult {
        public String fileName;
        public boolean hasTestFile;
        public int totalMethods;
        public int testedMethods;
        public List<String> untestedMethods = new ArrayList<>();

        public FileResult(String fileName) {
            this.fileName = fileName;
        }

        public double coveragePercent() {
            if (totalMethods == 0) return 0;
            return (testedMethods * 100.0) / totalMethods;
        }
    }

    public static class Result {
        public List<FileResult> files = new ArrayList<>();

        public double overallCoverage() {
            int totalMethods = 0;
            int totalTested = 0;
            for (FileResult f : files) {
                totalMethods += f.totalMethods;
                totalTested += f.testedMethods;
            }
            if (totalMethods == 0) return 0;
            return (totalTested * 100.0) / totalMethods;
        }
    }

    public static Result analyze(List<File> allFiles) {
        Result result = new Result();

        // Separa arquivos de teste dos arquivos de produção
        List<File> sourceFiles = new ArrayList<>();
        List<File> testFiles = new ArrayList<>();

        for (File file : allFiles) {
            if (isTestFile(file)) {
                testFiles.add(file);
            } else {
                sourceFiles.add(file);
            }
        }

        // Para cada arquivo de produção, verifica cobertura
        for (File source : sourceFiles) {
            FileResult fileResult = new FileResult(source.getName());

            // Extrai nomes dos métodos do arquivo de produção
            List<String> methods = extractMethodNames(source);
            fileResult.totalMethods = methods.size();

            // Verifica se existe arquivo de teste correspondente
            File testFile = findTestFile(source, testFiles);
            fileResult.hasTestFile = (testFile != null);

            if (testFile != null) {
                // Lê conteúdo do arquivo de teste
                String testContent = readContent(testFile);

                // Verifica quais métodos são referenciados no teste
                for (String method : methods) {
                    if (testContent.contains(method)) {
                        fileResult.testedMethods++;
                    } else {
                        fileResult.untestedMethods.add(method);
                    }
                }
            } else {
                // Sem arquivo de teste, todos os métodos estão descobertos
                fileResult.untestedMethods.addAll(methods);
            }

            result.files.add(fileResult);
        }

        return result;
    }

    // Verifica se o arquivo é de teste (nome contém Test ou está em pasta test)
    private static boolean isTestFile(File file) {
        String name = file.getName();
        String path = file.getAbsolutePath();
        return name.contains("Test") || name.contains("test")
                || path.contains("test") || path.contains("Test");
    }

    // Encontra o arquivo de teste correspondente ao arquivo de produção
    private static File findTestFile(File source, List<File> testFiles) {
        String baseName = source.getName().replace(".java", "");
        for (File test : testFiles) {
            if (test.getName().contains(baseName)) {
                return test;
            }
        }
        return null;
    }

    // Extrai nomes de métodos públicos do arquivo
    private static List<String> extractMethodNames(File file) {
        List<String> methods = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (isMethodSignature(trimmed)) {
                    String name = extractMethodName(trimmed);
                    if (name != null && !name.equals("desconhecido")) {
                        methods.add(name);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao ler: " + file.getName());
        }
        return methods;
    }

    private static boolean isMethodSignature(String line) {
        return (line.contains("(") && line.contains(")"))
                && (line.startsWith("public") || line.startsWith("private")
                        || line.startsWith("protected") || line.startsWith("static"))
                && !line.startsWith("//")
                && !line.contains("new ")
                && !line.contains(";");
    }

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

    private static String readContent(File file) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            System.out.println("Erro ao ler: " + file.getName());
        }
        return sb.toString();
    }

    public static String getStatus(double coverage) {
        if (coverage >= 80) return "[BOA - OK]";
        if (coverage >= 50) return "[PARCIAL - ATENÇÃO]";
        if (coverage > 0)   return "[INSUFICIENTE - PROBLEMÁTICO]";
        return "[SEM TESTES - CRÍTICO]";
    }
}