package afm2.plugins;

import afm2.image.Register;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.macro.Variable;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.stream.IntStream;

public class CorrelationMatrix_ implements PlugIn {
    int imaRaw;
    int radiusCC = 128;

    @Override
    public void run(String s) {
        if (Dialogue()) {
            ImagePlus rawPlus = WindowManager.getImage(imaRaw);
            ImageStack stack = rawPlus.getStack();
            int nbFrames = rawPlus.getNFrames();
            // compute correlation
            ResultsTable resultsTable = ResultsTable.getResultsTable();
            if(resultsTable == null) resultsTable = new ResultsTable();
            final ResultsTable rt = resultsTable;
            IntStream.rangeClosed(1, nbFrames).forEach(ref -> {
                IJ.log("Processing "+ref);
                Variable[] vars = new Variable[nbFrames];
                ImageProcessor refProcessor = stack.getProcessor(ref);
                IntStream.rangeClosed(1, nbFrames).forEach(f -> {
                    ImageProcessor processor = stack.getProcessor(f);
                    double cc = Register.correlation(processor, refProcessor, radiusCC);
                    vars[f-1] = new Variable(cc);
                });
                rt.setColumn("CC"+ref,vars);

            });

            rt.show("CC");
        }
    }

    private boolean Dialogue() {
        int nbima = WindowManager.getImageCount();
        String[] names = new String[nbima];
        for (int i = 0; i < nbima; i++) {
            names[i] = WindowManager.getImage(i + 1).getShortTitle();
        }
        imaRaw = 0;

        GenericDialog dia = new GenericDialog("AFM CORRELATION");
        dia.addChoice("Video", names, names[imaRaw]);
        dia.addNumericField("Radius for CC", radiusCC, 0);

        dia.showDialog();
        imaRaw = dia.getNextChoiceIndex() + 1;
        radiusCC = (int) dia.getNextNumber();

        return dia.wasOKed();
    }
}
