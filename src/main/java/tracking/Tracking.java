package tracking;

import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.io.*;
/**
 * author: Perrine Paul-Gilloteaux
 * TO DO: plotting of results to chech the parameters: by creating a stack with one color by associated particles
 * TO DO: pb of particles associated twice in the same frame
 * TO DO: add the non association option.$
 * TO DO: read the file
 */
import ij.text.TextWindow;

import ij.IJ;
import java.awt.Color;
import java.awt.Font;
import java.io.*;
//import java.util.Scanner;
import java.util.*;

import Jama.*;

/**
 * @author Perrine
 * @return This class mainly aim at performing the initialization of data and to run the simulated annealing.
 * TO DO: Adding a column for the line number
 * TO DO tuning automatically the number of iterations
 *  modified date: 01/02/2012
 */
public class Tracking {

    /**
     * frames load all frames to be processed
     */
    private TrackFrame[] frames;
    //public ObjecttoTrack [] objects;
    /**
     *  Particles_COUNT is the maximum number of particles in a frame.
     */
    private int Particles_COUNT_Max;
    /**
     * Frames_COUNT is the total number of frames to be processed
     */
    private int Frames_COUNT;
    /**
     * START_TEMPERATURE is the initial Temperature used
     * in the simulated annealing algorithm
     */
    public static final double START_TEMPERATURE = 100; //was 1000
    /**
     * TEMPERATURE_DELTA is the change in each iteration (T=temperature_Delta*T)
     */
    public static final double TEMPERATURE_DELTA = 0.9;
	
    /**
     * SIGMAmatrix is the covariance matrix
     */
    private Matrix SIGMAmatrix;
    /**
     * worker is the process associated to Tracking in order to
     *  optimize the associations between all particles in all frames
     */
    private SimulateAnnealing worker = null;
    /**
     * started is used in order to indicate that the process
     * started as a background thread
     */
    protected boolean started = false;
    public static float MaxNeighbourood;
    public static int Max_disapearance;
    public static int nbiteration ;
    boolean debug = true;

    /**
     * @param frames_COUNT the frames_COUNT to set
     */
    public void setFrames_COUNT(int frames_COUNT) {
        Frames_COUNT = frames_COUNT;
    }

    /**
     * @return the frames_COUNT
     */
    public int getFrames_COUNT() {
        return Frames_COUNT;
    }

    /**
     * @param particles_COUNT_Max the particles_COUNT_Max to set
     */
    public void setParticles_COUNT_Max(int particles_COUNT_Max) {
        Particles_COUNT_Max = particles_COUNT_Max;
    }

    /**
     * @return the particles_COUNT_Max
     */
    public int getParticles_COUNT_Max() {
        return Particles_COUNT_Max;
    }

    /**
     * @param frames the frames to set
     */
    public void setFrames(TrackFrame[] frames) {
        this.frames = frames;
    }

    /**
     * @return the frames
     */
    public TrackFrame[] getFrames() {
        return frames;
    }

   

    /**
     * This constructor will be used to initialize tracking in the AFM plugin
     *  initialize the covariance
     *  matrix SIGMAmatrix . PROCESS WILL BE STARTED BY CALLING start_tracking method, then there is a calulculate method.
     *  @param no parameters for now, but the content of the covariance matrix
     *  is hard coded and this should be changed.
     * called from VideoInterface1:
     *  Tracking AFM_tracking = new Tracking(trackingdirectory, trackingfilename, initialPvalue, lastPvalue, distance, disappearancetime);
     */
    public Tracking(String path, String filename, int start_frame, int last_frame, double myMaxNeighbourood, int max_disapearance, int nbiterat) {
        Tracking.MaxNeighbourood = (float) myMaxNeighbourood;
        Tracking.Max_disapearance = max_disapearance;
        Tracking.nbiteration=nbiterat;
        double sigmax = Math.sqrt(Tracking.MaxNeighbourood);
        double sigmay = Math.sqrt(Tracking.MaxNeighbourood);
        double sigmadata1 = 10;
        double sigmadata2 = 10;
        double covdata1data2 = 0;
        SIGMAmatrix = createSigma(sigmax, sigmay, sigmadata1, sigmadata2, covdata1data2);
        InitializeAFMPluginTracking(path, filename, start_frame, last_frame);
    }

    /**
     * Method to be called from the AFM tracking Panel to launch tracking
     * @return textWindow the results table (tag, frame, x, y, data 1 , data2, data3)
     * @throws InterruptedException 
     */
    public TextWindow Start_Tracking() {
        /**
         * start up the background thread
         *
         */
        started = true;

        if (worker != null) {
            worker = null;
        }
        worker = new SimulateAnnealing(this);
        //worker.setPriority(Thread.MAX_PRIORITY);
        worker.run(); 
        //worker.join();
      /*  try {
        	System.out.println("wait");
			worker.join();
			System.out.println("end of wait");
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("catched e");
		}*/
		
        //TextWindow tw = WriteObjectsTracked("Results Tracking");
        TextWindow tw = WriteTrajectories("Results Tracking");
        return tw;

    }

    /**
     * input: tracking restult from start_tracking, pixel size in angstrom, frame rate in ms
     * Method to be called from the AFM tracking panel to save a new file where pixels coordinates are in angstrom and time in ms.
     * NOT DONE YET
     */
    public TextWindow Calculate(TextWindow TrackingResults, double pixelsizeAngstrom, double framerate) {
        TextWindow tw = new TextWindow("", "", "", 500, 500);
        return tw;

    }

    /**
     * Initilization of the covariance matrix
     * @return Matrix a covariance matrix
     */
    private Matrix createSigma(double sigmax, double sigmay, double sigmadata1, double sigmadata2, double covdata1data2) {

        int n = 4;
        double[][] M = new double[n][n];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                M[i][j] = 0;
            }
        }
        M[0][0] = 1 / (sigmax * sigmax);
        M[1][1] = 1 / (sigmay * sigmay);
        M[2][2] = sigmadata1 * sigmadata1 / (sigmadata2 * sigmadata2 * sigmadata1 * sigmadata1 - covdata1data2 * covdata1data2);
        M[3][3] = sigmadata2 * sigmadata2 / (sigmadata2 * sigmadata2 * sigmadata1 * sigmadata1 - covdata1data2 * covdata1data2);
        M[2][3] = -covdata1data2 / (sigmadata2 * sigmadata2 * sigmadata1 * sigmadata1 - covdata1data2 * covdata1data2);
        M[3][2] = -covdata1data2 / (sigmadata2 * sigmadata2 * sigmadata1 * sigmadata1 - covdata1data2 * covdata1data2);
        return new Matrix(M);
    }

    
    /**
     *  Initializing frames[] from AFM software (Mohamad, Thomas and Simon Soft) file
     *  <p> it will load all txt file from ImageJ with the same suffix containing on each line
     *   started with peak
     *  <p> - The file name... test-1, -2, -3, etc corresponds obviously to the
     * frame number!
     *  <p>- The "nbpeak=57" indicates how many molecules the algorithm has found
     * in the image. This varies obviously given the number of molecules that
     *	are at a certain moment within the image, but also because the image
     * quality is not constant, so the algorithm does not always find all the
     * molecules, I guess - but we can not do it better.
     * <p>- "peak=0" is the peak number in the frame. (does not necessarily
     * correspond to "molecule=peak=0" in the next frame)
     * <p>- "x=186" is the X-coordinate
     * <p>- "y=172" is the Y-coordinate
     * <p>- "ang=-35.0" is the rotation angle with respect to the reference.
     * Attention: OmpF is a trimer. We have only analyzed 120 degrees of
     * angular range. "-35" is also "85" and "205" or "-155" and "-275". The
     * 120 degrees redundancy has also to be taken into account when
     * considering interactions of neighboring molecules.
     * <p>- "corr=0.540151" is the cross-correlation value. Sort of quality
     * value of the image in which the molecules is and the molecule itself.
     * <p>- "is=0.60931194" is the internal symmetry value. Sort of quality
     * value of the molecule itself.

     *   <p> for now there is only 2 data in addition to the position, data2 will be used as data3 in the feature vector.
     * <p> Frames_Count is initialized with the number of frames,
     *
     *  <p> frames is initialized as an array of frames_count frames containing the read objects features.
     *  */
  
 


    /**
     * Initialize tracking attributes from parameters given in the AFM plugin
     * @param path
     * @param filename
     * @param first
     * @param last
     */
    private void InitializeAFMPluginTracking(String path, String filename, int first, int last) {
        String suffix = ".txt";
        int possuffix = filename.indexOf(suffix);
        //String FileNamePrefix = listFiles[indexfile].substring(0, listFiles[indexfile].length() - (digit + suffix.length() + 1));
        String FileNamePrefix = filename.substring(0, possuffix-1);
        int digit = 1; // number of digits for padding with zeros found automatically
        File abstractpath = new File(path);
        String[] listFiles = abstractpath.list();
        int nbfiles = listFiles.length;
        if (debug) {
            IJ.log("Init Tracking : " + nbfiles + " files in the directory. Only Frames indicated will be processed");
        }
       
        int count = 0;
        Float[] d_data = new Float[6 * 500 * 200]; // For the moment max number of lines= 1000
        float test=Float.parseFloat(filename.substring(possuffix-digit,possuffix)); 
       while (test!=-1){
    	  
    	   try{
    	  digit=digit+1;
    	   test=Float.parseFloat(filename.substring(possuffix-digit,possuffix)); 
    	   }
    	   catch  (NumberFormatException e){
    		   digit=digit-1;
    		    FileNamePrefix = filename.substring(0, possuffix-digit);
    		    break;
    	   }
    	if (test==-1)   
    	  digit=digit-1;
       }
        if (debug) {
            IJ.log("Init Tracking : filename " + FileNamePrefix);
        }
        for (int framenumber = first; framenumber <= last; framenumber++) {
            try {
                String newframenumber = paddingwithZeros(framenumber, digit);
                //File file = new File(path + File.separator + FileNamePrefix + newframenumber + suffix);
                File file = new File(path + File.separator + FileNamePrefix + newframenumber + suffix);
                if (debug) {
                    IJ.log("Init Tracking : " + file.getName() + " analysed");
                }
                if (file.canRead()) {
                    Scanner fileScan = new Scanner(file);

                    fileScan.useDelimiter("\\s*peak=\\s*");
                    String linetobesplit = fileScan.next(); //two split to ignore at the beginning of these files.
                    linetobesplit = fileScan.next();


                    while (fileScan.hasNext()) {
                        linetobesplit = fileScan.next();
                        Scanner lineScan = new Scanner(linetobesplit).useDelimiter("\\s*=\\s*");
                        String tmp = lineScan.next(); //'0 x'

                        //we want to copy first the particle label, and its frame number
                        Scanner infoScan = new Scanner(tmp); //space default delimiter
                        d_data[count] = Float.parseFloat(infoScan.next()); //next to the equal label of the peak
                        count++;
                        d_data[count] = (float) framenumber;
                        count++;
                        for (int k = 0; k < 3; k++) { // we just want to get x, y and angle.
                            tmp = lineScan.next();
                            infoScan = new Scanner(tmp); //space default delimiter
                            d_data[count] = Float.parseFloat(infoScan.next()); //next to the equal
                            count++;
                            infoScan.close();
                        }
                        d_data[count] = (float) 0.0; // data 2 will not be used
                        count++;
                        lineScan.close();
                    }

                    fileScan.close();

                }//end if file can read.
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Then we read the number of frames
        int framesnumber = 1;
        for (int i = 1; i < count - 6; i += 6) {
            if ((d_data[i].floatValue()) != d_data[i + 6].floatValue()) {
                framesnumber++;
            }
        }

        this.setFrames_COUNT(framesnumber);
        this.setParticles_COUNT_Max(0); // initilization, will be replaced by the biggest number of particle in one frame
        // Then initialize each frame with particle objects.

        setFrames(new TrackFrame[this.getFrames_COUNT()]);
        int i = 0;
        int nbparticles = 1;

        for (int f = 0; f < this.getFrames_COUNT(); f++) {
            // count the particles.

            nbparticles = 1;
            int iinit = i;
            while ((i < count - 6) && (d_data[i + 1].floatValue() == d_data[i + 1 + 6].floatValue())) {
                i += 6;
                nbparticles++;
            }
            i += 6;
            ObjecttoTrack[] objects = new ObjecttoTrack[nbparticles];
            if (nbparticles > this.getParticles_COUNT_Max()) {
                this.setParticles_COUNT_Max(nbparticles);
            }
            for (int ip = 0; ip < nbparticles; ip++) {
                objects[ip] = new ObjecttoTrack(
                        (float) d_data[iinit + ip * 6 + 2], //x
                        (float) d_data[iinit + ip * 6 + 3], //y
                        f, // could also be d_data[i]
                        (float) 0,
                        (float) 0,
                        (float) d_data[iinit + ip * 6 + 4],// angles
                        SIGMAmatrix,
                        (int) Math.floor((double) d_data[iinit + ip * 6]));// label
            }
            getFrames()[f] = new TrackFrame(objects, f);
        }
        IJ.log("Init Tracking done " );
        return;
    }

    /**
     * padding with zeros 
     * @param framenumber
     * @param digit
     * @return the framenumber padded with 0 as a string
     * (ex: 4 becomes "004" if digit=3
     */
    private String paddingwithZeros(int framenumber, int digit) {
        String newframenumber = IJ.d2s(framenumber,0); 
        while (newframenumber.length() < digit) {
            newframenumber = "0" + newframenumber;
        }
        return newframenumber;
    }

   

    /**
     * This method write a result table with the object label, old tag, the frame number, x and y and data 1 and 2.
     * <p>
     * The object labels are the tracked ones, i.e a same object keep the same tag.
     * @param title is the name of the imaageJ result windows that will be created.
     * order is a worker attribute: it contains the trajectory number by frame.
     * A simple example tyo better figure it:
     * We have 3 frames (0, 1, 2) : frame 0 has 1 object, frame 1 has 2 object, frame 2 has 1 objects
     * oject frame x, y 
     *  0 0 10 15 ..
     *  0 1 50 50 ..
     *  1 1 11 15 ..
     *  0 2 51 50 ..
     * 
     *  Expected trajectories (object 0 in frame 0 becomes object 1 in frame 1, and then disappears in frame 2, onject 0 in frmae 1 becomes object 0 in frame 2)
     *  line is the trajectory number, object label the former label of the molecules, Frame number, x,y and 3 data not used (such as angle)
     *  0 0 0 10 15 ..
     *  0 1 1 11 15
     *  1 1 1 11 15
     *  0 -1  2 0 0
     *  1 0 2 51 50
     *  
     *  order then contain the following: 
     *  order[0]= [0 -1]
     *  order[1]=[1 0]
     *  order[2]=[-1 0]
     *
     */
    public TextWindow WriteObjectsTracked(String title) {
        float[] x = new float[this.getParticles_COUNT_Max()];
        float[] y = new float[this.getParticles_COUNT_Max()];
        float[] data1 = new float[this.getParticles_COUNT_Max()];
        float[] data2 = new float[this.getParticles_COUNT_Max()];
        float[] data3 = new float[this.getParticles_COUNT_Max()];
        float[] nbframes = new float[this.getParticles_COUNT_Max()];
        float[] label = new float[this.getParticles_COUNT_Max()];
        //String title = "Tracking Results Table";
        String headings = "line\tObject label\tFrame Number\tx\ty\tdata 1 \tdata 2 \t data 3(not used for tracking)";
        TextWindow tw = new TextWindow(title, headings, "", 500, 500);
        int line=0;
        for (int f = 0; f < this.getFrames_COUNT(); f++) {
        	line=0;
            x = new float[this.getParticles_COUNT_Max()]; // x-coordinates
            y = new float[this.getParticles_COUNT_Max()]; // y-coordinates
            data1 = new float[this.getParticles_COUNT_Max()];
            data2 = new float[this.getParticles_COUNT_Max()];
            data3 = new float[this.getParticles_COUNT_Max()];
            nbframes = new float[this.getParticles_COUNT_Max()];
            label = new float[this.getParticles_COUNT_Max()];
            for (int i = 0; i < this.getParticles_COUNT_Max(); i++) {
            	
                label[i] = getFrames()[f].getObject(worker.order[f][i]).getlabel(); // old label
                x[i] = getFrames()[f].getObject(worker.order[f][i]).getx();
                y[i] = getFrames()[f].getObject(worker.order[f][i]).gety();
                nbframes[i] = getFrames()[f].getObject(worker.order[f][i]).getframe();
                data1[i] = getFrames()[f].getObject(worker.order[f][i]).getdata1();
                data2[i] = getFrames()[f].getObject(worker.order[f][i]).getdata2();
                data3[i] = getFrames()[f].getObject(worker.order[f][i]).getdata3();
                tw.append(line+"\t"+IJ.d2s(label[i]) + "\t" + IJ.d2s(nbframes[i]) + "\t" + IJ.d2s(x[i], 2) + "\t" + IJ.d2s(y[i]) + "\t" + IJ.d2s(data1[i]) + "\t" + IJ.d2s(data2[i]) + "\t" + IJ.d2s(data3[i]));
                line=line+1; //new trajectory label
            }
        }
        return(tw);
    }

    public TextWindow WriteTrajectories(String title) {
        
        //String title = "Tracking Results Table";
        String headings = "line\tObject label\tFrame Number\tx\ty\tdata 1 \tdata 2 \t data 3(not used for tracking)";
        TextWindow tw = new TextWindow(title, headings, "", 500, 500);
                   
        float label, nbframes, x, y, data1,data2,data3 ;
            for (int i = 0; i < worker.listofTrajectories.size(); i++) // i is then the trajectory number.
            {
            	for (int j=0;j<worker.listofTrajectories.get(i).size();j++)
            	{
            		label = worker.listofTrajectories.get(i).get(j).getlabel(); // old label
                x = worker.listofTrajectories.get(i).get(j).getx();
                y = worker.listofTrajectories.get(i).get(j).gety();
                nbframes = worker.listofTrajectories.get(i).get(j).getframe();
                data1 = worker.listofTrajectories.get(i).get(j).getdata1();
                data2 = worker.listofTrajectories.get(i).get(j).getdata2();
                data3 = worker.listofTrajectories.get(i).get(j).getdata3();
                tw.append(i+"\t"+IJ.d2s(label) + "\t" + IJ.d2s(nbframes) + "\t" + IJ.d2s(x, 2) + "\t" + IJ.d2s(y) + "\t" + IJ.d2s(data1) + "\t" + IJ.d2s(data2) + "\t" + IJ.d2s(data3));
               
            }
        }
        return(tw);
    }


}
