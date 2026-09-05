package br.edu.pucminas.icei.binaryrecordmanager;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.Scanner;

import br.edu.pucminas.icei.livro.*;
import br.edu.pucminas.icei.pair.Pair;

public class BinaryRecordManager {
  public String FILE;

  public BinaryRecordManager() {

  }

  public BinaryRecordManager(String f) {
    FILE = f;
  }

  /*
   * Estrutura do arquivo
   * r_i é um resgistro de tamanho variável
   * [Id do Ultimo registro][r_1][r_2][...][r_n]
   */

  public void create(File csv) {
    File arquivoBinario = new File(FILE);
    final int PRIMEIRO_ID = 0;

    try (Scanner s = new Scanner(csv);
        RandomAccessFile raf = new RandomAccessFile(arquivoBinario, "rw")) {

      raf.setLength(0);

      if (raf.length() == 0) {
        raf.writeInt(PRIMEIRO_ID);
      }

      int ultimoId = 0;

      if (s.hasNextLine()) {
        s.nextLine();
      }

      while (s.hasNextLine()) {
        String linha = s.nextLine();
        try {
          String[] dados = linha.split(",");

          Livro livro = new Livro();

          livro.setLapide(false);
          livro.setid(Integer.parseInt(dados[0]));
          livro.setIsbn(dados[1]);
          livro.setTitulo(dados[2]);
          livro.setAutor(dados[3]);
          livro.setData(LocalDate.parse(dados[4]));
          livro.setGeneros(dados[5]);
          livro.setPreco(Float.parseFloat(dados[6]));
          livro.setPaginas(Short.parseShort(dados[7]));

          if (livro.getid() > ultimoId) {
            ultimoId = livro.getid();
          }

          // testes livro <->bytes
          byte[] bytes = livro.toByteArray();

          raf.writeInt(bytes.length);
          raf.write(bytes);

        } catch (Exception e) {
          System.err.println("Erro ao processar linha: " + linha + " -> " + e.getMessage());
        }
      }

      raf.seek(0);
      raf.writeInt(ultimoId);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void insert(Livro livro) {
    File arquivoBinario = new File(FILE);

    try (RandomAccessFile raf = new RandomAccessFile(arquivoBinario, "rw")) {
      int ultimoId = 0;

      if (raf.length() > 0) {
        ultimoId = raf.readInt();
      }

      livro.setid(++ultimoId);

      raf.seek(raf.length());

      byte[] bytes = livro.toByteArray();

      raf.writeInt(bytes.length);
      raf.write(bytes);
      raf.seek(0);
      raf.writeInt(ultimoId);

    } catch (IOException e) {
      System.err.println("Erro em BinaryRecordManager - insert: Erro na leitura do arquivo -> " + e.getMessage());
    }
  }

  public void update(int id, Livro atualizado) {
    Optional<Pair> p = find(id);

    if (p.isEmpty()) {
      System.out.println("O arquivo não contem esse registro!");
      return;
    }

    Pair pair = p.get();

    atualizado.setid(id);

    File arquivoBinario = new File(FILE);

    try (RandomAccessFile raf = new RandomAccessFile(arquivoBinario, "rw")) {
      long posInicialRegistroAntigo = pair.endereco;

      raf.seek(posInicialRegistroAntigo);

      long tamanhoRegistroAntigo = raf.readInt();

      byte[] bytesAtualizado = atualizado.toByteArray();
      int tamanhoRegistroAtualizado = bytesAtualizado.length;

      if (tamanhoRegistroAtualizado <= tamanhoRegistroAntigo) {
        raf.write(bytesAtualizado);
      } else {
        raf.seek(posInicialRegistroAntigo + 4);
        raf.writeBoolean(true);

        raf.seek(raf.length());

        raf.writeInt(tamanhoRegistroAtualizado);
        raf.write(bytesAtualizado);
      }
    } catch (IOException e) {
      System.err.println("Erro em BinaryRecordManager - update: Erro na leitura do arquivo -> " + e.getMessage());
    }

  }

  public Optional<Pair> find(int id) {
    File arquivoBinario = new File(FILE);

    try (RandomAccessFile raf = new RandomAccessFile(arquivoBinario, "r")) {
      raf.seek(4); // pula os 4 bytes do cabeçalho que armazena o ultimo id

      while (raf.getFilePointer() < raf.length()) {

        long posInicioRegistro = raf.getFilePointer();
        int tamanhoRegistro = raf.readInt();
        boolean lapideRegistro = raf.readBoolean();
        int idLivro = raf.readInt();

        if (!lapideRegistro && id == idLivro) {
          raf.seek(posInicioRegistro + 4); // +4 para pular os bytes que identificam o tamanho

          byte[] bytes = new byte[tamanhoRegistro];
          raf.readFully(bytes);

          Livro livro = new Livro();
          livro.fromByteArray(bytes);
          livro.setLapide(false);

          return Optional.of(new Pair(livro, posInicioRegistro));

        } else {
          raf.seek(posInicioRegistro + 4 + tamanhoRegistro); // voltamos para o início do registro, pulamos os 4 bytes
                                                             // que guardam o tamanho do registro
          // e depois pulamos o registro por si só
        }

      }

    } catch (EOFException e) {
      System.err.println("Erro em BinaryRecordManager - find: Erro na leitura do arquivo: Fim de arquivo alcançado -> "
          + e.getMessage());
    } catch (FileNotFoundException e) {
      System.err.println("Erro em BinaryRecordManager - find: O arquivo não existe");
    } catch (IOException e) {
      System.err.println("Erro em BinaryRecordManager - find: Erro na leitura do arquivo -> " + e.getMessage());
    }

    return Optional.empty();
  }

  public void read(int limiteLeitura) {
    File arquivoBinario = new File(FILE);

    try (RandomAccessFile raf = new RandomAccessFile(arquivoBinario, "r")) {
      if (raf.length() == 0) {
        System.out.println("Arquivo vazio.");
        return;
      }

      raf.readInt();

      int i = 0;

      while (i < limiteLeitura && raf.getFilePointer() < raf.length()) {
        int tamanhoRegistro = raf.readInt();
        byte[] bytes = new byte[tamanhoRegistro];

        raf.readFully(bytes);

        Livro livro = new Livro();
        livro.fromByteArray(bytes);

        if (!livro.getLapide()) {
          livro.exibirDetalhes();
        }

        i++;
      }

    } catch (EOFException e) {
      System.err.println("Erro em BinaryRecordManager - read: Erro na leitura do arquivo: Fim de arquivo alcançado -> "
          + e.getMessage());
    } catch (FileNotFoundException e) {
      System.err.println("Erro em BinaryRecordManager - read: O arquivo não existe");
    } catch (IOException e) {
      System.err.println("Erro em BinaryRecordManager - read: Erro na leitura do arquivo -> " + e.getMessage());
    }
  }

  public boolean delete(int id) {
    File arquivoBinario = new File(FILE);

    try (RandomAccessFile raf = new RandomAccessFile(arquivoBinario, "rw")) {

      if (raf.length() == 0) {
        return false;
      }

      raf.seek(4); // pula último ID

      while (raf.getFilePointer() < raf.length()) {
        int tamanhoRegistro = raf.readInt(); // lê o tamanho

        long posicaoRegistro = raf.getFilePointer();

        byte[] bytes = new byte[tamanhoRegistro];
        raf.readFully(bytes); // lê o registro e coloca o ponteiro no tamanho do próximo registro

        Livro livro = new Livro();
        livro.fromByteArray(bytes); // recupera livro

        if (!livro.getLapide() && livro.getid() == id) { // livro encontrado

          livro.setLapide(true); // atualiza lapide
          byte[] novosBytes = livro.toByteArray();

          raf.seek(posicaoRegistro); // volta o ponteiro
          raf.write(novosBytes); // escreve com a lapide atualizada

          return true;
        }
      }

    } catch (IOException e) {
      System.err.println(
          "Erro em BinaryRecordManager - delete: " + e.getMessage());
    }

    return false;
  }

  // esse método utiliza intercalação balanceada de 4 caminhos
  // um benefício de ter escolhido um tamanho de buffer como 25k
  // é a capacidade de ordenar os elementos apenas com 2 passadas:
  /*
   * 25k
   * 25k 50k 100k
   * 25k 50k
   * 25k
   */
  public void reorganizarArquivo() {

    final int TAM_BUFFER = 25000;
    File arquivoBinario = new File(FILE);
    ArrayList<Livro> buffer = new ArrayList<>(TAM_BUFFER);

    int i = 0;

    File temp1 = new File("temp1.bin");
    File temp2 = new File("temp2.bin");
    File temp3 = new File("temp3.bin");
    File temp4 = new File("temp4.bin");

    try {
      if (!temp1.exists()) {
        temp1.createNewFile();
      }
      if (!temp2.exists()) {
        temp2.createNewFile();
      }
      if (!temp3.exists()) {
        temp3.createNewFile();
      }
      if (!temp3.exists()) {
        temp3.createNewFile();
      }
      if (!temp4.exists()) {
        temp4.createNewFile();
      }
    } catch (IOException e) {
      System.out.println("Erro em BinaryRecordManager - reorganizarArquivo: Falha ao criar arquivo temporário ");
      e.printStackTrace();
    }

    try (RandomAccessFile raf = new RandomAccessFile(arquivoBinario, "r");
        RandomAccessFile rafTemp1 = new RandomAccessFile(temp1, "rw");
        RandomAccessFile rafTemp2 = new RandomAccessFile(temp2, "rw");
        RandomAccessFile rafTemp3 = new RandomAccessFile(temp3, "rw");
        RandomAccessFile rafTemp4 = new RandomAccessFile(temp4, "rw")) {

      rafTemp1.setLength(0);
      rafTemp2.setLength(0);
      rafTemp3.setLength(0);
      rafTemp4.setLength(0);

      RandomAccessFile[] rafsTemp = { rafTemp1, rafTemp2, rafTemp3, rafTemp4 };

      raf.readInt(); // pula o cabeçalho do arquivo

      while (raf.getFilePointer() < raf.length()) {
        int tamRegistro = raf.readInt();
        long posicaoAntesDaLapide = raf.getFilePointer();
        boolean lapide = raf.readBoolean();

        if (lapide) {
          raf.seek(posicaoAntesDaLapide + tamRegistro);
        } else {
          raf.seek(posicaoAntesDaLapide);

          Livro livro = new Livro();
          byte[] bytes = new byte[tamRegistro];
          raf.readFully(bytes);
          livro.fromByteArray(bytes);

          buffer.add(livro);

          if (buffer.size() == TAM_BUFFER) {
            buffer.sort(Comparator.comparingInt(l -> l.getid()));

            for (Livro l : buffer) {
              byte[] b = l.toByteArray();
              rafsTemp[i].writeInt(b.length);
              rafsTemp[i].write(b);
            }

            i = (i + 1) % 4;
            buffer.clear();
          }
        }
      }

      if (!buffer.isEmpty()) {
        buffer.sort(Comparator.comparingInt(l -> l.getid()));

        for (Livro l : buffer) {
          byte[] b = l.toByteArray();
          rafsTemp[i].writeInt(b.length);
          rafsTemp[i].write(b);
        }
      }
    } catch (IOException e) {
      System.err.println("Erro em BinaryRecordManager - read: Erro na leitura do arquivo -> " + e.getMessage());
    }
  }

  private Livro lerLivroTemporario(RandomAccessFile raf) throws IOException {
    if (raf.getFilePointer() >= raf.length()) {
      return null;
    }

    int tamRegistro = raf.readInt();

    byte[] bytes = new byte[tamRegistro];
    raf.readFully(bytes);

    Livro livro = new Livro();
    livro.fromByteArray(bytes);

    return livro;
  }

  private void escreverLivroTemporario(
      RandomAccessFile raf,
      Livro livro) throws IOException {

    byte[] bytes = livro.toByteArray();

    raf.writeInt(bytes.length);
    raf.write(bytes);
  }

  private boolean aindaTemDados(
      RandomAccessFile[] arquivos) throws IOException {

    for (RandomAccessFile raf : arquivos) {
      if (raf.getFilePointer() < raf.length()) {
        return true;
      }
    }

    return false;
  }

  private void intercalarUmaRun(
      RandomAccessFile[] entradas,
      RandomAccessFile saida,
      long tamanhoRun) throws IOException {

    Livro[] atuais = new Livro[4];

    // Quantos registros já foram consumidos da run
    // atual de cada arquivo
    long[] lidos = new long[4];

    // Pega o primeiro registro da run atual de cada um dos quatro caminhos.

    for (int i = 0; i < 4; i++) {

      if (entradas[i].getFilePointer() < entradas[i].length()) {

        atuais[i] = lerLivroTemporario(entradas[i]);
        lidos[i] = 1;
      }
    }

    while (true) {

      int menor = -1;

      // Procura o menor registro entre os quatro registros atualmente carregados

      for (int i = 0; i < 4; i++) {

        if (atuais[i] != null) {

          if (menor == -1 ||
              atuais[i].getid() < atuais[menor].getid()) {

            menor = i;
          }
        }
      }

      // Se todos estão null, todas as runs atuais terminaram.

      if (menor == -1) {
        break;
      }

      escreverLivroTemporario(
          saida,
          atuais[menor]);

      /*
       * Avança somente no caminho de onde saiu
       * o menor elemento.
       *
       * não ultrapassar tamanhoRun
       * pois depois dele começa a próxima run
       * daquele arquivo
       */

      if (lidos[menor] < tamanhoRun &&
          entradas[menor].getFilePointer() < entradas[menor].length()) {

        atuais[menor] = lerLivroTemporario(entradas[menor]);

        lidos[menor]++;

      } else {

        atuais[menor] = null;
      }
    }
  }

  private int executarPassada(
      File[] entradas,
      File[] saidas,
      long tamanhoRun) throws IOException {

    RandomAccessFile[] in = new RandomAccessFile[4];

    RandomAccessFile[] out = new RandomAccessFile[4];

    try {

      for (int i = 0; i < 4; i++) {

        in[i] = new RandomAccessFile(entradas[i], "r");

        out[i] = new RandomAccessFile(saidas[i], "rw");

        out[i].setLength(0);
      }

      int indiceSaida = 0;
      int quantidadeRuns = 0;

      /*
       * Enquanto existir pelo menos uma run
       * em algum dos quatro arquivos.
       */
      while (aindaTemDados(in)) {

        intercalarUmaRun(
            in,
            out[indiceSaida],
            tamanhoRun);

        quantidadeRuns++;

        // Distribuição balanceada das novas runs

        indiceSaida = (indiceSaida + 1) % 4;
      }

      return quantidadeRuns;

    } finally {

      for (int i = 0; i < 4; i++) {

        if (in[i] != null) {
          in[i].close();
        }

        if (out[i] != null) {
          out[i].close();
        }
      }
    }
  }

  private void copiarResultadoParaArquivoPrincipal(
      File arquivoResultado) throws IOException {

    File arquivoPrincipal = new File(FILE);

    int ultimoId;

    /*
     * Guarda o cabeçalho.
     *
     * Ele representa o último ID utilizado e
     * precisa continuar existindo depois da
     * reorganização.
     */
    try (RandomAccessFile original = new RandomAccessFile(arquivoPrincipal, "r")) {

      original.seek(0);
      ultimoId = original.readInt();
    }

    try (
        RandomAccessFile origem = new RandomAccessFile(arquivoResultado, "r");

        RandomAccessFile destino = new RandomAccessFile(arquivoPrincipal, "rw")) {

      /*
       * Apaga fisicamente o conteúdo antigo,
       * com os registros com lápide.
       */
      destino.setLength(0);

      // restaura cabeçalho
      destino.writeInt(ultimoId);

      while (origem.getFilePointer() < origem.length()) {

        int tamRegistro = origem.readInt();

        byte[] bytes = new byte[tamRegistro];

        origem.readFully(bytes);

        destino.writeInt(tamRegistro);
        destino.write(bytes);
      }
    }
  }

  private void limparArquivoPrincipal()
      throws IOException {

    File arquivoPrincipal = new File(FILE);

    try (RandomAccessFile raf = new RandomAccessFile(arquivoPrincipal, "rw")) {

      int ultimoId = 0;

      if (raf.length() >= 4) {
        ultimoId = raf.readInt();
      }

      /*
       * Se todos os registros estavam deletados,
       * sobra somente o cabeçalho.
       */
      raf.setLength(0);
      raf.writeInt(ultimoId);
    }
  }

  private void apagarTemporarios(
      File[] arquivos) {

    for (File arquivo : arquivos) {

      if (arquivo.exists()) {
        arquivo.delete();
      }
    }
  }

  public void finalizarReorganizacao() {

    final long TAM_BUFFER = 25000;

    /*
     * Esses são exatamente os quatro arquivos
     * produzidos pelo reorganizarArquivo()
     * existente.
     */
    File[] entrada = {
        new File("temp1.bin"),
        new File("temp2.bin"),
        new File("temp3.bin"),
        new File("temp4.bin")
    };

    /*
     * Segundo conjunto de quatro caminhos.
     *
     * Em cada passada entrada e saída trocam
     * de função.
     */
    File[] saida = {
        new File("saida1.bin"),
        new File("saida2.bin"),
        new File("saida3.bin"),
        new File("saida4.bin")
    };

    try {

      long tamanhoRun = TAM_BUFFER;

      while (true) {

        int quantidadeRuns = executarPassada(
            entrada,
            saida,
            tamanhoRun);

        /*
         * Nenhum registro válido foi encontrado.
         * Isso acontece, por exemplo, se todos
         * estiverem com lápide.
         */
        if (quantidadeRuns == 0) {

          limparArquivoPrincipal();

          apagarTemporarios(entrada);
          apagarTemporarios(saida);

          System.out.println(
              "Arquivo reorganizado. Nao existem registros ativos.");

          return;
        }

        /*
         * Uma única run significa que todos
         * os registros estão ordenados.
         *
         * Como indiceSaida começa em zero,
         * essa única run está em saida[0].
         */
        if (quantidadeRuns == 1) {

          copiarResultadoParaArquivoPrincipal(
              saida[0]);

          apagarTemporarios(entrada);
          apagarTemporarios(saida);

          System.out.println(
              "Arquivo reorganizado e ordenado com sucesso!");

          return;
        }

        /*
         * Como a intercalação é de quatro caminhos,
         * uma nova run pode ter até quatro vezes
         * o tamanho da run anterior.
         */
        tamanhoRun *= 4;

        /*
         * A saída desta passada vira a entrada
         * da próxima.
         */
        File[] aux = entrada;
        entrada = saida;
        saida = aux;
      }

    } catch (IOException e) {

      System.err.println(
          "Erro ao finalizar reorganizacao: "
              + e.getMessage());
    }
  }

  public void verificarArquivoTemporario(String nomeArquivo) {
    try (RandomAccessFile raf = new RandomAccessFile(nomeArquivo, "r")) {
      System.out.println("--- Lendo arquivo: " + nomeArquivo + " ---");

      int contagem = 0;
      int ultimoId = -1;
      boolean estaOrdenado = true;

      // Lê até o final do arquivo
      while (raf.getFilePointer() < raf.length()) {
        int tamRegistro = raf.readInt();
        byte[] bytes = new byte[tamRegistro];
        raf.readFully(bytes);

        Livro livro = new Livro();
        livro.fromByteArray(bytes); // Reconstrói o objeto

        // Imprime apenas os 5 primeiros para você dar uma olhada visual
        if (contagem < 5) {
          System.out.println("Registro " + (contagem + 1) + " -> ID: " + livro.getid());
        }

        // Verifica se a ordem crescente foi mantida
        if (ultimoId != -1 && livro.getid() < ultimoId) {
          estaOrdenado = false;
        }
        ultimoId = livro.getid();
        contagem++;
      }

      System.out.println("...");
      System.out.println("Total de registros encontrados: " + contagem);
      System.out.println("Os registros estão ordenados por ID? " + (estaOrdenado ? "SIM!" : "NÃO!"));
      System.out.println("----------------------------------------\n");

    } catch (IOException e) {
      System.out.println(
          "EErro em BinaryRecordManager - verificarArquivoTemporario erro ao ler o arquivo: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
