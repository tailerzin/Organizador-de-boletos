package com.empresa.boletos.service;

import com.empresa.boletos.model.Empresa;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê a planilha local "empresas.xlsx" (colunas: Nome | CNPJ | Tributo | Nº do Parcelamento,
 * cabeçalho na linha 1) e mantém a lista em memória para consulta durante o processamento.
 *
 * Uma mesma empresa pode aparecer em VÁRIAS linhas (uma por tributo/parcelamento diferente).
 * Por isso buscarTodasPorNomeNoTexto retorna uma lista, e quem decide qual linha usar é o
 * ProcessamentoService, cruzando com o tributo/número de parcelamento extraídos do PDF.
 */
public class EmpresaRepository {

    private final Path planilhaPath;
    private final String abaPreferida;
    private List<Empresa> empresas = new ArrayList<>();
    private String abaCarregada = null;
    private Map<String, String> nomeCanonicoPorCnpj = new HashMap<>();

    public EmpresaRepository(Path planilhaPath) {
        this(planilhaPath, null);
    }

    public EmpresaRepository(Path planilhaPath, String abaPreferida) {
        this.planilhaPath = planilhaPath;
        this.abaPreferida = abaPreferida;
    }

    public void garantirPlanilhaExiste() throws IOException {
        if (Files.exists(planilhaPath)) {
            return;
        }
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Empresas");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nome");
            header.createCell(1).setCellValue("CNPJ");
            header.createCell(2).setCellValue("Tributo");
            header.createCell(3).setCellValue("Nº do Parcelamento");

            Row exemplo = sheet.createRow(1);
            exemplo.createCell(0).setCellValue("Empresa Exemplo LTDA");
            exemplo.createCell(1).setCellValue("00.000.000/0001-00");
            exemplo.createCell(2).setCellValue("Simples Nacional");
            exemplo.createCell(3).setCellValue("");

            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);

            Path parent = planilhaPath.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (var out = Files.newOutputStream(planilhaPath)) {
                wb.write(out);
            }
        }
    }

    public void carregar() throws IOException {
        garantirPlanilhaExiste();
        List<Empresa> lista = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(planilhaPath.toFile());
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = escolherAba(wb);
            this.abaCarregada = sheet.getSheetName();
            DataFormatter formatter = new DataFormatter();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                String nome = formatter.formatCellValue(row.getCell(0)).trim();
                String cnpj = formatter.formatCellValue(row.getCell(1)).trim();
                String tributo = formatter.formatCellValue(row.getCell(2)).trim();
                String numeroParcelamento = formatter.formatCellValue(row.getCell(3)).trim();

                if (nome.isEmpty() && cnpj.isEmpty()) continue;

                lista.add(new Empresa(nome, cnpj, tributo, numeroParcelamento));
            }
        }

        this.empresas = lista;
        this.nomeCanonicoPorCnpj = construirNomesCanonicos(lista);
    }

    /** Para cada CNPJ, escolhe o nome mais completo cadastrado entre todas as linhas dele —
     *  evita criar pastas/arquivos diferentes para a mesma empresa quando a planilha tem a
     *  mesma empresa grafada de formas diferentes em linhas distintas. */
    private Map<String, String> construirNomesCanonicos(List<Empresa> lista) {
        Map<String, String> mapa = new HashMap<>();
        for (Empresa e : lista) {
            if (e.getCnpj().isEmpty()) continue;
            String atual = mapa.get(e.getCnpj());
            if (atual == null || e.getNomeExibicao().length() > atual.length()) {
                mapa.put(e.getCnpj(), e.getNomeExibicao());
            }
        }
        return mapa;
    }

    /** Nome a usar em pasta/arquivo para essa empresa: o canônico pelo CNPJ, se houver, senão
     *  o próprio nome de exibição dela. */
    public String getNomeCanonico(Empresa e) {
        if (e == null) return null;
        if (!e.getCnpj().isEmpty() && nomeCanonicoPorCnpj.containsKey(e.getCnpj())) {
            return nomeCanonicoPorCnpj.get(e.getCnpj());
        }
        return e.getNomeExibicao();
    }

    private Sheet escolherAba(Workbook wb) {
        if (abaPreferida != null && !abaPreferida.isBlank()) {
            Sheet porNome = wb.getSheet(abaPreferida.trim());
            if (porNome != null) {
                return porNome;
            }
        }
        DataFormatter formatter = new DataFormatter();
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet sheet = wb.getSheetAt(i);
            Row header = sheet.getRow(0);
            if (header == null) continue;
            String nome = formatter.formatCellValue(header.getCell(0)).trim();
            String cnpj = formatter.formatCellValue(header.getCell(1)).trim();
            if (nome.equalsIgnoreCase("Nome") && cnpj.equalsIgnoreCase("CNPJ")) return sheet;
        }
        return wb.getSheetAt(0);
    }

    public String getAbaCarregada() {
        return abaCarregada;
    }

    public List<Empresa> listar() {
        return empresas;
    }

    public Empresa buscarPorCnpj(String cnpjDigitos) {
        if (cnpjDigitos == null || cnpjDigitos.isEmpty()) return null;
        for (Empresa e : empresas) {
            if (e.getCnpj().equals(cnpjDigitos)) {
                return e;
            }
        }
        return null;
    }

    /** Como buscarPorCnpj, mas retorna TODAS as linhas com esse CNPJ — uma empresa pode ter
     *  várias linhas (uma por tributo/parcelamento), igual acontece na busca por nome. */
    public List<Empresa> buscarTodasPorCnpj(String cnpjDigitos) {
        List<Empresa> encontrados = new ArrayList<>();
        if (cnpjDigitos == null || cnpjDigitos.isEmpty()) return encontrados;
        for (Empresa e : empresas) {
            if (e.getCnpj().equals(cnpjDigitos)) {
                encontrados.add(e);
            }
        }
        return encontrados;
    }

    /** Retorna TODAS as linhas cujo nome aparece no texto do documento (pode ter mais de uma). */
    /** Retorna TODAS as linhas cujo nome aparece no texto do documento (pode ter mais de uma). */
    /** Retorna TODAS as linhas cujo nome aparece no texto do documento (pode ter mais de uma).
     *  Ignora anotações entre parênteses (ex: "(Lucro Real)") e pontuação (pontos/vírgulas) na
     *  hora de comparar, já que essas diferenças existem na planilha mas não no PDF. */
    public List<Empresa> buscarTodasPorNomeNoTexto(String textoDocumento) {
        List<Empresa> encontrados = new ArrayList<>();
        if (textoDocumento == null || textoDocumento.isEmpty()) return encontrados;
        String textoNormalizado = normalizarParaComparacao(textoDocumento);

        for (Empresa e : empresas) {
            String nomeNormalizado = normalizarParaComparacao(e.getNome());
            if (!nomeNormalizado.isEmpty() && textoNormalizado.contains(nomeNormalizado)) {
                encontrados.add(e);
            }
        }
        return encontrados;
    }

    private String normalizarParaComparacao(String texto) {
        if (texto == null) return "";
        String semParenteses = texto.replaceAll("\\([^)]*\\)", " ");
        // remove acentos/cedilha (Ç -> C, Á -> A, etc.)
        String semAcentos = java.text.Normalizer.normalize(semParenteses, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String semPontuacao = semAcentos.replaceAll("[.,]", " ").toUpperCase();
        // ignora conectores comuns em nomes de pessoa (podem aparecer ou não dependendo de onde
        // o nome foi digitado), mas não remove "E" sozinho — pode ser inicial de nome de empresa
        String semConectores = semPontuacao.replaceAll("\\b(DE|DA|DO|DOS|DAS)\\b", " ");
        // remove TODOS os espaços (não só duplicados) — cobre variações tipo "E. G. CAR" vs "E.G.CAR"
        return semConectores.replaceAll("\\s+", "");
    }
}
