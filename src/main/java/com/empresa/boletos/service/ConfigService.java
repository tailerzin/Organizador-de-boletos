package com.empresa.boletos.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Guarda as pastas configuradas (entrada, saída, tessdata, planilha) em um arquivo
 * boletos-organizer.properties dentro da pasta do usuário, para não precisar reconfigurar
 * toda vez que abrir o programa.
 */
public class ConfigService {

    private static final String ARQUIVO_CONFIG = "boletos-organizer.properties";

    private final Path arquivoConfig;
    private final Properties props = new Properties();

    public ConfigService() {
        Path pastaBase = Paths.get(System.getProperty("user.home"), ".boletos-organizer");
        this.arquivoConfig = pastaBase.resolve(ARQUIVO_CONFIG);
    }

    public void carregar() {
        if (!Files.exists(arquivoConfig)) return;
        try (InputStream in = Files.newInputStream(arquivoConfig)) {
            props.load(in);
        } catch (IOException e) {
            // configuração ausente/corrompida não deve impedir o programa de abrir
        }
    }

    public void salvar() throws IOException {
        Files.createDirectories(arquivoConfig.getParent());
        try (OutputStream out = Files.newOutputStream(arquivoConfig)) {
            props.store(out, "Configuração do Boletos Organizer");
        }
    }

    public String get(String chave, String padrao) {
        return props.getProperty(chave, padrao);
    }

    public void set(String chave, String valor) {
        props.setProperty(chave, valor == null ? "" : valor);
    }
}
