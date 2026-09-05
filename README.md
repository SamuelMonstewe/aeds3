# aeds3
# Guia Completo: Compilação, Modularização e Execução

Este guia reúne todas as etapas de configuração, compilação, geração do executável e execução do projeto.

---

## 1. Estrutura de Diretórios Esperada

Os arquivos com declaração de `package` devem obrigatoriamente seguir a mesma árvore de diretórios a partir da raiz do projeto:

```text
aeds3/
├── app.jar                    # Executável do Software
├── App.java                   # Contém o método main (sem package)
├── base_livros.csv            # Base de dados de leitura
├── br/
│   └── edu/
│       └── pucminas/
│           └── icei/
│               ├── livro/
│               │   └── Livro.java              # package br.edu.pucminas.icei.livro;
│               ├── pair/
│               │   └── Pair.java               # package br.edu.pucminas.icei.pair;
│               ├── binaryrecordmanager/
│               │   └── BinaryRecordManager.java # package br.edu.pucminas.icei.binaryrecordmanager;
│               └── gui/
│                   └── GUI.java                # package br.edu.pucminas.icei.gui;
```
## 2. Estrutura do arquivo de tamanho variável
```text
[ Cabeçalho: 4 bytes (Último ID) ]
  ├── [ Tamanho do Registro: 4 bytes (int) ]  <-- gravado por raf.writeInt(bytes.length)
  └── [ Registro Serializado: N bytes ]       <-- gravado por raf.write(bytes)
        ├── Lápide: 1 byte (boolean)
        ├── ID: 4 bytes (int)
        ├── ISBN: 13 bytes
        ├── Data: 8 bytes (long)
        ├── Preço: 4 bytes (float)
        ├── Páginas: 2 bytes (short)
        ├── Título: 2 bytes (tamanho) + M bytes (texto UTF-8)
        ├── Autor: 2 bytes (tamanho) + K bytes (texto UTF-8)
        └── Gêneros: 2 bytes (tamanho) + P bytes (texto UTF-8)
```
O tamanho total de um registro será de 38 + (M + K + P) bytes (não está incluído o tamanho do registro de 4 bytes).
