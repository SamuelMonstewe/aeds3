package br.edu.pucminas.icei.gui;

import java.io.File;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Scanner;

import br.edu.pucminas.icei.livro.Livro;
import br.edu.pucminas.icei.binaryrecordmanager.BinaryRecordManager;
import br.edu.pucminas.icei.pair.*;

public class GUI {

  public static void exibirMenu(BinaryRecordManager manager) {
    File csv = new File("base_livros.csv");

    Scanner sc = new Scanner(System.in);
    int opcao;

    do {
      System.out.println("\n========== SISTEMA DE LIVROS ==========");
      System.out.println("1 - Carregar base CSV");
      System.out.println("2 - Inserir livro");
      System.out.println("3 - Buscar livro por ID");
      System.out.println("4 - Atualizar livro");
      System.out.println("5 - Deletar livro");
      System.out.println("6 - Listar registros");
      System.out.println("7 - Ordenacao externa");
      System.out.println("8 - Montar Índice (essa operação pode levar até 1 min)");
      System.out.println("9 - Buscar Registro por indexação");
      System.out.println("10 - Atualizar Registro por Indexação");
      System.out.println("11 - Deletar Registro Por Indexação");

      System.out.println("0 - Sair");
      System.out.print("Opcao: ");

      opcao = sc.nextInt();
      sc.nextLine(); // consome o '\n'

      switch (opcao) {

        case 1: {
          manager.create(csv);
          System.out.println("Base carregada com sucesso!");
          break;

        }

        case 2: {
          Livro livro = lerLivro(sc);

          manager.insert(livro);

          System.out.println("Livro inserido com sucesso! ID: " + livro.getid());
          break;
        }

        case 3: {
          System.out.print("ID do livro: ");
          int id = sc.nextInt();
          sc.nextLine();

          long inicio = System.currentTimeMillis();
          Optional<Pair> resultado = manager.find(id);

          if (resultado.isPresent()) {
            resultado.get().livro.exibirDetalhes();
          } else {
            System.out.println("Livro nao encontrado.");
          }
          long fim = System.currentTimeMillis();
          long tempoFinal = fim - inicio;
          System.out.println("Tempo de consulta: " + tempoFinal + " ms");
          break;
        }

        case 4: {
          System.out.print("ID do livro que deseja atualizar: ");
          int id = sc.nextInt();
          sc.nextLine();

          System.out.println("Digite os novos dados:");

          Livro atualizado = lerLivro(sc);

          long ini = System.currentTimeMillis();
          manager.update(id, atualizado);

          long fim = System.currentTimeMillis();
          long tempoFinal = fim - ini;

          System.out.println("Tempo de atualização: " + tempoFinal + " ms");

          break;
        }

        case 5: {
          System.out.print("ID do livro que deseja deletar: ");
          int id = sc.nextInt();
          sc.nextLine();

          long ini = System.currentTimeMillis();
          if (manager.delete(id)) {
            System.out.println("Livro deletado com sucesso!");
          } else {
            System.out.println("Livro nao encontrado.");
          }

          long fim = System.currentTimeMillis();
          long tempoFinal = fim - ini;

          System.out.println("Tempo de deleção: " + tempoFinal + " ms");

          break;
        }

        case 6: {
          System.out.print("Quantidade de registros para listar: ");
          int quantidade = sc.nextInt();
          sc.nextLine();

          manager.read(quantidade);
          break;
        }

        case 7: {
          long ini = System.currentTimeMillis();
          manager.reorganizarArquivo();
          manager.finalizarReorganizacao();

          long fim = System.currentTimeMillis();
          long tempoFinal = fim - ini;
          System.out.println("Tempo de ordenção: " + tempoFinal + " ms");
          break;
        }

        case 8: {
          long ini = System.currentTimeMillis();
          manager.reconstruirIndice();
          long fim = System.currentTimeMillis();
          long tempoFinal = fim - ini;

          System.out.println("Tempo de montagem do índice: " + tempoFinal + " ms");
          System.out.println("Arquivo de índice construído!");

          break;
        }
        case 9: {
          System.out.print("ID do livro: ");
          int id = sc.nextInt();
          sc.nextLine();

          long ini = System.currentTimeMillis();
          Optional<Pair> opt = manager.buscarViaIndice(id);

          if (opt.isEmpty()) {
            System.out.println("Registro com o id: " + id + " não existe!");
          } else {
            opt.get().livro.exibirDetalhes();
          }
          long fim = System.currentTimeMillis();
          long tempoFinal = fim - ini;

          System.out.println("Tempo de consulta: " + tempoFinal + " ms");

          break;

        }
        case 10: {
          System.out.print("ID do livro: ");
          int id = sc.nextInt();
          sc.nextLine();

          System.out.println("Digite os novos dados:");

          Livro atualizado = lerLivro(sc);

          long ini = System.currentTimeMillis();
          manager.updateViaIndice(id, atualizado);

          long fim = System.currentTimeMillis();
          long tempoFinal = fim - ini;

          System.out.println("Tempo de operação: " + tempoFinal + " ms");
          break;
        }
        case 11: {
          System.out.print("ID do livro que deseja deletar: ");
          int id = sc.nextInt();
          sc.nextLine();

          long ini = System.currentTimeMillis();
          if (manager.deleteViaIndice(id)) {
            System.out.println("Livro deletado com sucesso!");
          } else {
            System.out.println("Livro nao encontrado.");
          }

          long fim = System.currentTimeMillis();
          long tempoFinal = fim - ini;

          System.out.println("Tempo de operação: " + tempoFinal + " ms");

          break;
        }

        case 0:
          System.out.println("Encerrando...");
          break;

        default:
          System.out.println("Opcao invalida.");
      }

    } while (opcao != 0);

    sc.close();

  }

  private static Livro lerLivro(Scanner sc) {

    Livro livro = new Livro();

    System.out.print("ISBN (13 caracteres): ");
    livro.setIsbn(sc.nextLine());
    System.out.print("Titulo: ");
    livro.setTitulo(sc.nextLine());
    System.out.print("Autor: ");
    livro.setAutor(sc.nextLine());

    System.out.print("Data (AAAA-MM-DD): ");
    livro.setData(LocalDate.parse(sc.nextLine()));

    System.out.print("Generos (separados por virgula): ");
    livro.setGeneros(sc.nextLine());

    System.out.print("Preco: ");
    livro.setPreco(Float.parseFloat(sc.nextLine()));

    System.out.print("Numero de paginas: ");
    livro.setPaginas(Short.parseShort(sc.nextLine()));
    livro.setLapide(false);

    return livro;
  }
}
