package tracking;

import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;
import ij.text.TextWindow;

public class GlobalTracking_ implements PlugIn {

    /**
     * Interface with ImageJ: run tracking as a plugin
     *  %TO DO Parameter to give: 
     * ResultTable  Tracking (MovieStack,PathName, Textfilename, start_frame,last_frame, max_neighbor, maximum_disapearance_time, imagesize,scansize, framerate
     *  ImageProcessor Moviestack
     *  int start_frame (inclusive)
     *  int last_frame (inclusive)
     *  String PathName
     *  String Textfilname test_1.txt or whatever file showing the pattern
     *  int maximum_dispearance_time
     *  float max_neighbor in pixels;
     *  ResultTable Correction (Resultable, imapeprocessormovieStack);
     *  Calulate button (Resulttablefromtracking,scansize,imagesize,framerate)
     *  int scan size= size of the original image in angstrom
     *  int imagesize size of the image in pixel)
     *  int framerate 
     *  replace by the time in ms, pixel corrdinates in angstrom
     *  Tracking will also displayed tracking on the movie and get back a text file.
     */
    public void run(String arg) {
        // read the data
        double max_neighbor = 50.0;
        int max_disapearance = 2;
        int nbiter=0;
        DirectoryChooser DC = new DirectoryChooser("Choose the directory containing directories to be processed");
        String path = DC.getDirectory();
        double convert = 5.5; // size of pixel in angstrom
        double framerate = 100; //in ms
        String filename = "test-1.txt";
        int first = 1;
        int last = 10;
        //new Tracking((float)max_neighbor);
        Tracking AFM_tracking = new Tracking(path, filename, first, last, max_neighbor, max_disapearance,nbiter);
        TextWindow resultTracking = AFM_tracking.Start_Tracking();
        TextWindow resultTrackinginrealunit = AFM_tracking.Calculate(resultTracking, convert, framerate);
        // display tracking ask thomas
    }
}
