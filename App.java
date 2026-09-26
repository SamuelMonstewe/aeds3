import br.edu.pucminas.icei.binaryrecordmanager.*;
import br.edu.pucminas.icei.gui.*;
import br.edu.pucminas.icei.arvoreB.*;

class App {
  public static void main(String args[]) {
    try (ArvoreB indexId = new ArvoreB(100, "idx_livros.idx")) {

      BinaryRecordManager manager = new BinaryRecordManager(indexId, args[0]);
      GUI.exibirMenu(manager);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
