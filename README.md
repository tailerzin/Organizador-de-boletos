# Boletos Organizer

Aplicação Java standalone (GUI Swing) para organizar PDFs de boletos e guias fiscais. O
programa roda localmente, lê uma planilha Excel `.xlsx` de empresas e não depende de Google
Sheets ou Drive.

## Como funciona

1. Configure as pastas e arquivos na janela do programa (as opções ficam salvas automaticamente
   em `%USERPROFILE%\.boletos-organizer\boletos-organizer.properties`):
   - **Pasta de entrada**: onde você joga os PDFs baixados para processar.
   - **Pasta de saída**: onde os PDFs organizados vão parar (`Empresa/Tributo/arquivo.pdf`).
   - **Planilha de empresas (.xlsx)**: lista de empresas com CNPJ e tributo. O botão `...`
     deste campo seleciona um arquivo Excel. Se o caminho escolhido não existir, o programa cria
     um modelo automaticamente (colunas: `Nome | CNPJ | Tributo | Nº do Parcelamento`).
   - **Pasta tessdata**: onde ficam os arquivos de idioma do Tesseract (necessário só para
     PDFs escaneados — veja seção OCR abaixo).
2. Clique em **"Abrir Pasta de Entrada"** para achar rápido onde jogar os PDFs baixados.
3. Clique em **"Processar Todos os Arquivos"**. O programa:
   - Extrai o texto de cada PDF (direto via PDFBox; se vier vazio, tenta OCR automaticamente).
   - Identifica a empresa por CNPJ ou, se não achar, por nome no texto.
   - Extrai número de parcela e data de vencimento via regex.
   - Move o arquivo, renomeado, para `saida/Empresa/`.
   - Arquivos não identificados vão para `saida/_NAO_IDENTIFICADO/` — **nunca são apagados
     nem sobrescritos**, e um erro em um arquivo não interrompe o processamento dos demais.

## Ajustando os regex

Os padrões de CNPJ/parcela/vencimento/tipo de tributo estão centralizados em
`DocumentParser.java`. Como os boletos reais variam de layout, é bem provável que seja
necessário ajustar esses regex depois de testar com PDFs de verdade — mande alguns exemplos
(com dados sensíveis borrados) que eu ajusto os padrões junto com você.

## OCR (PDFs escaneados)

Para PDFs sem texto selecionável, o programa usa Tesseract via Tess4J. Você precisa:

1. Instalar o Tesseract OCR (ou apenas baixar os arquivos `tessdata` compatíveis com sua
   versão do Tess4J — não precisa instalar o Tesseract completo no Windows, só apontar a
   pasta `tessdata` com pelo menos o arquivo `por.traineddata` — [disponível aqui](https://github.com/tesseract-ocr/tessdata)).
2. Apontar a pasta que contém esses arquivos no campo "Pasta tessdata" da janela.
3. **No Windows**, o Tess4J também precisa das DLLs nativas do Tesseract/Leptonica — a
   forma mais simples é instalar o [Tesseract para Windows (UB-Mannheim build)](https://github.com/UB-Mannheim/tesseract/wiki)
   e garantir que a pasta de instalação esteja no `PATH`, ou copiar as DLLs para a mesma
   pasta do `.exe` gerado.

## Build (no seu computador, com acesso à internet)

Pré-requisitos: JDK 17+ e Maven.

```bash
mvn clean package
```

Isso gera `target/boletos-organizer.jar` (fat jar com todas as dependências).
Testar direto:

```bash
java -jar target/boletos-organizer.jar
```

## Cuidados antes de publicar

- Não envie boletos reais, planilhas de clientes, configurações locais ou arquivos `tessdata` para
  o repositório. O `.gitignore` já exclui esses tipos de arquivo e as pastas de build/IDE.
- Os padrões de extração atendem layouts específicos e precisam ser validados com documentos de
  cada emissor. Confira os nomes e destinos gerados antes de usar em lote.
- O código não inclui documentos reais de exemplo; compartilhe apenas amostras com dados
  pessoais e financeiros removidos.

## Gerando o .exe nativo (jpackage — não depende de Java instalado na máquina do trabalho)

O `jpackage` já vem com o JDK 17+. No Windows, rode (ajuste os caminhos):

```powershell
jpackage --type exe `
  --input target `
  --main-jar boletos-organizer.jar `
  --main-class com.empresa.boletos.Main `
  --name "Boletos Organizer" `
  --app-version 1.0.0 `
  --win-dir-chooser `
  --win-menu `
  --win-shortcut
```

Isso empacota um JRE mínimo junto com o `.exe`, então a máquina do trabalho **não precisa
ter Java instalado**. Rode esse comando em um computador com Windows (pode ser o de casa
ou uma máquina virtual) — o `.exe` resultante roda em qualquer Windows compatível.

> Nota: o `jpackage` deve ser executado no mesmo sistema operacional do executável final
> (gerar `.exe` requer rodar em Windows; gerar `.dmg`/`.pkg` requer macOS, etc.).

## Estrutura do projeto

```
src/main/java/com/empresa/boletos/
  Main.java                        - ponto de entrada
  gui/MainWindow.java               - janela principal (Swing)
  model/Empresa.java                - dados de uma empresa cadastrada
  model/DocumentoFiscal.java        - dados extraídos de um PDF
  service/ConfigService.java        - persiste as pastas configuradas
  service/EmpresaRepository.java    - lê a planilha local de empresas
  service/PdfTextExtractor.java     - extração de texto (PDFBox + fallback OCR)
  service/DocumentParser.java       - regex de CNPJ/parcela/vencimento/tributo
  service/FileOrganizer.java        - renomeia e move para a pasta final
  service/ProcessamentoService.java - orquestra o processamento em lote
  util/FilenameSanitizer.java       - limpa nomes de arquivo/pasta
```

## Próximos ajustes recomendados

- Validar os regex de `DocumentParser` com PDFs reais de cada tipo de tributo.
- Se algumas empresas usarem tributos diferentes por parcelamento (não fixo na planilha),
  o programa já usa `tipoTributoDetectado` do texto como alternativa — validar se está
  batendo certo.
- Considerar logging em arquivo (além da tela) para auditoria de processamentos passados.
