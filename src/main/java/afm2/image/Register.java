package afm2.image;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;

import java.util.stream.IntStream;

public class Register {
    public static ImagePlus register(ImagePlus rawPlus,ImagePlus refPlus, int maxT, int maxAng, int radiusCC){
        // calibration
        Calibration cal = rawPlus.getCalibration();
        int nFrames = rawPlus.getNFrames();
        ImageStack stackAligned = new ImageStack(rawPlus.getWidth(), rawPlus.getHeight());
        ImageProcessor ref = refPlus.getProcessor();
        IntStream.rangeClosed(1, nFrames).forEach(f -> {
            IJ.log("\\Update:Aligning " + f + " / " + nFrames + "    ");
            ImagePlus plus = Register.extractCurrentStack(rawPlus, 0, f);
            ImageProcessor processor = plus.getProcessor();
            processor = Register.bestTranslation(processor, ref, maxT, radiusCC);
            processor = Register.bestRotation(processor, ref, maxAng, radiusCC);
            stackAligned.addSlice(processor);
        });
        ImagePlus plusAligned = new ImagePlus("Aligned", stackAligned);
        IJ.run(plusAligned, "Properties...", "channels=1 slices=1 frames=" + nFrames);
        plusAligned.setCalibration(cal);

        return plusAligned;
    }

    public static ImagePlus projectionAvg(ImagePlus plus){
        ZProjector zProjector = new ZProjector();
        zProjector.setMethod(ZProjector.AVG_METHOD);
        zProjector.setStartSlice(1);
        zProjector.setStopSlice(plus.getNSlices());
        zProjector.setImage(plus);
        zProjector.doProjection();
        return zProjector.getProjection();
    }

    public static ImageProcessor bestTranslation(ImageProcessor ima, ImageProcessor ref, int maxT, int radius) {
        double maxCC = Double.NEGATIVE_INFINITY;
        int bestX = 1000;
        int bestY = 1000;
        ImageProcessor best = ima.duplicate();
        for (int tx = -maxT; tx <= maxT; tx++) {
            for (int ty = -maxT; ty <= maxT; ty++) {
                double cc = correlationValueTranslate(ima, ref, tx, ty, radius);
                if (cc > maxCC) {
                    maxCC = cc;
                    bestX = tx;
                    bestY = ty;
                }
            }
        }
        best.setBackgroundValue(0);
        best.translate(bestX, bestY);

        return best;
    }

    public static ImageProcessor bestRotation(ImageProcessor ima, ImageProcessor ref, int maxAng, int radius) {
        double maxCC = Double.NEGATIVE_INFINITY;
        int bestAng = 1000;
        ImageProcessor best = ima.duplicate();
        for (int ang = -maxAng; ang <= maxAng; ang++) {
            double cc = correlationValueRotate(ima, ref, ang, radius);
            if (cc > maxCC) {
                maxCC = cc;
                bestAng = ang;
            }
        }
        best.setBackgroundValue(0);
        best.setInterpolationMethod(ImageProcessor.BICUBIC);
        best.rotate(bestAng);

        return best;
    }

    public static double correlation(ImageProcessor ima, ImageProcessor ref, int radius){
        int KernelWidth = ref.getWidth();
        int KernelHeight = ref.getHeight();
        int countpix = 0;
        double kmean = 0;
        double smean = 0;
        double tk = 0;
        double ts = 0;
        double skk = 0;
        double sss = 0;
        double sks = 0;
        double rfactor = 0;
        double tiny = 1.0e-20;
        double dist2;
        int radius2;
        if (radius > 0) {
            radius2 = radius * radius;
        } else {
            radius2 = Integer.MAX_VALUE;
        }
        int xc = KernelWidth / 2;
        int yc = KernelHeight / 2;

        for (int xx = 0; xx < KernelWidth; xx++) {
            for (int yy = 0; yy < KernelHeight; yy++) {
                dist2 = (xx - xc) * (xx - xc) + (yy - yc) * (yy - yc);
                if (dist2 < radius2) {
                    kmean += ref.getPixelValue(xx, yy);
                    smean += ima.getPixelValue(xx, yy);
                    countpix++;
                }
            }
        }
        kmean /= countpix;
        smean /= countpix;
        for (int xx = 0; xx < KernelWidth; xx++) {
            for (int yy = 0; yy < KernelHeight; yy++) {
                dist2 = (xx - xc) * (xx - xc) + (yy - yc) * (yy - yc);
                if (dist2 < radius2) {
                    tk = ref.getPixelValue(xx, yy) - kmean;
                    ts = ima.getPixelValue(xx, yy) - smean;
                    skk += tk * tk;
                    sss += ts * ts;
                    sks += tk * ts;
                }
            }
        }
        rfactor = sks / (Math.sqrt(skk * sss) + tiny);

        return rfactor;
    }

    private static double correlationValueTranslate(ImageProcessor ima, ImageProcessor ref, int tx, int ty, int radius) {
        int KernelWidth = ref.getWidth();
        int KernelHeight = ref.getHeight();
        int countpix = 0;
        double kmean = 0;
        double smean = 0;
        double tk = 0;
        double ts = 0;
        double skk = 0;
        double sss = 0;
        double sks = 0;
        double rfactor = 0;
        double tiny = 1.0e-20;
        int xxr;
        int yyr;
        double dist2;
        int radius2;
        if (radius > 0) {
            radius2 = radius * radius;
        } else {
            radius2 = Integer.MAX_VALUE;
        }
        int xc = KernelWidth / 2;
        int yc = KernelHeight / 2;

        for (int xx = 0; xx < KernelWidth; xx++) {
            for (int yy = 0; yy < KernelHeight; yy++) {
                dist2 = (xx - xc) * (xx - xc) + (yy - yc) * (yy - yc);
                if (dist2 < radius2) {
                    xxr = xx + tx;
                    yyr = yy + ty;
                    kmean += ref.getPixelValue(xxr, yyr);
                    smean += ima.getPixelValue(xx, yy);
                    countpix++;
                }
            }
        }
        kmean /= countpix;
        smean /= countpix;
        for (int xx = 0; xx < KernelWidth; xx++) {
            for (int yy = 0; yy < KernelHeight; yy++) {
                dist2 = (xx - xc) * (xx - xc) + (yy - yc) * (yy - yc);
                if (dist2 < radius2) {
                    xxr = xx + tx;
                    yyr = yy + ty;
                    tk = ref.getPixelValue(xxr, yyr) - kmean;
                    ts = ima.getPixelValue(xx, yy) - smean;
                    skk += tk * tk;
                    sss += ts * ts;
                    sks += tk * ts;
                }
            }
        }
        rfactor = sks / (Math.sqrt(skk * sss) + tiny);

        return rfactor;
    }

    private static double correlationValueRotate(ImageProcessor ima, ImageProcessor ref, double ang, int radius) {
        int KernelWidth = ref.getWidth();
        int KernelHeight = ref.getHeight();
        int xc = KernelWidth / 2;
        int yc = KernelHeight / 2;
        int countpix = 0;
        double dist2;
        int radius2;
        if (radius > 0) {
            radius2 = radius * radius;
        } else {
            radius2 = Integer.MAX_VALUE;
        }

        double cosa = Math.cos(Math.toRadians(-ang));
        double sina = Math.sin(Math.toRadians(-ang));
        double kmean = 0;
        double smean = 0;
        double tk = 0;
        double ts = 0;
        double skk = 0;
        double sss = 0;
        double sks = 0;
        double rfactor = 0;
        double tiny = 1.0e-20;
        double xxr;
        double yyr;

        for (int xx = 0; xx < KernelWidth; xx++) {
            for (int yy = 0; yy < KernelHeight; yy++) {
                dist2 = (xx - xc) * (xx - xc) + (yy - yc) * (yy - yc);
                if (dist2 < radius2) {
                    xxr = (xx - xc) * cosa + (yy - yc) * sina + xc;
                    yyr = (yy - yc) * cosa - (xx - xc) * sina + yc;
                    kmean += ref.getInterpolatedPixel(xxr, yyr);
                    smean += ima.getPixelValue(xx, yy);
                    countpix++;
                }
            }
        }
        kmean /= countpix;
        smean /= countpix;
        for (int xx = 0; xx < KernelWidth; xx++) {
            for (int yy = 0; yy < KernelHeight; yy++) {
                dist2 = (xx - xc) * (xx - xc) + (yy - yc) * (yy - yc);
                if (dist2 < radius2) {
                    xxr = (xx - xc) * cosa + (yy - yc) * sina + xc;
                    yyr = (yy - yc) * cosa - (xx - xc) * sina + yc;
                    tk = ref.getInterpolatedPixel(xxr, yyr) - kmean;
                    ts = ima.getPixelValue(xx, yy) - smean;
                    skk += tk * tk;
                    sss += ts * ts;
                    sks += tk * ts;
                }
            }
        }
        rfactor = sks / (Math.sqrt(skk * sss) + tiny);

        return rfactor;
    }

    public static ImagePlus extractCurrentStack(ImagePlus plus, int channelI, int frameI) {
        // check dimensions
        int[] dims = plus.getDimensions();//XYCZT
        int channel = channelI == 0 ? plus.getChannel() : channelI;
        int frame = frameI;
        ImagePlus stack;
        // crop actual frame
        if ((dims[2] > 1) || (dims[4] > 1)) {
            Duplicator duplicator = new Duplicator();
            stack = duplicator.run(plus, channel, channel, 1, dims[3], frame, frame);
        } else stack = plus.duplicate();

        return stack;
    }

}
