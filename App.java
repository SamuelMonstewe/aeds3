import java.util.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDate;

class Livro {
  int id;
  String isbn;
  String titulo;
  String autor;
  LocalDate data;
  String generos;
  float preco;
  short paginas;

  public Livro() {

  }

  public Livro(int id, String isbn, String autor, LocalDate data, String generos, float preco, short paginas) {
    setid(id);
    setIsbn(isbn);
    setAutor(autor);
    setData(data);
    setGeneros(generos);
    setPreco(preco);
    setPaginas(paginas);
  }

  public void setid(int id) {
    this.id = id;
  }

  public int getid() {
    return this.id;
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
    System.out.printf("%d", this.id);
  }
}

class CSV {

}

class App {
  public static void main(String args[]) {
    try {
      Scanner s = new Scanner(new File("base_livros.csv"));
      s.nextLine();
      while (s.hasNextLine()) {
        try {
          String linha = s.nextLine();
          String[] dados = linha.split(",");
          Livro livro = new Livro();

          livro.setid(Integer.parseInt(dados[0]));
          livro.setIsbn(dados[1]);
          livro.setTitulo(dados[2]);
          livro.setAutor(dados[3]);
          livro.setData(LocalDate.parse(dados[4]));
          livro.setGeneros(dados[5]);
          livro.setPreco(Float.parseFloat(dados[6]));
          livro.setPaginas(Short.parseShort(dados[7]));

        } catch (InputMismatchException e) {
          System.out.println("Entrada fornecida incompatível com seu tipo!");
        }

      }
      s.close();
    } catch (Exception e) {

    }
  }
}
