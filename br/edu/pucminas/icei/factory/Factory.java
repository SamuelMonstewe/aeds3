package br.edu.pucminas.icei.factory;

import java.time.LocalDate;

import br.edu.pucminas.icei.livro.*;

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
