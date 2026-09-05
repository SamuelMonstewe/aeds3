import br.edu.pucminas.icei.binaryrecordmanager.*;
import br.edu.pucminas.icei.gui.*;

class App {
  public static void main(String args[]) {
    BinaryRecordManager manager = new BinaryRecordManager(args[0]);

    GUI.exibirMenu(manager);
  }

}
