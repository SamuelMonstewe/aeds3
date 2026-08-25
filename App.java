import java.util.*;
import java.io.DataOutputStream;
import java.io.File;
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

class CSV {

}

class App {
  public static void main(String args[]) {
    File arqCsv = new File("base_livros.csv");
    File arqBin = new File("test.bin");
    final byte PRIMEIRO_ID = 0;

    try (Scanner s = new Scanner(arqCsv);
        RandomAccessFile raf = new RandomAccessFile(arqBin, "rw")) {

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

          Livro livroRecuperado = new Livro();
          livroRecuperado.fromByteArray(bytes);

          raf.writeInt(bytes.length);
          raf.write(bytes);

        } catch (Exception e) {
          System.err.println("Erro ao processar linha: " + linha + " -> " + e.getMessage());
        }
      }

      raf.seek(0);
      raf.writeInt(ultimoId);
      System.out.println("Arquivo gravado com sucesso! Último ID: " + ultimoId);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
