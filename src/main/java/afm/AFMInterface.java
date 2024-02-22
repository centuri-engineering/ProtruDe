package afm;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.frame.PlugInFrame;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Description of the Class
 *
 * @author thomas @created 23 octobre 2007
 */
public class AFMInterface extends PlugInFrame {

    static AFMInterface instance = null;
    ResourceBundle bundle;
    AFMImage afmimage;
    AFMAnalysis afmanalysis;
    // text field to enter values
    JTextField jtfCorrPct;
    JTextField jtfCorrPct2;
    JTextField istCorrPct;
    JTextField istCorrPct2;
    JTextField jtfNF;
    JTextField jtfRAD;
    JTextField jtfRADBIN;
    JTextField jtfAngInc;
    JTextField jtfAngMax;
    JTextField jtfRadPeaks;
    // variables to store values
    double pctcorr1 = 0.0;
    double pctcorr2 = 1.0;
    double istcorr1 = 0.0;
    double istcorr2 = 1.0;
    int nfold = 4;
    int radius = 0;
    int radiuspeaks = 0;
    int radbinpcf = 1;
    int anginc = 0;
    int angmax = 0;
    // calibration
    int imagePixel = 512;                            // added by Peter imagePixel, scanSize, zScale
    int scanSize = 1783;
    double zScale = 15.0;
    double convert = scanSize / (double) imagePixel; // [Angstrom/Pixel]
    double radiusInAng = 0;                          // Particle radius in Angstrom
    double radiuspeaksInAng = 0;                     // radiuspeaks in Angstrom
    boolean debug = false;

    /**
     * Constructor for the AFMInterface object
     */
    public AFMInterface() {
        super("AFMJ-1.0b");
        bundle = ResourceBundle.getBundle("i18n/AfmBundle");
        doInterface();
        drawInterface();

        afmimage = new AFMImage(bundle);
        afmanalysis = new AFMAnalysis(bundle);

        instance = this;
    }

    public static AFMInterface getAFMInterface() {
        return instance;
    }

    public double getIstcorr1() {
        return istcorr1;
    }

    public double getIstcorr2() {
        return istcorr2;
    }

    public int getNfold() {
        return nfold;
    }

    public double getPctcorr1() {
        return pctcorr1;
    }

    public double getPctcorr2() {
        return pctcorr2;
    }

    public int getRadiuspeaks() {
        return radiuspeaks;
    }

    public double getradiuspeaksInAng() {
        return radiuspeaksInAng;
    }

    public int getAngleInc() {
        return anginc;
    }

    public int getAngleMax() {
        return angmax;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadiusPeaks(int r) {
        radiuspeaks = r;
    }

    public void setradiuspeaksInAng(double r) {
        radiuspeaksInAng = r;
    }

    public double getConvert() {
        return convert;
    }

    public AFMAnalysis getAFMAnalysis() {
        return afmanalysis;
    }

    public AFMImage getAFMImage() {
        return afmimage;
    }

    /**
     * creation of the interface for averaging
     *
     * @return the average panel interface
     */
    public JPanel createImageAveragingPanel() {
        if (debug) {
            System.out.println("Function interface");
        }
        JPanel ImageAveragingP = new JPanel();
        ImageAveragingP.setBackground(Color.LIGHT_GRAY);
        ImageAveragingP.setLayout(new BoxLayout(ImageAveragingP, BoxLayout.Y_AXIS));

        // intro text 1
        JPanel head = new JPanel();
        head.setLayout(new FlowLayout());
        head.setBackground(Color.LIGHT_GRAY);
        head.add(new JLabel("Average Panel"));
        ImageAveragingP.add(head);

        // average button
        JPanel panelAvgButton = new JPanel();
        panelAvgButton.setLayout(new FlowLayout());
        panelAvgButton.setBackground(Color.LIGHT_GRAY);
        //JButton averageB = new JButton(bundle.getString("$AVE RAGE"));
        JButton averageB = new JButton("Calculate Average");
        averageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        ArrayList v = afmanalysis.getPeaksA();
                        ImageProcessor avgimage = afmimage.average(afmimage.getRaw(), afmimage.getRef(), v, 2 * afmimage.getRef().getWidth());
                        afmimage.setAverage(avgimage);
                        //  ImagePlus impavg = new ImagePlus(bundle.getString("$AVERAGE"), avgimage);
                        ImagePlus impavg = new ImagePlus(("$AVERAGE"), avgimage);
                        afmimage.setImpAverage(impavg);
                        impavg.show();
                        ImageProcessor varimage = afmimage.standardDeviation(afmimage.getRaw(), afmimage.getRef(), avgimage, v, 2 * afmimage.getRef().getWidth());
                        afmimage.setVariance(varimage);
                        //   ImagePlus impvar = new ImagePlus(bundle.getString("$VARIANCE"), varimage);
                        ImagePlus impvar = new ImagePlus(("$VARIANCE"), varimage);
                        afmimage.setImpVariance(impvar);
                        impvar.show();
                    }
                });
        panelAvgButton.add(averageB);
        ImageAveragingP.add(panelAvgButton);

        // open average image button
        JPanel panelImage = new JPanel();
        panelImage.setLayout(new FlowLayout());
        panelImage.setBackground(Color.LIGHT_GRAY);
        JButton ImageB = new JButton("Open Average");
        ImageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        afmimage.openAv();
                        radius = afmimage.getAverage().getWidth() / 2;
                    }
                });
        panelImage.add(ImageB);
        ImageAveragingP.add(panelImage);

        // nfold avg
        JPanel panelNFoldAvgButton = new JPanel();
        panelNFoldAvgButton.setLayout(new FlowLayout());
        panelNFoldAvgButton.setBackground(Color.LIGHT_GRAY);
        //   JButton nFoldAvgImageB = new JButton(bundle.getString("$NFOLDACTION") + " " + bundle.getString("$AVERAGE"));
        JButton nFoldAvgImageB = new JButton(("Do n-fold") + " " + ("Average"));
        nFoldAvgImageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        GenericDialog gd = new GenericDialog("Do n-fold average");
                        gd.addMessage("Please check if n-fold value is correct.");
                        gd.showDialog();

                        ImageProcessor avgsymimage = afmimage.nFold(afmimage.getAverage(), nfold, true);
                        afmimage.setAvgSym(avgsymimage);
                        // ImagePlus impavgsym = new ImagePlus(bundle.getString("$AVERAGESYM"), avgsymimage);
                        ImagePlus impavgsym = new ImagePlus(("$AVERAGESYM"), avgsymimage);
                        afmimage.setImpAvgSym(impavgsym);
                        impavgsym.show();
                    }
                });
        panelNFoldAvgButton.add(nFoldAvgImageB);
        ImageAveragingP.add(panelNFoldAvgButton);

        // average as ref button
        JPanel panelAvgRefButton = new JPanel();
        panelAvgRefButton.setLayout(new FlowLayout());
        panelAvgRefButton.setBackground(Color.LIGHT_GRAY);
        // JButton averageRefB = new JButton(bundle.getString("$USEAVGREF"));
        JButton averageRefB = new JButton(("Use as Refrence"));
        averageRefB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        afmimage.setAvgAsRef();
                    }
                });
        panelAvgRefButton.add(averageRefB);
        ImageAveragingP.add(panelAvgRefButton);

        // intro text 2
        JPanel head2 = new JPanel();
        head2.setLayout(new FlowLayout());
        head2.setBackground(Color.LIGHT_GRAY);
        head2.add(new JLabel("Analysis Panel"));
        ImageAveragingP.add(head2);

        // best symmetry button
        JPanel panelBestSymButton = new JPanel();
        panelBestSymButton.setLayout(new FlowLayout());
        panelBestSymButton.setBackground(Color.LIGHT_GRAY);
        //  JButton BestSymImageB = new JButton(bundle.getString("$BESTSYM"));
        JButton BestSymImageB = new JButton(("Best Symmetry"));
        BestSymImageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        IJ.log("Best symmetry avg : " + afmimage.bestSymmetry(afmimage.getAverage(), radius));
                    }
                });
        panelBestSymButton.add(BestSymImageB);
        ImageAveragingP.add(panelBestSymButton);


        // height range button
        JPanel panelHeight = new JPanel();
        panelHeight.setLayout(new FlowLayout());
        panelHeight.setBackground(Color.LIGHT_GRAY);
        JButton HeightB = new JButton("Height Range");
        HeightB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        GenericDialog gd = new GenericDialog("Height range");
                        gd.addMessage("Please check if z-scale value and the peak list A are correct.");
                        gd.showDialog();

                        ArrayList peaksA = afmanalysis.getPeaksA();
                        afmimage.analyseHeight(zScale, peaksA);
                    }
                });
        panelHeight.add(HeightB);
        ImageAveragingP.add(panelHeight);


        // resolution button particle image
        JPanel panelResButtonImage = new JPanel();
        panelResButtonImage.setLayout(new FlowLayout());
        panelResButtonImage.setBackground(Color.LIGHT_GRAY);
        JButton resolutionAvgB = new JButton("Resolution Average Image");
        resolutionAvgB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        GenericDialog gd = new GenericDialog("Resolution average image");
                        gd.addMessage("Please check if image size and scan size values are correct.");
                        gd.showDialog();

                        afmimage.Resolution(afmimage.getAverage(), convert, radius, "Resolution Image");
                    }
                });
        panelResButtonImage.add(resolutionAvgB);
        ImageAveragingP.add(panelResButtonImage);

        // resolution button peaklist
        JPanel panelResButtonList = new JPanel();
        panelResButtonList.setLayout(new FlowLayout());
        panelResButtonList.setBackground(Color.LIGHT_GRAY);
        JButton resolutionListB = new JButton("Resolution Peak List");
        resolutionListB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        GenericDialog gd = new GenericDialog("Resolution peak list");
                        gd.addMessage("Please check if image size and scan size values, and the peak list A are correct.");
                        gd.showDialog();

                        afmimage.resolutionList(afmimage.getRaw(), afmimage.getRef(), afmanalysis.getPeaksA(), convert);
                    }
                });
        panelResButtonList.add(resolutionListB);
        ImageAveragingP.add(panelResButtonList);

        // top ring analysis
        JPanel panelRing = new JPanel();
        panelRing.setLayout(new FlowLayout());
        panelRing.setBackground(Color.LIGHT_GRAY);
        JButton ringB = new JButton("Top Ring Analysis");
        ringB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        GenericDialog gd = new GenericDialog("top ring analysis");
                        gd.addMessage("Please check if raw image, image size and scan size values, and the peak list A are correct.");
                        gd.showDialog();

                        RadialProfile2 radprof = new RadialProfile2();
                        radprof.run(afmimage.raw, afmimage.impraw, afmimage.getRef(), afmanalysis.getPeaksA(), afmimage, convert);

                    }
                });
        panelRing.add(ringB);
        ImageAveragingP.add(panelRing);

//		// symetry resolution button average
//		// added by Peter
//		JPanel panelSymResButtonAvg = new JPanel();
//		panelSymResButtonAvg.setLayout(new FlowLayout());
//		panelSymResButtonAvg.setBackground(Color.magenta);
//		JButton resolutionSymAvgB = new JButton("Res. by Sym.");
//		resolutionSymAvgB.addActionListener(
//			new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					afmimage.SymResolution(afmimage.getAverage(), convert, radius, "Resolution by Symmetry of Average", nfold);
//				}
//			});
//		panelSymResButtonAvg.add(resolutionSymAvgB);
//		ImageAveragingP.add(panelSymResButtonAvg);

//
        return ImageAveragingP;
    }

//

    /**
     * Create the Image interface to load raw and ref images
     *
     * @return the image panel interface
     */
    public JPanel createParametersPanel() {
        JPanel ParametersP = new JPanel();
        ParametersP.setBackground(Color.LIGHT_GRAY);
        ParametersP.setLayout(new BoxLayout(ParametersP, BoxLayout.Y_AXIS));


        // intro text
        //  JPanel head = new JPanel();
        //  head.setLayout(new FlowLayout());
        //   head.setBackground(Color.LIGHT_GRAY);
        //   head.add(new JLabel("Parameters Panel"));
        //  ParametersP.add(head);

        //IP= Image Parameters
        JPanel IP = new JPanel();
        IP.setLayout(new FlowLayout());
        IP.setBackground(Color.LIGHT_GRAY);
        IP.add(new JLabel(" Image Parameters"));
        ParametersP.add(IP);


        // Get the Reference from opened image
        JPanel panelSetImage = new JPanel();
        panelSetImage.setLayout(new FlowLayout());
        panelSetImage.setBackground(Color.LIGHT_GRAY);
        //  JButton setImageB = new JButton(bundle.getString("$SETRAW"));
        JButton setImageB = new JButton("Set Raw Image");
        setImageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        afmimage.setRaw();
                    }
                });
        panelSetImage.add(setImageB);
        ParametersP.add(panelSetImage);

        // open ref image button
        JPanel panelRefImage = new JPanel();
        panelRefImage.setLayout(new FlowLayout());
        panelRefImage.setBackground(Color.LIGHT_GRAY);
        //    JButton refImageB = new JButton(bundle.getString("$OPENREF"));
        JButton refImageB = new JButton("Open Reference");
        refImageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        afmimage.openRef();
                        radius = afmimage.getRef().getWidth() / 2;
                        radiusInAng = radius * convert;
                        radiuspeaks = afmimage.getRef().getWidth() / 2;
                        radiuspeaksInAng = radiuspeaks * convert;
                        jtfRAD.setText(" " + radiusInAng);
                        jtfRadPeaks.setText(" " + radiuspeaksInAng);
                    }
                });
        panelRefImage.add(refImageB);
        ParametersP.add(panelRefImage);

        // crop ref image button
        JPanel panelCropRefImage = new JPanel();
        panelCropRefImage.setLayout(new FlowLayout());
        panelCropRefImage.setBackground(Color.LIGHT_GRAY);
        //    JButton cropRefImageB = new JButton(bundle.getString("$CROPREF"));
        JButton cropRefImageB = new JButton("Crop Reference");
        cropRefImageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // format number
                        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
                        nf.setMinimumFractionDigits(1);
                        nf.setMaximumFractionDigits(1);
                        afmimage.cropRef();
                        radius = afmimage.getRef().getWidth() / 2;
                        radiusInAng = radius * convert;
                        radiuspeaks = afmimage.getRef().getWidth() / 2;
                        radiuspeaksInAng = radiuspeaks * convert;
                        jtfRAD.setText(" " + nf.format(radiusInAng));
                        jtfRadPeaks.setText(" " + nf.format(radiuspeaksInAng));
                    }
                });
        panelCropRefImage.add(cropRefImageB);
        ParametersP.add(panelCropRefImage);

        // image size in pixel
        JPanel panelSizePixel = new JPanel();
        panelSizePixel.setLayout(new FlowLayout());
        panelSizePixel.setBackground(Color.LIGHT_GRAY);
        // panelSizePixel.add(new JLabel(bundle.getString("$PIXELSIZE")));
        panelSizePixel.add(new JLabel("Image Size (Pixel)"));
        final JTextField pixel = new JTextField("   " + imagePixel);
        panelSizePixel.add(pixel);
        pixel.addFocusListener(new FocusListener() {

            public void focusLost(FocusEvent fe) {
                String txt = pixel.getText();
                imagePixel = Integer.parseInt(txt.trim());
                convert = scanSize / (double) imagePixel;
                radiusInAng = radius * convert;
                radiuspeaksInAng = radiuspeaks * convert;
                jtfRAD.setText(" " + radiusInAng);
                jtfRadPeaks.setText(" " + radiuspeaksInAng);
            }

            public void focusGained(FocusEvent fe) {
            }
        });
        ParametersP.add(panelSizePixel);

        // scan size in Angstrom
        JPanel panelScanSize = new JPanel();
        panelScanSize.setLayout(new FlowLayout());
        panelScanSize.setBackground(Color.LIGHT_GRAY);
        //    panelScanSize.add(new JLabel(bundle.getString("$SCANSIZE")));
        panelScanSize.add(new JLabel("Scan Size (Angstrom)"));
        final JTextField scan = new JTextField("   " + scanSize);
        panelScanSize.add(scan);
        scan.addFocusListener(new FocusListener() {

            public void focusLost(FocusEvent fe) {
                String txt = scan.getText();
                scanSize = Integer.parseInt(txt.trim());
                convert = scanSize / (double) imagePixel;
                radiusInAng = radius * convert;
                radiuspeaksInAng = radiuspeaks * convert;
                jtfRAD.setText(" " + radiusInAng);
                jtfRadPeaks.setText(" " + radiuspeaksInAng);
            }

            public void focusGained(FocusEvent fe) {
            }
        });
        ParametersP.add(panelScanSize);

        // Z-Scale in Angstrom
        JPanel panelZScale = new JPanel();
        panelZScale.setLayout(new FlowLayout());
        panelZScale.setBackground(Color.LIGHT_GRAY);
        //     panelZScale.add(new JLabel(bundle.getString("$ZSCALE")));
        panelZScale.add(new JLabel("Z-Scale (Angstrom)"));
        final JTextField scale = new JTextField("   " + zScale);
        panelZScale.add(scale);
        scale.addFocusListener(new FocusListener() {

            public void focusLost(FocusEvent fe) {
                String txt = scale.getText();
                zScale = Double.parseDouble(txt.trim());
            }

            public void focusGained(FocusEvent fe) {
            }
        });
        ParametersP.add(panelZScale);

        //CP= Correlation Parameters
        JPanel CP = new JPanel();
        CP.setLayout(new FlowLayout());
        CP.setBackground(Color.LIGHT_GRAY);
        CP.add(new JLabel(" Correlation Parameters"));
        ParametersP.add(CP);


        // radius correlation value
        JPanel radP = new JPanel();
        radP.setLayout(new FlowLayout());
        radP.setBackground(Color.LIGHT_GRAY);
        //  radP.add(new JLabel(bundle.getString("$RADIUSCORR1")));
        radP.add(new JLabel("Radius (Angstrom)"));
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.setMaximumFractionDigits(3);
        jtfRAD = new JTextField();
        jtfRAD.setText("       " + radiusInAng);
        jtfRAD.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        String txt = jtfRAD.getText();
                        radiusInAng = Double.parseDouble(txt.trim());
                        radius = (int) Math.round(radiusInAng / convert);
                        IJ.log("radius in parameter txt = " + txt);
                        IJ.log("radius in parameter val = " + radius + " " + radiusInAng);

                        afmimage.killCorrelationImage();
                    }

                    public void focusGained(FocusEvent fe) {
                    }
                });
        radP.add(jtfRAD);
        ParametersP.add(radP);

        // angle inc correlation value
        JPanel angPinc = new JPanel();
        angPinc.setLayout(new FlowLayout());
        angPinc.setBackground(Color.LIGHT_GRAY);
        //    angPinc.add(new JLabel(bundle.getString("$ANGLEINC")));
        angPinc.add(new JLabel("Angle Increment"));
        jtfAngInc = new JTextField();
        jtfAngInc.setText("      " + anginc);
        jtfAngInc.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        String txt = jtfAngInc.getText();
                        anginc = Integer.parseInt(txt.trim());
                        afmimage.killCorrelationImage();
                    }

                    public void focusGained(FocusEvent fe) {
                    }
                });
        angPinc.add(jtfAngInc);
        ParametersP.add(angPinc);

        // angle max correlation value
        JPanel angPmax = new JPanel();
        angPmax.setLayout(new FlowLayout());
        angPmax.setBackground(Color.LIGHT_GRAY);
        // angPmax.add(new JLabel(bundle.getString("$ANGLEMAX")));
        angPmax.add(new JLabel("Angle Range"));
        jtfAngMax = new JTextField();
        jtfAngMax.setText("     " + angmax);
        jtfAngMax.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        String txt = jtfAngMax.getText();
                        angmax = Integer.parseInt(txt.trim());
                        afmimage.killCorrelationImage();
                    }

                    public void focusGained(FocusEvent fe) {
                    }
                });
        angPmax.add(jtfAngMax);
        ParametersP.add(angPmax);


        // PCP= Pecentage Correlation Parameters
        JPanel PCP = new JPanel();
        PCP.setLayout(new FlowLayout());
        PCP.setBackground(Color.LIGHT_GRAY);
        PCP.add(new JLabel(" Percentage Correlation Parameters"));
        ParametersP.add(PCP);


        // pct correlation range
        JPanel rfT = new JPanel();
        rfT.setLayout(new FlowLayout());
        rfT.setBackground(Color.LIGHT_GRAY);
        jtfCorrPct = new JTextField();
        jtfCorrPct.setHorizontalAlignment(JTextField.LEFT);
        jtfCorrPct.setText("  " + pctcorr1);
        jtfCorrPct.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        String txt = jtfCorrPct.getText();
                        pctcorr1 = Double.parseDouble(txt);
                    }

                    public void focusGained(FocusEvent fe) {
                    }
                });
        rfT.add(jtfCorrPct);
        //    rfT.add(new JLabel(bundle.getString("$PCTCORRNEW"), SwingConstants.CENTER));
        rfT.add(new JLabel("<=Correlation[%]<=", SwingConstants.CENTER));
        jtfCorrPct2 = new JTextField();
        jtfCorrPct2.setHorizontalAlignment(JTextField.RIGHT);
        jtfCorrPct2.setText("  " + pctcorr2);
        jtfCorrPct2.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        String txt = jtfCorrPct2.getText();
                        pctcorr2 = Double.parseDouble(txt);
                    }

                    public void focusGained(FocusEvent fe) {
                    }
                });
        rfT.add(jtfCorrPct2);
        ParametersP.add(rfT);

        // pct internal sym range
        JPanel isT = new JPanel();
        isT.setLayout(new FlowLayout());
        isT.setBackground(Color.LIGHT_GRAY);
        istCorrPct = new JTextField();
        istCorrPct.setHorizontalAlignment(JTextField.LEFT);
        istCorrPct.setText("  " + istcorr1);
        istCorrPct.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        String txt = istCorrPct.getText();
                        istcorr1 = Double.parseDouble(txt);
                    }

                    public void focusGained(FocusEvent fe) {
                    }
                });
        istCorrPct2 = new JTextField();
        istCorrPct2.setHorizontalAlignment(JTextField.RIGHT);
        istCorrPct2.setText("  " + istcorr2);
        istCorrPct2.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        String txt = istCorrPct2.getText();
                        istcorr2 = Double.parseDouble(txt);
                    }

                    public void focusGained(FocusEvent fe) {
                    }
                });
        isT.add(istCorrPct);
        //   isT.add(new JLabel(bundle.getString("$ISTCORR2"), SwingConstants.CENTER));
        isT.add(new JLabel("<=Internal Symmetry[%]<=", SwingConstants.CENTER));
        isT.add(istCorrPct2);
        ParametersP.add(isT);

        // radius peaks value
        JPanel radPeaksP = new JPanel();
        radPeaksP.setLayout(new FlowLayout());
        radPeaksP.setBackground(Color.LIGHT_GRAY);
        //radPeaksP.add(new JLabel(bundle.getString("$RADIUSPEAK1")));
        radPeaksP.add(new JLabel("Distance"));
        jtfRadPeaks = new JTextField();
        jtfRadPeaks.setText("      " + radiuspeaksInAng);
        jtfRadPeaks.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        String txt = jtfRadPeaks.getText();
                        radiuspeaksInAng = Double.parseDouble(txt.trim());
                        radiuspeaks = (int) Math.round(radiuspeaksInAng / convert);
                    }

                    public void focusGained(FocusEvent fe) {
                    }
                });
        radPeaksP.add(jtfRadPeaks);
        ParametersP.add(radPeaksP);


        return ParametersP;
    }

    public JPanel createImageAnalysisPanel() {
        JPanel ImageAnalysisP = new JPanel();
        ImageAnalysisP.setBackground(Color.LIGHT_GRAY);
        ImageAnalysisP.setLayout(new BoxLayout(ImageAnalysisP, BoxLayout.Y_AXIS));

        // intro text
        //    JPanel head = new JPanel();
        //  head.setLayout(new FlowLayout());
        //     head.setBackground(Color.LIGHT_GRAY);
        //    head.add(new JLabel("Image Panel"));
        //     ImageAnalysisP.add(head);

        //IAPU= Image Analysis Parameters usage
        JPanel IAPU = new JPanel();
        IAPU.setLayout(new FlowLayout());
        IAPU.setBackground(Color.LIGHT_GRAY);
        IAPU.add(new JLabel(" NOTE: To enter the values of Image Size, Scan Size and Z-Scale >>> Go to the 'Parameters' Panel"));
        ImageAnalysisP.add(IAPU);

        // open raw image button
        JPanel panelRawImage = new JPanel();
        panelRawImage.setLayout(new FlowLayout());
        panelRawImage.setBackground(Color.LIGHT_GRAY);
        // JButton rawImageB = new JButton(bundle.getString("$OPENRAW"));
        JButton rawImageB = new JButton("Open");
        rawImageB.addActionListener(ae -> afmimage.openRaw());
        panelRawImage.add(rawImageB);
        ImageAnalysisP.add(panelRawImage);

        // get raw from opened image
        JPanel panelSetImage = new JPanel();
        panelSetImage.setLayout(new FlowLayout());
        panelSetImage.setBackground(Color.LIGHT_GRAY);
        // JButton setImageB = new JButton(bundle.getString("$SETRAW"));
        JButton setImageB = new JButton("Set Raw Image");
        setImageB.addActionListener(ae -> afmimage.setRaw());
        panelSetImage.add(setImageB);
        ImageAnalysisP.add(panelSetImage);

        // open ref image button
        JPanel panelRefImage = new JPanel();
        panelRefImage.setLayout(new FlowLayout());
        panelRefImage.setBackground(Color.LIGHT_GRAY);
        //    JButton refImageB = new JButton(bundle.getString("$OPENREF"));
        JButton refImageB = new JButton("Open Reference");
        refImageB.addActionListener(
                ae -> {
                    afmimage.openRef();
                    radius = afmimage.getRef().getWidth() / 2;
                    radiusInAng = radius * convert;
                    radiuspeaks = afmimage.getRef().getWidth() / 2;
                    radiuspeaksInAng = radiuspeaks * convert;
                    jtfRAD.setText(" " + radiusInAng);
                    jtfRadPeaks.setText(" " + radiuspeaksInAng);
                });
        panelRefImage.add(refImageB);
        ImageAnalysisP.add(panelRefImage);

        // crop ref image button
        JPanel panelCropRefImage = new JPanel();
        panelCropRefImage.setLayout(new FlowLayout());
        panelCropRefImage.setBackground(Color.LIGHT_GRAY);
        //    JButton cropRefImageB = new JButton(bundle.getString("$CROPREF"));
        JButton cropRefImageB = new JButton("Crop Reference");
        cropRefImageB.addActionListener(
                ae -> {
                    // format number
                    NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
                    nf.setMinimumFractionDigits(1);
                    nf.setMaximumFractionDigits(1);
                    afmimage.cropRef();
                    radius = afmimage.getRef().getWidth() / 2;
                    radiusInAng = radius * convert;
                    radiuspeaks = afmimage.getRef().getWidth() / 2;
                    radiuspeaksInAng = radiuspeaks * convert;
                    jtfRAD.setText(" " + nf.format(radiusInAng));
                    jtfRadPeaks.setText(" " + nf.format(radiuspeaksInAng));
                });
        panelCropRefImage.add(cropRefImageB);
        ImageAnalysisP.add(panelCropRefImage);

        // best symmetry button
        JPanel panelBestSymButton = new JPanel();
        panelBestSymButton.setLayout(new FlowLayout());
        panelBestSymButton.setBackground(Color.LIGHT_GRAY);
        JButton BestSymImageB = new JButton("Best Symmetry");
        BestSymImageB.addActionListener(
                ae -> afmimage.bestSymmetry(afmimage.getRef(), radius));
        panelBestSymButton.add(BestSymImageB);
        ImageAnalysisP.add(panelBestSymButton);

        // nfold value
        JPanel nfP = new JPanel();
        nfP.setLayout(new FlowLayout());
        nfP.setBackground(Color.LIGHT_GRAY);
        //  nfP.add(new JLabel(bundle.getString("$NFOLDVALUE")));
        nfP.add(new JLabel("N-Fold Value"));
        jtfNF = new JTextField();
        jtfNF.setText("  " + nfold);
        jtfNF.addFocusListener(
                new FocusListener() {
                    public void focusLost(FocusEvent fe) {
                        String txt = jtfNF.getText();
                        nfold = Integer.parseInt(txt.trim());
                    }
                    public void focusGained(FocusEvent fe) {
                    }
                });
        nfP.add(jtfNF);
        ImageAnalysisP.add(nfP);

        // nfold button
        JPanel panelNFoldButton = new JPanel();
        panelNFoldButton.setLayout(new FlowLayout());
        panelNFoldButton.setBackground(Color.LIGHT_GRAY);
        //   JButton nFoldRefImageB = new JButton(bundle.getString("$NFOLDACTION"));
        JButton nFoldRefImageB = new JButton("N-Fold Action");
        nFoldRefImageB.addActionListener(
                ae -> {
                    GenericDialog gd = new GenericDialog("Do n-fold symmetry");
                    gd.addMessage("Please check if n-fold value is correct.");
                    gd.showDialog();

                    ImageProcessor ref = afmimage.getRef();
                    ImageProcessor tmp = afmimage.nFold(ref, nfold, true);
                    afmimage.setRef(tmp);
                    ImagePlus impref = afmimage.getImpref();
                    //       impref.setProcessor(bundle.getString("$REF"), afmimage.getRef());
                    impref.setProcessor(("REFERENCE"), afmimage.getRef());
                    impref.updateAndDraw();
                    afmimage.killCorrelationImage();
                });
        panelNFoldButton.add(nFoldRefImageB);
        ImageAnalysisP.add(panelNFoldButton);

        // 360 fold button
        JPanel panel360FoldButton = new JPanel();
        panel360FoldButton.setLayout(new FlowLayout());
        panel360FoldButton.setBackground(Color.LIGHT_GRAY);
        //    JButton nFold360RefImageB = new JButton(bundle.getString("$360FOLD"));
        JButton nFold360RefImageB = new JButton("360-Fold");
        nFold360RefImageB.addActionListener(
                ae -> {
                    ImageProcessor ref = afmimage.getRef();
                    ImageProcessor tmp = afmimage.nFold(ref, 360, false);
                    afmimage.setRef(tmp);
                    ImagePlus impref = afmimage.getImpref();
                    //   impref.setProcessor(bundle.getString("$REF"), afmimage.getRef());
                    impref.setProcessor(("REFERENCE"), afmimage.getRef());
                    impref.updateAndDraw();
                    afmimage.killCorrelationImage();
                });
        panel360FoldButton.add(nFold360RefImageB);
        ImageAnalysisP.add(panel360FoldButton);

        //* image size in pixel
        //   JPanel panelSizePixel = new JPanel();
        //    panelSizePixel.setLayout(new FlowLayout());
        //    panelSizePixel.setBackground(Color.LIGHT_GRAY);
        //* panelSizePixel.add(new JLabel(bundle.getString("$PIXELSIZE")));
        //    panelSizePixel.add(new JLabel("Image Size (Pixel)"));
        //  final JTextField pixel = new JTextField("   " + imagePixel);
        //    panelSizePixel.add(pixel);
        //     pixel.addFocusListener(new FocusListener() {

        //      public void focusLost(FocusEvent fe) {
        //         String txt = pixel.getText();
        //         imagePixel = Integer.parseInt(txt.trim());
        //        convert = scanSize / (double) imagePixel;
        //           radiusInAng = radius * convert;
        //          radiuspeaksInAng = radiuspeaks * convert;
        //          jtfRAD.setText(" " + radiusInAng);
        //             jtfRadPeaks.setText(" " + radiuspeaksInAng);
        //        }

        //        public void focusGained(FocusEvent fe) {
        //         }
        //   });
        //    ImageAnalysisP.add(panelSizePixel);

        //* scan size in Angstrom
        //    JPanel panelScanSize = new JPanel();
        //    panelScanSize.setLayout(new FlowLayout());
        //      panelScanSize.setBackground(Color.LIGHT_GRAY);
        //*    panelScanSize.add(new JLabel(bundle.getString("$SCANSIZE")));
        //   panelScanSize.add(new JLabel("Scan Size (Angstrom)"));
        //     final JTextField scan = new JTextField("   " + scanSize);
        //    panelScanSize.add(scan);
        //   scan.addFocusListener(new FocusListener() {

        //       public void focusLost(FocusEvent fe) {
        //            String txt = scan.getText();
        //          scanSize = Integer.parseInt(txt.trim());
        //            convert = scanSize / (double) imagePixel;
        //         radiusInAng = radius * convert;
        //             radiuspeaksInAng = radiuspeaks * convert;
        //             jtfRAD.setText(" " + radiusInAng);
        //           jtfRadPeaks.setText(" " + radiuspeaksInAng);
        //      }

        //      public void focusGained(FocusEvent fe) {
        //       }
        //     });
        //     ImageAnalysisP.add(panelScanSize);

        //* Z-Scale in Angstrom
        //    JPanel panelZScale = new JPanel();
        //      panelZScale.setLayout(new FlowLayout());
        //      panelZScale.setBackground(Color.LIGHT_GRAY);
        // *    panelZScale.add(new JLabel(bundle.getString("$ZSCALE")));
        //     panelZScale.add(new JLabel("Z-Scale (Angstrom)"));
        //     final JTextField scale = new JTextField("   " + zScale);
        //    panelZScale.add(scale);
        //  scale.addFocusListener(new FocusListener() {

        //        public void focusLost(FocusEvent fe) {
        //           String txt = scale.getText();
        //           zScale = Double.parseDouble(txt.trim());
        //        }

        //      public void focusGained(FocusEvent fe) {
        //         }
        //      });
        //     ImageAnalysisP.add(panelZScale);

        return ImageAnalysisP;
    }

    /**
     * Description of the Method
     *
     * @return Description of the Return Value
     */
    public JPanel createImageCorrelationPanel() {
        // format number
        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);

        JPanel ImageCorrelationP = new JPanel();
        ImageCorrelationP.setBackground(Color.LIGHT_GRAY);
        ImageCorrelationP.setLayout(new BoxLayout(ImageCorrelationP, BoxLayout.Y_AXIS));

        // intro text
        JPanel head = new JPanel();
        head.setLayout(new FlowLayout());
        head.setBackground(Color.LIGHT_GRAY);
        head.add(new JLabel("Correlation Panel" + " "));
        ImageCorrelationP.add(head);

        //ICPU= Image Correlation Parameters usage
        JPanel ICPU = new JPanel();
        ICPU.setLayout(new FlowLayout());
        ICPU.setBackground(Color.LIGHT_GRAY);
        ICPU.add(new JLabel(" NOTE: 1)To enter the values of Radius , Angle Increment and Angle Range,and"
                + " 2) To enter values of  Correlation % and Internal Symmetry % >>> Go to the 'Parameters' Panel"));
        ImageCorrelationP.add(ICPU);


        //* radius correlation value
        //     JPanel radP = new JPanel();
        //      radP.setLayout(new FlowLayout());
        //      radP.setBackground(Color.LIGHT_GRAY);
        //*  radP.add(new JLabel(bundle.getString("$RADIUSCORR1")));
        //    radP.add(new JLabel("Radius (Angstrom)"));
        //      NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        //      format.setMaximumFractionDigits(3);
        //    jtfRAD = new JTextField();
        //      jtfRAD.setText("       " + radiusInAng);
        //     jtfRAD.addFocusListener(
        //          new FocusListener() {

        //           public void focusLost(FocusEvent fe) {
        //                String txt = jtfRAD.getText();
        //        radiusInAng = Double.parseDouble(txt.trim());
        //             radius = (int) Math.round(radiusInAng / convert);
        //               afmimage.killCorrelationImage();
        //            }

        //               public void focusGained(FocusEvent fe) {
        //               }
        //            });
        //    radP.add(jtfRAD);
        //      ImageCorrelationP.add(radP);

        //* angle inc correlation value
        //     JPanel angPinc = new JPanel();
        //     angPinc.setLayout(new FlowLayout());
        //       angPinc.setBackground(Color.LIGHT_GRAY);
        //*    angPinc.add(new JLabel(bundle.getString("$ANGLEINC")));
        //      angPinc.add(new JLabel("Angle Increment"));
        //     jtfAngInc = new JTextField();
        //     jtfAngInc.setText("      " + anginc);
        //     jtfAngInc.addFocusListener(
        //          new FocusListener() {

        //               public void focusLost(FocusEvent fe) {
        //                  String txt = jtfAngInc.getText();
        //                   anginc = Integer.parseInt(txt.trim());
        //                   afmimage.killCorrelationImage();
        //                 }

        //              public void focusGained(FocusEvent fe) {
        //               }
        //            });
        //     angPinc.add(jtfAngInc);
        //      ImageCorrelationP.add(angPinc);

        //* angle max correlation value
        //    JPanel angPmax = new JPanel();
        //      angPmax.setLayout(new FlowLayout());
        //      angPmax.setBackground(Color.LIGHT_GRAY);
        //* angPmax.add(new JLabel(bundle.getString("$ANGLEMAX")));
        //      angPmax.add(new JLabel ("Angle Range"));
        //     jtfAngMax = new JTextField();
        //     jtfAngMax.setText("     " + angmax);
        //     jtfAngMax.addFocusListener(
        //            new FocusListener() {

        //              public void focusLost(FocusEvent fe) {
        //                   String txt = jtfAngMax.getText();
        //                  angmax = Integer.parseInt(txt.trim());
        //                   afmimage.killCorrelationImage();
        //               }

        //              public void focusGained(FocusEvent fe) {
        //              }
        //            });
        //    angPmax.add(jtfAngMax);
        //    ImageCorrelationP.add(angPmax);


        // Values intro
        //     JPanel head = new JPanel();
        //     head.setLayout(new FlowLayout());
        //      head.setBackground(Color.LIGHT_GRAY);
        //    head.add(new JLabel("To enter the values of Radius , Angle Increment and Angle Range please go to the Parameters folder"));
        ImageCorrelationP.add(head);


        // correlation button
        JPanel panelCorrButton = new JPanel();
        panelCorrButton.setLayout(new FlowLayout());
        panelCorrButton.setBackground(Color.LIGHT_GRAY);
        // JButton correlationB = new JButton(bundle.getString("$CORRELATION"));
        JButton correlationB = new JButton("Correlation");
        correlationB.addActionListener(
                ae -> {
                    if (afmimage.getCorrval() == null) {
                        afmimage.autoCorrCalc(afmimage.getRaw(), afmimage.getRef(), radius, anginc, angmax);
                    }
                    // correlation image
                    //afmimage.setImpcorrval(new ImagePlus(bundle.getString("$CORRIMG"), afmimage.getCorrval()));
                    afmimage.setImpcorrval(new ImagePlus("Correlation Image", afmimage.getCorrval()));
                    ImagePlus impcorr = afmimage.getImpcorrval();
                    impcorr.updateAndRepaintWindow();
                    impcorr.show();
                    // angle image
                    //    afmimage.setImpcorrang(new ImagePlus(bundle.getString("$ANGLEIMG"), afmimage.getCorrang()));
                    afmimage.setImpcorrang(new ImagePlus("Angle Correlation", afmimage.getCorrang()));
                    ImagePlus impang = afmimage.getImpcorrang();
                    impang.updateAndRepaintWindow();
                    impang.show();
                });
        panelCorrButton.add(correlationB);
        ImageCorrelationP.add(panelCorrButton);

        // open correlation results
        JPanel panelCorrResults = new JPanel();
        panelCorrResults.setLayout(new FlowLayout());
        panelCorrResults.setBackground(Color.LIGHT_GRAY);
        JButton rawImageB = new JButton("Open Correlation Results");
        rawImageB.addActionListener(
                ae -> {
                    afmimage.openImpcorr();
                    afmimage.openImpang();
                });
        panelCorrResults.add(rawImageB);
        ImageCorrelationP.add(panelCorrResults);

        //Plot Correlation button: Number of particles vs correlation treshold
        JPanel plotButtonCorr = new JPanel();
        plotButtonCorr.setLayout(new FlowLayout());
        plotButtonCorr.setBackground(Color.LIGHT_GRAY);
        JButton plotBCorr = new JButton("Plot Correlation");
        plotBCorr.addActionListener(
                ae -> {
                    ImageProcessor raw = afmimage.getRaw();
                    ImageProcessor ref = afmimage.getRef();
                    ImageProcessor corrval = afmimage.getCorrval();
                    ImageProcessor corrang = afmimage.getCorrang();
                    afmanalysis.plotCorrelation(raw, ref, corrval, corrang, radiuspeaks, nfold, istcorr1, istcorr2, radius);

                });
        plotButtonCorr.add(plotBCorr);
        ImageCorrelationP.add(plotButtonCorr);


        //Plot Symmetry button: Number of particles vs symmetry treshold
        JPanel plotButtonSym = new JPanel();
        plotButtonSym.setLayout(new FlowLayout());
        plotButtonSym.setBackground(Color.LIGHT_GRAY);
        JButton plotBSym = new JButton("Plot Internal Symmetry");
        plotBSym.addActionListener(
                ae -> {
                    ImageProcessor raw = afmimage.getRaw();
                    ImageProcessor ref = afmimage.getRef();
                    ImageProcessor corrval = afmimage.getCorrval();
                    ImageProcessor corrang = afmimage.getCorrang();
                    afmanalysis.plotSymmetry(raw, ref, corrval, corrang, radiuspeaks, nfold, pctcorr1, pctcorr2, radius);
                });
        plotButtonSym.add(plotBSym);
        ImageCorrelationP.add(plotButtonSym);

        //* pct correlation range
        //  JPanel rfT = new JPanel();
        //    rfT.setLayout(new FlowLayout());
        //   rfT.setBackground(Color.LIGHT_GRAY);
        //      jtfCorrPct = new JTextField();
        //   jtfCorrPct.setHorizontalAlignment(JTextField.LEFT);
        //    jtfCorrPct.setText("  " + pctcorr1);
        //     jtfCorrPct.addFocusListener(
        //           new FocusListener() {

        //            public void focusLost(FocusEvent fe) {
        //                String txt = jtfCorrPct.getText();
        //                  pctcorr1 = Double.parseDouble(txt);
        //                }

        //                public void focusGained(FocusEvent fe) {
        //                  }
        //           });
        //       rfT.add(jtfCorrPct);
        //*    rfT.add(new JLabel(bundle.getString("$PCTCORRNEW"), SwingConstants.CENTER));
        //     rfT.add(new JLabel("<=Correlation[%]<=", SwingConstants.CENTER));
        //      jtfCorrPct2 = new JTextField();
        //     jtfCorrPct2.setHorizontalAlignment(JTextField.RIGHT);
        //     jtfCorrPct2.setText("  " + pctcorr2);
        //             new FocusListener() {

        //               public void focusLost(FocusEvent fe) {
        //                    String txt = jtfCorrPct2.getText();
        //                    pctcorr2 = Double.parseDouble(txt);
        //                }

        //             public void focusGained(FocusEvent fe) {
        //                 }
        //            });
        //     rfT.add(jtfCorrPct2);
        //    ImageCorrelationP.add(rfT);

        //* pct internal sym range
        //     JPanel isT = new JPanel();
        //     isT.setLayout(new FlowLayout());
        //     isT.setBackground(Color.LIGHT_GRAY);
        //     istCorrPct = new JTextField();
        //     istCorrPct.setHorizontalAlignment(JTextField.LEFT);
        //     istCorrPct.setText("  " + istcorr1);
        //     istCorrPct.addFocusListener(
        //            new FocusListener() {

        //                public void focusLost(FocusEvent fe) {
        //                   String txt = istCorrPct.getText();
        //                   istcorr1 = Double.parseDouble(txt);
        //                }

        //               public void focusGained(FocusEvent fe) {
        //                 }
        //             });
        //     istCorrPct2 = new JTextField();
        //      istCorrPct2.setHorizontalAlignment(JTextField.RIGHT);
        //      istCorrPct2.setText("  " + istcorr2);
        //      istCorrPct2.addFocusListener(
        //             new FocusListener() {

        //               public void focusLost(FocusEvent fe) {
        //                  String txt = istCorrPct2.getText();
        //                   istcorr2 = Double.parseDouble(txt);
        //              }

        //               public void focusGained(FocusEvent fe) {
        //                 }
        //            });
        //      isT.add(istCorrPct);
        //*   isT.add(new JLabel(bundle.getString("$ISTCORR2"), SwingConstants.CENTER));
        //     isT.add(new JLabel("<=Internal Symmetry[%]<=", SwingConstants.CENTER));
        //     isT.add(istCorrPct2);
        //      ImageCorrelationP.add(isT);

        //* radius peaks value
        //      JPanel radPeaksP = new JPanel();
        //      radPeaksP.setLayout(new FlowLayout());
        //      radPeaksP.setBackground(Color.LIGHT_GRAY);
        //*radPeaksP.add(new JLabel(bundle.getString("$RADIUSPEAK1")));
        //     radPeaksP.add(new JLabel("Distance"));
        //     jtfRadPeaks = new JTextField();
        //      jtfRadPeaks.setText("      " + nf.format(radiuspeaksInAng));
//        jtfRadPeaks.addFocusListener(
        //           new FocusListener() {

        //                public void focusLost(FocusEvent fe) {
        //                    String txt = jtfRadPeaks.getText();
        //                   radiuspeaksInAng = Double.parseDouble(txt.trim());
        //                   radiuspeaks = (int) Math.round(radiuspeaksInAng / convert);
        //                 }

        //                public void focusGained(FocusEvent fe) {
        //               }
        //           });
        //    radPeaksP.add(jtfRadPeaks);
        //    ImageCorrelationP.add(radPeaksP);

        // peaks button
        JPanel panelPeaksButton = new JPanel();
        panelPeaksButton.setLayout(new FlowLayout());
        panelPeaksButton.setBackground(Color.LIGHT_GRAY);
        //JButton peaksButton = new JButton(bundle.getString("$FINDPEAKS"));
        JButton peaksButton = new JButton("Find Peaks");
        peaksButton.addActionListener(
                ae -> {
                    ImageProcessor raw = afmimage.getRaw();
                    ImageProcessor ref = afmimage.getRef();
                    ImageProcessor corrval = afmimage.getCorrval();
                    ImageProcessor corrang = afmimage.getCorrang();
                    ImageProcessor peaksave = afmanalysis.findPeaks(raw, ref, corrval, corrang, pctcorr1, pctcorr2, radiuspeaks, nfold, istcorr1, istcorr2, radius);
                    afmimage.setPeak(peaksave);
                    //     ImagePlus impeaks = new ImagePlus(bundle.getString("$PEAKSIMG") + pctcorr1 + " <= Corr [%] <= " + pctcorr2 + "; " + istcorr1 + " <= Int Sym [%] <= " + istcorr2, peaksave);
                    ImagePlus impeaks = new ImagePlus("PEAKS_IMAGE" + pctcorr1 + " <= Corr [%] <= " + pctcorr2 + "; " + istcorr1 + " <= Int Sym [%] <= " + istcorr2, peaksave);
                    afmimage.setImpPeak(impeaks);
                    impeaks.updateAndDraw();
                    impeaks.show();
                    //afmanalysis.createPointPicker(impeaks, afmanalysis.getPeaksA(), 0, true);
                    afmanalysis.createRoiManager(impeaks, afmanalysis.getPeaksA(), "ffff00", radiuspeaks, true);
                });
        panelPeaksButton.add(peaksButton);
        ImageCorrelationP.add(panelPeaksButton);


        // Update Peaks List


        //   JPanel UpdatePeaksListButton = new JPanel();
        //    UpdatePeaksListButton.setLayout(new FlowLayout());
        //    UpdatePeaksListButton.setBackground(Color.LIGHT_GRAY);
        //    JButton UpdatePeaksListB = new JButton("Update Peaks List");
        //    UpdatePeaksListB.addActionListener(
        //           new ActionListener() {

        //               public void actionPerformed(ActionEvent ae) {
        //           // open raw data video


        //               }
        ///           });
        //   UpdatePeaksListButton.add(UpdatePeaksListButton);
        //    ImageCorrelationP.add(UpdatePeaksListButton);
//

        // save peaks
        JPanel panelListButton = new JPanel();
        panelListButton.setLayout(new FlowLayout());
        panelListButton.setBackground(Color.LIGHT_GRAY);
        // JButton listB = new JButton(bundle.getString("$LIST"));
        JButton listB = new JButton("Save Peaks");
        listB.addActionListener(
                ae -> {
                    int ok = (int) (IJ.getNumber("update from roi manager 1", 1));
                    if (ok == 1) {
                        ArrayList al = afmanalysis.getPeaksFromRoiManager(null, null);
                        afmanalysis.setPeaksA(al);
                    }
                    ArrayList v = afmanalysis.getPeaksA();
                    afmanalysis.savePeaks(v, afmimage.getRaw(), afmimage.getRef(), angmax, anginc);
                });
        panelListButton.add(listB);
        ImageCorrelationP.add(panelListButton);

        return ImageCorrelationP;
    }

    /**
     * Panneau pour la correlation par paire
     *
     * @return Panel
     */
    public JPanel createImagePairCorrelationPanel() {
        JPanel ImagePairCorrelationP = new JPanel();
        Color col = Color.LIGHT_GRAY;
        ImagePairCorrelationP.setBackground(col);
        ImagePairCorrelationP.setLayout(new BoxLayout(ImagePairCorrelationP, BoxLayout.Y_AXIS));

        // intro text
        JPanel head = new JPanel();
        head.setLayout(new FlowLayout());
        head.setBackground(col);
        head.add(new JLabel("Pair Correlation Panel"));
        ImagePairCorrelationP.add(head);

        // save peaks
        // JPanel panelListButton = new JPanel();
        //  panelListButton.setLayout(new FlowLayout());
        //    panelListButton.setBackground(col);
        // JButton listB = new JButton(bundle.getString("$LIST"));
        //       JButton listB = new JButton("$LIST");
        //    listB.addActionListener(
        //         new ActionListener() {

        //          public void actionPerformed(ActionEvent ae) {
        //             ArrayList v = afmanalysis.getPeaksA();
        //            afmanalysis.savePeaks(v, afmimage.getRaw(), afmimage.getRef(), angmax, anginc);
        //         }
        //           });
        //     panelListButton.add(listB);
        //   ImagePairCorrelationP.add(panelListButton);

        // open peaks for particles A
        JPanel panelOpenPeaksButtonA = new JPanel();
        panelOpenPeaksButtonA.setLayout(new FlowLayout());
        panelOpenPeaksButtonA.setBackground(col);
        //   JButton openPeaksA = new JButton(bundle.getString("$OPENPEAKS") + " A");
        JButton openPeaksA = new JButton(("Open Peaks") + " A");
        openPeaksA.addActionListener(
                ae -> {
                    IJ.log("Open list peaks A");
                    ArrayList peaks = afmanalysis.openListePeaks();
                    afmanalysis.setPeaksA(peaks);
                    if (afmimage.getImpPeak() != null) {
                        //afmanalysis.createPointPicker(afmimage.getImpPeak(), peaks, 0, false);
                        afmanalysis.createRoiManager(afmimage.getImpPeak(), peaks, "ff0000", radiuspeaks, true);
                    } else {
                        ImageProcessor rawtmp = afmimage.getRaw();
                        ImageProcessor peaksave = rawtmp.createProcessor(rawtmp.getWidth(), rawtmp.getHeight());
                        peaksave.insert(rawtmp, 0, 0);
                        afmimage.setPeak(peaksave);
                        //     ImagePlus impeaks = new ImagePlus(bundle.getString("$PEAKSIMG"), peaksave);
                        ImagePlus impeaks = new ImagePlus(("PEAKS_IMAGE"), peaksave);
                        afmimage.setImpPeak(impeaks);
                        impeaks.updateAndDraw();
                        impeaks.show();
                        //afmanalysis.createPointPicker(impeaks, peaks, 0, true);
                        afmanalysis.createRoiManager(impeaks, peaks, "ff1010", radiuspeaks, true);
                    }
                    //IJ.log("Peaks A=" + peaks);
                });
        panelOpenPeaksButtonA.add(openPeaksA);
        ImagePairCorrelationP.add(panelOpenPeaksButtonA);

        // open peaks for particles B
        JPanel panelOpenPeaksButtonB = new JPanel();
        panelOpenPeaksButtonB.setLayout(new FlowLayout());
        panelOpenPeaksButtonB.setBackground(col);
        // JButton openPeaksB = new JButton(bundle.getString("$OPENPEAKS") + " B");
        JButton openPeaksB = new JButton(("Open Peaks") + " B");
        openPeaksB.addActionListener(
                ae -> {
                    IJ.log("Open list peaks B");
                    ArrayList peaks = afmanalysis.openListePeaks();
                    afmanalysis.setPeaksB(peaks);
                    //afmanalysis.createPointPicker(afmimage.getImpPeak(), peaks, 12, false);
                    afmanalysis.createRoiManager(afmimage.getImpPeak(), peaks, "1010ff", radiuspeaks, false);
                    IJ.log("Peaks B=" + peaks);
                });
        panelOpenPeaksButtonB.add(openPeaksB);
        ImagePairCorrelationP.add(panelOpenPeaksButtonB);

        // radius correlation value
        JPanel radbinP = new JPanel();
        radbinP.setLayout(new FlowLayout());
        radbinP.setBackground(col);
        //    radbinP.add(new JLabel(bundle.getString("$RADBIN")));
        radbinP.add(new JLabel("Radius Bin"));
        jtfRADBIN = new JTextField();
        jtfRADBIN.setText("   " + radbinpcf);
        jtfRADBIN.addFocusListener(
                new FocusListener() {
                    public void focusLost(FocusEvent fe) {
                        String txt = jtfRADBIN.getText();
                        radbinpcf = Integer.parseInt(txt.trim());
                    }
                    public void focusGained(FocusEvent fe) {
                    }
                });
        radbinP.add(jtfRADBIN);
        ImagePairCorrelationP.add(radbinP);

        // pair correlation button AA
        JPanel panelCorrButtonAA = new JPanel();
        panelCorrButtonAA.setLayout(new FlowLayout());
        panelCorrButtonAA.setBackground(col);
        //        JButton correlationAAB = new JButton(bundle.getString("$DOPCF") + " A");
        JButton correlationAAB = new JButton(("Perform PCF") + " A");
        correlationAAB.addActionListener(
                ae -> {
                    GenericDialog gd = new GenericDialog("Perform Pair Correlation A");
                    gd.addMessage("Please check if image size and scan size values are correct.");
                    gd.showDialog();

                    ArrayList peaksA = afmanalysis.getPeaksA();
                    ImageProcessor raw = afmimage.getRaw();
                    if (peaksA != null) {
                        afmanalysis.computePCF(raw, "A-A", peaksA, peaksA, radbinpcf, convert);
                    }
                });
        panelCorrButtonAA.add(correlationAAB);
        ImagePairCorrelationP.add(panelCorrButtonAA);

        // pair correlation button BB
        JPanel panelCorrButtonBB = new JPanel();
        panelCorrButtonBB.setLayout(new FlowLayout());
        panelCorrButtonBB.setBackground(col);
        //       JButton correlationBBB = new JButton(bundle.getString("$DOPCF") + " B");
        JButton correlationBBB = new JButton(("Perform PCF") + " B");
        correlationBBB.addActionListener(
                ae -> {
                    GenericDialog gd = new GenericDialog("Perform Pair Correlation B");
                    gd.addMessage("Please check if image size and scan size values are correct.");
                    gd.showDialog();

                    ArrayList peaksB = afmanalysis.getPeaksB();
                    ImageProcessor raw = afmimage.getRaw();
                    if (peaksB != null) {
                        afmanalysis.computePCF(raw, "B-B", peaksB, peaksB, radbinpcf, convert);
                    }
                });
        panelCorrButtonBB.add(correlationBBB);
        ImagePairCorrelationP.add(panelCorrButtonBB);

        // pair correlation button AB
        JPanel panelCorrButtonAB = new JPanel();
        panelCorrButtonAB.setLayout(new FlowLayout());
        panelCorrButtonAB.setBackground(col);
        //  JButton correlationABB = new JButton(bundle.getString("$DOPCF") + " AB");
        JButton correlationABB = new JButton(("Perform PCF") + " AB");
        correlationABB.addActionListener(
                ae -> {
                    GenericDialog gd = new GenericDialog("Perform Pair Correlation AB");
                    gd.addMessage("Please check if image size and scan size values are correct.");
                    gd.showDialog();

                    ArrayList peaksA = afmanalysis.getPeaksA();
                    ArrayList peaksB = afmanalysis.getPeaksB();
                    ImageProcessor raw = afmimage.getRaw();
                    if (peaksB != null) {
                        afmanalysis.computePCF(raw, "A-B", peaksA, peaksB, radbinpcf, convert);
                    }
                });
        panelCorrButtonAB.add(correlationABB);
        ImagePairCorrelationP.add(panelCorrButtonAB);

        return ImagePairCorrelationP;
    }

    /**
     * Description of the Method
     */
    private void doInterface() {

        JPanel ImageAnalysisPanel = createImageAnalysisPanel();
        // JSplitPane sp2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, ParametersPanel,  ImageAnalysisPanel);

        JPanel ImageCorrelationPanel = createImageCorrelationPanel();
        JSplitPane sp2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, ImageAnalysisPanel, ImageCorrelationPanel);

        JPanel ImagePairCorrelationPanel = createImagePairCorrelationPanel();
        JSplitPane sp3 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sp2, ImagePairCorrelationPanel);

        JPanel ImageAveragingPanel = createImageAveragingPanel();

        JSplitPane sp4 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sp3, ImageAveragingPanel);

        JPanel ParametersPanel = createParametersPanel();
        JSplitPane sp5 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sp4, ParametersPanel);

        add(sp5);
    }

    /**
     * Draw the interface
     */
    private void drawInterface() {
        setSize(1024, 768);
        setLayout(new GridLayout());
        setResizable(true);
        // setVisible(true);
        //new AboutWindowAFM();
    }

    /**
     * Close the plugin
     */
    public void close() {
        super.close();
    }
}
