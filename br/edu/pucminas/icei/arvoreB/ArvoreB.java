package br.edu.pucminas.icei.arvoreB;

import java.io.IOException;
import java.io.RandomAccessFile;

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

  /**
   * Realiza a inserção de um registro na Árvore B de forma recursiva, descendo
   * até as folhas e propagando eventuais divisões (splits) na volta (bottom-up).
   * Todo o tráfego de páginas é feito em disco usando offsets (endereços).
   * 
   * O fluxo de execução divide-se em 4 passos lógicos principais:
   * 
   * Passo 1: O Caso Base (Fundo da Árvore)
   * - Se o endereço recebido for -1L, a recursão alcançou o "chão" (abaixo de uma
   * folha).
   * - O método sinaliza que um registro precisa ser inserido marcando cresceu[0]
   * = true
   * e colocando o registro a ser inserido em regRetorno[0].
   * 
   * Passo 2: Leitura e Descida Recursiva
   * - Carrega a página atual do disco.
   * - Localiza a posição de descida (ou barra a operação se a chave for
   * duplicada).
   * - Chama a si mesmo recursivamente passando o offset do filho apropriado. A
   * execução
   * desta página fica pausada aguardando o retorno do nível inferior.
   * 
   * Passo 3: O Retorno - Inserção Simples (Sem Split)
   * - Ao retornar da recursão, verifica a flag cresceu[0]. Se for true, um
   * registro "subiu".
   * - Se a página atual ainda tiver espaço (n < mm), o registro emergente e o
   * ponteiro
   * para sua nova página irmã (retornado pela recursão) são inseridos nela.
   * - A página é salva no disco, cresceu[0] é setado para false (a propagação
   * para por aqui),
   * e a função retorna o offset original da página.
   * 
   * Passo 4: O Retorno - Divisão de Página (Split)
   * - Se a página atual estiver cheia (n == mm) ao receber um registro emergente,
   * ocorre o split:
   * a) Uma nova página (irmã direita) é criada.
   * b) Os registros (os originais + o que subiu) são distribuídos: metade fica na
   * página atual e a outra metade vai para a página nova.
   * c) A chave exata do meio (mediana) é destacada e atribuída a regRetorno[0].
   * d) A flag cresceu[0] permanece true para que o pai lide com o elemento do
   * meio.
   * e) Ambas as páginas são gravadas no disco e o método retorna o endereço da
   * nova irmã.
   * 
   * @param reg        O Item (registro) a ser inserido.
   * @param endAp      O offset (endereço) da página atual no RandomAccessFile.
   * @param regRetorno Array de 1 posição contendo o Item promovido durante um
   *                   split.
   * @param cresceu    Array de 1 posição (boolean) alertando a página superior
   *                   que houve divisão.
   * 
   * @return O offset no arquivo da página direita recém-criada (em caso de split)
   *         ou o endereço atual.
   * @throws IOException Se houver falha de I/O na leitura ou gravação do
   *                     RandomAccessFile.
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
        Pagina apTemp = new Pagina(this.mm); // (irmã) [ vazio | vazio | vazio | vazio ]
        apTemp.p[0] = -1L;

        // se i <= this.m, significa que o novo registro pertence a metade esquerda
        // (página original ap)
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


  public void remove(int id) throws IOException {
    if (this.enderecoRaiz == -1L) {
        System.out.println("Erro: Árvore vazia");
        return;
    }

    boolean[] diminuiu = new boolean[1];
    boolean[] encontrou = new boolean[1];

    remove(id, this.enderecoRaiz, diminuiu, encontrou);

    if (!encontrou[0]) {
        System.out.println("Erro: Chave não encontrada");
        return;
    }

    // A raiz pode ficar sem nenhuma chave após uma fusão
    Pagina raiz = lePagina(this.enderecoRaiz);

    if (raiz.n == 0) {
        // Se possuía um filho, esse filho vira a nova raiz
        // Se não possuía, a árvore ficou vazia
        this.enderecoRaiz = raiz.p[0];
        atualizarCabecalhoRaiz();
    }
}


private void remove(int id, long endAp,
                    boolean[] diminuiu,
                    boolean[] encontrou) throws IOException {

    if (endAp == -1L) {
        diminuiu[0] = false;
        encontrou[0] = false;
        return;
    }

    Pagina ap = lePagina(endAp);

    // Descobre a primeira posição cuja chave é >= id.
    int i = 0;

    while (i < ap.n && id > ap.r[i].id) {
        i++;
    }
    
    // CASO 1: encontramos a chave nesta página

    if (i < ap.n && id == ap.r[i].id) {

        encontrou[0] = true;

        // CASO A página é folha

        if (ap.p[0] == -1L) {

            removeDaPagina(ap, i);

            escrevePagina(ap);

            // Se a página ficou abaixo do mínimo
            diminuiu[0] =
                (ap.endereco != this.enderecoRaiz && ap.n < this.m);

            return;
        }

        
        // CASO A página é interna
        // Substituímos a chave pelo predecessor

        long endPred = ap.p[i];

        Pagina pred = lePagina(endPred);

        while (pred.p[pred.n] != -1L) {
            endPred = pred.p[pred.n];
            pred = lePagina(endPred);
        }

        Item predecessor = pred.r[pred.n - 1];

        // Copia o predecessor para a posição da chave removida.
        ap.r[i].id = predecessor.id;
        ap.r[i].regPtr = predecessor.regPtr;

        escrevePagina(ap);

        boolean[] diminuiuFilho = new boolean[1];
        boolean[] encontrouPred = new boolean[1];

        remove(
            predecessor.id,
            ap.p[i],
            diminuiuFilho,
            encontrouPred
        );

        if (diminuiuFilho[0]) {
            ap = lePagina(endAp);

            corrigeUnderflow(ap, i);

            ap = lePagina(endAp);

            diminuiu[0] =
                (ap.endereco != this.enderecoRaiz && ap.n < this.m);
        } else {
            diminuiu[0] = false;
        }

        return;
    }

    // CASO 2: chave não está nesta página
  
    // Se é folha, então a chave não existe.
    if (ap.p[0] == -1L) {
        encontrou[0] = false;
        diminuiu[0] = false;
        return;
    }

    // A posição i também indica qual filho devemos visitar.
    long endFilho = ap.p[i];

    remove(id, endFilho, diminuiu, encontrou);

    // Se nem encontramos a chave, não há nada para corrigir.
    if (!encontrou[0]) {
        diminuiu[0] = false;
        return;
    }
   
    // O filho ficou abaixo do mínimo.
    // Precisamos redistribuir ou fundir.

    if (diminuiu[0]) {

        ap = lePagina(endAp);

        corrigeUnderflow(ap, i);

        ap = lePagina(endAp);

        diminuiu[0] =
            (ap.endereco != this.enderecoRaiz && ap.n < this.m);
    }
}

}
