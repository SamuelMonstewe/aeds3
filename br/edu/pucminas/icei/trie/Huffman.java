package br.edu.pucminas.icei.trie;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.PriorityQueue;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;

/*
 * ===============================================================
 * ║ COMPRESSÃO VIA HUFFMAN UTILIZANDO SEDGEWICK COMO REFERÊNCIA ║
 * ===============================================================
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

  // --- CLASSE AUXILIAR PARA ESCRITA DE BITS MANUAL ---
  private static class BitOutputStream {
    private RandomAccessFile out;
    private int currentByte;
    private int numBits;

    public BitOutputStream(RandomAccessFile out) {
      this.out = out;
      this.currentByte = 0;
      this.numBits = 0;
    }

    public void writeBit(boolean bit) throws IOException {
      currentByte = (currentByte << 1) | (bit ? 1 : 0);
      numBits++;
      if (numBits == 8) {
        out.writeByte(currentByte);
        currentByte = 0;
        numBits = 0;
      }
    }

    public void flush() throws IOException {
      if (numBits > 0) {
        currentByte = currentByte << (8 - numBits); // Preenche o restante com 0 (padding)
        out.writeByte(currentByte);
        currentByte = 0;
        numBits = 0;
      }
    }
  }

  // --- CLASSE AUXILIAR PARA LEITURA DE BITS MANUAL ---
  private static class BitInputStream {
    private RandomAccessFile in;
    private int currentByte;
    private int numBits;

    public BitInputStream(RandomAccessFile in) {
      this.in = in;
      this.currentByte = 0;
      this.numBits = 0;
    }

    public boolean readBit() throws IOException {
      if (numBits == 0) {
        try {
          currentByte = in.readByte() & 0xFF; // Lê um byte tratado como unsigned (0-255)
        } catch (EOFException e) {
          return false; // Fim do arquivo
        }
        numBits = 8;
      }
      numBits--;
      boolean bit = ((currentByte >>> numBits) & 1) == 1;
      return bit;
    }
  }

  public static void expand() {
    try (RandomAccessFile in = new RandomAccessFile("dbHuffmanCompressao.bin.huf", "rw");
        RandomAccessFile out = new RandomAccessFile("decompressHuffman.bin", "rw")) {

      Node root = readTrie(in);
      long totalBytes = in.readLong(); // Lê o total de bytes originais

      BitInputStream bitIn = new BitInputStream(in);

      for (long i = 0; i < totalBytes; i++) {
        Node x = root;
        while (!x.isLeaf()) {
          if (bitIn.readBit()) {
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
      System.err.print("Erro em Huffman - writeTrie: " + e.getMessage());
    }
  }

  private static Node readTrie(RandomAccessFile in) {
    try {
      if (in.readBoolean()) {
        return new Node(in.readChar(), 0, null, null);
      }
    } catch (IOException e) {
      System.err.println("Erro em Huffman - readTrie: " + e.getMessage());
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
        RandomAccessFile out = new RandomAccessFile("dbHuffmanCompressao.bin.huf", "rw")) {

      Node root = buildTrie(freq);
      String[] st = new String[R];
      buildCode(st, root, "");

      writeTrie(root, out);
      out.writeLong(totalBytes);

      BitOutputStream bitOut = new BitOutputStream(out);

      int by;
      while ((by = b.read()) != -1) {
        String code = st[by];
        for (int j = 0; j < code.length(); j++) {
          if (code.charAt(j) == '1') {
            bitOut.writeBit(true);
          } else {
            bitOut.writeBit(false);
          }
        }
      }

      bitOut.flush();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
