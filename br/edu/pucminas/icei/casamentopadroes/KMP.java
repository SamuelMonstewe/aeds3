package br.edu.pucminas.icei.casamentopadroes;

import java.util.ArrayList;

public class KMP {
  private final String padrao;
  private final int[] lps;

  public KMP(String padrao) {
    if (padrao == null || padrao.isEmpty()) {
      throw new IllegalArgumentException("O padrao nao pode ser vazio.");
    }
    this.padrao = padrao;
    this.lps = construirLps(padrao);
  }

  private static int[] construirLps(String padrao) {
    int[] lps = new int[padrao.length()];
    int tamanhoPrefixo = 0;
    int i = 1;

    while (i < padrao.length()) {
      if (padrao.charAt(i) == padrao.charAt(tamanhoPrefixo)) {
        lps[i] = ++tamanhoPrefixo;
        i++;
      } else if (tamanhoPrefixo > 0) {

        tamanhoPrefixo = lps[tamanhoPrefixo - 1];
      } else {
        lps[i] = 0;
        i++;
      }
    }
    return lps;
  }

  public ResultadoCasamento buscar(String texto) {
    if (texto == null) {
      throw new IllegalArgumentException("O texto nao pode ser nulo.");
    }

    ArrayList<Integer> posicoes = new ArrayList<>();
    int i = 0; //indice no texto
    int j = 0; //indice no padrao
    long comparacoes = 0;

    while (i < texto.length()) {
      comparacoes++;
      if (texto.charAt(i) == padrao.charAt(j)) {
        i++;
        j++;

        if (j == padrao.length()) {
          posicoes.add(i - j);
          j = lps[j - 1];
        }
      } else if (j > 0) {
        j = lps[j - 1];
      } else {
        i++;
      }
    }
    return new ResultadoCasamento(posicoes, comparacoes);
  }
}
