package com.empresa.boletos.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentParser {

    // Exige a palavra "CNPJ" (ou "CGC") por perto — evita pegar por engano outro número de
    // 14 dígitos do documento (ex: número de parcelamento, código de barras).
    private static final Pattern CNPJ_PATTERN =
            Pattern.compile("(?i)(?:cnpj|cgc)[:\\s./nº-]{0,15}([\\d./-]{14,20})");

    // Formato mais comum: "Parcela 12/60", "Parcela: 12 de 60" — rótulo e valor colados no texto.
    private static final Pattern PARCELA_PATTERN =
            Pattern.compile("(?i)parcela[:\\s]*n?[ºo°]?\\s*(\\d{1,3})\\s*(?:/|de)\\s*(\\d{1,3})");

    // Formato "Parcela atual: 17" + "Total de parcelas: 60" — usado como ÚLTIMO recurso (ver
    // extrairNumeroParcela), porque em guias Estaduais reemitidas em lote no mesmo dia esse valor
    // vem igual em todas, mesmo sendo parcelas diferentes.
    private static final Pattern PARCELA_ATUAL_PATTERN =
            Pattern.compile("(?i)parcela\\s+atual\\s*:?\\s*(\\d{1,3})");
    private static final Pattern TOTAL_PARCELAS_PATTERN =
            Pattern.compile("(?i)total\\s+de\\s+parcelas\\s*:?\\s*(\\d{1,3})");

    // Valor específico desta guia (campo "13 - PARCELA" do DAE Estadual): aparece sozinho numa
    // linha, junto do código do município de 4 dígitos, seguido só de asteriscos — ex: "6 5044
    // *****************". Mais confiável que "Parcela atual", que repete o mesmo valor em guias
    // reemitidas em lote no mesmo dia.
    private static final Pattern PARCELA_CAMPO13_PATTERN =
            Pattern.compile("(?m)^\\s*(\\d{1,3})\\s+(\\d{4})\\s+\\*+\\s*$");

    // Formato do INMETRO/GRU: rótulo "Parcela(s):" e o valor real ("4/15") aparece só mais
    // adiante no texto extraído (tabela com rótulos e valores em blocos separados).
    private static final Pattern PARCELA_INMETRO_PATTERN =
            Pattern.compile("(?is)parcela\\(s\\)\\s*:.{0,300}?(\\d{1,3})\\s*/\\s*(\\d{1,3})");

    // Número de parcelamento da guia Estadual (SEFAZ-PE, campo "Documento origem do recolhimento",
    // formato "TIPO 14 Nº 2025.000003599715-39"). Específico desse layout por enquanto.
    private static final Pattern NUMERO_PARCELAMENTO_ESTADUAL_PATTERN =
            Pattern.compile("(?i)tipo\\s+14\\s+n[ºo°]?\\.?\\s*([0-9][0-9.\\-]{8,30})");

    // Número de negociação do PGFN, formato "PGFN-SISPAR:017129755" nas Observações do DARF.
    private static final Pattern NUMERO_SISPAR_PGFN_PATTERN =
            Pattern.compile("(?i)sispar\\s*:?\\s*([0-9]{5,})");

    private static final Pattern DATA_PATTERN =
            Pattern.compile("(\\d{2})[/\\-](\\d{2})[/\\-](\\d{4})");

    private static final Pattern VENCIMENTO_CONTEXTO =
            Pattern.compile("(?i)vencimento[:\\s]*" + DATA_PATTERN.pattern());

    public String extrairCnpj(String texto) {
        if (texto == null) return null;
        Matcher m = CNPJ_PATTERN.matcher(texto);
        if (m.find()) {
            String digitos = m.group(1).replaceAll("[^0-9]", "");
            if (digitos.length() == 14) {
                return digitos;
            }
        }
        return null;
    }

    public String extrairNumeroParcela(String texto) {
        if (texto == null) return null;

        Matcher campo13 = PARCELA_CAMPO13_PATTERN.matcher(texto);
        Matcher totalEstadual = TOTAL_PARCELAS_PATTERN.matcher(texto);
        if (campo13.find() && totalEstadual.find()) {
            return campo13.group(1) + "-" + totalEstadual.group(1);
        }

        Matcher direto = PARCELA_PATTERN.matcher(texto);
        if (direto.find()) {
            return direto.group(1) + "-" + direto.group(2);
        }

        Matcher inmetro = PARCELA_INMETRO_PATTERN.matcher(texto);
        if (inmetro.find()) {
            return inmetro.group(1) + "-" + inmetro.group(2);
        }

        // último recurso: menos preciso em lotes reemitidos no mesmo dia, mas melhor que nada
        Matcher atual = PARCELA_ATUAL_PATTERN.matcher(texto);
        Matcher total2 = TOTAL_PARCELAS_PATTERN.matcher(texto);
        if (atual.find() && total2.find()) {
            return atual.group(1) + "-" + total2.group(1);
        }

        // DARF sem rótulo nenhum (ex: Simplificado sem SISPAR): desativado — o regex estava
        // batendo no lugar errado do texto às vezes, dando número de parcela errado (pior que
        // não ter diferenciador). Nesse caso cai no vencimento mesmo, que é seguro.
        return null;
    }

    /** Número de parcelamento/negociação (Estadual ou SISPAR do PGFN). Retorna null nos demais
     *  tipos — é esperado, não é erro; o nome do arquivo usa outro diferenciador nesse caso. */
    public String extrairNumeroParcelamento(String texto) {
        if (texto == null) return null;

        Matcher estadual = NUMERO_PARCELAMENTO_ESTADUAL_PATTERN.matcher(texto);
        if (estadual.find()) {
            return estadual.group(1).trim();
        }

        Matcher sispar = NUMERO_SISPAR_PGFN_PATTERN.matcher(texto);
        if (sispar.find()) {
            return sispar.group(1).trim();
        }

        return null;
    }

    public String extrairDataVencimento(String texto) {
        if (texto == null) return null;

        Matcher comContexto = VENCIMENTO_CONTEXTO.matcher(texto);
        if (comContexto.find()) {
            return normalizarData(comContexto.group(1), comContexto.group(2), comContexto.group(3));
        }

        Matcher qualquerData = DATA_PATTERN.matcher(texto);
        if (qualquerData.find()) {
            return normalizarData(qualquerData.group(1), qualquerData.group(2), qualquerData.group(3));
        }
        return null;
    }

    private String normalizarData(String dia, String mes, String ano) {
        try {
            LocalDate data = LocalDate.of(Integer.parseInt(ano), Integer.parseInt(mes), Integer.parseInt(dia));
            return data.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception e) {
            return dia + "-" + mes + "-" + ano;
        }
    }

    public String detectarTipoTributo(String texto) {
        if (texto == null) return null;
        String t = texto.toUpperCase();

        if (t.contains("SIMPLES NACIONAL") || t.contains("SIMEI")) return "SIMPLES_NACIONAL";
        if (t.contains("PGFN") || t.contains("PROCURADORIA")) return "PGFN";
        if (t.contains("INSS") || t.contains("GPS")) return "INSS";
        if (t.contains("SEFAZ") || t.contains("ICMS") || t.contains("ESTADUAL")) return "ESTADUAL";
        if (t.contains("SIMPLIFICADO")) return "SIMPLIFICADO";
        if (t.contains("INMETRO") || t.contains("METROLOGIA")) return "INMETRO";

        return null;
    }
}