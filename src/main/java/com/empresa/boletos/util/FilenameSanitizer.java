package com.empresa.boletos.util;

import java.text.Normalizer;

public class FilenameSanitizer {

    /** Remove acentos, caracteres inválidos em nomes de arquivo/pasta no Windows, e espaços duplicados. */
    public static String sanitizar(String texto) {
        if (texto == null || texto.isBlank()) return "SEM-NOME";

        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String limpo = semAcento
                .replaceAll("[\\\\/:*?\"<>|]", "-")
                .replaceAll("\\s+", "_")
                .trim();

        return limpo.isEmpty() ? "SEM-NOME" : limpo;
    }
}
