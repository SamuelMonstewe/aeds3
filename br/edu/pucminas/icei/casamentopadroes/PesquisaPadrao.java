package br.edu.pucminas.icei.casamentopadroes;

import br.edu.pucminas.icei.livro.Livro;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;

public class PesquisaPadrao {
  public enum Campo {
    TITULO, AUTOR, GENEROS, TODOS
  }

  private static final int LIMITE_EXEMPLOS = 10;
  private final String arquivo;

  public PesquisaPadrao(String arquivo) {
    this.arquivo = arquivo;
  }

  public static class Relatorio {
    private final String padrao;
    private final Campo campo;
    private long registrosLidos;
    private long registrosAtivos;
    private long livrosEncontrados;
    private long ocorrencias;
    private long comparacoesKmp;
    private long comparacoesBoyerMoore;
    private long tempoKmpNs;
    private long tempoBoyerMooreNs;
    private final List<String> exemplos = new ArrayList<>();
    private final Set<Integer> idsNosExemplos = new HashSet<>();

    private Relatorio(String padrao, Campo campo) {
      this.padrao = padrao;
      this.campo = campo;
    }

    public long getRegistrosLidos() { return registrosLidos; }
    public long getRegistrosAtivos() { return registrosAtivos; }
    public long getLivrosEncontrados() { return livrosEncontrados; }
    public long getOcorrencias() { return ocorrencias; }
    public long getComparacoesKmp() { return comparacoesKmp; }
    public long getComparacoesBoyerMoore() { return comparacoesBoyerMoore; }
    public long getTempoKmpNs() { return tempoKmpNs; }
    public long getTempoBoyerMooreNs() { return tempoBoyerMooreNs; }
    public List<String> getExemplos() { return new ArrayList<>(exemplos); }

    public void exibir() {
      System.out.println("\n========== CASAMENTO DE PADROES ==========");
      System.out.println("Padrao: " + padrao + " | Campo(s): " + campo);
      System.out.println("Registros percorridos: " + registrosLidos);
      System.out.println("Registros ativos: " + registrosAtivos);
      System.out.println("Livros encontrados: " + livrosEncontrados);
      System.out.println("Ocorrencias totais (ambos os algoritmos): " + ocorrencias);
      System.out.println("\nAlgoritmo       Comparacoes       Tempo (ms)");
      System.out.printf(Locale.ROOT, "KMP             %-17d %.3f%n",
          comparacoesKmp, tempoKmpNs / 1_000_000.0);
      System.out.printf(Locale.ROOT, "Boyer-Moore     %-17d %.3f%n",
          comparacoesBoyerMoore, tempoBoyerMooreNs / 1_000_000.0);
      System.out.println("Tempos: pre-processamento + casamento; nao incluem leitura do arquivo.");
      System.out.println("Comparacoes: somente entre caracteres durante o casamento.");
      System.out.println("Busca sem diferenciar maiusculas/minusculas (acentos preservados).");
      if (tempoKmpNs < tempoBoyerMooreNs) {
        System.out.println("Menor tempo nesta execucao: KMP.");
      } else if (tempoBoyerMooreNs < tempoKmpNs) {
        System.out.println("Menor tempo nesta execucao: Boyer-Moore.");
      } else {
        System.out.println("Empate de tempo nesta execucao.");
      }
      if (exemplos.isEmpty()) {
        System.out.println("Nenhum livro corresponde ao padrao.");
      } else {
        System.out.println("\nExemplos (ate " + LIMITE_EXEMPLOS + " livros; posicoes iniciam em zero):");
        for (String exemplo : exemplos) {
          System.out.println(" - " + exemplo);
        }
      }
    }
  }

 
  public Relatorio pesquisar(String padrao, Campo campo) throws IOException {
    if (padrao == null || padrao.trim().isEmpty()) {
      throw new IllegalArgumentException("Informe um padrao nao vazio.");
    }
    if (campo == null) {
      throw new IllegalArgumentException("Selecione um campo valido.");
    }
    File origem = new File(arquivo);
    if (!origem.isFile()) {
      throw new IOException("Arquivo de dados inexistente: " + arquivo);
    }

   
    String normalizado = padrao.toLowerCase(Locale.ROOT);
    Relatorio relatorio = new Relatorio(padrao, campo);
    long inicio = System.nanoTime();
    KMP kmp = new KMP(normalizado);
    relatorio.tempoKmpNs += System.nanoTime() - inicio;
    inicio = System.nanoTime();
    BoyerMoore boyerMoore = new BoyerMoore(normalizado);
    relatorio.tempoBoyerMooreNs += System.nanoTime() - inicio;

    try (RandomAccessFile raf = new RandomAccessFile(origem, "r")) {
      if (raf.length() < Integer.BYTES) {
        throw new IOException("Arquivo invalido: cabecalho do ultimo ID ausente.");
      }
      raf.readInt(); // cabecalho: ultimo ID utilizado

      while (raf.getFilePointer() < raf.length()) {
        long inicioRegistro = raf.getFilePointer();
        if (raf.length() - inicioRegistro < Integer.BYTES) {
          throw new IOException("Tamanho do registro incompleto no offset " + inicioRegistro);
        }
        int tamanho = raf.readInt();
        long fimRegistro = raf.getFilePointer() + (long) tamanho;
        if (tamanho < 1 || fimRegistro > raf.length()) {
          throw new IOException("Registro invalido no offset " + inicioRegistro);
        }
        relatorio.registrosLidos++;

        boolean lapide = raf.readBoolean();
        if (lapide) {
          raf.seek(fimRegistro);
          continue;
        }
        raf.seek(inicioRegistro + Integer.BYTES);
        byte[] bytes = new byte[tamanho];
        raf.readFully(bytes);
        Livro livro = new Livro();
        livro.fromByteArray(bytes);
        relatorio.registrosAtivos++;

        // Marca apenas um livro encontrado, mesmo que haja varias ocorrencias.
        boolean encontrou = false;
        if (campo == Campo.TITULO || campo == Campo.TODOS) {
          encontrou |= pesquisarCampo("Titulo", livro.getTitulo(), livro, kmp, boyerMoore, relatorio);
        }
        if (campo == Campo.AUTOR || campo == Campo.TODOS) {
          encontrou |= pesquisarCampo("Autor", livro.getAutor(), livro, kmp, boyerMoore, relatorio);
        }
        if (campo == Campo.GENEROS || campo == Campo.TODOS) {
          encontrou |= pesquisarCampo("Generos", livro.getGeneros(), livro, kmp, boyerMoore, relatorio);
        }
        if (encontrou) {
          relatorio.livrosEncontrados++;
        }
      }
    }
    return relatorio;
  }

  private boolean pesquisarCampo(String nomeCampo, String valor, Livro livro,
      KMP kmp, BoyerMoore boyerMoore, Relatorio relatorio) {
    if (valor == null) {
      return false;
    }
    String texto = valor.toLowerCase(Locale.ROOT);

    long inicio = System.nanoTime();
    ResultadoCasamento resultadoKmp = kmp.buscar(texto);
    relatorio.tempoKmpNs += System.nanoTime() - inicio;
    relatorio.comparacoesKmp += resultadoKmp.getComparacoes();

    inicio = System.nanoTime();
    ResultadoCasamento resultadoBm = boyerMoore.buscar(texto);
    relatorio.tempoBoyerMooreNs += System.nanoTime() - inicio;
    relatorio.comparacoesBoyerMoore += resultadoBm.getComparacoes();

    if (!resultadoKmp.getPosicoes().equals(resultadoBm.getPosicoes())) {
      throw new IllegalStateException("KMP e Boyer-Moore divergiram no livro ID " + livro.getid());
    }

    List<Integer> posicoes = resultadoKmp.getPosicoes();
    relatorio.ocorrencias += posicoes.size();
    if (!posicoes.isEmpty() && relatorio.exemplos.size() < LIMITE_EXEMPLOS
        && relatorio.idsNosExemplos.add(livro.getid())) {
      relatorio.exemplos.add("ID " + livro.getid() + " | " + livro.getTitulo()
          + " | " + nomeCampo + " = '" + valor + "' | posicoes " + posicoes);
    }
    return !posicoes.isEmpty();
  }
}
