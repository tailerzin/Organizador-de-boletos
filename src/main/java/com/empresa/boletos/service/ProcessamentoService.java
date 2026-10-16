package com.empresa.boletos.service;

import com.empresa.boletos.model.DocumentoFiscal;
import com.empresa.boletos.model.Empresa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ProcessamentoService {

    private final EmpresaRepository empresaRepository;
    private final PdfTextExtractor extractor;
    private final DocumentParser parser;
    private final FileOrganizer organizer;

    public ProcessamentoService(EmpresaRepository empresaRepository,
                                PdfTextExtractor extractor,
                                DocumentParser parser,
                                FileOrganizer organizer) {
        this.empresaRepository = empresaRepository;
        this.extractor = extractor;
        this.parser = parser;
        this.organizer = organizer;
    }

    public static class Resumo {
        public int totalArquivos;
        public int identificados;
        public int naoIdentificados;
        public int comErro;
    }

    public Resumo processarPasta(Path pastaEntrada, Consumer<String> logger) throws IOException {
        Resumo resumo = new Resumo();

        List<Path> pdfs = listarPdfs(pastaEntrada);
        resumo.totalArquivos = pdfs.size();
        logger.accept(pdfs.size() + " arquivo(s) PDF encontrado(s) em " + pastaEntrada);

        for (Path pdfPath : pdfs) {
            try {
                processarArquivo(pdfPath, resumo, logger);
            } catch (Exception e) {
                resumo.comErro++;
                logger.accept("ERRO ao processar " + pdfPath.getFileName() + ": " + e.getMessage()
                        + " (arquivo mantido na pasta de entrada)");
            }
        }

        logger.accept(String.format(
                "Concluído. Identificados: %d | Não identificados: %d | Erros: %d",
                resumo.identificados, resumo.naoIdentificados, resumo.comErro));

        return resumo;
    }

    private void processarArquivo(Path pdfPath, Resumo resumo, Consumer<String> logger) throws IOException {
        logger.accept("Processando: " + pdfPath.getFileName());

        DocumentoFiscal doc = new DocumentoFiscal();
        doc.setArquivoOriginal(pdfPath);

        PdfTextExtractor.Resultado resultado = extractor.extrair(pdfPath.toFile());
        doc.setTextoExtraido(resultado.texto);
        doc.setTextoViaOcr(resultado.viaOcr);

        if (resultado.viaOcr) {
            logger.accept("  -> texto extraído via OCR (PDF parece escaneado)");
        }

        String cnpj = parser.extrairCnpj(resultado.texto);
        String tributoDetectado = parser.detectarTipoTributo(resultado.texto);
        String numeroParcelamento = parser.extrairNumeroParcelamento(resultado.texto);

        doc.setCnpjEncontrado(cnpj);
        doc.setNumeroParcela(parser.extrairNumeroParcela(resultado.texto));
        doc.setDataVencimento(parser.extrairDataVencimento(resultado.texto));
        doc.setTipoTributoDetectado(tributoDetectado);
        doc.setNumeroParcelamentoExtraido(numeroParcelamento);

        Empresa empresa = null;
        if (cnpj != null) {
            List<Empresa> porCnpj = empresaRepository.buscarTodasPorCnpj(cnpj);
            empresa = escolherMelhorCandidato(porCnpj, tributoDetectado, numeroParcelamento);
        }
        if (empresa == null) {
            List<Empresa> candidatos = empresaRepository.buscarTodasPorNomeNoTexto(resultado.texto);
            empresa = escolherMelhorCandidato(candidatos, tributoDetectado, numeroParcelamento);
        }
        doc.setEmpresa(empresa);
        if (empresa != null) {
            doc.setNomeParaArquivo(empresaRepository.getNomeCanonico(empresa));
        }

        if (empresa != null) {
            resumo.identificados++;
            logger.accept("  -> identificado: " + empresa.getNome() + " (" + empresa.getTributo() + ")");
        } else {
            resumo.naoIdentificados++;
            logger.accept("  -> NÃO identificado (CNPJ: " + (cnpj != null ? cnpj : "não encontrado") + ")");
        }

        Path destino = organizer.organizar(doc);
        logger.accept("  -> movido para: " + destino);
    }

    /**
     * Quando o mesmo nome bate em várias linhas da planilha (empresa com mais de um
     * tributo/parcelamento), desempata nesta ordem de prioridade:
     *   1) número de parcelamento extraído do PDF == número de parcelamento da linha (mais específico)
     *   2) tributo detectado no texto do PDF == tributo da linha
     *   3) primeira linha encontrada (comportamento antigo, usado só quando nada mais ajuda)
     */
    private Empresa escolherMelhorCandidato(List<Empresa> candidatos, String tributoDetectado, String numeroParcelamento) {
        if (candidatos.isEmpty()) return null;
        if (candidatos.size() == 1) return candidatos.get(0);

        if (numeroParcelamento != null && !numeroParcelamento.isBlank()) {
            String digitosPdf = numeroParcelamento.replaceAll("[^0-9]", "");
            for (Empresa e : candidatos) {
                String digitosPlanilha = e.getNumeroParcelamento().replaceAll("[^0-9]", "");
                if (!digitosPlanilha.isEmpty() && digitosPdf.equals(digitosPlanilha)) {
                    return e;
                }
            }
        }

        if (tributoDetectado != null) {
            String alvo = normalizarTributo(tributoDetectado);
            for (Empresa e : candidatos) {
                if (normalizarTributo(e.getTributo()).equals(alvo)) {
                    return e;
                }
            }
        }

        return candidatos.get(0);
    }

    private String normalizarTributo(String tributo) {
        if (tributo == null) return "";
        return tributo.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private List<Path> listarPdfs(Path pasta) throws IOException {
        List<Path> lista = new ArrayList<>();
        if (!Files.exists(pasta)) {
            return lista;
        }
        try (var stream = Files.list(pasta)) {
            stream.filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                    .sorted()
                    .forEach(lista::add);
        }
        return lista;
    }
}