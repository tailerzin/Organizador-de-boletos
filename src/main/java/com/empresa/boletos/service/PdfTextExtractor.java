package com.empresa.boletos.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Extrai texto de um PDF. Tenta primeiro extração direta (PDFBox, rápida, para PDFs digitais).
 * Se o resultado vier vazio/curto demais (indício de PDF escaneado/imagem), renderiza as páginas
 * como imagem e roda OCR (Tess4J).
 *
 * Limite mínimo de caracteres para considerar que a extração direta "funcionou".
 */
public class PdfTextExtractor {

    private static final int LIMITE_MINIMO_CARACTERES = 40;
    private static final float DPI_OCR = 300f;

    private final String caminhoTessdata; // pasta com os arquivos de idioma do Tesseract (ex: por.traineddata)
    private final int maxPaginasOcr;      // limite de páginas processadas via OCR (evita travar em PDFs enormes)

    public PdfTextExtractor(String caminhoTessdata, int maxPaginasOcr) {
        this.caminhoTessdata = caminhoTessdata;
        this.maxPaginasOcr = maxPaginasOcr;
    }

    public static class Resultado {
        public final String texto;
        public final boolean viaOcr;

        public Resultado(String texto, boolean viaOcr) {
            this.texto = texto;
            this.viaOcr = viaOcr;
        }
    }

    public Resultado extrair(File pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {

            PDFTextStripper stripper = new PDFTextStripper();
            // Essencial para formulários em tabela (como o DAE da SEFAZ): sem isso, o PDFBox
            // extrai o texto na ordem interna do arquivo (todos os rótulos juntos, depois todos
            // os valores juntos), em vez da ordem visual esquerda-direita/cima-baixo da página.
            stripper.setSortByPosition(true);
            String textoDireto = stripper.getText(doc);
            if (textoDireto != null && textoDireto.trim().length() >= LIMITE_MINIMO_CARACTERES) {
                return new Resultado(textoDireto, false);
            }

            // Fallback: PDF provavelmente escaneado -> OCR
            String textoOcr = extrairViaOcr(doc);
            return new Resultado(textoOcr, true);
        }
    }

    private String extrairViaOcr(PDDocument doc) throws IOException {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(caminhoTessdata);
        tesseract.setLanguage("por"); // português

        PDFRenderer renderer = new PDFRenderer(doc);
        StringBuilder textoCompleto = new StringBuilder();

        int paginas = Math.min(doc.getNumberOfPages(), maxPaginasOcr);
        for (int i = 0; i < paginas; i++) {
            BufferedImage imagem = renderer.renderImageWithDPI(i, DPI_OCR);
            try {
                String textoPagina = tesseract.doOCR(imagem);
                textoCompleto.append(textoPagina).append("\n");
            } catch (TesseractException e) {
                throw new IOException("Falha no OCR da página " + (i + 1) + ": " + e.getMessage(), e);
            }
        }

        return textoCompleto.toString();
    }
}