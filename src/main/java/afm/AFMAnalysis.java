package afm;

import ij.*;
import ij.gui.*;
import ij.plugin.frame.*;
import ij.process.*;
import ij.io.*;
import java.io.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.*;

/**
 *  Description of the Class
 *
 * @author     thomas
 * @created    23 octobre 2007
 */
public class AFMAnalysis {

    // PointPicker
    //pointToolbar tb;
    //pointHandler[] ph = null;
    //pointAction pa;
    // List to store the peaks position
    ArrayList peaksA = null;
    ArrayList peaksB = null;
    ResourceBundle bundle;

    /**
     *  Constructor for the AFMAnalysis object
     *
     * @param  bun  Description of the Parameter
     */
    public AFMAnalysis(ResourceBundle bun) {
        bundle = bun;
    }

    /**
     *  List correlation vs angle for each peak
     *
     * @param  p       The peaks
     * @param  ima     The raw image
     * @param  ref     The reference image
     * @param  angmax  Maximum angle range (degrees)
     * @param  anginc  Increment angle (degrees)
     */
    public void savePeaks(ArrayList p, ImageProcessor ima, ImageProcessor ref, double angmax, double anginc) {
        SaveDialog sd = new SaveDialog("Save peaks", "peaks", ".txt");
        String dir = sd.getDirectory();
        String name = sd.getFileName();
        String output = dir + name;
        savePeaks(p, ima, ref, output, angmax, anginc);
    }

    /**
     *  List correlation vs angle for each peak
     *
     * @param  p         The peaks
     * @param  ima       The raw image
     * @param  ref       The reference image
     * @param  filename  The file name to store results
     * @param  angmax    Maximum angle range (degrees)
     * @param  anginc    Increment angle (degrees)
     */
    public void savePeaks(ArrayList p, ImageProcessor ima, ImageProcessor ref, String filename, double angmax, double anginc) {
        int KernelWidth = 0;
        if (ref != null) {
            KernelWidth = ref.getWidth();
        }
        int KernelHeight = 0;
        if (ref != null) {
            KernelHeight = ref.getHeight();
        }
        int n = p.size();
        Peaks pp;
        int xc;
        int yc;
        double agp;
        if ((anginc <= 0) || (anginc >= 360)) {
            agp = Math.PI * 2;
        } else {
            agp = Math.toRadians(anginc);
        }
        double amax = Math.toRadians(angmax);
        ImageProcessor ip3;
        double corr;

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filename, false));
            out.write("# peaks list\n");
            out.write("nbpeak=" + p.size() + "\n");
            for (int v = 0; v < n; v++) {
                pp = (Peaks) (p.get(v));
                xc = pp.getX();
                yc = pp.getY();
                out.write("\npeak=" + v + " " + pp);
                if ((ima != null) && (ref != null)) {
                    ima.setRoi(new Rectangle(xc - KernelWidth / 2, yc - KernelHeight / 2, KernelWidth, KernelHeight));
                    ip3 = ima.crop();
                    // correlation vs angle
                    for (double a = -amax / 2; a < amax / 2; a += agp) {
                        corr = AFMImage.correlationValue(ip3, ref, a, KernelWidth / 2);
                        out.write("\n" + Math.toDegrees(a) + " " + corr);
                    }
                }
            }
            out.close();
            IJ.log("file " + filename + " saved");

        } catch (IOException e) {
        }
    }

    /**
     *  Description of the Method
     *
     * @return    Description of the Return Value
     */
    public ArrayList openListePeaks() {
        OpenDialog sd = new OpenDialog("Open peaks", "peaks", ".txt");
        String dir = sd.getDirectory();
        String name = sd.getFileName();
        String output = dir + name;
        return openListePeaks(output);
    }

    /**
     *  Description of the Method
     *
     * @param  filename  Description of the Parameter
     * @return           Description of the Return Value
     */
    ArrayList openListePeaks(String filename) {
        String data;
        String[] coord;
        int posx;
        int posy;
        float ang, corr, is;

        //angle
        //corr
        //Is.....
        ArrayList v = new ArrayList();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            data = in.readLine();
            while (data != null) {
                if (data.startsWith("peak")) {

                    coord = data.split(" ");
                    // x
                    data = coord[1].substring(2);
                    posx = Integer.parseInt(data);
                    //y
                    data = coord[2].substring(2);
                    posy = Integer.parseInt(data);
                    //ang
                    data = coord[3].substring(4);
                    ang = Float.parseFloat(data);
                    //corr
                    data = coord[4].substring(5);
                    corr = Float.parseFloat(data);
                    //is
                    data = coord[5].substring(3);
                    is = Float.parseFloat(data);

                    v.add(new Peaks(posx, posy, corr, ang, is));
                }
                data = in.readLine();
            }
            in.close();
        } catch (IOException e) {
            IJ.log("open peaks " + e);
        }
        return v;
    }

    /**
     *  Find the position of the peaks on the correlation image (construct the
     *  ArrayList)
     *
     * @param  corr        the correlation image
     * @param  p           the threshold value
     * @param  raw         the raw image to draw the position
     * @param  ref         the ref kernel image (for sizes)
     * @param  ang         the image with correlation angles
     * @param  rad         the radius to find max local
     * @param  nf          the nfold value for internal symmetry
     * @param  tfold       the threshold for internal symmetry
     * @param  radiusCorr  Radius to compute internal symmetry
     * @return             an image with the position marked
     */
    public ImageProcessor findPeaks(ImageProcessor raw, ImageProcessor ref, ImageProcessor corr, ImageProcessor ang, double p1, double p2, int rad, int nf, double tfold1, double tfold2, int radiusCorr) {
        ImageProcessor res = raw.createProcessor(raw.getWidth(), raw.getHeight());
        res.insert(raw, 0, 0);
        int KernelWidth = ref.getWidth();
        int KernelHeight = ref.getHeight();
        int ACWidth = raw.getWidth();
        int ACHeight = raw.getHeight();
        double rfactor;
        double dist;
        double rad2 = rad * rad;
        int boxmax = rad;
        boolean max;
        peaksA = new ArrayList();
        ImageProcessor crop;
        ImageProcessor nfold;
        double cv = 0.0;
        for (int x = KernelWidth / 2; x < ACWidth - KernelWidth / 2; x++) {
            for (int y = KernelHeight / 2; y < ACHeight - KernelHeight / 2; y++) {
                rfactor = corr.getPixelValue(x, y);
                // greater than threshold
                if (rfactor >= p1 && rfactor <= p2) {
                    max = true;
                    // max local
                    for (int xx = x - boxmax; xx <= x + boxmax; xx++) {
                        for (int yy = y - boxmax; yy <= y + boxmax; yy++) {
                            dist = (xx - x) * (xx - x) + (yy - y) * (yy - y);
                            if (dist < rad2) {
                                if (corr.getPixelValue(xx, yy) > rfactor) {
                                    max = false;
                                    break;
                                }
                            }
                        }
                    }
                    if (max) {    // if ((max) && (tfold1 > 0.0)) {
                        // Internal symmetry
                        raw.setRoi(x - radiusCorr, y - radiusCorr, 2 * radiusCorr, 2 * radiusCorr);
                        crop = raw.crop();
                        nfold = AFMImage.nFold(crop, nf, false);
                        cv = AFMImage.correlationValue(crop, nfold, 0, radiusCorr);
                        if (cv <= tfold1 || cv > tfold2) { // added by peter: || cv > tfold2
                            System.out.println("IS " + cv);
                            max = false;
                        }
                    }
                    if (max) {
                        peaksA.add(new Peaks(x, y, corr.getPixelValue(x, y), ang.getPixelValue(x, y), (float) cv));
                    }
                }
            }
        }
        IJ.log("\n" + peaksA.size() + " " + bundle.getString("$PEAKSFOUND") + " " + p1 + " <= Sym [%] <= " + p2 + ", and " + tfold1 + " <= Int Sym [%] <= " + tfold2 + " .");
        Peaks Pi;
        for (int v = 0; v < peaksA.size(); v++) {
            Pi = (Peaks) peaksA.get(v);
            IJ.log("peak : " + Pi);
        }

        return res;
    }

    /**
     *  creation of the roimanager interface on a window
     *
     * @param  imp     the window
     * @param  pos     Description of the Parameter
     * @param  color   Description of the Parameter
     * @param  create  Description of the Parameter
     */
    void createRoiManager(ImagePlus imp, ArrayList pos, String color, int rad, boolean reset) {
        WindowManager.setTempCurrentImage(imp);
        // roimanager
        RoiManager roimanager = RoiManager.getInstance();
        if (roimanager == null) {
            roimanager = new RoiManager();
        } else if (reset) {
            roimanager.runCommand("Deselect");
            roimanager.runCommand("Delete");
        }
        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(3);

        // add the peaks
        Peaks pp;
        Roi roi;
        int xc;
        int yc;
        String name;
        float cc, ag, is;
        for (int v = 0; v < pos.size(); v++) {
            pp = (Peaks) (pos.get(v));
            xc = pp.getX();
            yc = pp.getY();
            cc = pp.getCorr();
            ag = pp.getAngle();
            is = pp.getInternalSymmetry();
            roi = new Roi(xc - rad, yc - rad, 2 * rad, 2 * rad);
            name = "" + nf.format(cc) + " " + nf.format(ag) + " " + nf.format(is);
            IJ.log("peak " + v + " " + cc + " " + ag + " " + is + " " + name);
            roi.setName(name);
            imp.setRoi(roi);
            roimanager.runCommand("Add", color, rad);
        }
        // pb with first peak !!
        pp = (Peaks) (pos.get(0));
        cc = pp.getCorr();
        ag = pp.getAngle();
        is = pp.getInternalSymmetry();
        name = "" + nf.format(cc) + " " + nf.format(ag) + " " + nf.format(is);
        roimanager.select(0);
        roimanager.runCommand("Rename", name);


        roimanager.runCommand("Show All");
    }

    /**
     *  creation of the pointpicker interface on a window
     *
     * @param  imp     the window
     * @param  pos     Description of the Parameter
     * @param  color   Description of the Parameter
     * @param  create  Description of the Parameter
     */
    /*
    void createPointPicker(ImagePlus imp, ArrayList pos, int color, boolean create) {
    // create the point picker interface
    ImageCanvas ic = imp.getWindow().getCanvas();
    if ((tb == null) || (tb.isTerminated())) {
    tb = new pointToolbar(Toolbar.getInstance());
    }
    //tb.setMode(pointAction.REMOVE_CROSS);
    int stackSize = imp.getStackSize();
    if (create) {
    ph = new pointHandler[stackSize];
    for (int s = 0; (s < stackSize); s++) {
    ph[s] = new pointHandler(imp, tb);
    }
    pa = new pointAction(imp, ph, tb);
    for (int s = 0; (s < stackSize); s++) {
    ph[s].setPointAction(pa);
    }
    }
    // add the peaks to pointpicker
    Peaks pp;
    int xc;
    int yc;
    for (int v = 0; v < pos.size(); v++) {
    pp = (Peaks) (pos.get(v));
    xc = pp.getX();
    yc = pp.getY();
    if (color != -1) {
    ph[0].addPoint(xc, yc, color);
    } else {
    ph[0].addPoint(xc, yc);
    }
    }
    }
     * /


    /**
     *  Description of the Method
     *
     * @param  pe   Description of the Parameter
     * @param  raw  Description of the Parameter
     * @return      Description of the Return Value
     */
    double areaConvexPolygon(ImageProcessor raw, ArrayList pe) {
        int np = pe.size();
        Peaks pp;
        int[] xp = new int[np];
        int[] yp = new int[np];
        for (int i = 0; i < np; i++) {
            pp = (Peaks) (pe.get(i));
            xp[i] = pp.getX();
            yp[i] = pp.getY();
        }
        PolygonRoi roi = new PolygonRoi(xp, yp, np, Roi.POLYGON);

        // copy from ij:selection.java:makeConvexHull
        int n = roi.getNCoordinates();
        int[] xCoordinates = roi.getXCoordinates();
        int[] yCoordinates = roi.getYCoordinates();
        Rectangle r = roi.getBounds();
        int xbase = r.x;
        int ybase = r.y;
        int[] xx = new int[n];
        int[] yy = new int[n];
        int n2 = 0;
        int p1 = findFirstPoint(xCoordinates, yCoordinates, n, raw);
        int pstart = p1;
        int x1;
        int y1;
        int x2;
        int y2;
        int x3;
        int y3;
        int p2;
        int p3;
        int determinate;
        do {
            x1 = xCoordinates[p1];
            y1 = yCoordinates[p1];
            p2 = p1 + 1;
            if (p2 == n) {
                p2 = 0;
            }
            x2 = xCoordinates[p2];
            y2 = yCoordinates[p2];
            p3 = p2 + 1;
            if (p3 == n) {
                p3 = 0;
            }
            do {
                x3 = xCoordinates[p3];
                y3 = yCoordinates[p3];
                determinate = x1 * (y2 - y3) - y1 * (x2 - x3) + (y3 * x2 - y2 * x3);
                if (determinate > 0) {
                    x2 = x3;
                    y2 = y3;
                    p2 = p3;
                }
                p3 += 1;
                if (p3 == n) {
                    p3 = 0;
                }
            } while (p3 != p1);
            if (n2 < n) {
                xx[n2] = xbase + x1;
                yy[n2] = ybase + y1;
                n2++;
            }
            p1 = p2;
        } while (p1 != pstart);

        // aire du polygone
        int som1 = 0;
        for (int i = 0; i < n - 1; i++) {
            som1 += xx[i + 1] * yy[i];
        }
        som1 += xx[0] * yy[n - 1];

        int som2 = 0;
        for (int i = 0; i < n - 1; i++) {
            som2 += xx[i] * yy[i + 1];
        }
        som2 += xx[n - 1] * yy[0];

        return Math.abs(0.5 * (som1 - som2));
    }

    /**
     *  Finds the index of the upper right point that is guaranteed to be on convex
     *  hull Copy from ij : selection.java
     *
     * @param  xCoordinates  Description of the Parameter
     * @param  yCoordinates  Description of the Parameter
     * @param  n             Description of the Parameter
     * @param  imp           Description of the Parameter
     * @return               Description of the Return Value
     */
    int findFirstPoint(int[] xCoordinates, int[] yCoordinates, int n, ImageProcessor imp) {
        int smallestY = imp.getHeight();
        int x;
        int y;
        for (int i = 0; i < n; i++) {
            y = yCoordinates[i];
            if (y < smallestY) {
                smallestY = y;
            }
        }
        int smallestX = imp.getWidth();
        int p1 = 0;
        for (int i = 0; i < n; i++) {
            x = xCoordinates[i];
            y = yCoordinates[i];
            if (y == smallestY && x < smallestX) {
                smallestX = x;
                p1 = i;
            }
        }
        return p1;
    }

    /**
     *  Gets the points position From PointPicker
     *
     * @param  corrval  Description of the Parameter
     * @param  corrang  Description of the Parameter
     * @return          The peaksFromPicker value
     */
    public ArrayList getPeaksFromRoiManager(ImageProcessor corrval, ImageProcessor corrang) {
        ArrayList peaks = new ArrayList();
        RoiManager roimanager = RoiManager.getInstance();
        Roi[] rois = roimanager.getRoisAsArray();
        int s = rois.length;
        float corrvalue = 0;
        float angvalue = 0;
        float isvalue = 0;
        Point p;
        int xc;
        int yc;
        Roi roi;
        Rectangle rect;
        String name;
        String[] data = null;
        // the name of the peak should be 0.58 60 0.23 (correlation angle internal symetry)
        for (int v = 0; v < s; v++) {
            roi = rois[v];
            name = roi.getName();
            if (name.contains(" ")) {
                data = name.split(" ");
            } else {
                data = null;
            }
            //    IJ.log("roi "+v+" "+name);
            rect = roi.getBounds();
            xc = rect.x + rect.width / 2;
            yc = rect.y + rect.height / 2;
            if (corrval != null) {
                corrvalue = corrval.getPixelValue(xc, yc);
            } else {
                if (data != null) {
                    corrvalue = Float.parseFloat(data[0]);
                } else {
                    corrvalue = 0;
                }
            }
            if (corrang != null) {
                angvalue = corrang.getPixelValue(xc, yc);
            } else {
                if (data != null) {
                    angvalue = Float.parseFloat(data[1]);
                } else {
                    angvalue = 0;
                }
            }
            if (data != null) {
                try{
                isvalue = Float.parseFloat(data[2]);
                }
                catch(java.lang.NumberFormatException e){
                    IJ.log("Pb with number "+e);
                    isvalue=0;
                };
            } else {
                isvalue = 0;
            }
            //	peaks.add(new Peaks(xc, yc, corrval.getPixelValue(xc, yc), corrang.getPixelValue(xc, yc), 0.0f));
            peaks.add(new Peaks(xc, yc, corrvalue, angvalue, isvalue));
        }
        return peaks;
    }

    /**
     *  Gets the points position From PointPicker
     *
     * @param  corrval  Description of the Parameter
     * @param  corrang  Description of the Parameter
     * @return          The peaksFromPicker value
     */
    /*
    public ArrayList getPeaksFromPicker(ImageProcessor corrval, ImageProcessor corrang) {
    ArrayList peaks = new ArrayList();
    ArrayList pp = ph[0].getPoints();
    Point p;
    int xc;
    int yc;
    for (int v = 0; v < pp.size(); v++) {
    p = (Point) (pp.get(v));
    xc = (int) p.getX();
    yc = (int) p.getY();
    peaks.add(new Peaks(xc, yc, corrval.getPixelValue(xc, yc), corrang.getPixelValue(xc, yc), 0.0f));
    }
    return peaks;
    }
     * /


    /**
     *  Compute the Pair Correlation function
     *
     * @param  pe      the positions of the peaks
     * @param  radbin  Description of the Parameter
     * @param  qe      Description of the Parameter
     * @param  raw     Description of the Parameter
     * @param  type    Description of the Parameter
     */
    public void computePCF(ImageProcessor raw, String type, ArrayList pe, ArrayList qe, int radbin, double convert) {
        int np = pe.size();
        int nq = qe.size();
        int width = raw.getWidth();
        int height = raw.getHeight();
        int px;
        int py;
        int qx;
        int qy;
        double dist;
        double distmax = 0.0;
        int distbin = 0;
        double maxval = 0.0;
        int dp;
        int dpmax = 0;
        double dr = (double) radbin;
        double[] distpeaks = new double[(raw.getHeight() + raw.getWidth() / (int) dr)];
        double[] pcfhisto = new double[distpeaks.length];
        int[] histnb = new int[distpeaks.length];
        double[] rad = new double[distpeaks.length];
        for (int i = 0; i < pcfhisto.length; i++) {
            rad[i] = i * dr;
        }
        double surf = areaConvexPolygon(raw, pe);
        double dense = (double) np / surf;
        for (int p = 0; p < np; p++) {
            distmax = 0;
            distbin = 0;
            distpeaks = new double[(raw.getHeight() + raw.getWidth() / (int) dr)];
            px = ((Peaks) pe.get(p)).getX();
            py = ((Peaks) pe.get(p)).getY();
            dp = Math.min(px, py);
            dp = Math.min(dp, width - px);
            dp = Math.min(dp, height - py);
            for (int q = 0; q < nq; q++) {
                qx = ((Peaks) qe.get(q)).getX();
                qy = ((Peaks) qe.get(q)).getY();
                dist = Math.sqrt((px - qx) * (px - qx) + (py - qy) * (py - qy));
                if ((dist < dp) && (dist > 0.01)) {
                    distpeaks[(int) Math.floor(dist / dr)]++;
                    if (dist > distmax) {
                        distmax = dist;
                        distbin = (int) Math.floor(dist / dr);
                    }
                }
            }
            // normalisation et ajout histo general
            for (int i = 0; i <= distbin; i++) {
                distpeaks[i] /= Math.PI * (2 * dr * rad[i] + dr * dr);
                distpeaks[i] /= dense;
                pcfhisto[i] += distpeaks[i];
                histnb[i]++;
            }
        }

        // histogramme final
        maxval = 0;
        distmax = 0;
        int imax = 0;
        for (int i = 0; i < pcfhisto.length; i++) {
            pcfhisto[i] /= (double) histnb[i];
            if (pcfhisto[i] > maxval) {
                maxval = pcfhisto[i];
            }
            if (pcfhisto[i] > 0.0) {
                distmax = rad[i] * convert; //
                imax = i;
            }
        }

        distpeaks = new double[imax + 1];
        double[] radpeaks = new double[imax + 1];
        for (int i = 0; i < distpeaks.length; i++) {
            radpeaks[i] = rad[i] * convert;    //
            distpeaks[i] = pcfhisto[i];
        }

        // plot resolution
        PlotWindow plot = new PlotWindow("Pair Corr. Func. " + type, "radius [Angstrom]", "pcf", radpeaks, distpeaks);
        plot.setLimits(0.0, distmax, 0.0, maxval);
        plot.draw();
        plot.show();
    }

    /**
     *  Gets the peaksA attribute of the AFMAnalysis object
     *
     * @return    The peaksA value
     */
    public ArrayList getPeaksA() {
        return peaksA;
    }

    /**
     *  Sets the peaksA attribute of the AFMAnalysis object
     *
     * @param  v  The new peaksA value
     */
    public void setPeaksA(ArrayList v) {
        peaksA = v;
    }

    /**
     *  Sets the peaksB attribute of the AFMAnalysis object
     *
     * @param  v  The new peaksB value
     */
    public void setPeaksB(ArrayList v) {
        peaksB = v;
    }

    /**
     *  Gets the peaksB attribute of the AFMAnalysis object
     *
     * @return    The peaksB value
     */
    public ArrayList getPeaksB() {
        return peaksB;
    }

    private int countPeaks(ImageProcessor raw, ImageProcessor ref, ImageProcessor corr, ImageProcessor ang, double p1, double p2, int rad, int nf, double tfold1, double tfold2, int radiusCorr) {
//		ImageProcessor res = raw.createProcessor(raw.getWidth(), raw.getHeight());
//		res.insert(raw, 0, 0);
        int KernelWidth = ref.getWidth();
        int KernelHeight = ref.getHeight();
        int ACWidth = raw.getWidth();
        int ACHeight = raw.getHeight();
        double rfactor;
        double dist;
        double rad2 = rad * rad;
        int boxmax = rad;
        boolean max;
        peaksA = new ArrayList();
        ImageProcessor crop;
        ImageProcessor nfold;
        double cv = 0.0;
        for (int x = KernelWidth / 2; x < ACWidth - KernelWidth / 2; x++) {
            for (int y = KernelHeight / 2; y < ACHeight - KernelHeight / 2; y++) {
                rfactor = corr.getPixelValue(x, y);
                // greater than threshold
                if (rfactor >= p1 && rfactor <= p2) {
                    max = true;
                    // max local
                    for (int xx = x - boxmax; xx <= x + boxmax; xx++) {
                        for (int yy = y - boxmax; yy <= y + boxmax; yy++) {
                            dist = (xx - x) * (xx - x) + (yy - y) * (yy - y);
                            if (dist < rad2) {
                                if (corr.getPixelValue(xx, yy) > rfactor) {
                                    max = false;
                                    break;
                                }
                            }
                        }
                    }
                    if (max) {    // if ((max) && (tfold1 > 0.0)) {
                        // Internal symmetry
                        raw.setRoi(x - radiusCorr, y - radiusCorr, 2 * radiusCorr, 2 * radiusCorr);
                        crop = raw.crop();
                        nfold = AFMImage.nFold(crop, nf, false);
                        cv = AFMImage.correlationValue(crop, nfold, 0, radiusCorr);
                        if (cv <= tfold1 || cv > tfold2) { // added by peter: || cv > tfold2
                            //				System.out.println("IS " + cv);
                            max = false;
                        }
                    }
                    if (max) {
                        peaksA.add(new Peaks(x, y, corr.getPixelValue(x, y), ang.getPixelValue(x, y), (float) cv));
                    }
                }
            }
        }


        int count = peaksA.size();
        return peaksA.size();
    }

    void plotCorrelation(ImageProcessor raw, ImageProcessor ref, ImageProcessor corr, ImageProcessor ang, int rad, int nf, double tfold1, double tfold2, int radiusCorr) {
        double[] corrTresh = new double[51]; //101
        double[] numParticles = new double[corrTresh.length];

        //creates array with correlation thresholds and array with corresponding number of particles
        for (int v = 0; v < corrTresh.length; v++) {
            if (v == 0) {
                corrTresh[v] = 0;
            } else {
                corrTresh[v] = corrTresh[v - 1] + 0.02;
            } //0.01

            numParticles[v] = countPeaks(raw, ref, corr, ang, corrTresh[v], 1.0, rad, nf, tfold1, tfold2, radiusCorr);
        }

        //calculates histogram
        double[] histCorr = new double[corrTresh.length];
        double[] histPart = new double[histCorr.length];
        for (int v = 0; v < histCorr.length; v++) {
            histCorr[v] = corrTresh[v];
            if (v == 0) {
                histPart[v] = 0.0;
            } else {
                histPart[v] = numParticles[v - 1] - numParticles[v];
            }
        }


        // plot particles per correlation threshold
        PlotWindow plot = new PlotWindow("Particles found per correlation threshold", "correlation treshold", "Number of particles", corrTresh, numParticles);
        plot.draw();
        plot.show();

        // plot histogram
        PlotWindow plot2 = new PlotWindow("Histogram of Particles found at correlation value", "correlation value", "Number of particles", histCorr, histPart);
        plot2.draw();
        plot2.show();

    }

    void plotSymmetry(ImageProcessor raw, ImageProcessor ref, ImageProcessor corr, ImageProcessor ang, int rad, int nf, double corr1, double corr2, int radiusCorr) {
        double[] symTresh = new double[51]; //101
        double[] numParticles = new double[symTresh.length];

        //creates array with sym thresholds and array with corresponding number of particles
        for (int v = 0; v < symTresh.length; v++) {
            if (v == 0) {
                symTresh[v] = 0;
            } else {
                symTresh[v] = symTresh[v - 1] + 0.02;
            } //0.01

            numParticles[v] = countPeaks(raw, ref, corr, ang, corr1, corr2, rad, nf, symTresh[v], 1.0, radiusCorr);
        }

        //calculates histogram
        double[] histSym = new double[symTresh.length];
        double[] histPart = new double[histSym.length];
        for (int v = 0; v < histSym.length; v++) {
            histSym[v] = symTresh[v];
            if (v == 0) {
                histPart[v] = 0.0;
            } else {
                histPart[v] = numParticles[v - 1] - numParticles[v];
            }

//			if (v < histSym.length - 1) { histPart[v] = numParticles[v] - numParticles[v+1];}
//			else {histPart[v] = 0.0;}
        }


        // plot particles per sym threshold
        PlotWindow plot = new PlotWindow("Particles found per symmetry threshold", "symmetry treshold", "Number of particles", symTresh, numParticles);
        plot.draw();
        plot.show();

        // plot histogram
        PlotWindow plot2 = new PlotWindow("Histogram of Particles found at symmetry value", "symmetry value", "Number of particles", histSym, histPart);
        plot2.draw();
        plot2.show();

    }

    ArrayList<Peaks> sortPeaks(ArrayList<Peaks> peaks) {
        Peaks[] arr = new Peaks[1];
        arr = (Peaks[]) peaks.toArray();
        java.util.Arrays.sort(arr);
        ArrayList<Peaks> v = new ArrayList();
        for (int i = 0; i < arr.length; i++) {
            v.add(arr[i]);
        }
        return v;
    }
}
