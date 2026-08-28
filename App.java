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

      while (i < 100 && s.hasNextLine()) {
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

      // System.out.println("Arquivo gravado com sucesso! Último ID: " + ultimoId);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Optional<Livro> find(int id) {
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

          return Optional.of(livro);

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

            raf.seek(4); //pula último ID

            while(raf.getFilePointer()<raf.length()){
                int tamanhoRegistro = raf.readInt(); //lê o tamanho

                long posicaoRegistro = raf.getFilePointer();

                byte[] bytes = new byte[tamanhoRegistro];
                raf.readFully(bytes); //lê o registro e coloca o ponteiro no tamanho do próximo registro

                Livro livro = new Livro();
                livro.fromByteArray(bytes); //recupera livro

                if(!livro.getLapide() && livro.getid()==id){ //livro encontrado

                    livro.setLapide(true); //atualiza lapide
                    byte[] novosBytes = livro.toByteArray();

                    raf.seek(posicaoRegistro); //volta o ponteiro
                    raf.write(novosBytes); //escreve com a lapide atualizada

                    return true;
            }
        }

    } catch (IOException e){
        System.err.println(
            "Erro em BinaryRecordManager - delete: " + e.getMessage()
        );
    }

    return false;
    }
}

class App {
  public static void main(String args[]) {
    BinaryRecordManager manager = new BinaryRecordManager(args[0]);
    File csv = new File("base_livros.csv");
    manager.create(csv);

    Optional<Livro> livro = manager.find(100);
    livro.ifPresent(l -> l.exibirDetalhes());


    System.out.println("ANTES: ");
    manager.read(10);

    //deletando ID = 2
    
    if(manager.delete(2)){
        System.out.println("Livro deletado com sucesso");
    }else{
        System.out.println("Livro não encontrado");
    }
    
    System.out.println("Depois:\n");
    manager.read(10);
    
  }
}
