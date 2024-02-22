package tracking;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.NewImage;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.measure.ResultsTable;
import ij.plugin.frame.PlugInFrame;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import ij.util.Tools;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;


public class Visu_Tracking_Basedon_FCordelieresPlugin extends PlugInFrame implements ActionListener {
    /*Manual tracking v2.0, 15/06/05
    Fabrice P Cordeli�res, fabrice.cordelieres at curie.u-psud.fr
New features:
2D centring correction added
Directionality check added
Previous track files may be reloaded
3D features added (retrieve z coordinates, quantification and 3D representation as VRML file)
 */

    
   /*
    * Modified by Perrine for VideoAFM 
    */


    //Interface related variables-----------------------------------------------
    static Frame instance;
    int dotsize = 5; // Drawing parameter: default dot size
    double linewidth = 1; // Drawing parameter: default line width
    int fontsize = 12; // Drawing parameter: default font size
    Color[] col = {Color.blue, Color.green, Color.red, Color.cyan, Color.magenta, Color.yellow, Color.white}; //Values for color in the drawing options
    //Universal variables-------------------------------------------------------
    int i;
    int j;
    int k;
    int l;
    int m;
    int n;
    String txt;
    Font bold = new Font("", 3, 12);
    Panel panel;
    //Drawing
    Button butOvd;
    Button butOvl;
    Button butOvdl;
    Button butOverdots;
    Button butOverlines;
    Button butOverboth;
    Checkbox checkText;


    //Image related variables---------------------------------------------------
    ImagePlus img;
    String imgtitle;
    int Width;
    int Height;
    int Depth;
    int Slice;
    String SliceTitle;
    ImageCanvas canvas;
    ImagePlus ip;
    ImageStack stack;
    ImageWindow win;
    StackConverter sc;


    //Dialog boxes--------------------------------------------------------------
    GenericDialog gd;
    GenericDialog gd1;
    GenericDialog gd2;

    OpenDialog od;
    SaveDialog sd;
    String FileName; // Filename with extension
    String File; // Filename without extension
    String dir; // Directory


    ResultsTable rt;


    //Load Previous Track File related variables--------------------------------
    BufferedReader in; //Input file
    String line; //Input line from the input file
    StringTokenizer Token; //used to separate tab delimited values in the imported file


    public Visu_Tracking_Basedon_FCordelieresPlugin() {

        //Interface setup ------------------------------------------------------
        super("Drawing Tracking ");
        instance = this;
    }

    public void Visu_Tracking(String rttfile) {

        rt = new ResultsTable();

        try {
            if (i == 0) in = new BufferedReader(new FileReader(rttfile));
            in.readLine();
            while ((line = in.readLine()) != null) {
                i = 0;
                Token = new StringTokenizer(line);
                rt.incrementCounter();
                while (Token.hasMoreTokens()) {
                    rt.addValue(i, Tools.parseDouble(Token.nextToken()));
                    i++;
                }
            }
        } catch (IOException f) {
            IJ.error("Error...");
        }

        panel = new Panel();
        panel.setLayout(new GridLayout(0, 3));
        panel.setBackground(SystemColor.control);

        //---------------------------------Drawing
        panel.add(new Label());
        Label title2 = new Label();
        title2.setText("Drawing :");
        title2.setFont(bold);
        panel.add(title2);
        panel.add(new Label());

        //***
        butOvd = new Button("Dots");
        butOvd.addActionListener(this);
        panel.add(butOvd);
        butOvl = new Button("Progressive Lines");
        butOvl.addActionListener(this);
        panel.add(butOvl);
        butOvdl = new Button("Dots & Lines");
        butOvdl.addActionListener(this);
        panel.add(butOvdl);

        //***
        butOverdots = new Button("Overlay Dots");
        butOverdots.addActionListener(this);
        panel.add(butOverdots);
        butOverlines = new Button("Overlay Lines");
        butOverlines.addActionListener(this);
        panel.add(butOverlines);
        butOverboth = new Button("Overlay Dots & Lines");
        butOverboth.addActionListener(this);
        panel.add(butOverboth);
        add(panel, BorderLayout.CENTER);
        pack();
        setVisible(true);

        img = WindowManager.getCurrentImage();
        imgtitle = img.getTitle();
    }


    public void actionPerformed(ActionEvent e) {
        // Buttons Dots, Lines or Dots & Lines pressed--------------------------------------------------
        if (e.getSource() == butOvd || e.getSource() == butOvl || e.getSource() == butOvdl) {


            if (e.getSource() == butOvd) txt = "Dots ";
            if (e.getSource() == butOvl) txt = "Progressive Lines ";
            if (e.getSource() == butOvdl) txt = "Dots & Lines ";
            ip = NewImage.createRGBImage(txt + imgtitle, img.getWidth(), img.getHeight(), img.getStackSize(), 1);
            ip.show();
            stack = ip.getStack();
            if (e.getSource() == butOvd || e.getSource() == butOvdl) Dots();
            if (e.getSource() == butOvl || e.getSource() == butOvdl) ProLines();
            IJ.showStatus(txt + imgtitle + " Created !");
            return;
        }

        // Button Overlay Dots, Overlay Lines or Overlay Dots & Lines pressed------------------------------------------
        if (e.getSource() == butOverdots || e.getSource() == butOverlines || e.getSource() == butOverboth) {

            if (e.getSource() == butOverdots) txt = "Overlay Dots ";
            if (e.getSource() == butOverlines) txt = "Overlay Progressive Lines ";
            if (e.getSource() == butOverboth) txt = "Overlay Dots & Lines ";
            ip = img.duplicate();
            ip.show();
            (new StackConverter(ip)).convertToRGB();
            stack = ip.getStack();
            if (e.getSource() == butOverdots || e.getSource() == butOverboth) Dots();
            if (e.getSource() == butOverlines || e.getSource() == butOverboth) ProLines();
            IJ.showStatus(txt + imgtitle + " Created !");
            return;
        }
    }


    void Dots() {
        dotsize = (int) 5;

        for (i = 0; i < (rt.getCounter()); i++) {
            int nbtrack = (int) rt.getValueAsDouble(0, i);
            int objectlabel = (int) rt.getValueAsDouble(1, i);
            int nbslices = (int) rt.getValueAsDouble(2, i) + 1;
            int cx = (int) rt.getValueAsDouble(3, i);
            int cy = (int) rt.getValueAsDouble(4, i);
            if (objectlabel != -1) {
                j = nbtrack % 6;
                ImageProcessor ip = stack.getProcessor(nbslices);
                ip.setColor(col[j]);
                ip.setLineWidth(dotsize);
                ip.drawDot(cx, cy);

                Font font = new Font("SansSerif", Font.PLAIN, (int) 10);
                ip.setFont(font);
                ip.drawString("" + nbtrack, cx + (dotsize - 5) / 2, cy - (dotsize - 5) / 2);
            }
        }
    }


    void ProLines() {

        linewidth = 1;
        j = 0;
        k = 1;
        int cxold = 0;
        int cyold = 0;

        int cx = 0;
        int cy = 0;

        int nbtrack;
        int currentslice;


        //for each track
        for (int sliceplotted = 1; sliceplotted <= stack.getSize(); sliceplotted++) {
            int i = 0;
            while (i < rt.getCounter() - 1) {
                nbtrack = (int) rt.getValueAsDouble(0, i);
                j = nbtrack % 6;
                currentslice = (int) rt.getValueAsDouble(2, i) + 1;
                cxold = (int) rt.getValueAsDouble(3, i);
                cyold = (int) rt.getValueAsDouble(4, i);

                i++;
                while ((currentslice < sliceplotted) && ((i < rt.getCounter()) && (rt.getValueAsDouble(0, i) == nbtrack))) {

                    cx = (int) rt.getValueAsDouble(3, i);
                    cy = (int) rt.getValueAsDouble(4, i);
                    currentslice = (int) rt.getValueAsDouble(2, i) + 1;
                    ImageProcessor ip = stack.getProcessor(sliceplotted);
                    ip.setColor(col[j]);
                    ip.setLineWidth((int) linewidth);
                    ip.drawLine(cxold, cyold, cx, cy);
                    cxold = cx;
                    cyold = cy;
                    i++;
                }
            }
        }
    }
        /*for (countTrack=0;countTrack<rt.getCounter();countTrack++)
        {
        	 int k=0;
        	 j=countTrack%6;
        	  //for each line of the file, we test if the trajectory is here

        	 for (i=0; i<rt.getCounter(); i++) {
        			 nbtrack=(int) rt.getValueAsDouble(0,i);
        			if (nbtrack==countTrack){
        				
        				lastslice=(int) rt.getValueAsDouble(2,i)+1;
        			}
        	 }
        	
        	 //last slice is the higher frame
        	 for (nbslice=1; nbslice<=lastslice; nbslice++)
        	
        	 {
        		 k=0;
        		 for (i=0;i<rt.getCounter();i++)
        		 {
        		 	nbtrack=(int) rt.getValueAsDouble(0,i);
        		 	objectlabel=(int)  rt.getValueAsDouble(1,i);
        		 	currentslice=(int) rt.getValueAsDouble(2,i)+1;
        		 	if (objectlabel!=-1)
        		 	{
        		 		if ((nbtrack==countTrack)&&(currentslice<=nbslice))
        		 			{
        		 				k++;
        	 			
        		 				cx=(int) rt.getValueAsDouble(3,i);
        		 				cy=(int) rt.getValueAsDouble(4,i);
        		 				if (k==1)
        		 				{ //first appearance
        		 					cxold=cx;
        		 					cyold=cy;
        		 				}
        		 				else
        		 				{
        		 					ImageProcessor ip= stack.getProcessor(nbslice);
        		 					ip.setColor(col[j]);
        		 					ip.setLineWidth((int) linewidth);
        		 					ip.drawLine(cxold, cyold, cx, cy);
        		 					cxold=cx;
        		 					cyold=cy;
        		 				}
               	
        		 			}
        		 	}
        		 }
        			
          
        	 }
        }
    }*/
}
        
  

    
  
    
   
    
   
