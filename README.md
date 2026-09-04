# aeds3
Estrutura do arquivo de tamanho variável
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
