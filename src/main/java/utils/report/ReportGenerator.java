package src.main.java.utils.report;

import src.main.java.utils.analyzers.BenchmarkAnalyzer;
import src.main.java.utils.analyzers.CboAnalyzer;
import src.main.java.utils.analyzers.CoverageAnalyzer;
import src.main.java.utils.analyzers.CyclomaticAnalyzer;
import src.main.java.utils.analyzers.DryAnalyzer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportGenerator {

    public static void generate(
            String projectPath,
            List<CyclomaticAnalyzer.FileResult> cyclomaticResults,
            List<CboAnalyzer.FileResult> cboResults,
            DryAnalyzer.Result dryResult,
            CoverageAnalyzer.Result coverageResult,
            BenchmarkAnalyzer.Result benchmarkResult,
            String outputPath) {

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {

            String now = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            double overallCoverage = coverageResult.overallCoverage();
            int totalDuplicates = dryResult.totalPoints();
            double latencyIncrease = benchmarkResult.latencyIncreasePercent();

            writer.println("<!DOCTYPE html>");
            writer.println("<html lang='pt-BR'>");
            writer.println("<head>");
            writer.println("<meta charset='UTF-8'>");
            writer.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            writer.println("<title>Relatorio ISO/IEC 25010</title>");
            writer.println("<style>");
            writer.println("* { margin: 0; padding: 0; box-sizing: border-box; }");
            writer.println("body { font-family: 'Segoe UI', sans-serif; background: #f0f2f5; color: #333; }");
            writer.println("header { background: #1a1a2e; color: white; padding: 30px 40px; }");
            writer.println("header h1 { font-size: 1.8em; }");
            writer.println("header p { color: #aaa; margin-top: 5px; }");
            writer.println(".container { max-width: 1100px; margin: 30px auto; padding: 0 20px; }");
            writer.println(".summary { display: grid; grid-template-columns: repeat(5, 1fr); gap: 20px; margin-bottom: 30px; }");
            writer.println(".card { background: white; border-radius: 10px; padding: 20px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }");
            writer.println(".card h3 { font-size: 0.85em; color: #888; margin-bottom: 8px; text-transform: uppercase; }");
            writer.println(".card .value { font-size: 2em; font-weight: bold; }");
            writer.println(".ok { color: #27ae60; }");
            writer.println(".warn { color: #f39c12; }");
            writer.println(".danger { color: #e74c3c; }");
            writer.println(".section { background: white; border-radius: 10px; padding: 25px; margin-bottom: 25px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }");
            writer.println(".section h2 { font-size: 1.1em; border-bottom: 2px solid #1a1a2e; padding-bottom: 10px; margin-bottom: 20px; color: #1a1a2e; }");
            writer.println("table { width: 100%; border-collapse: collapse; font-size: 0.9em; }");
            writer.println("th { background: #1a1a2e; color: white; padding: 10px 15px; text-align: left; }");
            writer.println("td { padding: 10px 15px; border-bottom: 1px solid #eee; }");
            writer.println("tr:hover td { background: #f9f9f9; }");
            writer.println(".badge { padding: 3px 10px; border-radius: 20px; font-size: 0.8em; font-weight: bold; }");
            writer.println(".badge-ok { background: #d5f5e3; color: #27ae60; }");
            writer.println(".badge-warn { background: #fef9e7; color: #f39c12; }");
            writer.println(".badge-danger { background: #fdedec; color: #e74c3c; }");
            writer.println(".badge-critical { background: #f9ebea; color: #c0392b; }");
            writer.println("footer { text-align: center; padding: 20px; color: #aaa; font-size: 0.85em; }");
            writer.println("</style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("<header>");
            writer.println("<h1>&#128202; Relatorio de Qualidade - ISO/IEC 25010</h1>");
            writer.println("<p>Gerado em: " + now + " &nbsp;|&nbsp; Projeto: " + projectPath + "</p>");
            writer.println("</header>");
            writer.println("<div class='container'>");

            // Cards de resumo
            writer.println("<div class='summary'>");
            writer.println(summaryCard("Arquivos", String.valueOf(cyclomaticResults.size()), "ok"));
            writer.println(summaryCard("Duplicacoes", String.valueOf(totalDuplicates),
                    totalDuplicates == 0 ? "ok" : totalDuplicates <= 2 ? "warn" : "danger"));
            writer.println(summaryCard("Cobertura",
                    String.format("%.0f%%", overallCoverage),
                    overallCoverage >= 80 ? "ok" : overallCoverage >= 50 ? "warn" : "danger"));
            writer.println(summaryCard("Latencia",
                    benchmarkResult.loadResults.isEmpty() ? "N/A" :
                    String.format("+%.0f%%", latencyIncrease),
                    latencyIncrease < 50 ? "ok" : latencyIncrease < 200 ? "warn" : "danger"));
            writer.println(summaryCard("Status Geral",
                    overallCoverage >= 50 && totalDuplicates <= 2 ? "APROVADO" : "REPROVADO",
                    overallCoverage >= 50 && totalDuplicates <= 2 ? "ok" : "danger"));
            writer.println("</div>");

            // Secao Complexidade Ciclomatica
            writer.println("<div class='section'>");
            writer.println("<h2>&#128268; Modulo I - Complexidade Ciclomatica (McCabe)</h2>");
            writer.println("<table>");
            writer.println("<tr><th>Arquivo</th><th>Metodo</th><th>Complexidade</th><th>Status</th></tr>");

            for (CyclomaticAnalyzer.FileResult fr : cyclomaticResults) {
                for (CyclomaticAnalyzer.MethodResult mr : fr.methods) {
                    writer.printf("<tr><td>%s</td><td>%s</td><td>%d</td><td>%s</td></tr>%n",
                            fr.fileName, mr.methodName, mr.complexity,
                            badge(getCyclomaticLabel(mr.complexity)));
                }
            }
            writer.println("</table></div>");

            // Secao CBO
            writer.println("<div class='section'>");
            writer.println("<h2>&#128279; Modulo I - Acoplamento entre Objetos (CBO)</h2>");
            writer.println("<table>");
            writer.println("<tr><th>Arquivo</th><th>Classes Usadas</th><th>CBO</th><th>Status</th></tr>");

            for (CboAnalyzer.FileResult fr : cboResults) {
                writer.printf("<tr><td>%s</td><td>%s</td><td>%d</td><td>%s</td></tr>%n",
                        fr.fileName, fr.usedClasses.toString(), fr.getCbo(),
                        badge(getCboLabel(fr.getCbo())));
            }
            writer.println("</table></div>");

            // Secao DRY
            writer.println("<div class='section'>");
            writer.println("<h2>&#128260; Modulo I - Duplicacao de Codigo (DRY)</h2>");
            writer.println("<table>");
            writer.println("<tr><th>#</th><th>Encontrado em</th><th>Trecho</th></tr>");

            if (dryResult.duplicates.isEmpty()) {
                writer.println("<tr><td colspan='3'>Nenhuma duplicacao encontrada</td></tr>");
            } else {
                for (int i = 0; i < dryResult.duplicates.size(); i++) {
                    DryAnalyzer.DuplicateBlock block = dryResult.duplicates.get(i);
                    writer.printf("<tr><td>%d</td><td>%s</td><td><pre style='font-size:0.8em'>%s</pre></td></tr>%n",
                            i + 1, block.foundInFiles, escapeHtml(block.content));
                }
            }
            writer.println("</table></div>");

            // Secao Cobertura
            writer.println("<div class='section'>");
            writer.println("<h2>&#129518; Modulo III - Cobertura de Testes</h2>");
            writer.println("<table>");
            writer.println("<tr><th>Arquivo</th><th>Teste Encontrado</th><th>Metodos Totais</th><th>Testados</th><th>Cobertura</th><th>Status</th></tr>");

            for (CoverageAnalyzer.FileResult fr : coverageResult.files) {
                writer.printf("<tr><td>%s</td><td>%s</td><td>%d</td><td>%d</td><td>%.1f%%</td><td>%s</td></tr>%n",
                        fr.fileName,
                        fr.hasTestFile ? "Sim" : "Nao",
                        fr.totalMethods,
                        fr.testedMethods,
                        fr.coveragePercent(),
                        badge(getCoverageLabel(fr.coveragePercent())));
            }

            writer.printf("<tr style='font-weight:bold'><td colspan='4'>Cobertura Geral</td><td>%.1f%%</td><td>%s</td></tr>%n",
                    overallCoverage, badge(getCoverageLabel(overallCoverage)));
            writer.println("</table></div>");

            // Secao Benchmark
            writer.println("<div class='section'>");
            writer.println("<h2>&#9201; Modulo II - Benchmarking Dinamico</h2>");
            writer.println("<table>");
            writer.println("<tr><th>Carga</th><th>Tempo (ms)</th><th>Status</th></tr>");

            if (benchmarkResult.loadResults.isEmpty()) {
                writer.println("<tr><td colspan='3'>Benchmark nao executado</td></tr>");
            } else {
                for (BenchmarkAnalyzer.LoadResult lr : benchmarkResult.loadResults) {
                    String tempo = lr.timedOut ? "TIMEOUT" : lr.elapsedMs + "ms";
                    String status = lr.timedOut ? badge("CRITICO - REFATORAR") :
                            badge(getLatencyLabel(lr.elapsedMs));
                    writer.printf("<tr><td>%d registros</td><td>%s</td><td>%s</td></tr>%n",
                            lr.load, tempo, status);
                }
                writer.printf("<tr style='font-weight:bold'><td colspan='2'>Aumento total de latencia</td><td>%.1f%% %s</td></tr>%n",
                        latencyIncrease, badge(BenchmarkAnalyzer.getLatencyStatus(latencyIncrease)));
            }
            writer.println("</table></div>");

            writer.println("</div>");
            writer.println("<footer>Gerado pela ferramenta ISO/IEC 25010 Analyzer</footer>");
            writer.println("</body></html>");

            System.out.println("\nRelatorio gerado em: " + outputPath);

        } catch (IOException e) {
            System.out.println("Erro ao gerar relatorio: " + e.getMessage());
        }
    }

    private static String summaryCard(String title, String value, String type) {
        return "<div class='card'><h3>" + title + "</h3><div class='value " + type + "'>" + value + "</div></div>";
    }

    private static String badge(String label) {
    if (label.contains("OK") || label.contains("OTIMO")) return "<span class='badge badge-ok'>" + label + "</span>";
    if (label.contains("ATENCAO") || label.contains("ACEITAVEL")) return "<span class='badge badge-warn'>" + label + "</span>";
    if (label.contains("PROBLEMATICO")) return "<span class='badge badge-danger'>" + label + "</span>";
    return "<span class='badge badge-critical'>" + label + "</span>";
    
    }   

    private static String getCyclomaticLabel(int c) {
        if (c <= 4)  return "BAIXA - OK";
        if (c <= 7)  return "MEDIA - ATENCAO";
        if (c <= 10) return "ALTA - PROBLEMATICO";
        return "CRITICA - REFATORAR";
    }

    private static String getCboLabel(int c) {
        if (c <= 3)  return "BAIXO - OK";
        if (c <= 6)  return "MEDIO - ATENCAO";
        if (c <= 10) return "ALTO - PROBLEMATICO";
        return "CRITICO - REFATORAR";
    }

    private static String getCoverageLabel(double c) {
        if (c >= 80) return "BOA - OK";
        if (c >= 50) return "PARCIAL - ATENCAO";
        if (c > 0)   return "INSUFICIENTE - PROBLEMATICO";
        return "SEM TESTES - CRITICO";
    }

    private static String getLatencyLabel(long ms) {
        if (ms < 500)  return "OTIMO - OK";
        if (ms < 2000) return "ACEITAVEL - ATENCAO";
        if (ms < 5000) return "ALTO - PROBLEMATICO";
        return "CRITICO - REFATORAR";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}