package br.edu.pucminas.icei.trie;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.PriorityQueue;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

/* ================================================================= 
 *  COMPACTAÇÃO VIA HUFFMAN UTILIZANDO SEDGEWICK COMO REFERÊNCIA
 * =================================================================
 */

public class Huffman {
  private static int R = 256; // ASCII

  public static class Node implements Comparable<Node> {
    private char ch;
    private int freq;
    private final Node left, right;

    Node(char ch, int freq, Node left, Node right) {
      this.ch = ch;
      this.freq = freq;
      this.left = left;
      this.right = right;
    }

    public boolean isLeaf() {
      return left == null && right == null;
    }

    public int compareTo(Node that) {
      return this.freq - that.freq;
    }
  }

  public static void expand() {
    try (RandomAccessFile in = new RandomAccessFile("out.bin.huf", "rw");
        RandomAccessFile out = new RandomAccessFile("decompress.bin", "rw")) {

      Node root = readTrie(in);
      long N = in.readLong(); // Lê o total de bytes gravados no arquivo compactado

      for (long i = 0; i < N; i++) {
        Node x = root; // Volta para a raiz
        while (!x.isLeaf()) {
          if (in.readBoolean()) {
            x = x.right;
          } else {
            x = x.left;
          }
        }
        out.write(x.ch);
      }

    } catch (IOException e) {
      System.err.println("Erro em Huffman - expand: " + e.getMessage());
    }
  }

  private static String[] buildCode(Node root) {
    String[] st = new String[R];
    buildCode(st, root, "");
    return st;
  }

  private static void buildCode(String[] st, Node x, String s) {
    if (x.isLeaf()) {
      st[x.ch] = s;
      return;
    }

    buildCode(st, x.left, s + '0');
    buildCode(st, x.right, s + '1');
  }

  private static Node buildTrie(int[] freq) {
    PriorityQueue<Node> pq = new PriorityQueue<>();
    for (char c = 0; c < R; c++) {
      if (freq[c] > 0) {
        pq.add(new Node(c, freq[c], null, null));
      }
    }

    while (pq.size() > 1) {
      Node x = pq.remove();
      Node y = pq.remove();
      Node parent = new Node('\0', x.freq + y.freq, x, y);
      pq.add(parent);
    }

    return pq.remove();
  }

  private static void writeTrie(Node x, RandomAccessFile out) {
    try {
      if (x.isLeaf()) {
        out.writeBoolean(true);
        out.writeChar(x.ch);
        return;
      }

      out.writeBoolean(false);

      writeTrie(x.left, out);
      writeTrie(x.right, out);
    } catch (IOException e) {
      System.err.print("Erro em Huffman - writeTrie: Falha na escrita da trie [" + e.getMessage() + "]");
    }
  }

  private static Node readTrie(RandomAccessFile in) {
    try {
      if (in.readBoolean()) {
        return new Node(in.readChar(), 0, null, null);
      }
    } catch (IOException e) {
      System.err.println("Erro em Huffman - readTrie: falha na leitura do arquivo [" + e.getMessage() + "]");
    }

    return new Node('\0', 0, readTrie(in), readTrie(in));
  }

  public static void compress(final String fileName) {
    int[] freq = new int[R];
    long totalBytes = 0;

    try (BufferedInputStream b = new BufferedInputStream(new FileInputStream(fileName))) {
      int by;
      while ((by = b.read()) != -1) {
        freq[by]++;
        totalBytes++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    try (BufferedInputStream b = new BufferedInputStream(new FileInputStream(fileName));
        RandomAccessFile out = new RandomAccessFile("out.bin.huf", "rw")) {

      Node root = buildTrie(freq);
      String[] st = new String[R];
      buildCode(st, root, "");

      writeTrie(root, out);

      out.writeLong(totalBytes);

      int by;

      while ((by = b.read()) != -1) {
        String code = st[by];
        for (int j = 0; j < code.length(); j++) {
          if (code.charAt(j) == '1') {
            out.writeBoolean(true);
          } else {
            out.writeBoolean(false);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
