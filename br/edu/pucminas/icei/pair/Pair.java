package br.edu.pucminas.icei.pair;

import br.edu.pucminas.icei.livro.*;

public class Pair {
  public Livro livro;
  public long endereco;

  public Pair(Livro l, long e) {
    livro = l;
    endereco = e;
  }
}
