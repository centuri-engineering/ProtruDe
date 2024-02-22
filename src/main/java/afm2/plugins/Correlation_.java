package afm2.plugins;

import afm2.image.Register;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.IntStream;

public class Correlation_ implements PlugIn {
    int imaRaw, imaRef;
    int radiusCC = 128;

    @Override
    public void run(String s) {
        if (Dialogue()) {
            ImagePlus rawPlus = WindowManager.getImage(imaRaw);
            ImageStack rawStack = rawPlus.getStack();
            ImagePlus refPlus = WindowManager.getImage(imaRef);
            ImageStack refStack = refPlus.getStack();
            int nbRef = refStack.size();

            // compute correlation
            int nbFrames = rawPlus.getNFrames();
            double[] frames = new double[nbFrames];
            double[] values1 = new double[nbFrames];
            IntStream.rangeClosed(1, nbFrames).forEach(f -> {
                ImageProcessor processor = rawStack.getProcessor(f);
                final AtomicReference<Double> bestCC = new AtomicReference<>();
                bestCC.set(0.0);
                final AtomicInteger bestRef = new AtomicInteger(0);
                IntStream.rangeClosed(1, nbRef).forEach(r -> {
                    ImageProcessor ref = refStack.getProcessor(r);
                    double cc = Register.correlation(processor, ref, radiusCC);
                    if (cc > bestCC.get()) {
                        bestCC.set(cc);
                        bestRef.set(r);
                    }
                });
                IJ.log("Frame " + f + " : " + bestCC + " " + bestRef);
                frames[f - 1] = f;
                values1[f - 1] = bestRef.get();
            });

            // plot values
            Plot plot = new Plot("Correlation", "Frames", "CC Value");
            plot.setColor(Color.RED);
            plot.setLineWidth(2);
            plot.addPoints(frames, values1, Plot.LINE);
            plot.show();
        }
    }

    private boolean Dialogue() {
        int nbima = WindowManager.getImageCount();
        String[] names = new String[nbima];
        for (int i = 0; i < nbima; i++) {
            names[i] = WindowManager.getImage(i + 1).getShortTitle();
        }
        imaRaw = 0;
        imaRef = 1;

        GenericDialog dia = new GenericDialog("AFM CORRELATION");
        dia.addChoice("Video", names, names[imaRaw]);
        dia.addChoice("Ref", names, names[imaRef]);
        dia.addNumericField("Radius for CC", radiusCC, 0);

        dia.showDialog();
        imaRaw = dia.getNextChoiceIndex() + 1;
        imaRef = dia.getNextChoiceIndex() + 1;
        radiusCC = (int) dia.getNextNumber();

        return dia.wasOKed();
    }
}
