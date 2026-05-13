package src.main.java.utils.analyzers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkAnalyzer {

    public static class LoadResult {
        public int load;
        public long elapsedMs;
        public boolean timedOut;

        public LoadResult(int load, long elapsedMs, boolean timedOut) {
            this.load = load;
            this.elapsedMs = elapsedMs;
            this.timedOut = timedOut;
        }
    }

    public static class Result {
        public List<LoadResult> loadResults = new ArrayList<>();
        public String projectPath;

        public Result(String projectPath) {
            this.projectPath = projectPath;
        }

        public double latencyIncreasePercent() {
            if (loadResults.size() < 2) return 0;
            LoadResult first = loadResults.get(0);
            LoadResult last = loadResults.get(loadResults.size() - 1);
            if (first.elapsedMs == 0) return 0;
            return ((last.elapsedMs - first.elapsedMs) * 100.0) / first.elapsedMs;
        }
    }

    private static final int[] LOADS = {100, 500, 1000, 5000};

    public static Result analyze(String projectPath, String javaExePath) {
        Result result = new Result(projectPath);

        List<File> allFiles = getAllJavaFiles(new File(projectPath));

        if (allFiles.isEmpty()) {
            System.out.println("  [BENCHMARK] Nenhum arquivo Java encontrado.");
            return result;
        }

        System.out.println("  Total de arquivos Java: " + allFiles.size());
        System.out.println("  Executando benchmark com cargas: 100, 500, 1000, 5000\n");

        for (int load : LOADS) {
            System.out.println("  Testando carga: " + load + "...");
            long start = System.currentTimeMillis();

            int linesRead = 0;
            for (File file : allFiles) {
                if (linesRead >= load) break;
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null && linesRead < load) {
                        line.trim().length();
                        linesRead++;
                    }
                } catch (IOException e) {}
            }

            long elapsed = System.currentTimeMillis() - start;
            System.out.println("    Tempo: " + elapsed + "ms");
            result.loadResults.add(new LoadResult(load, elapsed, false));
        }

        return result;
    }

    private static List<File> getAllJavaFiles(File folder) {
        List<File> files = new ArrayList<>();
        if (folder == null || !folder.exists()) return files;
        File[] all = folder.listFiles();
        if (all == null) return files;
        for (File f : all) {
            if (f.isDirectory()) files.addAll(getAllJavaFiles(f));
            else if (f.getName().endsWith(".java")) files.add(f);
        }
        return files;
    }

    public static String getLatencyStatus(double increasePercent) {
        if (increasePercent < 50)   return "[OTIMO - OK]";
        if (increasePercent < 200)  return "[ACEITAVEL - ATENCAO]";
        if (increasePercent < 500)  return "[ALTO - PROBLEMATICO]";
        return "[CRITICO - REFATORAR]";
    }
}