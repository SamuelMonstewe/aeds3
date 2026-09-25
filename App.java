import java.io.IOException;
import java.io.RandomAccessFile;

import br.edu.pucminas.icei.binaryrecordmanager.*;
import br.edu.pucminas.icei.gui.*;

class Item {
  int id;
  long regPtr;
}

class ArvoreB {
  private RandomAccessFile arquivoIndice;
  long enderecoRaiz;
  int m, mm;

  public static class Pagina {
    int n;
    Item r[];
    long p[];
    long endereco; // Guarda a própria posição no arquivo

    public Pagina(int mm) {
      n = 0;
      endereco = -1; // -1 significa que é uma página nova (não salva)
      r = new Item[mm];
      p = new long[mm + 1];

      for (int i = 0; i < mm; i++) {
        r[i] = new Item(); // Evita NullPointerException na hora de escrever
      }
      for (int i = 0; i <= mm; i++) {
        p[i] = -1;
      }
    }
  }

  // Construtor modificado para abrir o arquivo e ler o cabeçalho
  public ArvoreB(int m, String caminhoArquivo) throws IOException {
    this.m = m;
    this.mm = 2 * m;
    this.arquivoIndice = new RandomAccessFile(caminhoArquivo, "rw");

    if (this.arquivoIndice.length() < 8) {
      // Arquivo novo: Inicializa o cabeçalho (raiz = -1 indica árvore vazia)
      this.enderecoRaiz = -1;
      atualizarCabecalhoRaiz();
    } else {
      // Arquivo já existe: lê o endereço da raiz
      this.arquivoIndice.seek(0);
      this.enderecoRaiz = this.arquivoIndice.readLong();
    }
  }

  // Agora recebe apenas a página e decide onde gravar
  public void escrevePagina(Pagina ap) throws IOException {
    if (ap.endereco == -1) {
      ap.endereco = arquivoIndice.length(); // Se é nova, vai pro final do arquivo
    }
    arquivoIndice.seek(ap.endereco);
    arquivoIndice.writeInt(ap.n);

    for (int i = 0; i < this.mm; i++) {
      if (i < ap.n) {
        arquivoIndice.writeInt(ap.r[i].id);
        arquivoIndice.writeLong(ap.r[i].regPtr);
      } else {
        arquivoIndice.writeInt(-1);
        arquivoIndice.writeLong(-1);
      }
    }

    for (int i = 0; i <= this.mm; i++) {
      arquivoIndice.writeLong(ap.p[i]);
    }
  }

  public Pagina lePagina(long offset) throws IOException {
    arquivoIndice.seek(offset);
    Pagina ap = new Pagina(this.mm);
    ap.endereco = offset; // Salva o endereço de onde essa página veio
    ap.n = arquivoIndice.readInt();

    for (int i = 0; i < this.mm; i++) {
      ap.r[i].id = arquivoIndice.readInt();
      ap.r[i].regPtr = arquivoIndice.readLong();
    }

    for (int i = 0; i <= this.mm; i++) {
      ap.p[i] = arquivoIndice.readLong();
    }

    return ap;
  }

  private void atualizarCabecalhoRaiz() throws IOException {
    arquivoIndice.seek(0); // Volta pro começo do arquivo
    arquivoIndice.writeLong(this.enderecoRaiz);
  }

  public Item pesquisa(Item reg) throws IOException {
    return this.pesquisa(reg, this.enderecoRaiz);
  }

  private Item pesquisa(Item reg, long offsetPagina) throws IOException {
    if (offsetPagina == -1) {
      return null;
    } else {
      int i = 0;
      Pagina ap = lePagina(offsetPagina);
      while ((i < ap.n - 1) && (reg.id > ap.r[i].id)) {
        i++;
      }

      if (reg.id == ap.r[i].id) {
        return ap.r[i];
      } else if (reg.id < ap.r[i].id) {
        return pesquisa(reg, ap.p[i]);
      } else {
        return pesquisa(reg, ap.p[i + 1]);
      }
    }
  }

  public void insere(Item reg) throws IOException {
    Item regRetorno[] = new Item[1];
    boolean cresceu[] = new boolean[1];
    long endApRetorno = this.insere(reg, this.enderecoRaiz, regRetorno, cresceu);

    if (cresceu[0]) {
      Pagina novaRaiz = new Pagina(this.mm);
      novaRaiz.r[0] = regRetorno[0];
      novaRaiz.p[0] = this.enderecoRaiz;
      novaRaiz.p[1] = endApRetorno;
      novaRaiz.n = 1;

      escrevePagina(novaRaiz);
      this.enderecoRaiz = novaRaiz.endereco;
      atualizarCabecalhoRaiz();
    }
  }

  /*
   * cresceu[] avisa para a página pai se a filha estourou
   * regRetorno[] é o registro que está subindo para o pai
   * Como em java não possui variáveis passadas por referencia,
   * usamos arrays de 1 posição
   */
  private long insere(Item reg, long endAp, Item[] regRetorno, boolean[] cresceu) throws IOException {
    // cheguei em null (-1)?
    if (endAp == -1L) {
      cresceu[0] = true; // (tem um novo item para inserir)
      regRetorno[0] = reg; // leva esse item até o pai
      return -1L;
    }

    Pagina ap = lePagina(endAp);

    int i = 0;
    while ((i < ap.n - 1) && (reg.id > ap.r[i].id)) {
      i++;
    }

    if (reg.id == ap.r[i].id) {
      System.out.println("Erro: Chave já existente no índice");
      cresceu[0] = false;
      return endAp;
    }

    // Se o reg.id for menor que ap.r[i].id, vá para a subárvore esquerda
    // da raiz atual; caso contrário, vá para a direita.

    long endFilho = (reg.id < ap.r[i].id) ? ap.p[i] : ap.p[i + 1];

    long endApRetorno = insere(reg, endFilho, regRetorno, cresceu);

    if (cresceu[0]) {
      // a página tem espaço?
      if (ap.n < this.mm) {
        insereNaPagina(ap, regRetorno[0], endApRetorno);
        escrevePagina(ap);
        cresceu[0] = false;
        return ap.endereco;
      } else {
        // se não tiver espaço, precisamos fazer o split
        Pagina apTemp = new Pagina(this.mm);
        apTemp.p[0] = -1L;

        if (i <= this.m) {
          insereNaPagina(apTemp, ap.r[this.mm - 1], ap.p[this.mm]);
          ap.n--;
          insereNaPagina(ap, regRetorno[0], endApRetorno);
        } else {
          insereNaPagina(apTemp, regRetorno[0], endApRetorno);
        }

        for (int j = this.m + 1; j < this.mm; j++) {
          insereNaPagina(apTemp, ap.r[j], ap.p[j + 1]);
          ap.p[j + 1] = -1L;
        }

        ap.n = this.m;// A página original agora fica só com metade dos registros
        apTemp.p[0] = ap.p[this.m + 1];
        ap.p[this.m + 1] = -1L;

        regRetorno[0] = ap.r[this.m]; // O item do MEIO é escolhido para subir!

        escrevePagina(apTemp);
        escrevePagina(ap);

        return apTemp.endereco;
      }
    }

    return ap.endereco;
  }

  private void insereNaPagina(Pagina ap, Item reg, long endApDir) {
    int k = ap.n - 1;
    while ((k >= 0) && (reg.id < ap.r[k].id)) {
      ap.r[k + 1].id = ap.r[k].id;
      ap.r[k + 1].regPtr = ap.r[k].regPtr;
      ap.p[k + 2] = ap.p[k + 1];
      k--;
    }

    ap.r[k + 1].id = reg.id;
    ap.r[k + 1].regPtr = reg.regPtr;
    ap.p[k + 2] = endApDir;
    ap.n++;
  }
}

class App {
  public static void main(String args[]) {
    BinaryRecordManager manager = new BinaryRecordManager(args[0]);

    GUI.exibirMenu(manager);
  }

}
