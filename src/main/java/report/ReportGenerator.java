package src.main.java.report;

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
            String outputPath) {

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {

            String now = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            double overallCoverage = coverageResult.overallCoverage();
            int totalDuplicates = dryResult.totalPoints();

            writer.println("""
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Relatório ISO/IEC 25010</title>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body { font-family: 'Segoe UI', sans-serif; background: #f0f2f5; color: #333; }
                        header { background: #1a1a2e; color: white; padding: 30px 40px; }
                        header h1 { font-size: 1.8em; }
                        header p { color: #aaa; margin-top: 5px; }
                        .container { max-width: 1100px; margin: 30px auto; padding: 0 20px; }
                        .summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 30px; }
                        .card { background: white; border-radius: 10px; padding: 20px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
                        .card h3 { font-size: 0.85em; color: #888; margin-bottom: 8px; text-transform: uppercase; }
                        .card .value { font-size: 2em; font-weight: bold; }
                        .ok { color: #27ae60; }
                        .warn { color: #f39c12; }
                        .danger { color: #e74c3c; }
                        .section { background: white; border-radius: 10px; padding: 25px; margin-bottom: 25px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
                        .section h2 { font-size: 1.1em; border-bottom: 2px solid #1a1a2e; padding-bottom: 10px; margin-bottom: 20px; color: #1a1a2e; }
                        table { width: 100%; border-collapse: collapse; font-size: 0.9em; }
                        th { background: #1a1a2e; color: white; padding: 10px 15px; text-align: left; }
                        td { padding: 10px 15px; border-bottom: 1px solid #eee; }
                        tr:hover td { background: #f9f9f9; }
                        .badge { padding: 3px 10px; border-radius: 20px; font-size: 0.8em; font-weight: bold; }
                        .badge-ok { background: #d5f5e3; color: #27ae60; }
                        .badge-warn { background: #fef9e7; color: #f39c12; }
                        .badge-danger { background: #fdedec; color: #e74c3c; }
                        .badge-critical { background: #f9ebea; color: #c0392b; }
                        footer { text-align: center; padding: 20px; color: #aaa; font-size: 0.85em; }
                    </style>
                </head>
                <body>
                <header>
                    <h1>&#128202; Relatório de Qualidade — ISO/IEC 25010</h1>
                    <p>Gerado em: """ + now + " &nbsp;|&nbsp; Projeto: " + projectPath + """
                    </p>
                </header>
                <div class="container">
                """);

            // Cards de resumo
            writer.println("<div class=\"summary\">");
            writer.println(summaryCard("Arquivos", String.valueOf(cyclomaticResults.size()), "ok"));
            writer.println(summaryCard("Duplicações", String.valueOf(totalDuplicates),
                    totalDuplicates == 0 ? "ok" : totalDuplicates <= 2 ? "warn" : "danger"));
            writer.println(summaryCard("Cobertura",
                    String.format("%.0f%%", overallCoverage),
                    overallCoverage >= 80 ? "ok" : overallCoverage >= 50 ? "warn" : "danger"));
            writer.println(summaryCard("Status Geral",
                    overallCoverage >= 50 && totalDuplicates <= 2 ? "APROVADO" : "REPROVADO",
                    overallCoverage >= 50 && totalDuplicates <= 2 ? "ok" : "danger"));
            writer.println("</div>");

            // Seção Complexidade Ciclomática
            writer.println("""
                <div class="section">
                <h2>&#128268; Módulo I — Complexidade Ciclomática (McCabe)</h2>
                <table>
                <tr><th>Arquivo</th><th>Método</th><th>Complexidade</th><th>Status</th></tr>
                """);

            for (CyclomaticAnalyzer.FileResult fr : cyclomaticResults) {
                for (CyclomaticAnalyzer.MethodResult mr : fr.methods) {
                    writer.printf("<tr><td>%s</td><td>%s</td><td>%d</td><td>%s</td></tr>%n",
                            fr.fileName, mr.methodName, mr.complexity,
                            badge(getCyclomaticLabel(mr.complexity)));
                }
            }
            writer.println("</table></div>");

            // Seção CBO
            writer.println("""
                <div class="section">
                <h2>&#128279; Módulo I — Acoplamento entre Objetos (CBO)</h2>
                <table>
                <tr><th>Arquivo</th><th>Classes Usadas</th><th>CBO</th><th>Status</th></tr>
                """);

            for (CboAnalyzer.FileResult fr : cboResults) {
                writer.printf("<tr><td>%s</td><td>%s</td><td>%d</td><td>%s</td></tr>%n",
                        fr.fileName, fr.usedClasses.toString(), fr.getCbo(),
                        badge(getCboLabel(fr.getCbo())));
            }
            writer.println("</table></div>");

            // Seção DRY
            writer.println("""
                <div class="section">
                <h2>&#128260; Módulo I — Duplicação de Código (DRY)</h2>
                <table>
                <tr><th>#</th><th>Encontrado em</th><th>Trecho</th></tr>
                """);

            if (dryResult.duplicates.isEmpty()) {
                writer.println("<tr><td colspan='3'>Nenhuma duplicação encontrada ✅</td></tr>");
            } else {
                for (int i = 0; i < dryResult.duplicates.size(); i++) {
                    DryAnalyzer.DuplicateBlock block = dryResult.duplicates.get(i);
                    writer.printf("<tr><td>%d</td><td>%s</td><td><pre style='font-size:0.8em'>%s</pre></td></tr>%n",
                            i + 1, block.foundInFiles, escapeHtml(block.content));
                }
            }
            writer.println("</table></div>");

            // Seção Cobertura
            writer.println("""
                <div class="section">
                <h2>&#129518; Módulo III — Cobertura de Testes</h2>
                <table>
                <tr><th>Arquivo</th><th>Teste Encontrado</th><th>Métodos Totais</th><th>Testados</th><th>Cobertura</th><th>Status</th></tr>
                """);

            for (CoverageAnalyzer.FileResult fr : coverageResult.files) {
                writer.printf("<tr><td>%s</td><td>%s</td><td>%d</td><td>%d</td><td>%.1f%%</td><td>%s</td></tr>%n",
                        fr.fileName,
                        fr.hasTestFile ? "✅ Sim" : "❌ Não",
                        fr.totalMethods,
                        fr.testedMethods,
                        fr.coveragePercent(),
                        badge(getCoverageLabel(fr.coveragePercent())));
            }

            writer.printf("<tr style='font-weight:bold'><td colspan='4'>Cobertura Geral</td><td>%.1f%%</td><td>%s</td></tr>%n",
                    overallCoverage, badge(getCoverageLabel(overallCoverage)));

            writer.println("</table></div>");

            writer.println("""
                </div>
                <footer>Gerado pela ferramenta ISO/IEC 25010 Analyzer</footer>
                </body></html>
                """);

            System.out.println("\nRelatório gerado em: " + outputPath);

        } catch (IOException e) {
            System.out.println("Erro ao gerar relatório: " + e.getMessage());
        }
    }

    private static String summaryCard(String title, String value, String type) {
        return String.format("""
            <div class="card">
                <h3>%s</h3>
                <div class="value %s">%s</div>
            </div>
            """, title, type, value);
    }

    private static String badge(String label) {
        String cls = switch (label) {
            case "BAIXA - OK", "BAIXO - OK", "BOA - OK", "NENHUMA - OK" -> "badge-ok";
            case "MÉDIA - ATENÇÃO", "MÉDIO - ATENÇÃO", "PARCIAL - ATENÇÃO", "BAIXA - ATENÇÃO" -> "badge-warn";
            case "ALTA - PROBLEMÁTICO", "ALTO - PROBLEMÁTICO", "INSUFICIENTE - PROBLEMÁTICO" -> "badge-danger";
            default -> "badge-critical";
        };
        return "<span class=\"badge " + cls + "\">" + label + "</span>";
    }

    private static String getCyclomaticLabel(int c) {
        if (c <= 4) return "BAIXA - OK";
        if (c <= 7) return "MÉDIA - ATENÇÃO";
        if (c <= 10) return "ALTA - PROBLEMÁTICO";
        return "CRÍTICA - REFATORAR";
    }

    private static String getCboLabel(int c) {
        if (c <= 3) return "BAIXO - OK";
        if (c <= 6) return "MÉDIO - ATENÇÃO";
        if (c <= 10) return "ALTO - PROBLEMÁTICO";
        return "CRÍTICO - REFATORAR";
    }

    private static String getCoverageLabel(double c) {
        if (c >= 80) return "BOA - OK";
        if (c >= 50) return "PARCIAL - ATENÇÃO";
        if (c > 0)   return "INSUFICIENTE - PROBLEMÁTICO";
        return "SEM TESTES - CRÍTICO";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}