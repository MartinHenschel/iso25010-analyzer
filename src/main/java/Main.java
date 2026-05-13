package src.main.java;

import src.main.java.utils.FileScanner;
import src.main.java.utils.analyzers.CyclomaticAnalyzer;
import src.main.java.utils.analyzers.CboAnalyzer;
import src.main.java.utils.analyzers.DryAnalyzer;
import src.main.java.utils.analyzers.CoverageAnalyzer;
import src.main.java.utils.analyzers.BenchmarkAnalyzer;
import src.main.java.utils.report.ReportGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("Digite o caminho do projeto Java:");
        String path = scanner.nextLine();

        File projectFolder = new File(path);
        List<File> javaFiles = FileScanner.getJavaFiles(projectFolder);

        System.out.println("\nArquivos encontrados: " + javaFiles.size() + "\n");

        // ============================================
        // MODULO I - COMPLEXIDADE CICLOMATICA
        // ============================================
        System.out.println("========================================");
        System.out.println("   MODULO I - COMPLEXIDADE CICLOMATICA");
        System.out.println("========================================\n");

        for (File file : javaFiles) {
            CyclomaticAnalyzer.FileResult result = CyclomaticAnalyzer.analyze(file);

            System.out.println("Arquivo: " + result.fileName);
            System.out.println("----------------------------------------");

            if (result.methods.isEmpty()) {
                System.out.println("  Nenhum metodo detectado.");
            }

            for (CyclomaticAnalyzer.MethodResult method : result.methods) {
                System.out.println("  Metodo: " + method.methodName);
                System.out.println("  Complexidade: " + method.complexity
                        + " " + getCyclomaticStatus(method.complexity));
                System.out.println();
            }

            System.out.println("  Complexidade Total do Arquivo: " + result.totalComplexity());
            System.out.println();
        }

        // ============================================
        // MODULO I - ACOPLAMENTO (CBO)
        // ============================================
        System.out.println("========================================");
        System.out.println("   MODULO I - ACOPLAMENTO (CBO)");
        System.out.println("========================================\n");

        for (File file : javaFiles) {
            CboAnalyzer.FileResult result = CboAnalyzer.analyze(file);

            System.out.println("Arquivo: " + result.fileName);
            System.out.println("----------------------------------------");
            System.out.println("  Classes usadas: " + result.usedClasses);
            System.out.println("  CBO: " + result.getCbo()
                    + " " + CboAnalyzer.getStatus(result.getCbo()));
            System.out.println();
        }

        // ============================================
        // MODULO I - DUPLICACAO DE CODIGO (DRY)
        // ============================================
        System.out.println("========================================");
        System.out.println("   MODULO I - DUPLICACAO DE CODIGO (DRY)");
        System.out.println("========================================\n");

        DryAnalyzer.Result dryResult;

        if (javaFiles.size() > 100) {
            System.out.println("  Projeto grande detectado (" + javaFiles.size() + " arquivos).");
            System.out.println("  Analisando amostra de 20 arquivos...\n");
            dryResult = DryAnalyzer.analyze(javaFiles.subList(0, 20));
        } else {
            dryResult = DryAnalyzer.analyze(javaFiles);
        }

        if (dryResult.duplicates.isEmpty()) {
            System.out.println("  Nenhuma duplicacao encontrada. [OK]");
        } else {
            for (int i = 0; i < dryResult.duplicates.size(); i++) {
                DryAnalyzer.DuplicateBlock block = dryResult.duplicates.get(i);
                System.out.println("  Bloco duplicado #" + (i + 1) + ":");
                System.out.println("  Encontrado em: " + block.foundInFiles);
                System.out.println("  Trecho:");
                System.out.println("    " + block.content.replace("\n", "\n    "));
                System.out.println();
            }
        }

        System.out.println("  Total de blocos duplicados: " + dryResult.totalPoints()
                + " " + DryAnalyzer.getStatus(dryResult.totalPoints()));

        // ============================================
        // MODULO III - COBERTURA DE TESTES
        // ============================================
        System.out.println("\n========================================");
        System.out.println("   MODULO III - COBERTURA DE TESTES");
        System.out.println("========================================\n");

        CoverageAnalyzer.Result coverageResult = CoverageAnalyzer.analyze(javaFiles);

        for (CoverageAnalyzer.FileResult fileResult : coverageResult.files) {
            System.out.println("Arquivo: " + fileResult.fileName);
            System.out.println("----------------------------------------");
            System.out.println("  Arquivo de teste encontrado: "
                    + (fileResult.hasTestFile ? "Sim" : "Nao"));
            System.out.println("  Metodos totais: " + fileResult.totalMethods);
            System.out.println("  Metodos testados: " + fileResult.testedMethods);
            System.out.printf("  Cobertura: %.1f%% %s%n",
                    fileResult.coveragePercent(),
                    CoverageAnalyzer.getStatus(fileResult.coveragePercent()));

            if (!fileResult.untestedMethods.isEmpty()) {
                System.out.println("  Metodos sem teste: " + fileResult.untestedMethods);
            }
            System.out.println();
        }

        System.out.printf("  Cobertura Geral do Projeto: %.1f%% %s%n",
                coverageResult.overallCoverage(),
                CoverageAnalyzer.getStatus(coverageResult.overallCoverage()));

        // ============================================
        // MODULO II - BENCHMARKING DINAMICO
        // ============================================
        System.out.println("\n========================================");
        System.out.println("   MODULO II - BENCHMARKING DINAMICO");
        System.out.println("========================================\n");

        System.out.println("Digite o caminho do executavel java.exe do projeto alvo");
        System.out.println("(pressione Enter para usar o java atual):");
        String javaExeInput = scanner.nextLine().trim();

        String javaExe = javaExeInput.isEmpty()
            ? "C:\\Program Files\\Eclipse Adoptium\\jdk-25.0.3.9-hotspot\\bin\\java.exe"
            : javaExeInput;

        BenchmarkAnalyzer.Result benchmarkResult = BenchmarkAnalyzer.analyze(path, javaExe);

        if (!benchmarkResult.loadResults.isEmpty()) {
            System.out.println("\n  Resultados por carga:");
            System.out.println("  ----------------------------------------");
            for (BenchmarkAnalyzer.LoadResult lr : benchmarkResult.loadResults) {
                System.out.printf("  Carga %5d: %s%n",
                    lr.load,
                    lr.timedOut ? "TIMEOUT (>5s)" : lr.elapsedMs + "ms");
            }
            double increase = benchmarkResult.latencyIncreasePercent();
            System.out.printf("%n  Aumento de latencia (100 -> 5000): %.1f%% %s%n",
                increase, BenchmarkAnalyzer.getLatencyStatus(increase));
        }

        // ============================================
        // MODULO IV - GERACAO DE RELATORIO HTML
        // ============================================
        System.out.println("\nGerando relatorio HTML...");

        List<CyclomaticAnalyzer.FileResult> cyclomaticResults = new ArrayList<>();
        List<CboAnalyzer.FileResult> cboResults = new ArrayList<>();

        for (File file : javaFiles) {
            cyclomaticResults.add(CyclomaticAnalyzer.analyze(file));
            cboResults.add(CboAnalyzer.analyze(file));
        }

        String reportPath = path + "\\relatorio_iso25010.html";

        ReportGenerator.generate(
            path,
            cyclomaticResults,
            cboResults,
            dryResult,
            coverageResult,
            benchmarkResult,
            reportPath
        );
    }

    private static String getCyclomaticStatus(int complexity) {
        if (complexity <= 4)  return "[BAIXA - OK]";
        if (complexity <= 7)  return "[MEDIA - ATENCAO]";
        if (complexity <= 10) return "[ALTA - PROBLEMATICO]";
        return "[CRITICA - REFATORAR]";
    }
}