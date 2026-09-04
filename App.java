import java.util.*;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;

class Pair {
  Livro livro;
  long endereco;

  public Pair(Livro l, long e) {
    livro = l;
    endereco = e;
  }
}

class Livro {
  int id;
  boolean lapide;
  String isbn;
  String titulo;
  String autor;
  LocalDate data;
  String generos;
  float preco;
  short paginas;

  public Livro() {

  }

  public Livro(int id, String isbn, String titulo, String autor, LocalDate data, String generos, float preco,
      short paginas) {
    setid(id);
    setIsbn(isbn);
    setTitulo(titulo);
    setAutor(autor);
    setData(data);
    setGeneros(generos);
    setPreco(preco);
    setPaginas(paginas);
    setLapide(false);
  }

  public void setid(int id) {
    this.id = id;
  }

  public int getid() {
    return this.id;
  }

  public void setLapide(boolean l) {
    this.lapide = l;
  }

  public boolean getLapide() {
    return this.lapide;
  }

  public void setIsbn(String isbn) {
    this.isbn = isbn;
  }

  public String getIsbn() {
    return this.isbn;
  }

  public void setTitulo(String titulo) {
    this.titulo = titulo;
  }

  public String getTitulo() {
    return this.titulo;
  }

  public void setAutor(String autor) {
    this.autor = autor;
  }

  public String getAutor() {
    return this.autor;
  }

  public void setData(LocalDate data) {
    this.data = data;
  }

  public LocalDate getData() {
    return this.data;
  }

  public void setGeneros(String generos) {
    this.generos = generos;
  }

  public String getGeneros() {
    return this.generos;
  }

  public void setPreco(float preco) {
    this.preco = preco;
  }

  public float getPreco() {
    return this.preco;
  }

  public void setPaginas(short paginas) {
    this.paginas = paginas;
  }

  public short getPaginas() {
    return this.paginas;
  }

  public void exibirDetalhes() {
    System.out.println("ID: " + id);
    System.out.println("ISBN: " + isbn);
    System.out.println("Titulo: " + titulo);
    System.out.println("Autor: " + autor);
    System.out.println("Data: " + data);
    System.out.println("Generos: " + generos);
    System.out.println("Preco: " + preco);
    System.out.println("Paginas: " + paginas);
    System.out.println("-------------------------");
  }

  public byte[] toByteArray() throws IOException { // livro -> bytes
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);

    dos.writeBoolean(lapide);
    dos.writeInt(id);

    for (int i = 0; i < 13; i++) { // isbn = string de tamanho fixo de 13 char, por isso nao pode usar o writeUTF
      dos.writeByte(isbn.charAt(i));
    }

    dos.writeLong(data.toEpochDay());
    dos.writeFloat(preco);
    dos.writeShort(paginas);

    dos.writeUTF(titulo);
    dos.writeUTF(autor);
    dos.writeUTF(generos);

    dos.flush();

    return baos.toByteArray(); // retorna array de bytes
  }

  public void fromByteArray(byte[] bytes) throws IOException { // bytes escritos -> recupera livro
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    DataInputStream dis = new DataInputStream(bais);
    // leitura na mesma ordem da escrita

    this.lapide = dis.readBoolean();
    this.id = dis.readInt();

    StringBuilder novoIsbn = new StringBuilder();

    for (int i = 0; i < 13; i++) { // reconstrução do isbn
      novoIsbn.append((char) dis.readByte());
    }

    this.isbn = novoIsbn.toString();

    this.data = LocalDate.ofEpochDay(dis.readLong());
    this.preco = dis.readFloat();
    this.paginas = dis.readShort();

    this.titulo = dis.readUTF();
    this.autor = dis.readUTF();
    this.generos = dis.readUTF();

  }

  public int getTamanhoRegistro() {
    try {
      return this.toByteArray().length;
    } catch (IOException e) {
      return 0;
    }
  }

}

class BinaryRecordManager {
  String FILE;

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

      int i = 0;

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

          i++;
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

    try (RandomAccessFile raf = new RandomAccessFile(arquivoBinario, "r");
        RandomAccessFile rafTemp1 = new RandomAccessFile(temp1, "rw");
        RandomAccessFile rafTemp2 = new RandomAccessFile(temp2, "rw");
        RandomAccessFile rafTemp3 = new RandomAccessFile(temp3, "rw");
        RandomAccessFile rafTemp4 = new RandomAccessFile(temp4, "rw")) {

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
}

class Factory {
  // Gera um livro padrão para testes rápidos
  public static Livro criarLivroValido() {
    return new Livro(
        1,
        "9788532511010",
        "Harry Potter e a Pedra Filosofal",
        "J.K. Rowling",
        LocalDate.of(2004, 9, 23),
        "Fantasia, Aventura",
        49.90f,
        (short) 224);
  }

  // Gera com ID dinâmico ou customizações simples
  public static Livro criarComTituloEPreco(String titulo, float preco) {
    Livro l = criarLivroValido();
    l.setTitulo(titulo);
    l.setPreco(preco);
    return l;
  }
}

class GUI {

  static void exibirMenu(BinaryRecordManager manager) {
    File csv = new File("base_livros.csv");

    Scanner sc = new Scanner(System.in);
    int opcao;

    do {
      System.out.println("\n========== SISTEMA DE LIVROS ==========");
      System.out.println("1 - Carregar base CSV");
      System.out.println("2 - Inserir livro");
      System.out.println("3 - Buscar livro por ID");
      System.out.println("4 - Atualizar livro");
      System.out.println("5 - Deletar livro");
      System.out.println("6 - Listar registros");
      System.out.println("7 - Ordenacao externa");
      System.out.println("0 - Sair");
      System.out.print("Opcao: ");

      opcao = sc.nextInt();
      sc.nextLine(); // consome o '\n'

      switch (opcao) {

        case 1:
          manager.create(csv);
          System.out.println("Base carregada com sucesso!");
          break;

        case 2: {
          Livro livro = lerLivro(sc);

          manager.insert(livro);

          System.out.println("Livro inserido com sucesso! ID: " + livro.getid());
          break;
        }

        case 3: {
          System.out.print("ID do livro: ");
          int id = sc.nextInt();
          sc.nextLine();

          Optional<Pair> resultado = manager.find(id);

          if (resultado.isPresent()) {
            resultado.get().livro.exibirDetalhes();
          } else {
            System.out.println("Livro nao encontrado.");
          }

          break;
        }

        case 4: {
          System.out.print("ID do livro que deseja atualizar: ");
          int id = sc.nextInt();
          sc.nextLine();

          System.out.println("Digite os novos dados:");

          Livro atualizado = lerLivro(sc);

          manager.update(id, atualizado);

          break;
        }

        case 5: {
          System.out.print("ID do livro que deseja deletar: ");
          int id = sc.nextInt();
          sc.nextLine();

          if (manager.delete(id)) {
            System.out.println("Livro deletado com sucesso!");
          } else {
            System.out.println("Livro nao encontrado.");
          }

          break;
        }

        case 6: {
          System.out.print("Quantidade de registros para listar: ");
          int quantidade = sc.nextInt();
          sc.nextLine();

          manager.read(quantidade);
          break;
        }

        case 7:
          // implementar
          System.out.println("Ordenacao externa ainda nao implementada.");
          break;

        case 0:
          System.out.println("Encerrando...");
          break;

        default:
          System.out.println("Opcao invalida.");
      }

    } while (opcao != 0);

    sc.close();

  }

  private static Livro lerLivro(Scanner sc) {

    Livro livro = new Livro();

    System.out.print("ISBN (13 caracteres): ");
    livro.setIsbn(sc.nextLine());
    System.out.print("Titulo: ");
    livro.setTitulo(sc.nextLine());
    System.out.print("Autor: ");
    livro.setAutor(sc.nextLine());

    System.out.print("Data (AAAA-MM-DD): ");
    livro.setData(LocalDate.parse(sc.nextLine()));

    System.out.print("Generos (separados por virgula): ");
    livro.setGeneros(sc.nextLine());

    System.out.print("Preco: ");
    livro.setPreco(Float.parseFloat(sc.nextLine()));

    System.out.print("Numero de paginas: ");
    livro.setPaginas(Short.parseShort(sc.nextLine()));
    livro.setLapide(false);

    return livro;
  }
}

class App {
  public static void main(String args[]) {
    BinaryRecordManager manager = new BinaryRecordManager(args[0]);

    GUI.exibirMenu(manager);
  }

}
