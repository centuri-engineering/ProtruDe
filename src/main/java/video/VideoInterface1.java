package video;

import afm.AFMAnalysis;
import afm.AFMImage;
import afm.AFMInterface;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.*;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.io.SaveDialog;
import ij.measure.CurveFitter;
import ij.plugin.frame.PlugInFrame;
import ij.process.*;
import ij.text.TextWindow;
import mcib3d.utils.ArrayUtil;
import tracking.Tracking;
import tracking.Visu_Tracking_Basedon_FCordelieresPlugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Video interface
 *
 * @author thomas dot boudier at snv dot jussieu dot fr @created 26 decembre
 *         2009
 */
public class VideoInterface1 extends PlugInFrame implements MouseListener {

    ResourceBundle bundle;
    // data
    ImageProcessor ContrastAdjustment;
    ImagePlus rawPlus;
    ImageStack rawVideo;
    ImagePlus FineAlignPlus;
    ImageStack FineAlignVideo;
    // contrast video
    ImageStack contrastVideo;
    ImagePlus contrastPlus;
    ImageStack ContrastAdjustmentVideo;
    ImagePlus ContrastAdjustmentPlus;
    ImageStack AdjustedAndFilteredVideo;
    ImagePlus AdjustedAndFilteredPlus;
    // rough align
    ImagePlus roughPlus;
    ImageStack roughAlignVideo;
    ImagePlus finePlus;
    ImageStack fineAlignVideo;
    ImagePlus patternPlus;
    ImageProcessor patternImage;
    // variables
    int nframes = 0;
    int width = 0;
    int height = 0;
    int cx = 0;
    int cy = 0;
    int radcon = 0;
    // best frame analysis
    int BestFrame = 0;
    double[] minrows;
    double[] maxrows;
    double[] avgrows;
    double[] stdrows;
    // rough align
    boolean clickCenter = false;
    double[] Xrough = null;
    double[] Yrough = null;
    double[] Xfine = null;
    double[] Yfine = null;
    double xmin = 0;
    double ymin = 0;
    double xmax = 0;
    double ymax = 0;
    Roi pattern = null;
    boolean updatePattern = false;
    // text field to enter values
    JTextField radiusFieldFineAlign;
    JTextField RadiusFieldMedian;
    JTextField gaussField;
    JTextField InitialField;
    JTextField LastField;
    JTextField jtfFramerate;
    JTextField jtfDispDistance;
    JTextField jtfframerate;
    JTextField jtfRadPeaks;
    JTextField jtfMaximumTime;
    JTextField jtfiter;
    boolean nfold = true;
    // variables to store values
    double radiusCorr = 10;
    double gauss = 0.0;
    int initialPvalue = 1;
    int lastPvalue = 1000;
    int distance = 0;
    int imagePixel = 512;
    int scanSize = 1783;
    double framerate = 0;
    int disappearancetime = 0;
    int nbiterat = 0;
    //Calibration
    double convert = scanSize / (double) imagePixel; // [Angstrom/Pixel]
    double distanceInAng = 0;                          // Max displacement tracking in Angstrom
    String trackingdirectory;
    String trackingfilename;
    TextWindow resultTracking;
    // double radiuspeaksInAng = 0;                     // radiuspeaks in Angstrom
    // algo for analysis
    boolean algoMean = false;
    boolean algoStd = false;
    boolean algoMin = false;
    boolean algoMax = false;
    // debug
    boolean debug = true;
    // AFM instances
    AFMInterface AFMInstance = AFMInterface.getAFMInterface();
    AFMImage afmImage = AFMInstance.getAFMImage();
    // private String rad;
    // private Component ImagePairCorrelationPanel;
    // public ContrastAdjustmentPlus;
    // private String[] Ycontrastr;

    public VideoInterface1() {
        super("VideoJ");
        bundle = ResourceBundle.getBundle("i18n/videoBundle");
        doInterface();
        drawInterface();
    }

    /**
     * Correlation value between one image and ref
     *
     * @param ima    The original image
     * @param ref    The pattern
     * @param radius The radius within to compute correlation
     * @return The correlation value
     */
    static double correlationValue(ImageProcessor ima, ImageProcessor ref, int x0, int y0, int radius) {
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
        double kmean = 0;
        double smean = 0;
        double tk = 0;
        double ts = 0;
        double skk = 0;
        double sss = 0;
        double sks = 0;
        double rfactor = 0;
        double tiny = 1.0e-20;

        // center in original image
        int xx0 = x0 - KernelWidth / 2;
        int yy0 = y0 - KernelHeight / 2;

        for (int xx = 0; xx < KernelWidth; xx++) {
            for (int yy = 0; yy < KernelHeight; yy++) {
                dist2 = (xx - xc) * (xx - xc) + (yy - yc) * (yy - yc);
                if (dist2 < radius2) {
                    kmean += (double) (ref.getPixel(xx, yy));
                    smean += (double) (ima.getPixelValue(xx0 + xx, yy0 + yy));
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
                    tk = ref.getPixel(xx, yy) - kmean;
                    ts = ima.getPixelValue(xx0 + xx, yy0 + yy) - smean;
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
     * Constructor for the AFMInterface object
     */
    public int getDistance() {
        return distance;
    }

    public void setDistance(int d) {
        distance = d;
    }

    public double getDistanceInAng() {
        return distanceInAng;
    }

    public void setdistanceInAng(double d) {
        distanceInAng = d;
    }

    public double getConvert() {
        return convert;
    }
// Create a Panel for Contrast Adjustment
// Perrine: temporary comment of all this panel causing errors (I may miss a library: my arrayUtils seems to be only for integers

    /**
     * Create the Image interface to load raw data
     *
     * @return the panel interface
     */
    private JPanel createAboutPanel() {
        JPanel ImageP = new JPanel();
        ImageP.setBackground(Color.LIGHT_GRAY);
        ImageP.setLayout(new BoxLayout(ImageP, BoxLayout.Y_AXIS));


        // about button
        JPanel panelAboutButton = new JPanel();
        panelAboutButton.setLayout(new FlowLayout());
        panelAboutButton.setBackground(Color.LIGHT_GRAY);
        JButton aboutB = new JButton("ABOUT");
        aboutB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        new AboutWindowVideoJ();
                    }
                });
        panelAboutButton.add(aboutB);
        ImageP.add(panelAboutButton);

        return ImageP;
    }

    private JPanel createContrastAdjustmentPanel() {
        JPanel ContrastAdjustmentP = new JPanel();
        ContrastAdjustmentP.setBackground(Color.LIGHT_GRAY);
        ContrastAdjustmentP.setLayout(new BoxLayout(ContrastAdjustmentP, BoxLayout.Y_AXIS));


        // Introduction text for the CA panel
        JPanel head = new JPanel();
        head.setLayout(new FlowLayout());
        head.setBackground(Color.LIGHT_GRAY);
        head.add(new JLabel("ContrastAdjustment Panel"));
        ContrastAdjustmentP.add(head);


        // open raw data button
        JPanel panelRawImage = new JPanel();
        panelRawImage.setLayout(new FlowLayout());
        panelRawImage.setBackground(Color.LIGHT_GRAY);
        JButton rawImageB = new JButton("Open Raw");
        rawImageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // open raw data video
                        openRaw();
                    }
                });
        panelRawImage.add(rawImageB);
        ContrastAdjustmentP.add(panelRawImage);


        // Best Frame button
        JPanel panelBestFrameButton = new JPanel();
        panelBestFrameButton.setLayout(new FlowLayout());
        panelBestFrameButton.setBackground(Color.LIGHT_GRAY);
        JButton BestFrameImageB = new JButton("Best Frame");
        BestFrameImageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        BestFrame = rawPlus.getCurrentSlice() + 0;
                        IJ.log("Best frame=" + BestFrame);
                    }
                });
        panelBestFrameButton.add(BestFrameImageB);
        ContrastAdjustmentP.add(panelBestFrameButton);

        // Analysis button
        JPanel panelAnalysisButton = new JPanel();
        panelAnalysisButton.setLayout(new FlowLayout());
        panelAnalysisButton.setBackground(Color.LIGHT_GRAY);
        JButton AnalysisImageB = new JButton("Analysis");
        AnalysisImageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        getArraysStatFromRows(rawVideo.getProcessor(BestFrame + 1));
                        displayHistogram(minrows, "minima");
                        displayHistogram(maxrows, "maxima");
                        displayHistogram(avgrows, "avg");
                        displayHistogram(stdrows, "std");
                    }
                });
        panelAnalysisButton.add(AnalysisImageB);
        ContrastAdjustmentP.add(panelAnalysisButton);

        //Options for Selection of Adjust Lines

        JPanel AllAlgosPanel = new JPanel();
        AllAlgosPanel.setLayout(new BoxLayout(AllAlgosPanel, BoxLayout.X_AXIS));

        // Maximum
        JPanel MaximumP = new JPanel();
        MaximumP.setLayout(new FlowLayout());
        MaximumP.setBackground(Color.LIGHT_GRAY);
        //updateP.add(new JLabel(bundle.getString("$Maximum")));
        JCheckBox MaximumBox = new JCheckBox("Maximum", algoMax);
        MaximumBox.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        //adjustlines
                        algoMax = !algoMax;
                        //System.out.println("Maximum "+ maximum);
                    }
                });
        MaximumP.add(MaximumBox);
        AllAlgosPanel.add(MaximumP);

        //JSplitPane sp1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, LoadPanel, RoughAlignPanel);

        // Minimum
        JPanel MinimumP = new JPanel();
        MinimumP.setLayout(new FlowLayout());
        MinimumP.setBackground(Color.LIGHT_GRAY);
        //MinimumP.add(new JLabel(bundle.getString("$Minimum")));
        JCheckBox MinimumBox = new JCheckBox("Minimum", algoMin);
        MinimumBox.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        //adjustlines
                        algoMin = !algoMin;
                        //System.out.println("Minimum "+ Minimum);
                    }
                });
        MinimumP.add(MinimumBox);
        AllAlgosPanel.add(MinimumP);

        // Mean
        JPanel MeanP = new JPanel();
        MeanP.setLayout(new FlowLayout());
        MeanP.setBackground(Color.LIGHT_GRAY);
        //MeanP.add(new JLabel(bundle.getString("$Mean")));
        JCheckBox MeanBox = new JCheckBox("Mean", algoMean);
        MeanBox.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        //adjustlines
                        algoMean = !algoMean;
                        //System.out.println("Mean "+ Mean);
                    }
                });
        MeanP.add(MeanBox);
        AllAlgosPanel.add(MeanP);

        // Standard Deviation
        JPanel StdP = new JPanel();
        StdP.setLayout(new FlowLayout());
        StdP.setBackground(Color.LIGHT_GRAY);
        //StdP.add(new JLabel(bundle.getString("$Std")));
        JCheckBox StdBox = new JCheckBox("Standard Deviation", algoStd);
        StdBox.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        //adjustlines
                        algoStd = !algoStd;
                        //System.out.println("Std "+ Std);
                    }
                });
        StdP.add(StdBox);
        AllAlgosPanel.add(StdP);

        ContrastAdjustmentP.add(AllAlgosPanel);

        // Median Filter Size
        JPanel MedianFilterP = new JPanel();
        MedianFilterP.setLayout(new FlowLayout());
        MedianFilterP.setBackground(Color.LIGHT_GRAY);
        MedianFilterP.add(new JLabel("Median Filter Size"));
        RadiusFieldMedian = new JTextField();
        RadiusFieldMedian.setText("   " + radcon);
        RadiusFieldMedian.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        //IJ.log("Median Filter Size");
                        String txt = RadiusFieldMedian.getText();
                        radcon = (int) Double.parseDouble(txt.trim());
                        IJ.log("Median filter Size = " + radcon);
                    }

                    public void focusGained(FocusEvent fe) {
                    }
                });
        MedianFilterP.add(RadiusFieldMedian);
        ContrastAdjustmentP.add(MedianFilterP);


        // Adjust Lines button
        JPanel panelAdjustLinesButton = new JPanel();
        panelAdjustLinesButton.setLayout(new FlowLayout());
        panelAdjustLinesButton.setBackground(Color.LIGHT_GRAY);
        JButton AdjustLinesImageB = new JButton("Adjust Lines");
        AdjustLinesImageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // ArrayUtil tabmin = new ArrayUtil(minrows);
                        //  float medmin = tabmin.median();
                        // ArrayUtil tabmax = new ArrayUtil(maxrows);
                        //   float medmax = tabmax.median();
                        // float goodRange = medmax - medmin;
                        // IJ.log("min=" + medmin + " max=" + medmax + "good range=" + goodRange);
                        // analyse goods/bads and filter bads
                        //int rad = 1;
                        // original was int rad = 1;
                        float mindefault = 0;
                        float maxdefault = 255;

                        // Averages (Computes all the Average for the Best Frames)
                        double minavg, maxavg;
                        if (algoMean) {
                            ArrayUtil tabavg = new ArrayUtil(avgrows);
                            minavg = tabavg.getMinimum();
                            maxavg = tabavg.getMaximum();
                        } else {
                            minavg = mindefault;
                            maxavg = maxdefault;
                        }

                        // Standard Deviation
                        double minstd, maxstd;
                        if (algoStd) {
                            ArrayUtil tabstd = new ArrayUtil(stdrows);
                            minstd = tabstd.getMinimum();
                            maxstd = tabstd.getMaximum();
                        } else {
                            minstd = mindefault;
                            maxstd = maxdefault;
                        }

                        // Minimum
                        double minmin, maxmin;
                        if (algoMin) {
                            ArrayUtil tabmin = new ArrayUtil(minrows);
                            minmin = tabmin.getMinimum();
                            maxmin = tabmin.getMaximum();
                        } else {
                            minmin = mindefault;
                            maxmin = maxdefault;
                        }

                        // Maximum
                        double minmax, maxmax;
                        if (algoMax) {
                            ArrayUtil tabmax = new ArrayUtil(maxrows);
                            minmax = tabmax.getMinimum();
                            maxmax = tabmax.getMaximum();
                        } else {
                            minmax = mindefault;
                            maxmax = maxdefault;
                        }

                        if (radcon == 0) {
                            IJ.showMessage("Radius is Zero; No Filtering");
                        } else {
                            // choice 1 = range max - min (Check if it is a bad line and filter it)
                            AdjustedAndFilteredVideo = analyseAndFilterLines(rawVideo, minavg, maxavg, minstd, maxstd, minmin, maxmin, minmax, maxmax, radcon);

                            AdjustedAndFilteredPlus = new ImagePlus("Adjusted and Filtered", AdjustedAndFilteredVideo);
                            AdjustedAndFilteredPlus.show();
                        }
                        // choice 2 = avg

                        //      public void actionPerformed(ActionEvent ae) {
                        //          ArrayUtil tabmin = new ArrayUtil(minrows);
                        //           float medmin = tabmin.median();
                        //        ArrayUtil tabmax = new ArrayUtil(maxrows);
                        //          float medmax = tabmax.median();
                        //             float goodRange = medmax - medmin;
                        //     IJ.log("min=" + medmin + " max=" + medmax + "good range=" + goodRange);
                        // analyse goods/bads and filter bads
                        //      int rad = 1;
                        // choice 1 = range max - min
                        //        ImageStack filtered = analyseAndFilterLines(rawVideo, goodRange,0,255,0,255, rad);
                        // choice 2 = avg
                        //      ArrayUtil tabavg = new ArrayUtil(avgrows);
                        //     float meandavg = tabavg.getMean();
                        //     float stdavg = tabavg.getSigma();
                        // ImageStack filtered = analyseAndFilterLines(rawVideo, 0,meanavg-2*stdavg,meanavg+2*stdavg,0,255, rad);
                        //        ArrayUtil tabstd = new ArrayUtil(stdrows);
                        //      float  meanstd= tabstd.getMean();
                        //      float stdstd = tabstd.getSigma();
                        // ImageStack filtered = analyseAndFilterLines(rawVideo, 0,0,255,meanstd-2*stdstd,meanstd+2*stdstd,rad);
                        // adjust min and max
                        // medmin and medmax
                        //        contrastVideo = adjustLinesMinMax(filtered, 0, 255);
                        //      contrastPlus = new ImagePlus("contrast video", contrastVideo);
                        //       contrastPlus.show();
                    }
                });
        panelAdjustLinesButton.add(AdjustLinesImageB);
        ContrastAdjustmentP.add(panelAdjustLinesButton);


        // Adjust Frame button
        //JPanel panelAdjustFrameButton = new JPanel();
        // panelAdjustFrameButton.setLayout(new FlowLayout());
        // panelAdjustFrameButton.setBackground(Color.LIGHT_GRAY);
        // JButton AdjustFrameImageB = new JButton("ADJUST FRAME");
        // AdjustFrameImageB.addActionListener(
        //  new ActionListener() {

        //    public void actionPerformed(ActionEvent ae) {
        //            lineAnalysis(BestFrame);
        //      }
        //    });
        // panelAdjustFrameButton.add(AdjustFrameImageB);
        //ContrastAdjustmentP.add(panelAdjustFrameButton);


        // Manual Adjustment
        //JPanel Manualhead = new JPanel();
        //  Manualhead.setLayout(new FlowLayout());
        //   Manualhead.setBackground(Color.LIGHT_GRAY);
        // Manualhead.add(new JLabel(" Manual Adjustment "));
        //     ContrastAdjustmentP.add(Manualhead);

        // Over all Contrast Min & Max button (Now) Raw-Filtered-Adjusted
        JPanel panelMinMaxButton = new JPanel();
        panelMinMaxButton.setLayout(new FlowLayout());
        panelMinMaxButton.setBackground(Color.LIGHT_GRAY);
        JButton MinMaxImageB = new JButton(" Raw-Filtered-Adjusted");
        MinMaxImageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        if (AdjustedAndFilteredVideo == null) {

                            ImageStack res = adjustLinesMinMax(rawVideo, 0, 255);
                            ContrastAdjustmentVideo = res;
                            ContrastAdjustmentPlus = new ImagePlus("Contrast Adjusted", ContrastAdjustmentVideo);
                            ContrastAdjustmentPlus.show();
                        } else {
                            IJ.log("Adjusting Lines");
                            ImageStack res = adjustLinesMinMax(AdjustedAndFilteredVideo, 0, 255);
                            IJ.log("Contrast Adjusting");
                            ContrastAdjustmentVideo = res;
                            IJ.log("Filtering " + ContrastAdjustmentVideo);
                            ContrastAdjustmentPlus = new ImagePlus("Contrast Filtered and Adjusted", ContrastAdjustmentVideo);
                            IJ.log("Contrast Adjustment Plus");
                            ContrastAdjustmentPlus.show();
                        }
                        //            lineAnalysis(BestFrame);
                    }
                });
        panelMinMaxButton.add(MinMaxImageB);
        ContrastAdjustmentP.add(panelMinMaxButton);


        // Over all Contrast Avg. SD button (Now)Raw-Filtered-Adjusted-Contrasted
        //  JPanel panelDenoiseButton = new JPanel();
        //      panelDenoiseButton.setLayout(new FlowLayout());
        //    panelDenoiseButton.setBackground(Color.LIGHT_GRAY);
        //    JButton DenoiseImageB = new JButton("Raw-Filtered-Adjusted-Contrasted");
        //    DenoiseImageB.addActionListener(
        //        new ActionListener() {

        //       public void actionPerformed(ActionEvent ae) {
        //        if (AdjustedAndFilteredVideo == null) {
        //           ImageStack res = adjustLinesMinMax(rawVideo, 0, 255);
        //             ContrastAdjustmentVideo = res;
        //            ContrastAdjustmentPlus.setStack("contrast adjusted", ContrastAdjustmentVideo);
        //             ContrastAdjustmentPlus.show();
        //           } else {
        //               ImageStack res = adjustLinesMinMax(AdjustedAndFilteredVideo, 0, 255);
        //              ContrastAdjustmentVideo = res;
        //               ContrastAdjustmentPlus.setStack("contrast", ContrastAdjustmentVideo);
        //                 ContrastAdjustmentPlus.show();
        //  IJ.run("Enhance Contrast", "saturated=0.4 normalize_all");
        //               }
        //            lineAnalysis(BestFrame);
        //            }
        //        });
        //   panelDenoiseButton.add(DenoiseImageB);
        //   ContrastAdjustmentP.add(panelDenoiseButton);


        // StartFrame value

        //  JPanel initialP = new JPanel();
        // initialP.setLayout(new FlowLayout());
        //  initialP.setBackground(Color.LIGHT_GRAY);
        // initialP.add(new JLabel("START FRAME"));
        //  InitialField = new JTextField();
        //   InitialField.setText("   " + initialPvalue);
        //   InitialField.addFocusListener(
        //     new FocusListener() {

        //     public void focusLost(FocusEvent fe) {
        //      String txt = InitialField.getText();
        //       initialPvalue = Integer.parseInt(txt.trim());
        //      }

        //        public void focusGained(FocusEvent fe) {
        //          }
        //      });
        //  initialP.add(InitialField);
        //  ContrastAdjustmentP.add(initialP);


        // FinalFrame value

        //    JPanel lastP = new JPanel();
        //   lastP.setLayout(new FlowLayout());
        //    lastP.setBackground(Color.LIGHT_GRAY);
        //   lastP.add(new JLabel("FINAL FRAME"));
        //   LastField = new JTextField();
        //   LastField.setText("   " + lastPvalue);
        //    LastField.addFocusListener(
        //          new FocusListener() {
//
        //          public void focusLost(FocusEvent fe) {
        //               String txt = LastField.getText();
        //              lastPvalue = Integer.parseInt(txt.trim());
        //          }

        //            public void focusGained(FocusEvent fe) {
        //           }
        //        });

        //   lastP.add(LastField);
        //    ContrastAdjustmentP.add(lastP);

        // Save Contrast Adjustment
        JPanel panelSaveContrastAdjustment = new JPanel();
        panelSaveContrastAdjustment.setLayout(new FlowLayout());
        panelSaveContrastAdjustment.setBackground(Color.LIGHT_GRAY);
        JButton SaveContrastAdjustment = new JButton("Save Contrast-adjusted and Filtered");
        SaveContrastAdjustment.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        IJ.log("Saving Contrast Adjusted Video");
                        saveCurrentVideo("Contrast Adjusted");
                    }
                    // private void SaveContrastAdjustment() {
                    //    throw new UnsupportedOperationException("Not yet implemented");
                    //  }
                });

        panelSaveContrastAdjustment.add(SaveContrastAdjustment);
        ContrastAdjustmentP.add(panelSaveContrastAdjustment);


        return ContrastAdjustmentP;
    }

    public JPanel createPanelAlgo() {
        JPanel panel = new JPanel();

        panel.setBackground(Color.LIGHT_GRAY);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        //JButton button1 = new JButton("Minimum");
        //JButton button2 = new JButton("Maximum");
        //JButton button3 = new JButton("Average");
        //JButton button4 = new JButton("Standard Deviation");


        // updateP. add(new JLabel ("ADJUST LINES"));
        JCheckBox algoMaxBox = new JCheckBox("Maximum", algoMax);
        algoMaxBox.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // Adjust with respect to Maximum
                        algoMax = !algoMax;
                        //System.out.println("update "+ maximum);
                    }
                });

        panel.add(algoMaxBox);

        // updateP. add(new JLabel ("ADJUST LINES"));
        JCheckBox algoMinBox = new JCheckBox("Minimum", algoMin);
        algoMinBox.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // Adjust with respect to Minimum
                        algoMin = !algoMin;
                        //System.out.println("update "+ Minimum);
                    }
                });

        panel.add(algoMinBox);

        // updateP. add(new JLabel ("ADJUST LINES"));
        JCheckBox algoMeanBox = new JCheckBox("Average", algoMean);
        algoMeanBox.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // Adjust with respect to Average
                        algoMean = !algoMean;
                        //System.out.println("update "+ Average);
                    }
                });

        panel.add(algoMeanBox);

        // updateP. add(new JLabel ("ADJUST LINES"));
        JCheckBox algoStdBox = new JCheckBox("Standard Deviation", algoStd);
        algoStdBox.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // Adjust with respect to Standard Deviation
                        algoStd = !algoStd;
                        //System.out.println("update "+ Standard deviation);
                    }
                });

        panel.add(algoStdBox);

        // panel.add(button1);
        // panel.add(button2);
        // panel.add(button3);
        // panel.add(button4);
// Should there be return JPanel here instead of return Panel???
        return panel;
    }

    /**
     * Create the Panel to rough align data
     *
     * @return the panel interface
     */
    private JPanel createRoughAlignPanel() {
        JPanel ImageP = new JPanel();
        ImageP.setBackground(Color.LIGHT_GRAY);
        ImageP.setLayout(new BoxLayout(ImageP, BoxLayout.Y_AXIS));

        // Open Contrast Adjustment Video

        JPanel panelContrastAdjustmentVideo = new JPanel();
        panelContrastAdjustmentVideo.setLayout(new FlowLayout());
        panelContrastAdjustmentVideo.setBackground(Color.LIGHT_GRAY);
        JButton ContrastAdjustmentVideoB = new JButton("Open Raw Filtered Contrasted Movie ");
        ContrastAdjustmentVideoB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // open raw data video
                        openRaw();
                    }
                });
        panelContrastAdjustmentVideo.add(ContrastAdjustmentVideoB);
        ImageP.add(panelContrastAdjustmentVideo);

        // define center button
        JPanel panelDefineCenter = new JPanel();
        panelDefineCenter.setLayout(new FlowLayout());
        panelDefineCenter.setBackground(Color.LIGHT_GRAY);
        JButton DefineCenter = new JButton("Manual Alignment");
        DefineCenter.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // define center on all frames

                        defineCenter();
                    }
                });
        panelDefineCenter.add(DefineCenter);
        ImageP.add(panelDefineCenter);

        // rough align
        JPanel panelRoughAlign = new JPanel();
        panelRoughAlign.setLayout(new FlowLayout());
        panelRoughAlign.setBackground(Color.LIGHT_GRAY);
        JButton RoughAlign = new JButton("Rough Alignment");
        RoughAlign.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // rough align
                        roughAlign();
                        // plot shift
                        plotCoordinatesRough();
                    }
                });
        panelRoughAlign.add(RoughAlign);
        ImageP.add(panelRoughAlign);

        // rough save
        JPanel panelRoughSave = new JPanel();
        panelRoughSave.setLayout(new FlowLayout());
        panelRoughSave.setBackground(Color.LIGHT_GRAY);
        JButton RoughSave = new JButton("Save Rough");
        RoughSave.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // rough align
                        saveRough();
                    }
                });
        panelRoughSave.add(RoughSave);
        ImageP.add(panelRoughSave);


        return ImageP;
    }

    /**
     * Create the Panel to fine align data
     *
     * @return the panel interface
     */
    private JPanel createFineAlignPanel() {
        JPanel ImageP = new JPanel();
        ImageP.setBackground(Color.LIGHT_GRAY);
        ImageP.setLayout(new BoxLayout(ImageP, BoxLayout.Y_AXIS));

        // define open button
        JPanel panelOpenRough = new JPanel();
        panelOpenRough.setLayout(new FlowLayout());
        panelOpenRough.setBackground(Color.LIGHT_GRAY);
        JButton OpenRough = new JButton("Open");
        OpenRough.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // open rough
                        openRough();
                    }
                });
        panelOpenRough.add(OpenRough);
        ImageP.add(panelOpenRough);

        JPanel panelDrift = new JPanel();
        panelDrift.setLayout(new FlowLayout());
        panelDrift.setBackground(Color.LIGHT_GRAY);
        JButton DriftButton = new JButton("Open Drift");
        DriftButton.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // Drift,
                        openDrift();
                        createFineAlign();
                    }
                });
        panelDrift.add(DriftButton);
        ImageP.add(panelDrift);

        // define pattern
        JPanel panelPattern = new JPanel();
        panelPattern.setLayout(new FlowLayout());
        panelPattern.setBackground(Color.LIGHT_GRAY);
        JButton DefinePattern = new JButton("Define Pattern");
        DefinePattern.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // define pattern
                        definePattern();
                    }
                });
        panelPattern.add(DefinePattern);
        ImageP.add(panelPattern);

        // radius correlation value
        JPanel radP = new JPanel();
        radP.setLayout(new FlowLayout());
        radP.setBackground(Color.LIGHT_GRAY);
        radP.add(new JLabel("Correlation Radius"));
        radiusFieldFineAlign = new JTextField();
        radiusFieldFineAlign.setText("   " + radiusCorr);
        radiusFieldFineAlign.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        //IJ.log("radius of the pattern");
                        String txt = radiusFieldFineAlign.getText();
                        // IJ.log("radius of the pattern txt = "+txt);
                        radiusCorr = (int) Double.parseDouble(txt.trim());
                        IJ.log("radius of the pattern = " + radiusCorr);
                    }

                    public void focusGained(FocusEvent fe) {
                    }
                });
        radP.add(radiusFieldFineAlign);
        ImageP.add(radP);

        // update pattern
        JPanel updateP = new JPanel();
        updateP.setLayout(new FlowLayout());
        updateP.setBackground(Color.LIGHT_GRAY);
        //updateP.add(new JLabel(bundle.getString("$RADIUSCORR")));
        JCheckBox updateBox = new JCheckBox("UPDATEPATTERN", updatePattern);
        updateBox.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // fine align
                        updatePattern = !updatePattern;
                        //System.out.println("update "+updatePattern);
                    }
                });
        updateP.add(updateBox);
        ImageP.add(updateP);


        // fine align
        JPanel panelFineAlign = new JPanel();
        panelFineAlign.setLayout(new FlowLayout());
        panelFineAlign.setBackground(Color.LIGHT_GRAY);
        JButton FineAlign = new JButton("Fine Alignment");
        FineAlign.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // fine align
                        //IJ.log("radius of the pattern 2 = " + radiusCorr);
                        fineAlign((int) radiusCorr, updatePattern);
                        createFineAlign();
                        // plot
                        plotCoordinatesFine();
                    }
                });
        panelFineAlign.add(FineAlign);
        ImageP.add(panelFineAlign);

        // Save fine align
        JPanel panelSaveFineAlign = new JPanel();
        panelSaveFineAlign.setLayout(new FlowLayout());
        panelSaveFineAlign.setBackground(Color.LIGHT_GRAY);
        JButton SaveFineAlign = new JButton("Save Fine");
        SaveFineAlign.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        saveFine();

                    }
                });
        panelSaveFineAlign.add(SaveFineAlign);
        ImageP.add(panelSaveFineAlign);


        return ImageP;
    }

    private JPanel createMovieAnalysisPanel() {
        JPanel MovieAnalysisP = new JPanel();
        MovieAnalysisP.setBackground(Color.LIGHT_GRAY);
        MovieAnalysisP.setLayout(new BoxLayout(MovieAnalysisP, BoxLayout.Y_AXIS));


        //MAPU= Movie Analysis Parameters usage
        JPanel MAPU = new JPanel();
        MAPU.setLayout(new FlowLayout());
        MAPU.setBackground(Color.LIGHT_GRAY);
        MAPU.add(new JLabel(" NOTE: To enter the values of 1)Image Parameters, "
                + " 2)Correlation Parameters"
                + " Go to the 'Parameters' Panel"));
        MovieAnalysisP.add(MAPU);

        // intro text
        JPanel head = new JPanel();
        head.setLayout(new FlowLayout());
        head.setBackground(Color.LIGHT_GRAY);
        head.add(new JLabel("Movie Analysis Panel"));
        MovieAnalysisP.add(head);
        // FirstFrame value
        //  JPanel initialP = new JPanel();
        //   initialP.setLayout(new FlowLayout());
        //   initialP.setBackground(Color.LIGHT_GRAY);
        //   initialP.add(new JLabel("FIRST FRAME"));
        //   InitialField = new JTextField();
        //    InitialField.setText("   " + initialPvalue);
        //      InitialField.addFocusListener(
        //     new FocusListener() {

        //         public void focusLost(FocusEvent fe) {
        //           String txt = InitialField.getText();
        //               initialPvalue = Integer.parseInt(txt.trim());
        //            IJ.log("firsr frame="+initialPvalue);
        //               }

        //             public void focusGained(FocusEvent fe) {
        //              }
        //             });
        //    initialP.add(InitialField);
        //    AverageP.add(initialP);


        // Last Frame value
        //    JPanel lastP = new JPanel();
        //    lastP.setLayout(new FlowLayout());
        //    lastP.setBackground(Color.LIGHT_GRAY);
        //     lastP.add(new JLabel("LAST FRAME"));
        //     LastField = new JTextField();
        //     LastField.setText("   " + lastPvalue);
        //    LastField.addFocusListener(
        //        new FocusListener() {

        //             public void focusLost(FocusEvent fe) {
        //                  String txt = LastField.getText();
        //                 lastPvalue = Integer.parseInt(txt.trim());
        //            }

        //             public void focusGained(FocusEvent fe) {
        //               }
        //           });
        //      lastP.add(LastField);
        //      AverageP.add(lastP);

        // Open button (Generally Opens The fine Aligned Movie)


        JPanel panelfineImage = new JPanel();
        panelfineImage.setLayout(new FlowLayout());
        panelfineImage.setBackground(Color.LIGHT_GRAY);
        JButton fineImageB = new JButton("Open");
        fineImageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // open raw data video
                        openRaw();
                    }
                });
        panelfineImage.add(fineImageB);
        MovieAnalysisP.add(panelfineImage);

        //    MovieAnalysisP.add(AFMInstance.createParametersPanel());


        // radius peaks value
        /*
         * JPanel radPeaksP = new JPanel(); radPeaksP.setLayout(new
         * FlowLayout()); radPeaksP.setBackground(Color.LIGHT_GRAY);
         * //radPeaksP.add(new JLabel(bundle.getString("$RADIUSPEAK1")));
         * radPeaksP.add(new JLabel("Distance")); jtfRadPeaks = new
         * JTextField(); jtfRadPeaks.setText(" "+AFMInstance.getRadius());
         * jtfRadPeaks.addFocusListener( new FocusListener() {
         *
         *
         *
         * public void focusLost(FocusEvent fe) { String txt =
         * jtfRadPeaks.getText(); double radiuspeaksInAng =
         * Double.parseDouble(txt.trim());
         * AFMInstance.setradiuspeaksInAng(radiuspeaksInAng); int radiuspeaks =
         * (int) Math.round(radiuspeaksInAng / AFMInstance.getConvert());
         * AFMInstance.setRadiusPeaks(radiuspeaks);
         *
         * }
         *
         * public void focusGained(FocusEvent fe) { } });
         * radPeaksP.add(jtfRadPeaks); MovieAnalysisP.add(radPeaksP);
         *
         */


        // scan size in Angstrom
        //    JPanel panelScanSize = new JPanel();
        //    panelScanSize.setLayout(new FlowLayout());
        //     panelScanSize.setBackground(Color.LIGHT_GRAY);
        //    panelScanSize.add(new JLabel(bundle.getString("$SCANSIZE")));
        //    panelScanSize.add(new JLabel("Scan Size"));
        //    final JTextField scan = new JTextField("   " + scanSize);
        //    panelScanSize.add(scan);
        //   scan.addFocusListener(new FocusListener() {

        //        public void focusLost(FocusEvent fe) {
        //         String txt = scan.getText();
        //         scanSize = Integer.parseInt(txt.trim());
        //          convert = scanSize / (double) imagePixel;
        //           radiusInAng = radius * convert;
        //            radiuspeaksInAng = radiuspeaks * convert;
        //          jtfRAD.setText(" " + radiusInAng);
        //           jtfRadPeaks.setText(" " + radiuspeaksInAng);
        //       }

        //       public void focusGained(FocusEvent fe) {
        //       }
        //  });
        //    MovieAnalysisP.add(panelScanSize);


        // Averaging values
        JPanel averagingvalue = new JPanel();
        averagingvalue.setLayout(new FlowLayout());
        averagingvalue.setBackground(Color.LIGHT_GRAY);
        JButton Averaging = new JButton("Do Average");
        Averaging.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        computeAverageParticle();
                    }
                });
        averagingvalue.add(Averaging);
        MovieAnalysisP.add(averagingvalue);

        // update pattern
        JPanel nfoldpanel = new JPanel();
        nfoldpanel.setLayout(new FlowLayout());
        nfoldpanel.setBackground(Color.LIGHT_GRAY);
        //updateP.add(new JLabel(bundle.getString("$RADIUSCORR")));
        JCheckBox nfoldBox = new JCheckBox("nfoldpanel", nfold);
        nfoldBox.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // fine align
                        nfold = !nfold;
                        //System.out.println("update "+updatePattern);
                    }
                });
        nfoldpanel.add(nfoldBox);
        MovieAnalysisP.add(nfoldpanel);

        return MovieAnalysisP;
    }

    public JPanel createParticleTrackingPanel() {
        JPanel ParticleTrackingP = new JPanel();
        ParticleTrackingP.setBackground(Color.LIGHT_GRAY);
        ParticleTrackingP.setLayout(new BoxLayout(ParticleTrackingP, BoxLayout.Y_AXIS));

        // 1) Particle Tracking Parameters usage= PTPU

        JPanel PTPU = new JPanel();
        PTPU.setLayout(new FlowLayout());
        PTPU.setBackground(Color.LIGHT_GRAY);
        PTPU.add(new JLabel("NOTE : Enter the values of Image Size, Scan Size before loading coordinate"
                + "files (Go to Parameters Panel to enter the values)"));
        ParticleTrackingP.add(PTPU);


        // 2)  Open Video for Tracking button- Opens the Fine aligned movie.
        JPanel panelRawImage = new JPanel();
        panelRawImage.setLayout(new FlowLayout());
        panelRawImage.setBackground(Color.LIGHT_GRAY);
        JButton rawImageB = new JButton("Read Movie File");
        rawImageB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // open raw data video
                        openFineAlign();

                    }
                });
        panelRawImage.add(rawImageB);
        ParticleTrackingP.add(panelRawImage);

        // 3) Frame Rate (ms/frame)- Enters the value of the Frame rate of the scan.

        JPanel panelFrameRate = new JPanel();
        panelFrameRate.setLayout(new FlowLayout());
        panelFrameRate.setBackground(Color.LIGHT_GRAY);
        // panelSizePixel.add(new JLabel(bundle.getString("$PIXELSIZE")));
        panelFrameRate.add(new JLabel("Frame Rate (ms/Frame)"));
        final JTextField rate = new JTextField("   " + framerate);
        panelFrameRate.add(rate);
        rate.addFocusListener(new FocusListener() {

            public void focusLost(FocusEvent fe) {
                String txt = rate.getText();
                framerate = Double.parseDouble(txt.trim());

            }

            public void focusGained(FocusEvent fe) {
            }
        });

        ParticleTrackingP.add(panelFrameRate);


        // 4)  No. of Frames Selection
        // StartFrame value

        JPanel initialP = new JPanel();
        initialP.setLayout(new FlowLayout());
        initialP.setBackground(Color.LIGHT_GRAY);
        initialP.add(new JLabel("START FRAME"));
        InitialField = new JTextField();
        InitialField.setText("   " + initialPvalue);
        InitialField.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        String txt = InitialField.getText();
                        initialPvalue = Integer.parseInt(txt.trim());
                        IJ.log("Frame number of First tracking = " + initialPvalue);
                    }

                    public void focusGained(FocusEvent fe) {
                    }
                });
        initialP.add(InitialField);
        ParticleTrackingP.add(initialP);


        // FinalFrame value

        JPanel lastP = new JPanel();
        lastP.setLayout(new FlowLayout());
        lastP.setBackground(Color.LIGHT_GRAY);
        lastP.add(new JLabel("FINAL FRAME"));
        LastField = new JTextField();
        LastField.setText("   " + lastPvalue);
        LastField.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        String txt = LastField.getText();
                        lastPvalue = Integer.parseInt(txt.trim());
                        IJ.log("Frame number of last tracking = " + lastPvalue);
                    }

                    public void focusGained(FocusEvent fe) {
                    }
                });

        lastP.add(LastField);
        ParticleTrackingP.add(lastP);

        // 5) Select Directory  - Reads the coordinate files (text file) containing the coordinates of the molecules.
        // we need one to get the name of the tracking

        JPanel CoordinateFilesP = new JPanel();
        CoordinateFilesP.setLayout(new FlowLayout());
        CoordinateFilesP.setBackground(Color.LIGHT_GRAY);
        JButton CoordinateFilesB = new JButton("Read Coordinate Files");
        CoordinateFilesB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // Assign Trajectory
                        OpenDialog od = new OpenDialog(("Select coordinate file"), "");
                        trackingfilename = od.getFileName();
                        trackingdirectory = od.getDirectory();
                        IJ.log("dir=" + trackingdirectory + " name=" + trackingfilename);

                    }
                });
        CoordinateFilesP.add(CoordinateFilesB);
        ParticleTrackingP.add(CoordinateFilesP);


        // 6) Maximum Displacement Button (in pixels)

        JPanel MaxDisDisP = new JPanel();
        MaxDisDisP.setLayout(new FlowLayout());
        MaxDisDisP.setBackground(Color.LIGHT_GRAY);
        MaxDisDisP.add(new JLabel("Maximum Displacement Distance"));
        jtfDispDistance = new JTextField();
        jtfDispDistance.setText("      " + distanceInAng);
        jtfDispDistance.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        String txt = jtfDispDistance.getText();
                        distance = Integer.parseInt(txt.trim());
                        distanceInAng = (double) (distance * convert);
                        IJ.log("Maximum Displacement Distance = " + distance);
                    }

                    public void focusGained(FocusEvent e) {
                        //   throw new UnsupportedOperationException("Not supported yet.");
                    }
                });

        MaxDisDisP.add(jtfDispDistance);
        ParticleTrackingP.add(MaxDisDisP);


        // 7) Maximum Disappearance Time (Frames)
        JPanel MaxDistimeP = new JPanel();
        MaxDistimeP.setLayout(new FlowLayout());
        MaxDistimeP.setBackground(Color.LIGHT_GRAY);
        MaxDistimeP.add(new JLabel("Maximum Disappearance Time"));
        jtfMaximumTime = new JTextField();
        jtfMaximumTime.setText("      " + disappearancetime);
        jtfMaximumTime.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        String txt = jtfMaximumTime.getText();
                        disappearancetime = Integer.parseInt(txt.trim());
                        IJ.log("Maximum Disappearance Time = " + disappearancetime);
                    }

                    public void focusGained(FocusEvent e) {
                        //   throw new UnsupportedOperationException("Not supported yet.");
                    }
                });

        MaxDistimeP.add(jtfMaximumTime);
        ParticleTrackingP.add(MaxDistimeP);
        // 7 bis) nb iteration for tracking (0 No disap time) (Frames)
        JPanel NbIterP = new JPanel();
        NbIterP.setLayout(new FlowLayout());
        NbIterP.setBackground(Color.LIGHT_GRAY);
        NbIterP.add(new JLabel("Nb Iterations (0: direct association)"));
        jtfiter = new JTextField();
        jtfiter.setText("      " + nbiterat);
        jtfiter.addFocusListener(
                new FocusListener() {

                    public void focusLost(FocusEvent fe) {
                        String txt = jtfiter.getText();
                        nbiterat = Integer.parseInt(txt.trim());
                        IJ.log("Tracking nb of iterations = " + nbiterat);
                    }

                    public void focusGained(FocusEvent e) {
                        //   throw new UnsupportedOperationException("Not supported yet.");
                    }
                });

        NbIterP.add(jtfiter);
        ParticleTrackingP.add(NbIterP);
        // 8)Track Particle

        JPanel panelTrackParticles = new JPanel();
        panelTrackParticles.setLayout(new FlowLayout());
        panelTrackParticles.setBackground(Color.LIGHT_GRAY);
        JButton TrackParticlesB = new JButton("Track Particles");
        TrackParticlesB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // Assign Trajectory
                        // tracking(trackingdirectory, trackingfilename,start,end,maxdistance,disapperancetime);
                        IJ.log("Tracking ...");
                        Tracking AFM_tracking = new Tracking(trackingdirectory, trackingfilename, initialPvalue, lastPvalue, distance, disappearancetime + 1, nbiterat);
                        IJ.log("Tracking initialized");
                        IJ.log("Tracking started");

                        resultTracking = AFM_tracking.Start_Tracking();
                        //IJ.saveAs("Text", trackingdirectory + "/trackingResults.txt");

                        resultTracking.setVisible(true);
                        IJ.log("Tracking finished");
                    }
                });
        panelTrackParticles.add(TrackParticlesB);
        ParticleTrackingP.add(panelTrackParticles);


        // Save Particle Tracking Results , STR= savetrackingresults

        JPanel panelsavetrackingresults = new JPanel();
        panelsavetrackingresults.setLayout(new FlowLayout());
        panelsavetrackingresults.setBackground(Color.LIGHT_GRAY);
        JButton savetrackingresults = new JButton("Save");
        savetrackingresults.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        IJ.log("Saving Results");
                        WindowManager.setWindow(resultTracking);
                        int pos = trackingfilename.lastIndexOf(".");
                        String name2 = trackingfilename.substring(0, pos);
                        IJ.saveAs("Text", trackingdirectory + "/" + name2 + "-trackresult-" + initialPvalue + "-" + lastPvalue + ".txt");
                    }
                });

        panelsavetrackingresults.add(savetrackingresults);
        ParticleTrackingP.add(panelsavetrackingresults);

        // 9)Display Tracking Results

        JPanel panelCorrectTracking = new JPanel();
        panelCorrectTracking.setLayout(new FlowLayout());
        panelCorrectTracking.setBackground(Color.LIGHT_GRAY);
        JButton CorrectTrackingB = new JButton(" Display Tracking ");
        CorrectTrackingB.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        // Open Manual Tracking results
                        IJ.log(" Opening Display Tracking Plots");
                        // ResultsTable RTTtmp=new ResultsTable();
                        int pos = trackingfilename.lastIndexOf(".");
                        String name2 = trackingfilename.substring(0, pos);
                        /*
                         * try {
                         *
                         * RTTtmp=ResultsTable.open(trackingdirectory + "/" +
                         * name2 + "-trackresult-" + initialPvalue + "-" +
                         * lastPvalue + ".txt"); RTTtmp.show("Read results
                         * Table"); } catch (IOException e) { // TODO
                         * Auto-generated catch block e.printStackTrace(); }
                         */

                        Visu_Tracking_Basedon_FCordelieresPlugin tmpclass = new Visu_Tracking_Basedon_FCordelieresPlugin();
                        tmpclass.Visu_Tracking(trackingdirectory + "/" + name2 + "-trackresult-" + initialPvalue + "-" + lastPvalue + ".txt");
                    }
                });
        panelCorrectTracking.add(CorrectTrackingB);
        ParticleTrackingP.add(panelCorrectTracking);
        return ParticleTrackingP;

    }

    private void computeAverageParticle() {
        // parameters from afm
        IJ.log("Start of  Averaging Value");
        int size;
        if (fineAlignVideo == null) {
            if (roughAlignVideo == null) {
                size = rawVideo.getSize();
            } else {
                size = roughAlignVideo.getSize();
            }
        } else {
            size = fineAlignVideo.getSize();

        }

        if (size < lastPvalue) {
            lastPvalue = size;
        }
        AFMImage afmimage = AFMInstance.getAFMImage();
        IJ.log("Final Value");

        int rad = AFMInstance.getRadius();
        int aginc = AFMInstance.getAngleInc();
        int agmax = AFMInstance.getAngleMax();
        double pc1 = AFMInstance.getPctcorr1();
        double pc2 = AFMInstance.getPctcorr2();
        int radp = AFMInstance.getRadiuspeaks();
        int nf = AFMInstance.getNfold();
        double ic1 = AFMInstance.getIstcorr1();
        double ic2 = AFMInstance.getIstcorr2();

        IJ.log(" To look for the parameters");

        // compute autocorrelation images
        // ref from afm
        ImageProcessor particle = afmimage.getRef();
        ImageStack stackres = new ImageStack(particle.getWidth(), particle.getHeight());
        //raw from video
        // movie for nfold
        ImageStack nfoldvideo = new ImageStack(particle.getWidth(), particle.getHeight());

        ImageStack video = rawVideo;
        if (roughAlignVideo != null) {
            video = roughAlignVideo;
        }
        if (fineAlignVideo != null) {
            video = fineAlignVideo;
        }

        ImageProcessor raw, corrima, angima, avg, nfoldaverage;
        AFMAnalysis afmanalysis = AFMInstance.getAFMAnalysis();

        for (int s = initialPvalue; s <= lastPvalue; s++) {
            raw = video.getProcessor(s);
            afmimage.autoCorrCalc(raw, particle, rad, aginc, agmax);
            // get correlation images
            corrima = afmimage.getCorrval();
            angima = afmimage.getCorrang();
            // find peaks in correlation images
            afmanalysis.findPeaks(raw, particle, corrima, angima, pc1, pc2, radp, nf, ic1, ic2, rad);
            ArrayList vpeaks = afmanalysis.getPeaksA();
            afmanalysis.savePeaks(vpeaks, raw, particle, "test-" + s + ".txt", agmax, aginc);
            // compute average image
            avg = afmimage.average(raw, particle, vpeaks, rad);
            stackres.addSlice("", avg);
            //n-fold the average image
            if (nfold == true) {
                nfoldaverage = AFMImage.nFold(avg, nf, false);
                nfoldvideo.addSlice("", nfoldaverage);
            }

        }
        if (nfold == true) {

            ImagePlus plusavgnfold = new ImagePlus("avg nflod", nfoldvideo);
            plusavgnfold.show();
        }
        ImagePlus plusavg = new ImagePlus("avg", stackres);
        plusavg.show();
    }

    /**
     * Description of the Method
     */
    private void doInterface() {
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel LoadPanel = createAboutPanel();
        JPanel RoughAlignPanel = createRoughAlignPanel();
        //JSplitPane sp1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, LoadPanel, RoughAlignPanel);
        JPanel FineAlignPanel = createFineAlignPanel();
        //JSplitPane sp2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sp1, FineAlignPanel);
        JPanel MovieAnalysisPanel = createMovieAnalysisPanel();
        //JSplitPane sp3 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sp2, AveragingPanel);
        JPanel ContrastAdjustmentPanel = createContrastAdjustmentPanel();
        //JSplitPane sp3 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sp2, ContrastAdjustmentPanel);
        //JPanel ParticleTrackingPanel2 = createFineAlignPanel(); //Perrine just to have the same number of panel
        JPanel ParticleTrackingPanel = createParticleTrackingPanel();


        tabbedPane.addTab("About", null, LoadPanel, "About");
        tabbedPane.setBackgroundAt(0, Color.YELLOW);
        tabbedPane.addTab("Contrast Adjustment", null, ContrastAdjustmentPanel, "Contrast Adjustment");
        //tabbedPane.addTab("dummy", null, ParticleTrackingPanel2, "dummy");
        tabbedPane.setBackgroundAt(1, Color.YELLOW);
        tabbedPane.addTab("RoughAlign", null, RoughAlignPanel, "RoughAlign");
        tabbedPane.setBackgroundAt(2, Color.YELLOW);
        tabbedPane.addTab("FineAlign", null, FineAlignPanel, "FineAlign");
        tabbedPane.setBackgroundAt(3, Color.YELLOW);
        tabbedPane.addTab("MovieAnalysis", null, MovieAnalysisPanel, "MovieAnalysis");
        tabbedPane.setBackgroundAt(4, Color.YELLOW);
        tabbedPane.addTab("ParticleTracking", null, ParticleTrackingPanel, "ParticleTracking");
        tabbedPane.setBackgroundAt(5, Color.YELLOW);
        // AFM Panels

        JPanel ImageAnalysisPanel = AFMInstance.createImageAnalysisPanel();
        tabbedPane.addTab("Image Analysis", null, ImageAnalysisPanel, "Image Analysis");
        tabbedPane.setBackgroundAt(6, Color.PINK);
        JPanel ImageCorrelationPanel = AFMInstance.createImageCorrelationPanel();
        tabbedPane.addTab("Image Correlation", null, ImageCorrelationPanel, "Image Correlation");
        tabbedPane.setBackgroundAt(7, Color.PINK);
        JPanel ImagePairCorrelationPanel = AFMInstance.createImagePairCorrelationPanel();
        tabbedPane.addTab("Image Pair Correlation", null, ImagePairCorrelationPanel, "Image Pair Correlation");
        tabbedPane.setBackgroundAt(8, Color.PINK);
        JPanel ImageAveragingPanel = AFMInstance.createImageAveragingPanel();
        tabbedPane.addTab("ImageAveraging", null, ImageAveragingPanel, "ImageAveraging");
        tabbedPane.setBackgroundAt(9, Color.PINK);
        JPanel ParametersPanel = AFMInstance.createParametersPanel();
        tabbedPane.addTab("Parameters", null, ParametersPanel, "Parameters");
        tabbedPane.setBackgroundAt(10, Color.PINK);
        add(tabbedPane);

// JPanel afmImagePanel = AFMInstance.createImageAnalysisPanel();
        //   tabbedPane.addTab("Image AFM", null, afmImagePanel, "AFM Image");
        //   JPanel afmCorrelationPanel = AFMInstance.createCorrelationPanel();
        //  tabbedPane.addTab("Correlation", null, afmCorrelationPanel, "Correlation");
        //  JPanel afmPairCorrelationPanel = AFMInstance.createPairCorrelationPanel();
        //   tabbedPane.addTab(" Pair Correlation", null, afmPairCorrelationPanel, "Pair Correlation");
        //   JPanel afmAveragePanel = AFMInstance.createAveragePanel();
        //   tabbedPane.addTab("Average", null, afmAveragePanel, "Average");

        add(tabbedPane);
    }

    /**
     * Draw the interface
     */
    private void drawInterface() {
        setSize(900, 1000);
        setResizable(true);
        setVisible(true);


    }

    /**
     * Close the plugin
     */
    @Override
    public void close() {
        super.close();


    }

    public void openRaw() {
        //    OpenDialog od = new OpenDialog(bundle.getString("$OPENRAW"), "");
        OpenDialog od = new OpenDialog(("Open Raw"), "");
        String name = od.getFileName();


        if (name != null) {
            String dir = od.getDirectory();
            Opener op = new Opener();


            if (rawPlus != null) {
                rawPlus.hide();
                rawPlus.flush();


            }
            rawPlus = op.openImage(dir, name);
            //          rawPlus.setTitle(bundle.getString("$RAW"));
            rawPlus.setTitle("Raw");
            rawPlus.show();
            rawVideo = rawPlus.getStack();
            nframes = rawVideo.getSize();
            width = rawVideo.getWidth();
            height = rawVideo.getHeight();
            // add mouse listener
            ImageCanvas canvas = rawPlus.getCanvas();
            canvas.addMouseListener(this);
            LastField.setText(" " + nframes);


        }
    }

    public void openFineAlign() {
        //    OpenDialog od = new OpenDialog(bundle.getString("$OPENRAW"), "");
        OpenDialog od = new OpenDialog(("Open Fine"), "");
        String name = od.getFileName();


        if (name != null) {
            String dir = od.getDirectory();
            Opener op = new Opener();


            if (finePlus != null) {
                finePlus.hide();
                finePlus.flush();


            }
            finePlus = op.openImage(dir, name);
            //          rawPlus.setTitle(bundle.getString("$RAW"));
            finePlus.setTitle("Fine Aligned");
            finePlus.show();
            fineAlignVideo = finePlus.getStack();
            nframes = fineAlignVideo.getSize();
            width = fineAlignVideo.getWidth();
            height = fineAlignVideo.getHeight();
            // add mouse listener
            ImageCanvas canvas = finePlus.getCanvas();
            canvas.addMouseListener(this);
            LastField.setText(" " + nframes);
            this.lastPvalue = nframes;
        }
    }

    public void saveRough() {
        // select directory and file name
        SaveDialog sd = new SaveDialog(("ROUGHSAVE"), ("ROUGH"), ".tif");
        String dir = sd.getDirectory();
        String fn = sd.getFileName();
        //IJ.log("dir="+dir+" "+fn);
        // Save image
        FileSaver fs = new FileSaver(roughPlus);
        fs.saveAsTiffStack(dir + fn);
        // save text
        String fn2 = fn.replaceFirst(".tif", ".txt");


        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(dir + fn2, false));
            out.write("# Rough Align\n");


            for (int v = 0; v
                    < nframes; v++) {
                out.write(v + "\t" + Xrough[v] + "\t" + Yrough[v] + "\n");


            }
            out.close();
            //IJ.write("file " + fn2 + " saved");


        } catch (IOException e) {
        }
    }

    public void saveCurrentVideo(String name) {
        ImagePlus plus = IJ.getImage();
        //name=plus.getTitle();
        SaveDialog sd = new SaveDialog("Save " + name, name, ".tif");
        String dir = sd.getDirectory();
        String fn = sd.getFileName();
        //IJ.log("dir="+dir+" "+fn);
        // Save image
        FileSaver fs = new FileSaver(plus);
        fs.saveAsTiffStack(dir + fn);
    }

    public void saveContrastAdjustment() {
        // select directory and file name
        SaveDialog sd = new SaveDialog(("SaveContrastAdjustment"), ("ContrastAdjustment"), ".tif");
        String dir = sd.getDirectory();
        String fn = sd.getFileName();
        //IJ.log("dir="+dir+" "+fn);
        // Save image
        FileSaver fs = new FileSaver(contrastPlus);
        fs.saveAsTiffStack(dir + fn);
        // save text
        // String fn2 = fn.replaceFirst(".tif", ".txt");


        //   try {
        //  BufferedWriter out = new BufferedWriter(new FileWriter(dir + fn2, false));
        //out.write("# ContrastAdjustment\n");


        //    for (int v = 0; v
        //    < nframes; v++) {
        //     out.write(v + "\t" + Xcontrast[v] + "\t" + Ycontrast[v] + "\n");


        //    }
        //  out.close();
        //IJ.write("file " + fn2 + " saved");


        //    } catch (IOException e) {
        //    }
    }

    public void saveFine() {
        // select directory and file name
        SaveDialog sd = new SaveDialog(("SaveFine"), ("Fine"), ".tif");
        String dir = sd.getDirectory();
        String fn = sd.getFileName();
        //IJ.log("dir="+dir+" "+fn);
        // Save image
        FileSaver fs = new FileSaver(finePlus);
        fs.saveAsTiffStack(dir + fn);
        // save text
        String fn2 = fn.replaceFirst(".tif", ".txt");


        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(dir + fn2, false));
            out.write("# Fine Align\n");


            for (int v = 0; v
                    < nframes; v++) {
                out.write(v + "\t" + Xfine[v] + "\t" + Yfine[v] + "\n");


            }
            out.close();
            //IJ.write("file " + fn2 + " saved");


        } catch (IOException e) {
        }
    }

    public void openRough() {
        OpenDialog od = new OpenDialog("OPENROUGH", "");
        String name = od.getFileName();


        if (name != null) {
            String dir = od.getDirectory();
            Opener op = new Opener();


            if (roughPlus != null) {
                roughPlus.hide();
                roughPlus.flush();


            }
            roughPlus = op.openImage(dir, name);
            //              roughPlus.setTitle(bundle.getString("$ROUGH"));
            roughPlus.setTitle("$ROUGH");
            roughPlus.show();
            roughAlignVideo = roughPlus.getStack();
            nframes = roughAlignVideo.getSize();
            width = roughAlignVideo.getWidth();
            height = roughAlignVideo.getHeight();
            //IJ.log("rough "+width+" "+height+" "+nframes);

            openDrift(dir, name.replace(".tif", ".txt"));

        }
    }

    private void openDrift() {
        OpenDialog od = new OpenDialog("OPEN DRIFT", "");
        String name = od.getFileName();
        String dir = od.getDirectory();

        openDrift(dir, name);
    }

    private void openDrift(String dir, String driftname) {

        Xrough = new double[nframes];
        Yrough = new double[nframes];

        String data;
        String[] nn;
        // reads rough align txt
        try {
            BufferedReader in = new BufferedReader(new FileReader(driftname));
            // header
            data = in.readLine();

            for (int v = 0; v < nframes; v++) {
                data = in.readLine();
                nn = data.split("\t");
                Xfine[v] = Double.parseDouble(nn[1]);
                Yfine[v] = Double.parseDouble(nn[2]);
            }
            in.close();
            IJ.log("file " + driftname + " read");

        } catch (IOException e) {
        }

    }

    public void defineCenter() {
        IJ.setTool(ij.gui.Toolbar.POINT);
        Xrough = new double[nframes];
        Yrough = new double[nframes];
        clickCenter = true;
        cx = width / 2;
        cy = height / 2;
        xmin = width;
        ymin = height;
        xmax = -xmin;
        ymax = -ymin;


    }

    public void roughAlign() {
        findMinMaxRough();
        int newW = (int) (width + Math.abs(xmin) + Math.abs(xmax));
        int newH = (int) (height + Math.abs(ymin) + Math.abs(ymax));

        int tx = (newW - width) / 2;
        int ty = (newH - height) / 2;

        ImageProcessor raw, tmp;
        roughAlignVideo = new ImageStack(newW, newH);

        for (int i = 0; i < nframes; i++) {
            raw = rawVideo.getProcessor(i + 1);
            tmp = raw.createProcessor(newW, newH);
            tmp.copyBits(raw, (int) (tx + Xrough[i]), (int) (ty + Yrough[i]), Blitter.COPY);
            roughAlignVideo.addSlice("", tmp);
        }
        //         roughPlus = new ImagePlus(bundle.getString("$ROUGH"), roughAlignVideo);
        roughPlus = new ImagePlus(("$ROUGH"), roughAlignVideo);
        roughPlus.show();
    }

    public void plotCoordinatesRough() {
        double[] tabt = new double[nframes];

        for (int i = 0; i < nframes; i++) {
            tabt[i] = i;
        }
        Plot plotX = new Plot("X shift", "t", "displ X", tabt, Xrough);
        plotX.show();
        Plot plotY = new Plot("Y shift", "t", "displ Y", tabt, Yrough);
        plotY.show();
    }

    public void plotCoordinatesFine() {
        double[] tabt = new double[nframes];

        for (int i = 0; i < nframes; i++) {
            tabt[i] = i;

        }
        Plot plotX = new Plot("X shift", "t", "displ X", tabt, Xfine);
        plotX.show();
        Plot plotY = new Plot("Y shift", "t", "displ Y", tabt, Yfine);
        plotY.show();

        // fitting
        // X
        CurveFitter fitterX = new CurveFitter(tabt, Xfine);
        fitterX.doFit(CurveFitter.STRAIGHT_LINE);
        IJ.log("X fit : " + fitterX.getResultString());


        double[] parX = fitterX.getParams();


        double[] lineX = new double[nframes];


        for (int i = 0; i
                < nframes; i++) {
            lineX[i] = CurveFitter.f(CurveFitter.STRAIGHT_LINE, parX, tabt[i]);


        }
        plotX.addPoints(tabt, lineX, Plot.CIRCLE);

        // Y
        CurveFitter fitterY = new CurveFitter(tabt, Yfine);
        fitterY.doFit(CurveFitter.STRAIGHT_LINE);
        IJ.log("Y fit : " + fitterY.getResultString());


        double[] parY = fitterY.getParams();

        double[] lineY = new double[nframes];

        for (int i = 0; i
                < nframes; i++) {
            lineY[i] = CurveFitter.f(CurveFitter.STRAIGHT_LINE, parY, tabt[i]);
        }
        plotY.addPoints(tabt, lineY, Plot.CIRCLE);


    }

    /**
     * mouseclicked
     *
     * @param m mouseevent
     */
    public void mouseClicked(MouseEvent m) {
        if (debug) {
            System.out.println("mouse button " + m.getButton());
        }

        if (clickCenter) {
            Roi roi = rawPlus.getRoi();
            Rectangle rect = roi.getBounds();

            int acts = rawPlus.getCurrentSlice();

            Xrough[acts - 1] = cx - rect.x;
            Yrough[acts - 1] = cy - rect.y;

            if (debug) {
                System.out.println("coord " + Xrough[acts - 1] + " " + Yrough[acts - 1] + " " + (acts - 1));
            }

            IJ.wait(100);
            rawPlus.setSlice(acts + 1);

        }
    }

    /**
     * mouseexited
     *
     * @param m mouseevent
     */
    public void mouseExited(MouseEvent m) {
    }

    /**
     * mousePressed
     *
     * @param m mouseevent
     */
    public void mousePressed(MouseEvent m) {
    }

    /**
     * mouse entered
     *
     * @param m mouseevent
     */
    public void mouseEntered(MouseEvent m) {
    }

    /**
     * mouse released
     *
     * @param m mouseevent
     */
    public void mouseReleased(MouseEvent m) {
    }

    private void findMinMaxRough() {
        for (int i = 0; i
                < nframes; i++) {
            if (Xrough[i] < xmin) {
                xmin = Xrough[i];
            }
            if (Xrough[i] > xmax) {
                xmax = Xrough[i];
            }
            if (Yrough[i] < ymin) {
                ymin = Yrough[i];
            }
            if (Yrough[i] > ymax) {
                ymax = Yrough[i];
            }
        }
    }

    private void findMinMaxFine() {
        xmin = Xfine[0];
        xmax = Xfine[0];
        ymin = Yfine[0];
        ymax = Yfine[0];

        for (int i = 0; i
                < nframes; i++) {
            if (Xfine[i] < xmin) {
                xmin = Xfine[i];
            }
            if (Xfine[i] > xmax) {
                xmax = Xfine[i];
            }
            if (Yfine[i] < ymin) {
                ymin = Yfine[i];
            }
            if (Yfine[i] > ymax) {
                ymax = Yfine[i];
            }
        }
    }

    public void definePattern() {
        pattern = roughPlus.getRoi();

        int s = roughPlus.getCurrentSlice();
        ImageProcessor tmp = roughAlignVideo.getProcessor(s);
        tmp.setRoi(pattern);
        patternImage = tmp.crop();
        //     patternPlus = new ImagePlus(bundle.getString("$PATTERN"), patternImage);
        patternPlus = new ImagePlus(("$PATTERN"), patternImage);
        patternPlus.show();
    }

    public void fineAlign(int RadSearch, boolean update) {

        int rw = roughAlignVideo.getWidth();
        int rh = roughAlignVideo.getHeight();

        // correlation video between pattern and rough video
        ImageStack corrStack = new ImageStack(rw, rh);

        ImageProcessor regPattern = patternImage.duplicate();
        ImageProcessor tmp;
        int wp = regPattern.getWidth();
        int hp = regPattern.getHeight();
        FloatProcessor ccproc;
        Rectangle rect = pattern.getBounds();
        int ccx = rect.x + rect.width / 2;
        int ccy = rect.y + rect.height / 2;


        double cc;
        double ccmax;
        int rad;

        int i, x, y, xx = 0, yy = 0;
        Roi roiUpdate = null;

        Xfine = new double[nframes];
        Yfine = new double[nframes];


        if (rect.width < rect.height) {
            rad = rect.height;
        } else {
            rad = rect.width;
        }

        // compute the displacements in X and Y
        for (i = 0; i < nframes; i++) {
            ccmax = 0;
            IJ.showStatus("Processing " + (i + 1) + "/" + nframes);
            tmp = roughAlignVideo.getProcessor(i + 1);
            ccproc = new FloatProcessor(rw, rh);

            for (x = ccx - RadSearch; x <= ccx + RadSearch; x++) {
                for (y = ccy - RadSearch; y < ccy + RadSearch; y++) {
                    cc = correlationValue(tmp, regPattern, x, y, rad);
                    if (cc > ccmax) {
                        ccmax = cc;
                        xx = x;
                        yy = y;
                    }
                    ccproc.setf(x, y, (float) cc);
                }
            }
            Xfine[i] = Xrough[i] + (ccx - xx);
            Yfine[i] = Yrough[i] + (ccy - yy);
            corrStack.addSlice("", ccproc);

            // update pattern
            if (update) {
                roiUpdate = new OvalRoi(xx - wp / 2, yy - hp / 2, wp, hp);
                tmp.setRoi(roiUpdate);
                regPattern = tmp.crop();
                //(new ImagePlus("pattern "+i,regPattern)).show();
            }
        }
        ImagePlus corrPlus = new ImagePlus("Corr", corrStack);
        corrPlus.show();
    }

    private void createFineAlign() {
        ImageProcessor tmp;
        findMinMaxFine();

        boolean isRaw = (rawVideo != null);

        // create fine align video
        int newW = (int) (width + Math.abs(xmin) + Math.abs(xmax));
        int newH = (int) (height + Math.abs(ymin) + Math.abs(ymax));

        int tx = (newW - width) / 2;
        int ty = (newH - height) / 2;

        ImageProcessor raw;
        fineAlignVideo = new ImageStack(newW, newH);


        for (int i = 0; i < nframes; i++) {
            // raw video is present, crop from raw
            if (isRaw) {
                raw = rawVideo.getProcessor(i + 1);
                tmp = raw.createProcessor(newW, newH);
                tmp.copyBits(raw, (int) (tx + Xfine[i]), (int) (ty + Yfine[i]), Blitter.COPY);
            } // crop from rough
            else {
                raw = roughAlignVideo.getProcessor(i + 1);
                tmp = raw.createProcessor(newW, newH);
                tmp.copyBits(raw, (int) (tx + Xfine[i] - Xrough[i]), (int) (ty + Yfine[i] - Yrough[i]), Blitter.COPY);
            }
            fineAlignVideo.addSlice("", tmp);
        }
        finePlus = new ImagePlus("fine align video", fineAlignVideo);
        finePlus.show();
    }

    private void lineAnalysis(int frame) {
        int[] line = new int[width];
        int min;
        int[] arrayMinLine = new int[height];

        ImageProcessor ima = rawVideo.getProcessor(frame);
        for (int j = 0; j < height; j++) {
            ima.getRow(0, j, line, width);
            // compute min max
            min = line[0];
            for (int k = 1; k < width; k++) {
                if (line[k] < min) {
                    min = line[k];
                }
            }
            arrayMinLine[j] = min;
            IJ.log("min for line " + j + " is " + min);
        }
    }

    private void getArraysStatFromRows(ImageProcessor ip) {
        int h = ip.getHeight();

        minrows = new double[h];
        maxrows = new double[h];
        avgrows = new double[h];
        stdrows = new double[h];

        int r;

        ByteStatistics stat;

        for (r = 0; r < h; r++) {
            stat = getLineStatistics(ip, r);
            minrows[r] = (float) stat.min;
            maxrows[r] = (float) stat.max;
            avgrows[r] = (float) stat.mean;
            stdrows[r] = (float) stat.stdDev;
        }
    }

    private ByteStatistics getLineStatistics(ImageProcessor ip, int row) {

        byte[] tab = new byte[ip.getWidth()];
        int tl = tab.length;
        int[] tab2 = new int[ip.getWidth()];

        ByteStatistics stat;
        ByteProcessor img = new ByteProcessor(tl, 1);

        ip.getRow(0, row, tab2, tl);
        for (int i = 0; i < tl; i++) {
            tab[i] = (byte) tab2[i];
        }
        img.setPixels(tab);
        stat = new ByteStatistics(img);

        return stat;
    }

    private void displayHistogram(double[] tab, String name) {
        // histogram of stats rows
        int l = tab.length;
        FloatProcessor img = new FloatProcessor(l, 1);
        float[] tabf = new float[l];
        for (int i = 0; i < tabf.length; i++) {
            tabf[i] = (float) tab[i];
        }
        img.setPixels(tabf);
        ImagePlus plus = new ImagePlus(name, img);
        HistogramWindow hist = new HistogramWindow(name, plus, 255, 0, 255);
        hist.setVisible(true);
    }
//Perrine median lines was temporary removed because of ArrayUtils (for development purposes only)

    private ImageStack analyseAndFilterLines(ImageStack stack, double minavg, double maxavg, double minstd, double maxstd, double minmin, double maxmin, double minmax, double maxmax, int rad) {
        boolean[] goods;
        ImageStack stackres = new ImageStack(stack.getWidth(), stack.getHeight());
        ImageProcessor ip;

        for (int s = 1; s <= stack.getSize(); s++) {
            IJ.showStatus("analysing      frame " + s);
            ip = stack.getProcessor(s);
            ImageProcessor res = ip.duplicate();
            goods = analyzeLines(rawVideo, s, minavg, maxavg, minstd, maxstd, minmin, maxmin, minmax, maxmax);

            for (int r = 0; r < ip.getHeight(); r++) {
                if (!goods[r]) {
                    medianLine(res, r, rad);
                }
            }
            stackres.addSlice("", res);
        }

        return stackres;
    }

    private boolean analyzeLine(ImageStack stack, int frame, int row, double minavg, double maxavg, double minstd, double maxstd, double minmin, double maxmin, double minmax, double maxmax) {
        boolean res;

        float min, max, avg, std;

        ByteStatistics stat;

        stat = getLineStatistics(stack.getProcessor(frame), row);
        min = (float) stat.min;
        max = (float) stat.max;
        avg = (float) stat.mean;
        std = (float) stat.stdDev;
        // test
        if ((minavg < avg) && (avg < maxavg) && (minstd < std) && (std < maxstd) && (minmin < min) && (min < maxmin) && (minmax < max) && (max < maxmax)) {
            res = true;
        } else {
            res = false;
        }
        if (!res) {
            IJ.log("bad " + frame + " " + row + " : " + min + " " + max + " " + avg + " " + std + " ");
        }

        return res;
    }

    private boolean[] analyzeLines(ImageStack stack, int frame, double minavg, double maxavg, double minstd, double maxstd, double minmin, double maxmin, double minmax, double maxmax) {
        int h = stack.getHeight();
        boolean[] res = new boolean[h];


        for (int r = 0; r < h; r++) {
            res[r] = analyzeLine(stack, frame, r, minavg, maxavg, minstd, maxstd, minmin, maxmin, minmax, maxmax);
        }
        return res;
    }
//    Perrine: same bug ArrayUtils

    private void medianLine(ImageProcessor img, int row, int rad) {
        ArrayUtil tabu;

        // get the line into a tabutil 
        int[] tab = new int[img.getWidth()];
        int tl = tab.length;
        int[] tabres = new int[tl];
        img.getRow(0, row, tab, tl);
        double[] tab2 = new double[tl];

        for (int i = 0; i < tl; i++) {
            tab2[i] = (float) tab[i];
        }
        tabu = new ArrayUtil(tab2);

        int s0, s1;
        ArrayUtil tabu2;
        for (int i = 0; i < tl; i++) { // neighborhood on line 
            s0 = Math.max(0, i - rad);
            s1 = Math.min(tl - 1, i + rad);
            tabu2 = tabu.getSubTabUtil(s0, s1 - s0 + 1);
            tabres[i] = (int) tabu2.median();

        }
        img.putRow(0, row, tabres, tl);
    }

    private ImageStack adjustLinesMinMax(ImageStack stack, int minvalue, int maxvalue) {
        ImageProcessor ip;
        int[] tab = new int[stack.getWidth()];
        int[] tab2;

        ImageStack stackres = new ImageStack(stack.getWidth(), stack.getHeight());

        for (int s = 1; s <= stack.getSize(); s++) {
            IJ.showStatus("adjusting frame " + s);
            ip = stack.getProcessor(s);
            ImageProcessor res = ip.createProcessor(ip.getWidth(), ip.getHeight());

            for (int r = 0; r < ip.getHeight(); r++) {
                ip.getRow(0, r, tab, tab.length);
                tab2 = changeMinMax(minvalue, maxvalue, tab);
                res.putRow(0, r, tab2, tab.length);
            }
            stackres.addSlice("", res);
        }
        return stackres;
    }

    private int[] changeMinMax(int min, int max, int[] tab) {
        int[] tab2 = new int[tab.length];

        // find min max
        int mi = tab[0];
        int ma = tab[0];

        for (int i = 1; i < tab.length; i++) {
            if (tab[i] > ma) {
                ma = tab[i];
            }
            if (tab[i] < mi) {
                mi = tab[i];
            }
        }

        // set new value 
        double diff = (double) (max - min) / (double) (ma - mi);
        //IJ.log("" + mi + " " + ma + " " + min + " " + max + " " + diff);
        double val;
        double newv;
        for (int i = 0; i < tab.length; i++) {
            val = tab[i];
            newv = ((val - mi) * diff) + min;
            tab2[i] = (int) newv;
        }

        return tab2;
    }
}
