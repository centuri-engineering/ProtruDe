package afm;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import mcib3d.image3d.legacy.FHTImage3D;

import java.awt.*;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Description of the Class
 *
 * @author thomas
 *  23 octobre 2007
 */
public class AFMImage {

    static ResourceBundle bundle;
    // windows to store images
    ImagePlus impraw;
    ImagePlus impref;
    ImagePlus impcorr;
    ImagePlus impang;
    ImagePlus impavg;
    ImagePlus impvar;
    ImagePlus impavgsym;
    ImagePlus impvarsym;
    ImagePlus impeaks;

    // the images
    ImageProcessor ref;
    ImageProcessor raw;
    ImageProcessor corrsave;
    ImageProcessor angsave;
    ImageProcessor avgimage;
    ImageProcessor varimage;
    ImageProcessor avgsymimage;
    ImageProcessor varsymimage;
    ImageProcessor peaksave;

    boolean debug = false;

    /**
     * Constructor for the AFMImage object
     *
     * @param bun Description of the Parameter
     */
    public AFMImage(ResourceBundle bun) {
        bundle = bun;
    }

    /**
     * Kill correlation and angle windows
     */
    public void killCorrelationImage() {
        if (corrsave != null) {
            corrsave = null;
            impcorr.unlock();
            impcorr.hide();
            impcorr.flush();
        }
        if (angsave != null) {
            angsave = null;
            impang.unlock();
            impang.hide();
            impang.flush();
        }
    }

    /**
     * Compute the cross correlation with rotation between the raw and ref image
     * (Taken from Auto_Corr from Markus Hasselblatt)
     *
     * @param raw    the raw image
     * @param ref    the ref kernel iamge
     * @param radius the radius for correlation into the ref image
     * @param angi   the increment for rotation (degrees)
     * @param angm   the maximum angle for rotation (degrees)
     */
    public void autoCorrCalc(ImageProcessor raw, ImageProcessor ref, int radius, int angi, int angm) {
        int KernelWidth = ref.getWidth();
        int KernelHeight = ref.getHeight();
        int ACWidth = raw.getWidth();
        int ACHeight = raw.getHeight();
        ImageProcessor ip3;

        IJ.showStatus(bundle.getString("$AUTOCORR"));

        double pi2 = Math.PI * 2;

        double rfactor = 0;
        double rfactormax = 0;
        double angmax = 0;
        double agp;
        if ((angi <= 0) || (angi >= 360)) {
            agp = pi2;
        } else {
            agp = Math.toRadians(angi);
        }
        double amax = Math.toRadians(angm);

        corrsave = new FloatProcessor(raw.getWidth(), raw.getHeight());
        angsave = new FloatProcessor(raw.getWidth(), raw.getHeight());

        // main loop in all pixels of the raw image
        IJ.showProgress(0);
        for (int x = KernelWidth / 2; x < ACWidth - KernelWidth / 2; x++) {
            for (int y = KernelHeight / 2; y < ACHeight - KernelHeight / 2; y++) {

                raw.setRoi(new Rectangle(x - KernelWidth / 2, y - KernelHeight / 2, KernelWidth, KernelHeight));
                ip3 = raw.crop();
                rfactormax = -1.0;
                angmax = 0;
                // rotate the kernel
                for (double a = -amax / 2; a <= amax / 2; a += agp) {

                    rfactor = correlationValue(ip3, ref, a, radius);

                    if (rfactor > rfactormax) {
                        rfactormax = rfactor;
                        angmax = a;
                    }
                }
                // save the max value and the corresponding angle in images
                corrsave.putPixelValue(x, y, rfactormax);
                angsave.putPixelValue(x, y, Math.toDegrees(angmax));
            }
            IJ.showStatus(bundle.getString("$AUTOCORR") + " " + (int) ((double) (100 * (x - KernelWidth / 2)) / (double) (ACWidth - KernelWidth)) + " %");
            IJ.showProgress((double) (x - KernelWidth / 2) / (double) (ACWidth - KernelWidth));
        }
        IJ.showProgress(1);
        corrsave.setMinAndMax(-0.95, 0.95);
        angsave.setMinAndMax(-angm / 2, angm / 2);
    }

    /**
     * Find the best symmetry
     *
     * @param ima    The image
     * @param radius The radius to compute correlation
     * @return The best symmetry
     */
    int bestSymmetry(ImageProcessor ima, int radius) {
        ImageProcessor nf;
        double cc;
        double max = 0;
        int best = 0;
        float[] ntab = new float[12];
        float[] cctab = new float[12];
        ntab[0] = 1;
        cctab[0] = 1;
        ImageStack nfstack = new ImageStack(ima.getWidth(), ima.getHeight());
        nfstack.addSlice("n=1", ima);
        for (int i = 2; i <= 12; i++) {
            nf = nFold(ima, i, false);
            cc = correlationValue(ima, nf, 0.0, radius);
            IJ.log("i=" + i + " cc=" + cc);
            ntab[i - 1] = i;
            cctab[i - 1] = (float) cc;
            nfstack.addSlice("n=" + i, nf);
            if (cc > max) {
                max = cc;
                best = i;
            }
        }
        // plot
        PlotWindow plot = new PlotWindow(bundle.getString("$BESTSYM"), "n", "corr", ntab, cctab);
        plot.draw();
        plot.show();

        // stack
        ImagePlus plusnfstack = new ImagePlus(bundle.getString("$BESTSYM"), nfstack);
        plusnfstack.show();

        return best;
    }

    /**
     * Correlation value between one image and one possibly rotated within a
     * radius
     *
     * @param ima    The reference image
     * @param ref    The other image rotated
     * @param ang    The angle of rotation (radians)
     * @param radius The radius within to compute correlation
     * @return The correlation value
     */
    static double correlationValue(ImageProcessor ima, ImageProcessor ref, double ang, int radius) {
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
        double cosa = Math.cos(-ang);
        double sina = Math.sin(-ang);
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

    /**
     * Description of the Method
     *
     * @param ima    Description of the Parameter
     * @param ref    Description of the Parameter
     * @param ang    Description of the Parameter
     * @param radius Description of the Parameter
     * @return Description of the Return Value
     */
    double meanDiffValue(ImageProcessor ima, ImageProcessor ref, double ang, int radius) {
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
        double cosa = Math.cos(-ang);
        double sina = Math.sin(-ang);
        double sum = 0;
        double xxr;
        double yyr;
        double kmean;
        double smean;

        for (int xx = 0; xx < KernelWidth; xx++) {
            for (int yy = 0; yy < KernelHeight; yy++) {
                dist2 = (xx - xc) * (xx - xc) + (yy - yc) * (yy - yc);
                if (dist2 < radius2) {
                    xxr = (xx - xc) * cosa + (yy - yc) * sina + xc;
                    yyr = (yy - yc) * cosa - (xx - xc) * sina + yc;
                    kmean = (double) (ref.getInterpolatedPixel(xxr, yyr));
                    smean = (double) (ima.getPixelValue(xx, yy));
                    sum += (kmean - smean) * (kmean - smean);
                    countpix++;
                }
            }
        }

        return Math.sqrt(sum / countpix);
    }

    /**
     * rotational n fold
     *
     * @param im the image to process
     * @param n  the number of symmetry
     * @param sd show std dev image
     * @return the n-folded rotational image
     */
    public static ImageProcessor nFold(ImageProcessor im, int n, boolean sd) {
        double cosa;
        double sina;
        double xx;
        double yy;
        double pix;
        int count;

        ImageProcessor res = im.createProcessor(im.getWidth(), im.getHeight());
        int cx = im.getWidth() / 2;
        int cy = im.getHeight() / 2;
        int w = im.getWidth();
        int h = im.getHeight();

        double pi2 = 2 * Math.PI;
        double inc = pi2 / n;

        IJ.showStatus(bundle.getString("$NFOLD") + "-" + n);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                pix = 0;
                count = 0;
                for (double a = 0; a < pi2; a += inc) {
                    cosa = Math.cos(a);
                    sina = Math.sin(a);
                    xx = (x - cx) * cosa + (y - cy) * sina + cx;
                    yy = (y - cy) * cosa - (x - cx) * sina + cy;
                    if ((xx > 1) && (xx < w - 1) && (yy > 1) && (yy < h - 1)) {
                        pix += im.getInterpolatedValue(xx, yy);
                        count++;
                    }
                    res.putPixelValue(x, y, (int) (pix / count));
                }
            }
        }

        // show sd
        if (sd) {
            new ImagePlus(bundle.getString("$NFOLDVAR") + "-" + n, nFoldVariance(im, res, n)).show();
        }

        return res;
    }

    /**
     * rotational n fold variance (std dev)
     *
     * @param im  the image to process
     * @param n   the number of symmetry
     * @param avg the average n-fold image
     * @return the n-folded rotational image
     */
    public static ImageProcessor nFoldVariance(ImageProcessor im, ImageProcessor avg, int n) {
        double cosa;
        double sina;
        double xx;
        double yy;
        double pix;
        int count;

        ImageProcessor res = im.createProcessor(im.getWidth(), im.getHeight());
        int cx = im.getWidth() / 2;
        int cy = im.getHeight() / 2;
        int w = im.getWidth();
        int h = im.getHeight();

        double pi2 = 2 * Math.PI;
        double inc = pi2 / n;
        double difpix;

        IJ.showStatus(bundle.getString("$NFOLDVAR") + "-" + n);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                pix = 0;
                count = 0;
                for (double a = 0; a < pi2; a += inc) {
                    cosa = Math.cos(a);
                    sina = Math.sin(a);
                    xx = (x - cx) * cosa + (y - cy) * sina + cx;
                    yy = (y - cy) * cosa - (x - cx) * sina + cy;
                    if ((xx > 1) && (xx < w - 1) && (yy > 1) && (yy < h - 1)) {
                        difpix = (im.getInterpolatedValue(xx, yy) - avg.getPixelValue(x, y));
                        pix += difpix * difpix;
                        count++;
                    }
                    // std dev computation
                    difpix = Math.sqrt(pix / (count - 1));
                    res.putPixelValue(x, y, (int) (difpix));
                }
            }
        }

        return res;
    }

    /**
     * Put zero values in ring
     *
     * @param fht  FHT image to zero
     * @param rmin Radius min (inclusive)
     */
    void zeroRing(FHTImage3D fht, int rmin) {
        int w = fht.getSizex();
        int h = fht.getSizey();
        double dist2;
        int xc = w / 2;
        int yc = h / 2;
        int rmin2 = rmin * rmin;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                dist2 = (x - xc) * (x - xc) + (y - yc) * (y - yc);
                if (dist2 > rmin2) {
                    fht.zeroValue(x, y, 0);
                }
            }
        }
    }

    /**
     * Description of the Method
     *
     * @param ima  Description of the Parameter
     * @param rmin Description of the Parameter
     * @return Description of the Return Value
     */
    ImageProcessor lowPass(ImageProcessor ima, int rmin) {
        FHTImage3D fht = new FHTImage3D(ima);
        fht = fht.center();
        zeroRing(fht, rmin);
        fht = fht.decenter();
        fht = fht.doInverseTransform();

        return fht.getStack().getProcessor(1);
    }

    /**
     * Computes the FRC of the average image
     *
     * @param raw original raw image (for averaging)
     * @param ref ref image (for sizes)
     * @param pos position of the peaks in the raw image
     */
    public void FRC(ImageProcessor raw, ImageProcessor ref, ArrayList pos, double convert, double radius) {
        // divise into two subsets
        ArrayList v1 = new ArrayList();
        ArrayList v2 = new ArrayList();
        int n = pos.size();
        for (int i = 0; i < n - 1; i += 2) {
            v1.add(pos.get(i));
            v2.add(pos.get(i + 1));
        }
        // compute 2 averages
        ImageProcessor m1;
        ImageProcessor m2;
        m1 = average(raw, ref, v1, ref.getWidth() / 2);
        m2 = average(raw, ref, v2, ref.getWidth() / 2);

        // insert into 2^n images
        ImageProcessor m1_2;
        ImageProcessor m2_2;
        m1_2 = insert2(m1, ref.getWidth() / 2 - 2, false);
        m2_2 = insert2(m2, ref.getWidth() / 2 - 2, false);

        // compute FHT
        FHTImage3D fht1 = new FHTImage3D(m1_2);
        FHTImage3D fht2 = new FHTImage3D(m2_2);
        fht1 = fht1.center();
        fht2 = fht2.center();

        // compute FRC in each ring of size 1
        double[] up = new double[1 + ref.getWidth() / 2];
        double[] low1 = new double[1 + ref.getWidth() / 2];
        double[] low2 = new double[1 + ref.getWidth() / 2];
        double[] frc = new double[1 + ref.getWidth() / 2];
        double[] NN = new double[1 + ref.getWidth() / 2];
        for (int a = 0; a < 1 + ref.getWidth() / 2; a++) {
            NN[a] = 0;
            up[a] = 0;
            low1[a] = 0;
            low2[a] = 0;
        }

        double dist;
        double n1;
        double n2;
        double ang;
        int xc = ref.getWidth() / 2;
        int yc = ref.getHeight() / 2;
        int index;

        for (int x = 0; x < ref.getWidth(); x++) {
            for (int y = 0; y < ref.getHeight(); y++) {
                dist = Math.sqrt((x - xc) * (x - xc) + (y - yc) * (y - yc));
                if (dist < ref.getWidth() / 2) {
                    index = (int) (dist + 0.5);
                    n1 = fht1.getNorm(x, y, 0);
                    n2 = fht2.getNorm(x, y, 0);
                    ang = fht1.getAngle(x, y, 0) - fht2.getAngle(x, y, 0);
                    up[index] += n1 * n2 * Math.cos(ang);
                    low1[index] += n1 * n1;
                    low2[index] += n2 * n2;
                    NN[index]++;
                }
            }
        }

        // print results
        IJ.log("\nFRC Results:");
        for (int a = 0; a < 1 + ref.getWidth() / 2; a++) {
            frc[a] = up[a] / (Math.sqrt(low1[a] * low2[a]));
            IJ.log("res[A]=" + ((convert * 2 * radius) / a) + " frc=" + frc[a] + "  N=" + (2 / Math.sqrt(NN[a])));
        }
    }

    /**
     * Computes the resolution of the image
     *
     * @param ima Description of the Parameter
     */
    public void Resolution(ImageProcessor ima, double convert, double radius, String title) {

        int w = ima.getWidth();
        int h = ima.getHeight();
        int wurzel = (int) (w / Math.sqrt(2.0)) + 1;

        ImageProcessor filtree;
        ImageProcessor filterCut;

        double[] rad = new double[wurzel];  //changed from double[w / 2 + 1] to double[w / 2]
        double[] corr = new double[wurzel]; //changed from double[w / 2 + 1] to double[w / 2]
        double[] mean = new double[wurzel]; //changed from double[w / 2 + 1] to double[w / 2]

        int index = 0;

        // insert image int a 2^n image
        ImageProcessor ima2 = insert2(ima, w / 2 - 2, false);
        ImageStack stack = new ImageStack(ima2.getWidth(), ima2.getHeight());
        ImageStack stack2 = new ImageStack(w, h);


        for (int r = wurzel; r > 0; r--) {
//			index++;
            filtree = lowPass(ima2, r);
            stack.addSlice("filtered r=" + r, filtree);
            filterCut = inverseInsert2(filtree, h, w);
            stack2.addSlice("check r=" + r, filterCut);
            rad[index] = r;
            corr[index] = correlationValue(filterCut, ima, 0, w / 2);
            mean[index] = meanDiffValue(filterCut, ima, 0, w / 2);
            index++;
        }
        new ImagePlus("filtered", stack2).show();

        // normalisation and display
        for (int i = 0; i < mean.length; i++) {
            mean[i] /= mean[mean.length - 1];
            rad[i] = rad[i] / (convert * 2 * radius); // feature size as 1/Angstrom
        }
        for (int i = mean.length - 1; i >= 0; i--) {
            IJ.log("Resolution results");
            IJ.log("res[1 / Angstrom]=" + rad[i] + " ACV=" + corr[i] + " diff=" + mean[i]);
        }

        PlotWindow plot = new PlotWindow(title, "Resolution Cut-Off [1 / Angstrom]", "ACV", rad, corr);
        plot.setLimits(rad[0], rad[rad.length - 1], 0.0, 1.0);
        plot.draw();
        plot.addPoints(rad, mean, PlotWindow.CROSS);
        plot.draw();
        plot.show();

        // calculate 1st derivation of ACV 
        double[] rad1stDer = new double[wurzel - 1];
        for (int i = 0; i < rad1stDer.length; i++) {
            rad1stDer[i] = (rad[i] + rad[i + 1]) / 2;
        }
        double[] ccv1stDer = new double[rad1stDer.length];
        for (int j = 0; j < rad1stDer.length; j++) {
            ccv1stDer[j] = (corr[j + 1] - corr[j]) / (rad[j + 1] - rad[j]);
        }

        PlotWindow plot2 = new PlotWindow(title, "Resolution Cut-Off [1 / Angstrom]", "1st derivation of ACV", rad1stDer, ccv1stDer);
        plot2.draw();
        plot2.show();

    }

    public void resolutionList(ImageProcessor raw, ImageProcessor ref, ArrayList peakList, double convert) {

        int w = ref.getWidth();
        int h = ref.getHeight();
        int wurzel = (int) (w / Math.sqrt(2.0)) + 1;

        ImageProcessor img;
        ImageProcessor filtree;
        ImageProcessor filterCut;

        ImageStack particleImages = new ImageStack(w, h);
        ImageStack stack2 = new ImageStack(w, h);

        for (int i = 0; i < peakList.size(); i++) {
            img = imageFromPeak(raw, ref, (Peaks) (peakList.get(i)), 2 * w);
            particleImages.addSlice("" + i, img);
        }

        double[] rad = new double[wurzel];
        for (int i = 0; i < rad.length; i++) {
            rad[rad.length - i - 1] = (i + 1.0) / (convert * w);
        }

        double[] rad1stDer = new double[(wurzel) - 1];
        for (int i = 0; i < rad1stDer.length; i++) {
            rad1stDer[i] = (rad[i] + rad[i + 1]) / 2;
        }
        double[] rad2ndDer = new double[(wurzel) - 2];
        for (int i = 0; i < rad2ndDer.length; i++) {
            rad2ndDer[i] = (rad1stDer[i] + rad1stDer[i + 1]) / 2;
        }

        double[] corr = new double[wurzel];
        double[][] ccvResults = new double[wurzel][particleImages.getSize()];

        //ccv results
        ResultsTable ccv = new ResultsTable();
        //1st derivation of ccv results
        ResultsTable first = new ResultsTable();

        //for average image	
        double[] AvgCorr = new double[wurzel];
        double[] Avg1stDer = new double[wurzel - 1];
        ImageProcessor avgimage = average(raw, ref, peakList, 2 * w); //calculate average image
        //calculate ACV for average
        ImageProcessor avgimage2 = insert2(avgimage, w / 2 - 2, false);
        int index = 0;
        for (int r = wurzel; r > 0; r--) {
            filtree = lowPass(avgimage2, r);
            filterCut = inverseInsert2(filtree, h, w);
            AvgCorr[index] = correlationValue(filterCut, avgimage, 0, w / 2);
            index++;
        }
        //calculate 1st derrivation of acv of average
//		double[][] ccv1stDer = new double[rad1stDer.length][particleImages.getSize()];
//		for (int i = 0; i < particleImages.getSize(); i++) {
        for (int j = 0; j < rad1stDer.length; j++) {
            Avg1stDer[j] = (AvgCorr[j + 1] - AvgCorr[j]) / (rad[j + 1] - rad[j]);
        }
//		}
        //write acv of average to results table
        for (int i = 0; i < corr.length; i++) {
            ccv.incrementCounter();
            ccv.addValue(1, rad[i]);
            ccv.setHeading(1, "radius [1 / A]");
        }
        for (int j = 0; j < AvgCorr.length; j++) {
            ccv.setValue(2, j, AvgCorr[j]);
        }
        ccv.setHeading(2, "average image");
        ccv.show("ACV");
        //write 1st derivation acv of average to results table
        for (int i = 0; i < (corr.length - 1); i++) {
            first.incrementCounter();
            first.addValue(1, rad1stDer[i]);
            first.setHeading(1, "radius [1 / A]");
        }
        for (int j = 0; j < Avg1stDer.length; j++) {
            first.setValue(2, j, Avg1stDer[j]);
        }
        first.setHeading(2, "average image");
        first.show("1st derivation of ACV");
        //plot acv of average
        PlotWindow plot = new PlotWindow("ACV of average image", "Resolution Cut-Off [1 / Angstrom]", "ACV", rad, AvgCorr);
        plot.setLimits(rad[0], rad[rad.length - 1], 0.0, 1.0);
        plot.draw();
        plot.show();
        //plot 1st derivation of acv of average
        PlotWindow plot2 = new PlotWindow("features of average image", "Resolution Cut-Off [1 / Angstrom]", "1st derivation of ACV", rad1stDer, Avg1stDer);
        plot2.draw();
        plot2.show();

        new ImagePlus("Images", particleImages).show();

//		boolean doHighPass = true;
//		int HighPassFilterRadius = 0;
//		GenericDialog gd = new GenericDialog("Resolution List");
//		gd.addCheckbox("Perform High Pass Filtering", doHighPass);
//		gd.showDialog();
//		doHighPass = gd.getNextBoolean();

//		if (doHighPass) {
//			GenericDialog gd2 = new GenericDialog("High Pass");
//			gd2.addNumericField("filter radius:", HighPassFilterRadius, 0, 3, "pixel" );
//			gd2.showDialog();
//			HighPassFilterRadius = (int)gd2.getNextNumber();
//			for (int i = 1; i <= particleImages.getSize(); i++) {
//				img = particleImages.getProcessor(i);
//				img2 = insert2(img, w / 2 - 2, false);
//				filtree = highPass(img2, HighPassFilterRadius);
//				img = inverseInsert2(filtree, h, w);
//				label = particleImages.getSliceLabel(i);
//				stack2.addSlice(label, img);	
//			}
//			particleImages = stack2;
//			
//			new ImagePlus("Highpassed Images", particleImages).show();
//		}


        for (int i = 1; i <= particleImages.getSize(); i++) {

            index = 0;

            // insert image int a 2^n image
            ImageProcessor ima = particleImages.getProcessor(i);
            ImageProcessor ima2 = insert2(ima, w / 2 - 2, false);
            ImageStack s1 = new ImageStack(ima2.getWidth(), ima2.getHeight());
            ImageStack s2 = new ImageStack(w, h);

            for (int r = wurzel; r > 0; r--) {
                filtree = lowPass(ima2, r);
                s1.addSlice("filtered r=" + r, filtree);
                filterCut = inverseInsert2(filtree, h, w);
                s2.addSlice("check r=" + r, filterCut);
                corr[index] = correlationValue(filterCut, ima, 0, w / 2);
                index++;
            }


            //write output to results
            IJ.log("Resolution results molecule image: " + i);
            for (int j = 0; j < corr.length; j++) {
                IJ.log("res[1 / A]=" + rad[j] + " ACV=" + corr[j]);
            }

            //write output to array
            for (int j = 0; j < corr.length; j++) {
                ccvResults[j][i - 1] = corr[j];
            }


        }


        for (int i = 1; i <= particleImages.getSize(); i++) {
            for (int j = 0; j < corr.length; j++) {
                ccv.setValue(i + 2, j, ccvResults[j][i - 1]);
            }
            ccv.setHeading(i + 2, "molecule " + i);

        }
        ccv.show("ACV");


        //calculate 1st derrivation of ccv
        double[][] ccv1stDer = new double[rad1stDer.length][particleImages.getSize()];
        for (int i = 0; i < particleImages.getSize(); i++) {
            for (int j = 0; j < rad1stDer.length; j++) {
                ccv1stDer[j][i] = (ccvResults[j + 1][i] - ccvResults[j][i]) / (rad[j + 1] - rad[j]);
            }
        }


        //write 1st derrivation of ccv to results table 
//		for  (int i = 0; i < (corr.length -1); i++) {
//		first.incrementCounter();
//		first.addValue(1, rad1stDer[i]);
//		first.setHeading(1, "radius [1 / A]");
//		}
        for (int i = 1; i <= particleImages.getSize(); i++) {
            //			first.addColumns();
            for (int j = 0; j < (corr.length - 1); j++) {
                first.setValue(i + 2, j, ccv1stDer[j][i - 1]);
            }
            first.setHeading(i + 2, "molecule " + i);

        }
        first.show("1st derivation of ACV");

//		//calculate 2nd derrivation of ccv
//		double[][] ccv2ndDer = new double[rad2ndDer.length][particleImages.getSize()];
//		for (int i = 0; i < particleImages.getSize(); i++) {
//			for (int j = 0; j < rad2ndDer.length; j++) {	
//				ccv2ndDer[j][i] = (ccv1stDer[j+1][i] - ccv1stDer[j][i]) / (rad1stDer[j+1] - rad1stDer[j]);
//			}
//		}


        //write 2nd derrivation of ccv to results table 
//		for  (int i = 0; i < (corr.length -2); i++) {
//		second.incrementCounter();
//		second.addValue(1, rad2ndDer[i]);
//		second.setHeading(1, "radius [A]");
//		}
//		for (int i = 1; i <= particleImages.getSize(); i++) {
//	//			second.addColumns();
//				for  (int j = 0; j < (corr.length-2); j++) {
//					second.setValue(i+1, j, ccv2ndDer[j][i-1]);
//				}
//			second.setHeading(i+1, "particle "+i);
//			
//		}
//		second.show("2nd derivation of CCV");
//		
//		//calculate standard deviation 2nd derrivation of ccv
//		double[] stDev2ndDer = new double[rad2ndDer.length];
//		double[] data = new double[particleImages.getSize()];
//		for (int i = 0; i < rad2ndDer.length; i++) {
//			for (int j = 0; j < particleImages.getSize(); j++) {
//				data[j] = ccv2ndDer[i][j];
//				stDev2ndDer[i] = sdFast(data);
//			}
//		}
//		
//		
//		//plot StDev of 2nd derivation
//		PlotWindow plot = new PlotWindow("Resolution", "Angstrom", "StDev of 2nd derivation of CCV", rad2ndDer, stDev2ndDer);
//		plot.draw();
//		plot.show();


    }

//	/**
//	 *  Computes the resolution of the image on the Basis of Symmetry
//	 *
//	 * @param  ima  Description of the Parameter
//	 */
//	public void SymResolution(ImageProcessor ima, double convert, double radius, String title, int nf) {
//		int w = ima.getWidth();
//		int h = ima.getHeight();
//		ImageProcessor filtree;
//		ImageProcessor filterCut;
//		ImageProcessor sym;
//		ImageProcessor sym360;
//		ImageProcessor symCut;
//		ImageProcessor sym360Cut;
//
//		double[] rad = new double[w / 2];
//		double[] corr = new double[w / 2];
//
//
//		int index = 0;
//
//		// insert image int a 2^n image
//		ImageProcessor ima2 = insert2(ima, w / 2 - 2, false);
//
//		ImageStack stack = new ImageStack(ima2.getWidth(), ima2.getHeight());
//		ImageStack stack2 = new ImageStack(w, h);
//		ImageStack stackSym = new ImageStack(w, h);
//
//		for (int r = w / 2; r > 0; r--) {
//			filtree = lowPass(ima2, r);
//			filterCut = inverseInsert2(filtree, h, w);
//			stack2.addSlice("filtered r=" + r, filterCut);
//			sym = nFold(filtree, nf, false);	//symmetricized version of filtree
//			symCut = inverseInsert2(sym, h, w);
//			stackSym.addSlice("filtered r=" + r, symCut);
//			sym360 = nFold(filtree, 360, false);       //symmetricized circularily
//			sym360Cut = inverseInsert2(sym360, h, w);
//			rad[index] = r;
//			if (debug) System.out.println ("rad = " +rad[index]);
//			corr[index] = correlationValue(filterCut, symCut, 0, w / 2) / correlationValue(filterCut, sym360Cut, 0, w / 2); 
//			if (debug) System.out.println ("corr= "+ corr[index]);
//			index++;
//			
//		}
//		new ImagePlus("filtered", stack2).show();
//		new ImagePlus("Sym-filtered", stackSym).show();
//
//		double corr0=corr[0];
//		// normalisation and display
//			corr[i] /= corr0;
//			rad[i]   = (convert*2*radius)/rad[i];
//		}
//		
//		IJ.write("Sym Resolution results:");
//		for (int i = rad.length - 1; i >= 0; i--) {
//			IJ.write("res[A]=" + rad[i] + " is=" + corr[i]);
//		}
//
//		PlotWindow plot = new PlotWindow(title, "Sym. Resolution Cut-Off [Angstrom]", "internal symmetry", rad, corr);
//		plot.draw();
//		plot.show();
//	}

    /**
     * Computes the resolution of the image on the Basis of Symmetry //modified to measure symmetry of bandpassed images
     *
     * @param ima Description of the Parameter
     */
    public void SymResolution(ImageProcessor ima, double convert, double radius, String title, int nf) {
        int w = ima.getWidth();
        int h = ima.getHeight();
        ImageProcessor filtree;
        ImageProcessor filterCut;
        ImageProcessor sym;
        ImageProcessor sym360;
        ImageProcessor symCut;
        ImageProcessor sym360Cut;

        double[] rad = new double[w / 2];
        double[] corr = new double[w / 2];


        int index = 0;

        // insert image int a 2^n image
        ImageProcessor ima2 = insert2(ima, w / 2 - 2, false);

        ImageStack stack = new ImageStack(ima2.getWidth(), ima2.getHeight());
        ImageStack stack2 = new ImageStack(w, h);
        ImageStack stackSym = new ImageStack(w, h);

        for (int r = w / 2; r > 0; r--) {
//			filtree = lowPass(ima2, r);
            filtree = bandPass(ima2, r + 1, r);
            filterCut = inverseInsert2(filtree, h, w);
            stack2.addSlice("filtered r=" + r, filterCut);
            sym = nFold(filtree, nf, false);    //symmetricized version of filtree
            symCut = inverseInsert2(sym, h, w);
            stackSym.addSlice("filtered r=" + r, symCut);
            sym360 = nFold(filtree, 360, false);       //symmetricized circularily
            sym360Cut = inverseInsert2(sym360, h, w);
            rad[index] = r;
            if (debug) {
                System.out.println("rad = " + rad[index]);
            }
//			corr[index] = correlationValue(filterCut, symCut, 0, w / 2) / correlationValue(filterCut, sym360Cut, 0, w / 2); 
            corr[index] = correlationValue(filterCut, symCut, 0, w / 2);
            if (debug) {
                System.out.println("corr= " + corr[index]);
            }
            index++;

        }
        new ImagePlus("filtered", stack2).show();
        new ImagePlus("Sym-filtered", stackSym).show();

        double corr0 = corr[0];
        // normalisation and display
        for (int i = 0; i < rad.length; i++) {
            corr[i] /= corr0;
            rad[i] = (convert * 2 * radius) / rad[i];
        }

        IJ.log("Sym Resolution results:");
        for (int i = rad.length - 1; i >= 0; i--) {
            IJ.log("res[A]=" + rad[i] + " is=" + corr[i]);
        }

        PlotWindow plot = new PlotWindow(title, "Sym. Resolution Cut-Off [Angstrom]", "internal symmetry", rad, corr);
        plot.draw();
        plot.show();
    }

    /**
     * computes the average image at the position marked in the position ArrayList,
     * takes also into account the angle
     *
     * @param raw    the raw image
     * @param ref    the ref image (for sizes)
     * @param pos    the positions
     * @param radius radius for averaging
     * @return the average image
     */
    public ImageProcessor average(ImageProcessor raw, ImageProcessor ref, ArrayList pos, int radius) {
        int kw = ref.getWidth();
        int kh = ref.getHeight();
        int rcx = kw / 2;
        int rcy = kh / 2;
        int n = pos.size();
        Peaks pp;
        int xp;
        int yp;
        int xc;
        int yc;
        double ag;
        double pix;
        double cosa;
        double sina;
        double xxp;
        double yyp;
        double dist2;
        FloatProcessor resf = new FloatProcessor(kw, kh);

        for (int x = 0; x < kw; x++) {
            for (int y = 0; y < kh; y++) {
                if (radius > 0) {
                    dist2 = (x - rcx) * (x - rcx) + (y - rcy) * (y - rcy);
                    if (dist2 < radius * radius) {
                        for (int v = 0; v < n; v++) {
                            pp = (Peaks) (pos.get(v));
                            xc = pp.getX();
                            yc = pp.getY();
                            ag = pp.getAngle();
                            cosa = Math.cos(Math.toRadians(ag));
                            sina = Math.sin(Math.toRadians(ag));
                            xp = xc - kw / 2 + x;
                            yp = yc - kh / 2 + y;
                            xxp = (xp - xc) * cosa + (yp - yc) * sina + xc;
                            yyp = (yp - yc) * cosa - (xp - xc) * sina + yc;
                            pix = raw.getInterpolatedPixel(xxp, yyp);
                            resf.putPixelValue(x, y, resf.getPixelValue(x, y) + pix / n);
                        }
                    }
                }
            }
        }

        ImageProcessor res = new ByteProcessor(resf.getWidth(), resf.getHeight());
        for (int x = 0; x < kw; x++) {
            for (int y = 0; y < kh; y++) {
                res.putPixel(x, y, (int) resf.getPixelValue(x, y));
            }
        }

        return res;
    }

    /**
     * computes the average image from the pattern
     * (no rotation)
     *
     * @return the average image
     */
    public ImageProcessor averageParticle(ImageProcessor raw, ImageProcessor ref, int radCorr, double pctcorr1, double pctcorr2, int radiuspeaks) {
        AFMInterface afmInterface = AFMInterface.getAFMInterface();
        this.autoCorrCalc(raw, ref, radCorr, 0, 0); // computes corrsave et angsave
        //(new ImagePlus("corr image",corrsave)).show();
        AFMAnalysis analysis = afmInterface.getAFMAnalysis();
        analysis.findPeaks(raw, ref, corrsave, angsave, pctcorr1, pctcorr2, radiuspeaks, 1, 0, 1, 10);

        return this.average(raw, ref, analysis.getPeaksA(), radCorr);
    }

    /**
     * computes the image at the postion marked in the position ArrayList,
     * takes also into account the angle
     *
     * @param raw    the raw image
     * @param ref    the ref image (for sizes)
     * @param radius radius for averaging
     * @return the image
     */
    ImageProcessor imageFromPeak(ImageProcessor raw, ImageProcessor ref, Peaks pp, int radius) {
        int kw = ref.getWidth();
        int kh = ref.getHeight();
        int rcx = kw / 2;
        int rcy = kh / 2;
        int xp;
        int yp;
        int xc;
        int yc;
        double ag;
        double pix;
        double cosa;
        double sina;
        double xxp;
        double yyp;
        double dist2;
        FloatProcessor resf = new FloatProcessor(kw, kh);

        for (int x = 0; x < kw; x++) {
            for (int y = 0; y < kh; y++) {
                if (radius > 0) {
                    dist2 = (x - rcx) * (x - rcx) + (y - rcy) * (y - rcy);
                    if (dist2 < radius * radius) {
                        xc = pp.getX();
                        yc = pp.getY();
                        ag = pp.getAngle();
                        cosa = Math.cos(Math.toRadians(ag));
                        sina = Math.sin(Math.toRadians(ag));
                        xp = xc - kw / 2 + x;
                        yp = yc - kh / 2 + y;
                        xxp = (xp - xc) * cosa + (yp - yc) * sina + xc;
                        yyp = (yp - yc) * cosa - (xp - xc) * sina + yc;
                        pix = raw.getInterpolatedPixel(xxp, yyp);
                        resf.putPixelValue(x, y, pix);

                    }
                }
            }
        }

        ImageProcessor res = new ByteProcessor(resf.getWidth(), resf.getHeight());
        for (int x = 0; x < kw; x++) {
            for (int y = 0; y < kh; y++) {
                res.putPixel(x, y, (int) resf.getPixelValue(x, y));
            }
        }

        return res;
    }

    /**
     * computes the variance image at the postion marked in the position ArrayList,
     * takes also into account the angle
     *
     * @param raw    the raw image
     * @param ref    the ref image (for sizes)
     * @param avg    the average image
     * @param pos    the positions
     * @param radius Description of the Parameter
     * @return the variance image
     */
    ImageProcessor variance(ImageProcessor raw, ImageProcessor ref, ImageProcessor avg, ArrayList pos, int radius) {
        int kw = ref.getWidth();
        int kh = ref.getHeight();
        int rcx = kw / 2;
        int rcy = kh / 2;
        int n = pos.size();
        Peaks pp;
        int xp;
        int yp;
        int xc;
        int yc;
        double ag;
        double av;
        double s2;
        double pix;
        double cosa;
        double sina;
        double xxp;
        double yyp;
        double dist2;
        double max = 0;
        FloatProcessor resf = new FloatProcessor(kw, kh);

        for (int x = 0; x < kw; x++) {
            for (int y = 0; y < kh; y++) {
                if (radius > 0) {
                    dist2 = (x - rcx) * (x - rcx) + (y - rcy) * (y - rcy);
                    if (dist2 < radius * radius) {
                        for (int v = 0; v < n; v++) {
                            pp = (Peaks) (pos.get(v));
                            xc = pp.getX();
                            yc = pp.getY();
                            ag = pp.getAngle();
                            cosa = Math.cos(Math.toRadians(ag));
                            sina = Math.sin(Math.toRadians(ag));
                            xp = xc - kw / 2 + x;
                            yp = yc - kh / 2 + y;
                            xxp = (xp - xc) * cosa + (yp - yc) * sina + xc;
                            yyp = (yp - yc) * cosa - (xp - xc) * sina + yc;
                            pix = raw.getInterpolatedPixel(xxp, yyp);
                            av = avg.getPixelValue(x, y);
                            s2 = (pix - av) * (pix - av);
                            resf.putPixelValue(x, y, resf.getPixelValue(x, y) + s2 / (n - 1));
                        }
                    }
                }
            }
        }
        return resf;
    }

    /**
     * computes the standard deviation image at the postion marked in the position
     * ArrayList, takes also into account the angle
     *
     * @param raw    the raw image
     * @param ref    the ref image (for sizes)
     * @param avg    the average image
     * @param pos    the positions
     * @param radius Description of the Parameter
     * @return the standard deviation image
     */
    public ImageProcessor standardDeviation(ImageProcessor raw, ImageProcessor ref, ImageProcessor avg, ArrayList pos, int radius) {
        ImageProcessor var = variance(raw, ref, avg, pos, radius);
        var.sqrt();
        //var.resetMinAndMax();
        ImageProcessor sd = imageToByte(var);

        return sd;
    }

    /**
     * Description of the Method
     *
     * @param ima Description of the Parameter
     * @return Description of the Return Value
     */
    private ByteProcessor imageToByte(ImageProcessor ima) {
        ByteProcessor res = new ByteProcessor(ima.getWidth(), ima.getHeight());
        for (int x = 0; x < ima.getWidth(); x++) {
            for (int y = 0; y < ima.getHeight(); y++) {
                res.putPixel(x, y, (int) (ima.getPixelValue(x, y)));
            }
        }

        return res;
    }

    /**
     * Description of the Method
     *
     * @param G      Description of the Parameter
     * @param rayon  Description of the Parameter
     * @param tapper Description of the Parameter
     * @return Description of the Return Value
     */
    private ImageProcessor insert2(ImageProcessor G, int rayon, boolean tapper) {
        int x;
        int y;
        int larg = G.getWidth();
        int haut = G.getHeight();
        int r0 = rayon + 5;
        int x0;
        int y0;
        int X0;
        int Y0;
        float pix;
        double coeff;
        double val;
        double rapport;

        double kl = Math.log(larg) / Math.log(2);
        double kh = Math.log(haut) / Math.log(2);
        int kkl = (int) (Math.pow(2.0, (int) (kl + 0.99)));
        int kkh = (int) (Math.pow(2.0, (int) (kh + 0.99)));

        int size = Math.max(kkl, kkh);

        ImageProcessor res = G.createProcessor(size, size);
        int centre = size / 2;
        double r = 0.0;

        float moy = Moyenne(G, r0);

        for (x = 0; x < size; x++) {
            for (y = 0; y < size; y++) {
                if (tapper) {
                    r = Math.sqrt((x - centre) * (x - centre) + (y - centre) * (y - centre));
                    if (r >= r0) {
                        res.putPixelValue(x, y, moy);
                    } else if (r >= rayon) {
                        x0 = centre + (int) (((float) (rayon) / (float) r) * (x - centre));
                        y0 = centre + (int) (((float) (rayon) / (float) r) * (y - centre));
                        X0 = x0 - (size - larg) / 2;
                        if (X0 >= larg) {
                            X0 = larg - 1;
                        }
                        Y0 = y0 - (size - haut) / 2;
                        if (Y0 >= haut) {
                            Y0 = haut - 1;
                        }
                        pix = G.getPixelValue(X0, Y0);

                        rapport = (r - (rayon)) / (r0 - rayon);
                        coeff = Math.cos(1.57 * rapport);
                        val = coeff * pix + (1.0 - coeff) * moy;
                        res.putPixelValue(x, y, val);
                    } else {
                        res.putPixelValue(x, y, G.getPixelValue(x - (size - larg) / 2, y - (size - haut) / 2));
                    }
                } else {
                    res.putPixelValue(x, y, G.getPixelValue(x - (size - larg) / 2 - 1, y - (size - haut) / 2 - 1));
                }
            }
        }
        return res;
    }

    /**
     * Description of the Method
     *
     * @param G     Description of the Parameter
     * @param rayon Description of the Parameter
     * @return Description of the Return Value
     */
    private float Moyenne(ImageProcessor G, double rayon) {
        double sum = 0.0;
        int nb = 0;
        int l = G.getWidth();
        int h = G.getHeight();
        double dist2;
        double rad2 = rayon*rayon;
        for (int x = (int) (l / 2 - rayon); x <= (int) (l / 2 + rayon); x++) {
            for (int y = (int) (h / 2 - rayon); y <= (int) (h / 2 + rayon); y++) {
                dist2 = Math.pow((x - l / 2), 2) + Math.pow((y - h / 2), 2);
                if (dist2 < rad2) {
                    sum += G.getPixelValue(x, y);
                    nb++;
                }
            }
        }
        return (float) (sum / nb);
    }

    /**
     * Returns the value of impraw.
     *
     * @return The impraw value
     */
    public ImagePlus getImpraw() {
        return impraw;
    }

    /**
     * Sets the value of impraw.
     *
     * @param impraw The value to assign impraw.
     */
    public void setImpraw(ImagePlus impraw) {
        this.impraw = impraw;
    }

    /**
     * Returns the value of impraw.
     *
     * @return The impraw value
     */
    public ImagePlus getImpref() {
        return impref;
    }

    /**
     * Sets the value of impraw.
     *
     * @param imp The new impref value
     */
    public void setImpref(ImagePlus imp) {
        this.impref = imp;
    }

    /**
     * Returns the value of raw.
     *
     * @return The raw value
     */
    public ImageProcessor getRaw() {
        return raw;
    }

    /**
     * Sets the value of raw.
     *
     * @param raw The value to assign raw.
     */
    public void setRaw(ImageProcessor raw) {
        this.raw = raw;
    }

    /**
     * Returns the value of raw.
     *
     * @return The raw value
     */
    public ImageProcessor getRef() {
        return ref;
    }

    /**
     * Sets the value of raw.
     *
     * @param ref The new ref value
     */
    public void setRef(ImageProcessor ref) {
        this.ref = ref;
    }

    /**
     * Description of the Method
     */
    public void openRaw() {
        OpenDialog od = new OpenDialog(bundle.getString("$OPENRAW"), "");
        String name = od.getFileName();
        if (name != null) {
            String dir = od.getDirectory();
            Opener op = new Opener();
            if (impraw != null) {
                impraw.hide();
                impraw.flush();
            }
            impraw = op.openImage(dir, name);
            impraw.setTitle(bundle.getString("$RAW"));
            impraw.show();
            raw = impraw.getProcessor();
            killCorrelationImage();
        }
    }

    public void setRaw() {
        ImagePlus tmpRaw = WindowManager.getCurrentImage();
        if (tmpRaw != null) {
            int nbs = tmpRaw.getNSlices();
            if (nbs == 1) {
                ImageProcessor tmp = tmpRaw.getProcessor();
                raw = tmp.duplicate();
            } else {
                ImageProcessor tmp = (tmpRaw.getStack()).getProcessor(tmpRaw.getCurrentSlice());
                raw = tmp.duplicate();
            }
            if (impraw != null) {
                impraw.hide();
                impraw.flush();
            }

            impraw = new ImagePlus("Raw Image", raw);
            impraw.show();
            killCorrelationImage();
        }
    }

    /**
     * Description of the Method
     */
    public void openRef() {
        OpenDialog od = new OpenDialog(bundle.getString("$OPENREF"), "");
        String name = od.getFileName();
        if (name != null) {
            String dir = od.getDirectory();
            Opener op = new Opener();
            if (impref != null) {
                impref.hide();
                impref.flush();
            }
            impref = op.openImage(dir, name);
            impref.setTitle(bundle.getString("$REF"));
            impref.show();
            ref = impref.getProcessor();

            killCorrelationImage();
        }
    }

    /**
     * Description of the Method
     */
    public void openAv() {
        OpenDialog od = new OpenDialog("Open Particle Image", "");
        String name = od.getFileName();
        if (name != null) {
            String dir = od.getDirectory();
            Opener op = new Opener();
            if (impavg != null) {
                impavg.hide();
                impavg.flush();
            }
            impavg = op.openImage(dir, name);
            impavg.setTitle("Particle Image");
            impavg.show();
            avgimage = impavg.getProcessor();
        }
    }

    /**
     * Description of the Method
     */
    public void openImpcorr() {
        OpenDialog od = new OpenDialog("Open correlation image", "");
        String name = od.getFileName();
        if (name != null) {
            String dir = od.getDirectory();
            Opener op = new Opener();
            if (impcorr != null) {
                impcorr.hide();
                impcorr.flush();
            }
            impcorr = op.openImage(dir, name);
            impcorr.setTitle("Correlation image");
            impcorr.show();
            corrsave = impcorr.getProcessor();
        }
    }

    /**
     * Description of the Method
     */
    public void openImpang() {
        OpenDialog od = new OpenDialog("Open angle image", "");
        String name = od.getFileName();
        if (name != null) {
            String dir = od.getDirectory();
            Opener op = new Opener();
            if (impang != null) {
                impang.hide();
                impang.flush();
            }
            impang = op.openImage(dir, name);
            impang.setTitle("Angle max image");
            impang.show();
            angsave = impang.getProcessor();
        }
    }

    /**
     * Description of the Method
     */
    public void cropRef() {
        Roi roi = impraw.getRoi();
        raw.setRoi(roi);
        ref = raw.crop();
        if (impref != null) {
            impref.hide();
            impref.flush();
        }
        impref = new ImagePlus("Reference", ref);
        impref.show();

        killCorrelationImage();
    }

    /**
     * Gets the corrsave attribute of the AFMImage object
     *
     * @return The corrsave value
     */
    public ImageProcessor getCorrval() {
        return corrsave;
    }

    /**
     * Gets the impcorr attribute of the AFMImage object
     *
     * @return The impcorr value
     */
    public ImagePlus getImpcorrval() {
        return impcorr;
    }

    /**
     * Sets the impcorr attribute of the AFMImage object
     *
     * @param plus The new impcorr value
     */
    public void setImpcorrval(ImagePlus plus) {
        impcorr = plus;
    }

    /**
     * Sets the peak attribute of the AFMImage object
     *
     * @param ip The new peak value
     */
    public void setPeak(ImageProcessor ip) {
        peaksave = ip;
    }

    /**
     * Sets the impPeak attribute of the AFMImage object
     *
     * @param plus The new impPeak value
     */
    public void setImpPeak(ImagePlus plus) {
        impeaks = plus;
    }

    /**
     * Description of the Method
     *
     * @return Description of the Return Value
     */
    public ImagePlus getImpPeak() {
        return impeaks;
    }

    /**
     * Gets the impAvgSym attribute of the AFMImage object
     *
     * @return The impAvgSym value
     */
    public ImagePlus getImpAvgSym() {
        return impavgsym;
    }

    /**
     * Gets the impVarSym attribute of the AFMImage object
     *
     * @return The impVarSym value
     */
    public ImagePlus getImpVarSym() {
        return impvarsym;
    }

    /**
     * Gets the corrsave attribute of the AFMImage object
     *
     * @return The corrsave value
     */
    public ImageProcessor getCorrang() {
        return angsave;
    }

    /**
     * Gets the impcorr attribute of the AFMImage object
     *
     * @return The impcorr value
     */
    public ImagePlus getImpcorrang() {
        return impang;
    }

    /**
     * Sets the impcorr attribute of the AFMImage object
     *
     * @param plus The new impcorr value
     */
    public void setImpcorrang(ImagePlus plus) {
        impang = plus;
    }

    /**
     * Sets the average attribute of the AFMImage object
     *
     * @param ip The new average value
     */
    public void setAverage(ImageProcessor ip) {
        avgimage = ip;
    }

    /**
     * Gets the average attribute of the AFMImage object
     *
     * @return The average value
     */
    public ImageProcessor getAverage() {
        return avgimage;
    }

    /**
     * Gets the impAverage attribute of the AFMImage object
     *
     * @return The impAverage value
     */
    public ImagePlus getImpAverage() {
        return impavg;
    }

    /**
     * Gets the impVariance attribute of the AFMImage object
     *
     * @return The impVariance value
     */
    public ImagePlus getImpVariance() {
        return impvar;
    }

    /**
     * Gets the variance attribute of the AFMImage object
     *
     * @return The variance value
     */
    public ImageProcessor getVariance() {
        return varimage;
    }

    /**
     * Sets the variance attribute of the AFMImage object
     *
     * @param ip The new variance value
     */
    public void setVariance(ImageProcessor ip) {
        varimage = ip;
    }

    /**
     * Sets the average attribute of the AFMImage object
     *
     * @param ip The new average value
     */
    public void setImpAverage(ImagePlus ip) {
        impavg = ip;
    }

    /**
     * Sets the avgSym attribute of the AFMImage object
     *
     * @param ip The new avgSym value
     */
    public void setAvgSym(ImageProcessor ip) {
        avgsymimage = ip;
    }

    /**
     * Sets the varSym attribute of the AFMImage object
     *
     * @param ip The new varSym value
     */
    public void setVarSym(ImageProcessor ip) {
        varsymimage = ip;
    }

    /**
     * Sets the impAvgSym attribute of the AFMImage object
     *
     * @param ip The new impAvgSym value
     */
    public void setImpAvgSym(ImagePlus ip) {
        impavgsym = ip;
    }

    /**
     * Sets the impVarSym attribute of the AFMImage object
     *
     * @param ip The new impVarSym value
     */
    public void setImpVarSym(ImagePlus ip) {
        impvarsym = ip;
    }

    /**
     * Sets the variance attribute of the AFMImage object
     *
     * @param ip The new variance value
     */
    public void setImpVariance(ImagePlus ip) {
        impvar = ip;
    }

    /**
     * Sets the avgAsRef attribute of the AFMImage object
     */
    public void setAvgAsRef() {
        ref = avgimage;
        impavg.hide();
        if (impref != null) {
            impref.hide();
            impref.flush();
        }
        impref = new ImagePlus(bundle.getString("$REF"), ref);
        impref.show();
        killCorrelationImage();
    }

    void showHeightRange(double zScale) {
        ImagePlus img = new ImagePlus("Particle Image", avgimage);
        ImageStatistics stats = img.getStatistics();
        double stdDev = stats.stdDev;
        double mean = stats.mean;
        double min = stats.min;
        double max = stats.max;
        double diff = max - min;
        double height = diff * zScale / 256;

        IJ.log("Mean:       " + mean);
        IJ.log("StdDev:     " + stdDev);
        IJ.log("Min:        " + min);
        IJ.log("Max:        " + max);
        IJ.log("Height [A]: " + height);
    }

    /**
     * creates a stack of particle images from a peaklist and an image
     *
     * @param raw the raw image
     * @param ref the ref image (for sizes)
     * @param pos the positions
     * @return stack of particle images
     */
    ImageStack createParticleStack(ImageProcessor raw, ImageProcessor ref, ArrayList pos) {
        int kw = ref.getWidth();
        int kh = ref.getHeight();
        int rcx = kw / 2;
        int rcy = kh / 2;
        int n = pos.size();
        Peaks pp;
        int xp;
        int yp;
        int xc;
        int yc;
        double ag;
        double pix;
        double cosa;
        double sina;
        double xxp;
        double yyp;
        double dist2;

        ImageStack stack = new ImageStack(kw, kh);

        for (int v = 0; v < n; v++) {
            FloatProcessor resf = new FloatProcessor(kw, kh);
            ImageProcessor res = new ByteProcessor(resf.getWidth(), resf.getHeight());
            pp = (Peaks) (pos.get(v));
            xc = pp.getX();
            yc = pp.getY();
            ag = pp.getAngle();
            cosa = Math.cos(Math.toRadians(ag));
            sina = Math.sin(Math.toRadians(ag));
            for (int x = 0; x < kw; x++) {
                for (int y = 0; y < kh; y++) {
                    xp = xc - kw / 2 + x;
                    yp = yc - kh / 2 + y;
                    xxp = (xp - xc) * cosa + (yp - yc) * sina + xc;
                    yyp = (yp - yc) * cosa - (xp - xc) * sina + yc;
                    pix = raw.getInterpolatedPixel(xxp, yyp);
                    resf.putPixelValue(x, y, pix);
                }
            }
            for (int x = 0; x < kw; x++) {
                for (int y = 0; y < kh; y++) {
                    res.putPixel(x, y, (int) resf.getPixelValue(x, y));
                }
            }
            stack.addSlice("peak " + v, res);
        }
        return stack;
    }

    void analyseHeight(double zScale, ArrayList peaklist) {
        ImageStack stack = createParticleStack(raw, ref, peaklist);
        new ImagePlus("Particles", stack).show();
        IJ.log("Peak MeanPixVal StdDev MinPixVal MaxPixVal Diff");
        double sumMin = 0;
        double sumHeightRange = 0;
        int n = stack.getSize();
        for (int x = 1; x <= n; x++) {
            ImagePlus img = new ImagePlus("Particle Image", stack.getProcessor(x));
            ImageStatistics stats = img.getStatistics();
            double mean = stats.mean * zScale / 256;
            double stdDev = stats.stdDev * zScale / 256;
            double min = stats.min * zScale / 256;
            double max = stats.max * zScale / 256;
            double heightRange = max - min;
            sumMin = sumMin + min;
            sumHeightRange = sumHeightRange + heightRange;
            IJ.log(x + " " + mean + " " + stdDev + " " + min + " " + max + " " + heightRange);
        }
        double meanMin = sumMin / n;
        double meanHeightRange = sumHeightRange / n;
        IJ.log("");
        IJ.log("*******");
        IJ.log("Height Analysis:");
        IJ.log(n + " Peaks");
        IJ.log(meanMin + "< height [A] < " + (meanMin + meanHeightRange));

    }

    /**
     * inverses insert2
     *
     * @param height height of outputimage
     * @param width  width of outputimage
     * @return image cut to size before insert2 application
     */
    private ImageProcessor inverseInsert2(ImageProcessor G, int height, int width) {
        int x;
        int y;
        int size = G.getWidth();
        ImageProcessor res = G.createProcessor(width, height);
        for (x = 0; x < width; x++) {
            for (y = 0; y < height; y++) {
                res.putPixelValue(x, y, G.getPixelValue(x + (size - width) / 2 + 1, y + (size - height) / 2 + 1));
            }
        }
        return res;
    }

    /**
     * createLowPass
     */
    void createLowPass() {
        ImageProcessor raw = getRaw();
        int width = raw.getWidth();
        int height = raw.getHeight();
        ImageProcessor img2 = insert2(raw, width / 2 - 2, false);
        int img2Width = img2.getWidth();
        ImageStack stack = new ImageStack(width, height);
        ImageProcessor filter2;
        ImageProcessor filterCut;
        for (int r = img2Width; r > 0; r--) {
            filter2 = lowPass(img2, r);
            filterCut = inverseInsert2(filter2, height, width);
            stack.addSlice("r = " + r, filterCut);
        }
        new ImagePlus("low pass", stack).show();
    }

    /**
     * createHighPass
     */
    void createHighPass() {
        ImageProcessor raw = getRaw();
        int width = raw.getWidth();
        int height = raw.getHeight();
        ImageProcessor img2 = insert2(raw, width / 2 - 2, false);
        int img2Width = img2.getWidth();
        ImageStack stack = new ImageStack(width, height);
        ImageProcessor filter2;
        ImageProcessor filterCut;
        for (int r = img2Width; r > 0; r--) {
            filter2 = highPass(img2, r);
            filterCut = inverseInsert2(filter2, height, width);
            stack.addSlice("r = " + r, filterCut);
        }
        new ImagePlus("high pass", stack).show();
    }

    /**
     * createBandPass
     */
    void createBandPass() {
        ImageProcessor raw = getRaw();
        int width = raw.getWidth();
        int height = raw.getHeight();
        ImageProcessor img2 = insert2(raw, width / 2 - 2, false);
        int img2Width = img2.getWidth();
        ImageStack stack = new ImageStack(width, height);
        ImageProcessor filter2;
        ImageProcessor filterCut;
        for (int r = img2Width; r > 0; r--) {
            filter2 = bandPass(img2, r + 1, r);
            filterCut = inverseInsert2(filter2, height, width);
            stack.addSlice("r = " + r, filterCut);
        }
        new ImagePlus("low pass", stack).show();
    }

    /**
     * Description of the Method
     *
     * @param ima  Description of the Parameter
     * @return Description of the Return Value
     */
    ImageProcessor highPass(ImageProcessor ima, int rmax) {
        FHTImage3D fht = new FHTImage3D(ima);
        fht = fht.center();
        inverseZeroRing(fht, rmax);
        fht = fht.decenter();
        fht = fht.doInverseTransform();
        ImageProcessor res = fht.getStack().getProcessor(1);

        return res;
    }

    /**
     * Description of the Method
     *
     * @param ima  Description of the Parameter
     * @param rmin Description of the Parameter inclusive
     * @param rmax Description of the Parameter inclusive
     * @return Description of the Return Value
     */
    ImageProcessor bandPass(ImageProcessor ima, int rmin, int rmax) {
        FHTImage3D fht = new FHTImage3D(ima);
        fht = fht.center();
        zeroRing(fht, rmin);
        inverseZeroRing(fht, rmax);
        fht = fht.decenter();
        fht = fht.doInverseTransform();
        ImageProcessor res = fht.getStack().getProcessor(1);

        return res;
    }

    /**
     * Put zero values in ring
     *
     * @param fht  FHT image to zero
     */
    void inverseZeroRing(FHTImage3D fht, int rmin) {
        int w = fht.getSizex();
        int h = fht.getSizey();
        double dist2;
        int xc = w / 2;
        int yc = h / 2;
        int rmin2 = rmin * rmin;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                dist2 = (x - xc) * (x - xc) + (y - yc) * (y - yc);
                if (dist2 < rmin2) {
                    fht.zeroValue(x, y, 0);
                }
            }
        }
    }

    /**
     * Calculates the standard deviation of an array
     * of numbers.
     * see http://davidmlane.com/hyperstat/A16252.html
     *
     * @param data Numbers to compute the standard deviation of.
     *             Array must contain two or more numbers.
     * @return standard deviation estimate of population
     * ( to get estimate of sample, use n instead of n-1 in last line )
     */
    public static double sdFast(double[] data) {
        // sd is sqrt of sum of (values-mean) squared divided by n - 1
        // Calculate the mean
        double mean = 0;
        final int n = data.length;
        if (n < 2) {
            return Double.NaN;
        }
        for (int i = 0; i < n; i++) {
            mean += data[i];
        }
        mean /= n;
        // calculate the sum of squares
        double sum = 0;
        for (int i = 0; i < n; i++) {
            final double v = data[i] - mean;
            sum += v * v;
        }
        return Math.sqrt(sum / (n - 1));
    }
}
