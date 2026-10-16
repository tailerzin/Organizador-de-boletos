package com.empresa.boletos.service;

import com.empresa.boletos.model.DocumentoFiscal;
import com.empresa.boletos.util.FilenameSanitizer;

import java.io.IOException;
import java.nio.file.*;

/**
 * Move o PDF processado para: raizSaida / Empresa / NomePadronizado.pdf
 *
 * Nome do arquivo (sem carimbo de data/hora, pronto pra mandar ao cliente):
 *   "{Empresa} Parcelamento {Tributo} Parcela {X} de {Y} {últimos dígitos do nº de parcelamento}.pdf"
 * Cada parte só entra se o dado existir pra esse documento. A data de vencimento só é
 * incluída no nome se, sem ela, o arquivo colidisse com outro já existente na pasta.
 *
 * O nome da empresa usado em pasta/arquivo vem de doc.getNomeParaArquivo() (nome "canônico" por
 * CNPJ, calculado no ProcessamentoService) — não de doc.getEmpresa().getNomeExibicao() direto —
 * pra evitar criar pastas diferentes quando a planilha tem a mesma empresa grafada de dois jeitos.
 */
public class FileOrganizer {

    private final Path raizSaida;

    public FileOrganizer(Path raizSaida) {
        this.raizSaida = raizSaida;
    }

    public Path organizar(DocumentoFiscal doc) throws IOException {
        Path pastaDestino = doc.isIdentificado()
                ? raizSaida.resolve(FilenameSanitizer.sanitizar(doc.getNomeParaArquivo()))
                : raizSaida.resolve("_NAO_IDENTIFICADO");

        Files.createDirectories(pastaDestino);

        Path destino = resolverCaminhoFinal(pastaDestino, doc);

        // Sem REPLACE_EXISTING: em nenhuma condição uma colisão pode apagar um boleto anterior.
        // O caminho já foi escolhido com sufixo quando o nome padrão está ocupado.
        return Files.move(doc.getArquivoOriginal(), destino);
    }

    private String tributoFinal(DocumentoFiscal doc) {
        if (doc.isIdentificado()) {
            String tributoEmpresa = doc.getEmpresa().getTributo();
            if (tributoEmpresa != null && !tributoEmpresa.isBlank()) {
                return tributoEmpresa;
            }
        }
        return doc.getTipoTributoDetectado() != null ? doc.getTipoTributoDetectado() : "TRIBUTO_NAO_INFORMADO";
    }

    private String montarNomeArquivo(DocumentoFiscal doc, boolean incluirVencimento) {
        StringBuilder nome = new StringBuilder();

        String nomeEmpresa = doc.isIdentificado() ? doc.getNomeParaArquivo() : "SEM-IDENTIFICACAO";
        nome.append(limpar(nomeEmpresa));
        nome.append(" Parcelamento ").append(limpar(tributoFinal(doc)));

        if (doc.getNumeroParcela() != null) {
            String[] partes = doc.getNumeroParcela().split("-");
            if (partes.length == 2) {
                nome.append(" Parcela ").append(partes[0]).append(" de ").append(partes[1]);
            } else {
                nome.append(" Parcela ").append(doc.getNumeroParcela());
            }
        }

        String numParcelamento = doc.getNumeroParcelamentoExtraido();
        if (numParcelamento != null && !numParcelamento.isBlank()) {
            String digitos = numParcelamento.replaceAll("[^0-9]", "");
            String ultimos = digitos.length() > 4 ? digitos.substring(digitos.length() - 4) : digitos;
            if (!ultimos.isEmpty()) {
                nome.append(" ").append(ultimos);
            }
        }

        if (incluirVencimento && doc.getDataVencimento() != null) {
            nome.append(" Venc ").append(doc.getDataVencimento());
        }

        nome.append(".pdf");
        return nome.toString();
    }

    /** Remove caracteres inválidos em nome de arquivo no Windows, mantendo espaços. */
    private String limpar(String texto) {
        if (texto == null) return "";
        return texto.trim().replaceAll("[\\\\/:*?\"<>|]", "").replaceAll("\\s+", " ");
    }

    /**
     * 1) tenta o nome limpo, sem vencimento.
     * 2) se colidir com um arquivo já existente, tenta de novo incluindo o vencimento.
     * 3) se ainda assim colidir (bem raro: mesma empresa+tributo+parcela+parcelamento+vencimento),
     *    cai no sufixo numérico " (1)", " (2)"...
     */
    private Path resolverCaminhoFinal(Path pastaDestino, DocumentoFiscal doc) {
        String nomeSemVenc = montarNomeArquivo(doc, false);
        Path candidato = pastaDestino.resolve(nomeSemVenc);
        if (!Files.exists(candidato)) {
            return candidato;
        }

        if (doc.getDataVencimento() != null) {
            String nomeComVenc = montarNomeArquivo(doc, true);
            Path candidatoComVenc = pastaDestino.resolve(nomeComVenc);
            if (!Files.exists(candidatoComVenc)) {
                return candidatoComVenc;
            }
            return resolverComSufixoNumerico(pastaDestino, nomeComVenc);
        }

        return resolverComSufixoNumerico(pastaDestino, nomeSemVenc);
    }

    private Path resolverComSufixoNumerico(Path pasta, String nomeArquivo) {
        String base = nomeArquivo.replaceFirst("\\.pdf$", "");
        int contador = 1;
        Path alternativo;
        do {
            alternativo = pasta.resolve(base + " (" + contador + ").pdf");
            contador++;
        } while (Files.exists(alternativo));

        return alternativo;
    }
}
