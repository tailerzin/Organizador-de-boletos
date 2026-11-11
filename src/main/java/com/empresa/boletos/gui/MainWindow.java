package com.empresa.boletos.gui;

import com.empresa.boletos.service.DocumentParser;
import com.empresa.boletos.service.EmpresaRepository;
import com.empresa.boletos.service.FileOrganizer;
import com.empresa.boletos.service.PdfTextExtractor;
import com.empresa.boletos.service.ProcessamentoService;
import com.empresa.boletos.service.ConfigService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainWindow extends JFrame {

    private static final Color FUNDO = new Color(244, 247, 251);
    private static final Color AZUL_ESCURO = new Color(28, 48, 76);
    private static final Color AZUL = new Color(42, 99, 171);
    private static final Color TEXTO = new Color(38, 50, 66);
    private static final Color TEXTO_SECUNDARIO = new Color(105, 119, 137);
    private static final Color BORDA = new Color(222, 229, 237);
    private static final Color VERDE = new Color(37, 132, 94);

    private final ConfigService config = new ConfigService();

    private final JTextField campoPastaEntrada = new JTextField();
    private final JTextField campoPastaSaida = new JTextField();
    private final JTextField campoPlanilhaEmpresas = new JTextField();
    private final JTextField campoTessdata = new JTextField();
    private final JTextField campoAbaPlanilha = new JTextField();
    private final JTextArea areaLog = new JTextArea();
    private final JButton botaoProcessar = new JButton("Processar PDFs");
    private final JProgressBar progresso = new JProgressBar();
    private final JLabel estado = new JLabel("Pronto para organizar seus arquivos");

    public MainWindow() {
        super("Boletos Organizer");
        config.carregar();

        java.net.URL icone = MainWindow.class.getResource("/icons/boletos-organizer.jpg");
        if (icone != null) {
            setIconImage(new ImageIcon(icone).getImage());
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(820, 700));
        setSize(940, 790);
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout(0, 18));
        raiz.setBackground(FUNDO);
        raiz.setBorder(new EmptyBorder(20, 22, 18, 22));
        raiz.add(montarCabecalho(), BorderLayout.NORTH);
        raiz.add(montarConteudo(), BorderLayout.CENTER);
        raiz.add(montarRodape(), BorderLayout.SOUTH);
        setContentPane(raiz);

        carregarValoresPadrao();
    }

    private JPanel montarCabecalho() {
        JPanel painel = new JPanel(new BorderLayout(16, 0));
        painel.setBackground(AZUL_ESCURO);
        painel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel marca = new JPanel();
        marca.setOpaque(false);
        marca.setLayout(new BoxLayout(marca, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Boletos Organizer");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 23));

        JLabel subtitulo = new JLabel("Organize boletos e guias fiscais em PDF com poucos cliques.");
        subtitulo.setForeground(new Color(205, 217, 232));
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitulo.setBorder(new EmptyBorder(5, 1, 0, 0));

        marca.add(titulo);
        marca.add(subtitulo);

        JLabel selo = new JLabel("PROCESSAMENTO LOCAL");
        selo.setOpaque(true);
        selo.setBackground(new Color(47, 73, 106));
        selo.setForeground(new Color(222, 233, 246));
        selo.setFont(new Font("Segoe UI", Font.BOLD, 10));
        selo.setBorder(new EmptyBorder(8, 11, 8, 11));

        painel.add(marca, BorderLayout.CENTER);
        painel.add(selo, BorderLayout.EAST);
        return painel;
    }

    private JPanel montarConteudo() {
        JPanel conteudo = new JPanel(new BorderLayout(0, 16));
        conteudo.setOpaque(false);
        conteudo.add(montarPainelConfiguracao(), BorderLayout.NORTH);
        conteudo.add(montarPainelLog(), BorderLayout.CENTER);
        return conteudo;
    }

    private JPanel montarPainelConfiguracao() {
        JPanel painel = criarCartao();
        painel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 13, 0);

        JLabel titulo = new JLabel("Configuração");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(TEXTO);
        c.gridy = 0;
        c.insets = new Insets(0, 0, 3, 0);
        painel.add(titulo, c);

        JLabel ajuda = new JLabel("Escolha onde estão os PDFs, a planilha e onde salvar os arquivos organizados.");
        ajuda.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ajuda.setForeground(TEXTO_SECUNDARIO);
        c.gridy++;
        c.insets = new Insets(0, 0, 14, 0);
        painel.add(ajuda, c);

        adicionarCampo(painel, c, "Pasta de entrada", campoPastaEntrada, "PDFs que serão processados", TipoCampo.PASTA);
        adicionarCampo(painel, c, "Pasta de saída", campoPastaSaida, "Destino organizado por empresa", TipoCampo.PASTA);
        adicionarCampo(painel, c, "Planilha de empresas (.xlsx)", campoPlanilhaEmpresas, "Nomes, CNPJs e tributos cadastrados", TipoCampo.PLANILHA);
        adicionarCampo(painel, c, "Pasta de dados do OCR (tessdata)", campoTessdata, "Usada somente quando o PDF não tem texto selecionável", TipoCampo.PASTA);
        adicionarCampo(painel, c, "Aba da planilha (opcional)", campoAbaPlanilha, "Vazia: usa a última aba; nome inexistente: também usa a última", TipoCampo.NENHUM);

        return painel;
    }

    private void adicionarCampo(JPanel painel, GridBagConstraints c, String rotulo, JTextField campo,
                                String descricao, TipoCampo tipo) {
        JPanel grupo = new JPanel(new BorderLayout(0, 5));
        grupo.setOpaque(false);

        JLabel label = new JLabel(rotulo);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(TEXTO);
        grupo.add(label, BorderLayout.NORTH);

        JPanel linha = new JPanel(new BorderLayout(8, 0));
        linha.setOpaque(false);
        prepararCampo(campo);
        linha.add(campo, BorderLayout.CENTER);
        if (tipo != TipoCampo.NENHUM) {
            JButton escolher = criarBotao("Procurar", false);
            escolher.addActionListener(e -> {
                if (tipo == TipoCampo.PLANILHA) escolherPlanilha(campo);
                else escolherPasta(campo);
            });
            linha.add(escolher, BorderLayout.EAST);
        }
        grupo.add(linha, BorderLayout.CENTER);

        JLabel dica = new JLabel(descricao);
        dica.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dica.setForeground(TEXTO_SECUNDARIO);
        grupo.add(dica, BorderLayout.SOUTH);

        c.gridy++;
        c.insets = new Insets(0, 0, 12, 0);
        painel.add(grupo, c);
    }

    private JPanel montarPainelLog() {
        JPanel cartao = criarCartao();
        cartao.setLayout(new BorderLayout(0, 12));

        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setOpaque(false);
        JLabel titulo = new JLabel("Atividade");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(TEXTO);
        JLabel dica = new JLabel("O andamento e os resultados aparecem aqui.");
        dica.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dica.setForeground(TEXTO_SECUNDARIO);
        cabecalho.add(titulo, BorderLayout.NORTH);
        cabecalho.add(dica, BorderLayout.SOUTH);
        cartao.add(cabecalho, BorderLayout.NORTH);

        areaLog.setEditable(false);
        areaLog.setLineWrap(true);
        areaLog.setWrapStyleWord(true);
        areaLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        areaLog.setForeground(new Color(48, 62, 80));
        areaLog.setBackground(new Color(249, 251, 253));
        areaLog.setBorder(new EmptyBorder(12, 13, 12, 13));

        JScrollPane scroll = new JScrollPane(areaLog);
        scroll.setBorder(new LineBorder(BORDA, 1, false));
        scroll.getViewport().setBackground(areaLog.getBackground());
        cartao.add(scroll, BorderLayout.CENTER);

        return cartao;
    }

    private JPanel montarRodape() {
        JPanel rodape = new JPanel(new BorderLayout(14, 0));
        rodape.setOpaque(false);

        JPanel status = new JPanel(new BorderLayout(0, 7));
        status.setOpaque(false);
        estado.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        estado.setForeground(VERDE);
        progresso.setIndeterminate(true);
        progresso.setPreferredSize(new Dimension(180, 5));
        progresso.setVisible(false);
        status.add(estado, BorderLayout.NORTH);
        status.add(progresso, BorderLayout.SOUTH);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        botoes.setOpaque(false);
        JButton abrirEntrada = criarBotao("Abrir entrada", false);
        abrirEntrada.addActionListener(e -> abrirPasta(campoPastaEntrada, "entrada"));
        JButton abrirSaida = criarBotao("Abrir saída", false);
        abrirSaida.addActionListener(e -> abrirPasta(campoPastaSaida, "saída"));
        botaoProcessar.setPreferredSize(new Dimension(155, 40));
        estilizarBotao(botaoProcessar, true);
        botaoProcessar.addActionListener(e -> processarTudo());
        botoes.add(abrirEntrada);
        botoes.add(abrirSaida);
        botoes.add(botaoProcessar);

        rodape.add(status, BorderLayout.CENTER);
        rodape.add(botoes, BorderLayout.EAST);
        return rodape;
    }

    private JPanel criarCartao() {
        JPanel cartao = new JPanel();
        cartao.setBackground(Color.WHITE);
        cartao.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDA, 1, false), new EmptyBorder(17, 18, 5, 18)));
        return cartao;
    }

    private void prepararCampo(JTextField campo) {
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        campo.setForeground(TEXTO);
        campo.setBackground(Color.WHITE);
        campo.setCaretColor(AZUL);
        campo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDA, 1, false), new EmptyBorder(8, 10, 8, 10)));
    }

    private JButton criarBotao(String texto, boolean primario) {
        JButton botao = new JButton(texto);
        botao.setPreferredSize(new Dimension(98, 38));
        estilizarBotao(botao, primario);
        return botao;
    }

    private void estilizarBotao(JButton botao, boolean primario) {
        botao.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botao.setForeground(primario ? Color.WHITE : TEXTO);
        botao.setBackground(primario ? AZUL : Color.WHITE);
        botao.setFocusPainted(false);
        botao.setOpaque(true);
        botao.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(primario ? AZUL : BORDA, 1, false), new EmptyBorder(7, 12, 7, 12)));
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
        config.set("pastaEntrada", campoPastaEntrada.getText().trim());
        config.set("pastaSaida", campoPastaSaida.getText().trim());
        config.set("planilhaEmpresas", campoPlanilhaEmpresas.getText().trim());
        config.set("tessdata", campoTessdata.getText().trim());
        config.set("abaPlanilha", campoAbaPlanilha.getText().trim());
        try {
            config.salvar();
        } catch (Exception e) {
            log("Aviso: não foi possível salvar as configurações: " + e.getMessage());
        }
    }

    private void escolherPasta(JTextField campoDestino) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar pasta");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File atual = campoDestino.getText().isBlank() ? null : new File(campoDestino.getText());
        if (atual != null) chooser.setCurrentDirectory(atual.isDirectory() ? atual : atual.getParentFile());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            campoDestino.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void escolherPlanilha(JTextField campoDestino) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar planilha de empresas");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("Planilhas Excel (*.xlsx)", "xlsx"));
        File atual = campoDestino.getText().isBlank() ? null : new File(campoDestino.getText());
        if (atual != null) {
            File diretorio = atual.isDirectory() ? atual : atual.getParentFile();
            if (diretorio != null) chooser.setCurrentDirectory(diretorio);
            if (atual.isFile()) chooser.setSelectedFile(atual);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            campoDestino.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void abrirPasta(JTextField campo, String descricao) {
        try {
            Path pasta = Paths.get(campo.getText().trim());
            Files.createDirectories(pasta);
            Desktop.getDesktop().open(pasta.toFile());
            log("Pasta de " + descricao + " aberta: " + pasta);
        } catch (Exception e) {
            log("Não foi possível abrir a pasta de " + descricao + ": " + e.getMessage());
        }
    }

    private void processarTudo() {
        final Path pastaEntrada;
        final Path pastaSaida;
        final Path planilha;
        final String tessdata;
        final String abaPreferida;
        try {
            if (campoPastaEntrada.getText().isBlank() || campoPastaSaida.getText().isBlank()
                    || campoPlanilhaEmpresas.getText().isBlank()) {
                mostrarErro("Preencha as pastas de entrada e saída e o caminho da planilha.");
                return;
            }
            pastaEntrada = Paths.get(campoPastaEntrada.getText().trim()).toAbsolutePath().normalize();
            pastaSaida = Paths.get(campoPastaSaida.getText().trim()).toAbsolutePath().normalize();
            planilha = Paths.get(campoPlanilhaEmpresas.getText().trim()).toAbsolutePath().normalize();
            if (pastaEntrada.equals(pastaSaida)) {
                mostrarErro("As pastas de entrada e saída precisam ser diferentes.");
                return;
            }
            tessdata = campoTessdata.getText().trim();
            abaPreferida = campoAbaPlanilha.getText().trim();
        } catch (Exception e) {
            mostrarErro("Confira os caminhos informados. Há um caminho inválido.");
            return;
        }

        salvarValoresAtuais();
        botaoProcessar.setEnabled(false);
        progresso.setVisible(true);
        estado.setForeground(AZUL);
        estado.setText("Processando PDFs…");
        areaLog.setText("");
        log("Preparando pastas e planilha...");

        java.util.function.Consumer<String> logger = linha -> SwingUtilities.invokeLater(() -> log(linha));
        SwingWorker<ProcessamentoService.Resumo, Void> worker = new SwingWorker<>() {
            @Override
            protected ProcessamentoService.Resumo doInBackground() {
                try {
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
                    return servico.processarPasta(pastaEntrada, logger);
                } catch (Exception e) {
                    logger.accept("ERRO GERAL: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    return null;
                }
            }

            @Override
            protected void done() {
                botaoProcessar.setEnabled(true);
                progresso.setVisible(false);
                try {
                    ProcessamentoService.Resumo resumo = get();
                    if (resumo == null) {
                        estado.setForeground(new Color(177, 76, 58));
                        estado.setText("O processamento terminou com erro. Confira a atividade acima.");
                    } else {
                        estado.setForeground(VERDE);
                        estado.setText("Concluído · " + resumo.identificados + " identificados · "
                                + resumo.naoIdentificados + " para revisar · " + resumo.comErro + " erros");
                    }
                } catch (Exception e) {
                    estado.setForeground(new Color(177, 76, 58));
                    estado.setText("Não foi possível concluir o processamento.");
                    log("ERRO: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void mostrarErro(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Verifique a configuração", JOptionPane.WARNING_MESSAGE);
    }

    private void log(String linha) {
        areaLog.append(linha + "\n");
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    private enum TipoCampo { PASTA, PLANILHA, NENHUM }
}
