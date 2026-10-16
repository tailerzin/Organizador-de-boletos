package com.empresa.boletos.model;

import java.nio.file.Path;

/**
 * Resultado da análise de um PDF: dados extraídos + empresa identificada (se houver).
 */
public class DocumentoFiscal {

    private Path arquivoOriginal;
    private String textoExtraido;

    private boolean textoViaOcr;

    private String cnpjEncontrado;
    private String numeroParcela;
    private String dataVencimento; // já normalizada, formato dd-MM-yyyy
    private String numeroParcelamentoExtraido;
    private String nomeParaArquivo; // nome "canônico" da empresa (mesmo CNPJ = mesmo nome, mesmo que a planilha tenha grafias diferentes)
    private Empresa empresa;       // null se não identificada
    private String tipoTributoDetectado; // pode vir do texto, mesmo sem empresa

    private String motivoFalha;    // preenchido quando não foi possível processar

    public String getNumeroParcelamentoExtraido() { return numeroParcelamentoExtraido; }
    public void setNumeroParcelamentoExtraido(String v) { this.numeroParcelamentoExtraido = v; }

    public Path getArquivoOriginal() { return arquivoOriginal; }
    public void setArquivoOriginal(Path arquivoOriginal) { this.arquivoOriginal = arquivoOriginal; }

    public String getTextoExtraido() { return textoExtraido; }
    public void setTextoExtraido(String textoExtraido) { this.textoExtraido = textoExtraido; }

    public boolean isTextoViaOcr() { return textoViaOcr; }
    public void setTextoViaOcr(boolean textoViaOcr) { this.textoViaOcr = textoViaOcr; }

    public String getCnpjEncontrado() { return cnpjEncontrado; }
    public void setCnpjEncontrado(String cnpjEncontrado) { this.cnpjEncontrado = cnpjEncontrado; }

    public String getNumeroParcela() { return numeroParcela; }
    public void setNumeroParcela(String numeroParcela) { this.numeroParcela = numeroParcela; }

    public String getNomeParaArquivo() { return nomeParaArquivo; }
    public void setNomeParaArquivo(String v) { this.nomeParaArquivo = v; }

    public String getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(String dataVencimento) { this.dataVencimento = dataVencimento; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public String getTipoTributoDetectado() { return tipoTributoDetectado; }
    public void setTipoTributoDetectado(String tipoTributoDetectado) { this.tipoTributoDetectado = tipoTributoDetectado; }

    public String getMotivoFalha() { return motivoFalha; }
    public void setMotivoFalha(String motivoFalha) { this.motivoFalha = motivoFalha; }

    public boolean isIdentificado() {
        return empresa != null;
    }
}
