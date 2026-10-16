package com.empresa.boletos.gui;

import com.empresa.boletos.service.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainWindow extends JFrame {

    private final ConfigService config = new ConfigService();

    private final JTextField campoPastaEntrada = new JTextField();
    private final JTextField campoPastaSaida = new JTextField();
    private final JTextField campoPlanilhaEmpresas = new JTextField();
    private final JTextField campoTessdata = new JTextField();
    private final JTextField campoAbaPlanilha = new JTextField();
    private final JTextArea areaLog = new JTextArea();
    private final JButton botaoProcessar = new JButton("Processar Todos os Arquivos");

    public MainWindow() {
        super("Organizador de Boletos e Guias Fiscais");
        config.carregar();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 590);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(montarPainelConfiguracao(), BorderLayout.NORTH);
        add(montarPainelLog(), BorderLayout.CENTER);
        add(montarPainelBotoes(), BorderLayout.SOUTH);

        carregarValoresPadrao();
    }

    private JPanel montarPainelConfiguracao() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        adicionarLinha(painel, c, 0, "Pasta de entrada (PDFs a processar):", campoPastaEntrada, true);
        adicionarLinha(painel, c, 1, "Pasta de saída (organizada por Empresa/Tributo):", campoPastaSaida, true);
        adicionarLinha(painel, c, 2, "Planilha de empresas (.xlsx):", campoPlanilhaEmpresas, true);
        adicionarLinha(painel, c, 3, "Pasta tessdata (dados de idioma do OCR):", campoTessdata, true);
        adicionarLinha(painel, c, 4, "Aba da planilha a usar (vazio = última aba):", campoAbaPlanilha, false);

        return painel;
    }

    private void adicionarLinha(JPanel painel, GridBagConstraints c, int linha, String rotulo, JTextField campo, boolean comBotaoEscolher) {
        c.gridx = 0; c.gridy = linha; c.weightx = 0;
        painel.add(new JLabel(rotulo), c);

        c.gridx = 1; c.weightx = 1;
        painel.add(campo, c);

        if (comBotaoEscolher) {
            c.gridx = 2; c.weightx = 0;
            JButton botaoEscolher = new JButton("...");
            botaoEscolher.addActionListener(e -> {
                if (campo == campoPlanilhaEmpresas) escolherPlanilha(campo);
                else escolherPasta(campo);
            });
            painel.add(botaoEscolher, c);
        }
    }

    private JScrollPane montarPainelLog() {
        areaLog.setEditable(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(areaLog);
        scroll.setBorder(BorderFactory.createTitledBorder("Log de processamento"));
        return scroll;
    }

    private JPanel montarPainelBotoes() {
        JPanel painel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton botaoAbrirEntrada = new JButton("Abrir Pasta de Entrada");
        botaoAbrirEntrada.addActionListener(e -> abrirPastaEntrada());

        botaoProcessar.addActionListener(e -> processarTudo());

        painel.add(botaoAbrirEntrada);
        painel.add(botaoProcessar);
        return painel;
    }

    private void carregarValoresPadrao() {
        String base = Paths.get(System.getProperty("user.home"), "BoletosOrganizer").toString();
        campoPastaEntrada.setText(config.get("pastaEntrada", base + File.separator + "entrada"));
        campoPastaSaida.setText(config.get("pastaSaida", base + File.separator + "saida"));
        campoPlanilhaEmpresas.setText(config.get("planilhaEmpresas", base + File.separator + "empresas.xlsx"));
        campoTessdata.setText(config.get("tessdata", base + File.separator + "tessdata"));
        campoAbaPlanilha.setText(config.get("abaPlanilha", ""));
    }

    private void salvarValoresAtuais() {
        config.set("pastaEntrada", campoPastaEntrada.getText());
        config.set("pastaSaida", campoPastaSaida.getText());
        config.set("planilhaEmpresas", campoPlanilhaEmpresas.getText());
        config.set("tessdata", campoTessdata.getText());
        config.set("abaPlanilha", campoAbaPlanilha.getText());
        try {
            config.salvar();
        } catch (Exception e) {
            log("Aviso: não foi possível salvar as configurações: " + e.getMessage());
        }
    }

    private void escolherPasta(JTextField campoDestino) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (!campoDestino.getText().isBlank()) {
            chooser.setCurrentDirectory(new File(campoDestino.getText()));
        }
        int resultado = chooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            campoDestino.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void escolherPlanilha(JTextField campoDestino) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar planilha de empresas");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Planilhas Excel (*.xlsx)", "xlsx"));
        if (!campoDestino.getText().isBlank()) {
            File arquivoAtual = new File(campoDestino.getText());
            File diretorio = arquivoAtual.isDirectory() ? arquivoAtual : arquivoAtual.getParentFile();
            if (diretorio != null) chooser.setCurrentDirectory(diretorio);
            if (arquivoAtual.isFile()) chooser.setSelectedFile(arquivoAtual);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            campoDestino.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void abrirPastaEntrada() {
        try {
            Path pasta = Paths.get(campoPastaEntrada.getText());
            Files.createDirectories(pasta);
            Desktop.getDesktop().open(pasta.toFile());
            log("Pasta de entrada pronta: " + pasta);
        } catch (Exception e) {
            log("Erro ao abrir/criar pasta de entrada: " + e.getMessage());
        }
    }

    private void processarTudo() {
        salvarValoresAtuais();
        botaoProcessar.setEnabled(false);
        areaLog.setText("");

        java.util.function.Consumer<String> logger = linha -> SwingUtilities.invokeLater(() -> log(linha));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    Path pastaEntrada = Paths.get(campoPastaEntrada.getText());
                    Path pastaSaida = Paths.get(campoPastaSaida.getText());
                    Path planilha = Paths.get(campoPlanilhaEmpresas.getText());
                    String tessdata = campoTessdata.getText();
                    String abaPreferida = campoAbaPlanilha.getText();

                    Files.createDirectories(pastaEntrada);
                    Files.createDirectories(pastaSaida);

                    EmpresaRepository empresaRepository = new EmpresaRepository(planilha, abaPreferida);
                    empresaRepository.carregar();
                    logger.accept("Aba da planilha usada: " + empresaRepository.getAbaCarregada());
                    logger.accept("Empresas carregadas: " + empresaRepository.listar().size());

                    PdfTextExtractor extractor = new PdfTextExtractor(tessdata, 15);
                    DocumentParser parser = new DocumentParser();
                    FileOrganizer organizer = new FileOrganizer(pastaSaida);

                    ProcessamentoService servico = new ProcessamentoService(
                            empresaRepository, extractor, parser, organizer);

                    servico.processarPasta(pastaEntrada, logger);

                } catch (Throwable e) {
                    logger.accept("ERRO GERAL: " + e.getClass().getName() + ": " + e.getMessage());
                    java.io.StringWriter sw = new java.io.StringWriter();
                    e.printStackTrace(new java.io.PrintWriter(sw));
                    logger.accept(sw.toString());
                }
                return null;
            }

            @Override
            protected void done() {
                botaoProcessar.setEnabled(true);
            }
        };

        worker.execute();
    }

    private void log(String linha) {
        areaLog.append(linha + "\n");
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }
}
