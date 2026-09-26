import br.edu.pucminas.icei.binaryrecordmanager.*;
import br.edu.pucminas.icei.gui.*;
import br.edu.pucminas.icei.arvoreB.*;

class App {
  public static void main(String args[]) {
    try (ArvoreB indexId = new ArvoreB(100, "indice.bin")) {

      BinaryRecordManager manager = new BinaryRecordManager(indexId, "dados.bin");
      GUI.exibirMenu(manager);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
