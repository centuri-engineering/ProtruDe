package afm2.plugins;

import afm2.image.Register;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;

// alignment based on cross correlation with first image
public class Alignment2_ implements PlugIn {
    int imaRaw;
    int radiusCC = 128;
    int maxT = 10;
    int maxAng = 10;
    private int refFrame = 1;

    @Override
    public void run(String s) {
        if (Dialogue()) {
            ImagePlus rawPlus = WindowManager.getImage(imaRaw);
            // calibration
            Calibration cal = rawPlus.getCalibration();

            //  CC1
            ImagePlus plus0 = Register.extractCurrentStack(rawPlus, 0, refFrame - 1);
            ImagePlus plusTrans2 = Register.register(rawPlus, plus0, maxT, maxAng, radiusCC);
            plusTrans2.show();

            // Z project AVG
            IJ.log("Performing maximum Z-projection");
            Register.projectionAvg(plusTrans2).show();
        }
    }

    private boolean Dialogue() {
        int nbima = WindowManager.getImageCount();
        String[] names = new String[nbima];
        for (int i = 0; i < nbima; i++) {
            names[i] = WindowManager.getImage(i + 1).getShortTitle();
        }
        imaRaw = 0;

        GenericDialog dia = new GenericDialog("AFM ALIGN 2");
        dia.addChoice("Align1", names, names[imaRaw]);
        dia.addNumericField("Radius for CC", radiusCC, 0);
        dia.addNumericField("Translation max", maxT, 0);
        dia.addNumericField("Angle max", maxAng, 0);
        dia.addNumericField("Ref frame",refFrame,0);

        dia.showDialog();
        imaRaw = dia.getNextChoiceIndex() + 1;
        radiusCC = (int) dia.getNextNumber();
        maxT = (int) dia.getNextNumber();
        maxAng = (int) dia.getNextNumber();
        refFrame = (int) dia.getNextNumber();

        return dia.wasOKed();
    }
}
