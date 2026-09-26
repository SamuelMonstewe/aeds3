import java.io.IOException;
import java.io.RandomAccessFile;

import br.edu.pucminas.icei.binaryrecordmanager.*;
import br.edu.pucminas.icei.gui.*;
import br.edu.pucminas.icei.arvoreB.*;

class App {
  public static void main(String args[]) {
    BinaryRecordManager manager = new BinaryRecordManager(new ArvoreB(100, "idx_livros.idx"), args[0]);

    GUI.exibirMenu(manager);
  }

}
