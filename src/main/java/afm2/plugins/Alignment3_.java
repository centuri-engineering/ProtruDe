package afm2.plugins;

import afm2.image.Register;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;

// alignment based on cross correlation with an average image
public class Alignment3_ implements PlugIn {
    int imaRaw, imaAvg;
    int radiusCC = 128;
    int maxT = 5;
    int maxAng = 5;

    @Override
    public void run(String s) {
        if (Dialogue()) {
            ImagePlus rawPlus = WindowManager.getImage(imaRaw);
            ImagePlus avg = WindowManager.getImage(imaAvg);
            // calibration
            Calibration cal = rawPlus.getCalibration();

            // compute alignment
            ImagePlus plusTrans = Register.register(rawPlus, avg, maxT, maxAng, radiusCC);
            plusTrans.show();

            // Z project AVG
            IJ.log("Performing maximum Z-projection");
            Register.projectionAvg(plusTrans).show();
        }
    }

    private boolean Dialogue() {
        int nbima = WindowManager.getImageCount();
        String[] names = new String[nbima];
        for (int i = 0; i < nbima; i++) {
            names[i] = WindowManager.getImage(i + 1).getShortTitle();
        }
        imaRaw = 0;
        imaAvg = nbima > 1 ? nbima - 1 : 0;

        GenericDialog dia = new GenericDialog("AFM ALIGN 3");
        dia.addChoice("Align2", names, names[imaRaw]);
        dia.addChoice("AVG", names, names[imaAvg]);
        dia.addNumericField("Radius for CC", radiusCC, 0);
        dia.addNumericField("Translation max", maxT, 0);
        dia.addNumericField("Angle max", maxAng, 0);

        dia.showDialog();
        imaRaw = dia.getNextChoiceIndex() + 1;
        imaAvg = dia.getNextChoiceIndex() + 1;
        radiusCC = (int) dia.getNextNumber();
        maxT = (int) dia.getNextNumber();
        maxAng = (int) dia.getNextNumber();

        return dia.wasOKed();
    }
}
