package br.edu.pucminas.icei.casamentopadroes;

import java.util.ArrayList;
import java.util.Arrays;

public class BoyerMoore {
  private final String padrao;
  private final int[] ultimaOcorrencia;
  private final int[] deslocamento;

  public BoyerMoore(String padrao) {
    if (padrao == null || padrao.isEmpty()) {
      throw new IllegalArgumentException("O padrao nao pode ser vazio.");
    }
    this.padrao = padrao;
  
    this.ultimaOcorrencia = new int[Character.MAX_VALUE + 1];
    Arrays.fill(ultimaOcorrencia, -1);
    for (int i = 0; i < padrao.length(); i++) {
      ultimaOcorrencia[padrao.charAt(i)] = i;
    }
    this.deslocamento = construirDeslocamentos(padrao);
  }

  private static int[] construirDeslocamentos(String padrao) {
    int m = padrao.length();
    int[] shift = new int[m + 1];
    int[] borda = new int[m + 1];
    int i = m;
    int j = m + 1;
    borda[i] = j;

    
    while (i > 0) {
      while (j <= m && padrao.charAt(i - 1) != padrao.charAt(j - 1)) {
        if (shift[j] == 0) {
          shift[j] = j - i;
        }
        j = borda[j];
      }
      i--;
      j--;
      borda[i] = j;
    }

    j = borda[0];
    for (i = 0; i <= m; i++) {
      if (shift[i] == 0) {
        shift[i] = j;
      }
      if (i == j) {
        j = borda[j];
      }
    }
    return shift;
  }

  public ResultadoCasamento buscar(String texto) {
    if (texto == null) {
      throw new IllegalArgumentException("O texto nao pode ser nulo.");
    }

    ArrayList<Integer> posicoes = new ArrayList<>();
    int n = texto.length();
    int m = padrao.length();
    int inicio = 0;
    long comparacoes = 0;

    while (inicio <= n - m) {
      int j = m - 1;
      while (j >= 0) {
        comparacoes++;
        if (padrao.charAt(j) != texto.charAt(inicio + j)) {
          break;
        }
        j--;
      }

      if (j < 0) {
        posicoes.add(inicio);
    
        inicio += deslocamento[0];
      } else {
        int regraCaractereRuim = j - ultimaOcorrencia[texto.charAt(inicio + j)];
        int regraBomSufixo = deslocamento[j + 1];
        inicio += Math.max(1, Math.max(regraCaractereRuim, regraBomSufixo));
      }
    }
    return new ResultadoCasamento(posicoes, comparacoes);
  }
}
