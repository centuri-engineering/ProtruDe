import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;

public class HS_AFM_File_Open  implements PlugIn{

	public void run(String arg) {
		

HSAFMFileChoose open = new HSAFMFileChoose();
open.FileOpenMYG();



	}

}
