import ij.plugin.*;


/**
 *  Description of the Class
 *
 * @author     thomas
 * @created    31 aout 2005
 */
public class AFMJ_ implements PlugIn {

	/**
	 *  Main processing method for the AFMJ_ object
	 *
	 * @param  arg  plugin parameter
	 */
	public void run(String arg) {

		new afm.AFMInterface();
		new video.VideoInterface1 ();
	}

}


