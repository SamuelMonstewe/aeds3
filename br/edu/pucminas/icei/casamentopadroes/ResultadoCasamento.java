package br.edu.pucminas.icei.casamentopadroes;

import java.util.List;

public class ResultadoCasamento {
  private final List<Integer> posicoes;
  private final long comparacoes;

  public ResultadoCasamento(List<Integer> posicoes, long comparacoes) {
    this.posicoes = posicoes;
    this.comparacoes = comparacoes;
  }

  public List<Integer> getPosicoes() {
    return posicoes;
  }

  public long getComparacoes() {
    return comparacoes;
  }
}
