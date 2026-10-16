# Organizador de Boletos

Este programa ajuda a separar e renomear boletos e guias fiscais em PDF. Ele lê as informações do documento, procura a empresa na sua planilha Excel e organiza o arquivo em uma pasta com o nome dela. O processamento acontece localmente no computador.

## O que acontece quando você processa

O programa lê cada PDF da pasta de entrada e tenta localizar o CNPJ. Se não encontrar um CNPJ, tenta reconhecer o nome da empresa. Também procura informações como tributo, parcela, número do parcelamento e vencimento para compor um nome mais fácil de identificar.

Os documentos reconhecidos vão para `saida/Nome da Empresa/`. Se o programa não conseguir associar um arquivo a uma empresa, ele o coloca em `saida/_NAO_IDENTIFICADO/` para você conferir. Se ocorrer um erro ao ler um documento, ele fica na pasta de entrada.

> **Atenção:** processar um arquivo significa movê-lo da pasta de entrada para a pasta de saída. Coloque na entrada somente os PDFs que deseja organizar e mantenha uma cópia de segurança dos documentos importantes.

## Primeira configuração

Abra o programa e confira os campos da janela:

- **Pasta de entrada:** local onde você colocará os novos PDFs. O botão **Abrir Pasta de Entrada** cria a pasta, se necessário, e a abre no Explorador de Arquivos.
- **Pasta de saída:** destino dos PDFs organizados.
- **Planilha de empresas (.xlsx):** arquivo Excel com nomes, CNPJs e tributos cadastrados. Use o botão `...` deste campo para selecionar o arquivo `.xlsx`.
- **Pasta tessdata:** usada somente para PDFs escaneados ou sem texto selecionável. Veja [Leitura de PDFs escaneados](#leitura-de-pdfs-escaneados).
- **Aba da planilha:** opcional. Se ficar vazia, o programa procura uma aba com `Nome` e `CNPJ` na primeira linha. Se não encontrar, usa a primeira aba. Você também pode informar o nome de uma aba específica.

Na primeira execução, os caminhos sugeridos ficam dentro de uma pasta `BoletosOrganizer` na sua pasta de usuário. Você pode escolher outros locais. As configurações ficam salvas para as próximas vezes em `%USERPROFILE%\.boletos-organizer\boletos-organizer.properties`.

Se a planilha indicada não existir, o programa cria um modelo. Troque a linha de exemplo pelos dados reais antes de processar seus documentos.

## Como preparar a planilha

Use um arquivo `.xlsx` com as colunas abaixo, nesta ordem, começando na primeira linha:

| Nome | CNPJ | Tributo | Nº do Parcelamento |
| --- | --- | --- | --- |
| Empresa Exemplo LTDA | 00.000.000/0001-00 | Simples Nacional | *(opcional)* |

Cada linha representa uma combinação de empresa e tributo/parcelamento. Se uma empresa tiver mais de um tributo ou parcelamento, cadastre uma linha para cada combinação. O número do parcelamento é opcional, mas ajuda a selecionar a linha correta quando ele também aparece no documento.

Digite o CNPJ como texto para preservar zeros à esquerda. Não altere a ordem das colunas. Se o arquivo tiver várias abas, o programa procura os cabeçalhos na primeira linha; você pode indicar uma aba pelo campo correspondente.

## Sua rotina de uso

1. Salve na **Pasta de entrada** os PDFs que deseja organizar.
2. Abra o programa e confira se as pastas e a planilha selecionadas estão corretas.
3. Clique em **Processar Todos os Arquivos** e acompanhe as mensagens no log.
4. Ao final, confira quantos arquivos foram identificados, não identificados ou tiveram erro.
5. Revise `saida/_NAO_IDENTIFICADO/` e confirme os nomes e destinos antes de encaminhar os documentos.

Os PDFs identificados ficam em uma pasta por empresa. Se dois documentos tiverem o mesmo nome, o programa acrescenta o vencimento e, se ainda houver colisão, um número ao final para não substituir o arquivo existente. Os documentos com erro permanecem na entrada para você verificar e tentar novamente.

## Leitura de PDFs escaneados

O programa primeiro tenta ler o texto já presente no PDF. Se o arquivo for uma digitalização ou imagem, pode usar o Tesseract OCR. Para isso, configure o Tesseract e indique no campo **Pasta tessdata** uma pasta que contenha o arquivo de idioma português `por.traineddata`.

No Windows, o OCR também precisa das bibliotecas nativas do Tesseract. Uma opção é instalar a [versão UB-Mannheim do Tesseract](https://github.com/UB-Mannheim/tesseract/wiki) e deixar as bibliotecas disponíveis no `PATH` do sistema. PDFs digitais, com texto selecionável, não precisam do OCR.

## Se uma empresa não for reconhecida

Confira se o CNPJ está correto na planilha e se os cabeçalhos estão na primeira linha da aba escolhida. Se o documento não tiver um CNPJ legível, o nome cadastrado precisa aparecer no PDF. Para documentos escaneados, verifique se o OCR está configurado.

Os formatos de boletos variam. Por isso, parcela, tributo ou vencimento podem não ser reconhecidos em alguns modelos. Confira os nomes e os destinos gerados antes de usar os arquivos.

## Executar o programa

Para compilar o projeto, é necessário ter JDK 17 ou mais recente e Maven. Abra um terminal na pasta do projeto e execute:

```bash
mvn clean package
```

Depois, inicie o programa com:

```bash
java -jar target/boletos-organizer.jar
```

No Windows, também é possível empacotar um instalador `.exe` com o `jpackage`, incluído no JDK. O empacotamento deve ser feito no Windows.

## Privacidade

Boletos e planilhas podem conter dados pessoais, fiscais e financeiros. Não inclua documentos reais, planilhas de clientes, configurações locais ou arquivos do Tesseract no repositório. O `.gitignore` exclui PDFs, planilhas Excel, configurações locais e arquivos de build, mas confira os arquivos selecionados no Git antes de publicar.
