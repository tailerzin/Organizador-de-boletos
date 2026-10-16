package com.empresa.boletos.model;

/**
 * Representa uma linha da planilha local de empresas:
 * Nome | CNPJ | Tributo | Nº do Parcelamento (opcional, hoje só usado para Estadual)
 */
public class Empresa {

    private final String nome;                 // como está na planilha, inteiro
    private final String nomeExibicao;          // sem anotações "(...)" — usado em pasta/arquivo
    private final String cnpj;
    private final String tributo;
    private final String numeroParcelamento;

    public Empresa(String nome, String cnpj, String tributo, String numeroParcelamento) {
        this.nome = nome == null ? "" : nome.trim();
        this.nomeExibicao = this.nome.replaceAll("\\([^)]*\\)", " ").replaceAll("\\s+", " ").trim();
        this.cnpj = cnpj == null ? "" : cnpj.replaceAll("[^0-9]", "");
        this.tributo = tributo == null ? "" : tributo.trim();
        this.numeroParcelamento = numeroParcelamento == null ? "" : numeroParcelamento.trim();
    }

    public String getNome() {
        return nome;
    }

    /** Nome sem anotações entre parênteses (ex: "(Lucro Real)", "(carla)") — para pasta/arquivo. */
    public String getNomeExibicao() {
        return nomeExibicao;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getTributo() {
        return tributo;
    }

    public String getNumeroParcelamento() {
        return numeroParcelamento;
    }

    @Override
    public String toString() {
        return nome + " (" + cnpj + ") - " + tributo
                + (numeroParcelamento.isEmpty() ? "" : " [" + numeroParcelamento + "]");
    }
}