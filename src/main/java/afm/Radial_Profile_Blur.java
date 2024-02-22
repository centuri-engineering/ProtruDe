package afm;

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import ij.measure.Calibration;
import java.awt.*;
import java.util.*;

/**
 *  Description of the Class
 *
 * @author     thomas
 * @created    28 juin 2005
 */
public class Radial_Profile_Blur implements PlugInFilter {

	ImagePlus imp;
	boolean canceled = false;
	double X0;
	double Y0;
	double mR;
	Rectangle rct;
	int nBins = 100;
	//static boolean doNormalize = true;
	static boolean useCalibration = false;
	PlotWindow pw;


	/**
	 *  Description of the Method
	 *
	 * @param  arg  Description of the Parameter
	 * @param  imp  Description of the Parameter
	 * @return      Description of the Return Value
	 */
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL + NO_UNDO + ROI_REQUIRED;
	}


	/**
	 *  Main processing method for the Radial_Profile object
	 *
	 * @param  ip  Description of the Parameter
	 */
	public void run(ImageProcessor ip) {
		setXYcenter();
		IJ.makeOval((int) (X0 - mR), (int) (Y0 - mR), (int) (2 * mR), (int) (2 * mR));
		//doDialog();
		IJ.makeOval((int) (X0 - mR), (int) (Y0 - mR), (int) (2 * mR), (int) (2 * mR));
		imp.startTiming();
		if (canceled) {
			return;
		}
		ip.smooth();
		imp.unlock();
		doRadialDistribution(ip);
	}


	/**
	 *  Sets the xYcenter attribute of the Radial_Profile object
	 */
	private void setXYcenter() {
		//rct = imp.getRoi().getBoundingRect();
		rct=imp.getRoi().getBounds();
		X0 = (double) rct.x + (double) rct.width / 2;
		Y0 = (double) rct.y + (double) rct.height / 2;
		mR = (rct.width + rct.height) / 4.0;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  ip  Description of the Parameter
	 */
	private void doRadialDistribution(ImageProcessor ip) {
		nBins = (int) (mR * 2);
		int thisBin;
		float[][] Accumulator = new float[2][nBins];
		double R;
		double xmin = X0 - mR;
		double xmax = X0 + mR;
		double ymin = Y0 - mR;
		double ymax = Y0 + mR;
		for (double i = xmin; i < xmax; i++) {
			for (double j = ymin; j < ymax; j++) {
				R = Math.sqrt((i - X0) * (i - X0) + (j - Y0) * (j - Y0));
				thisBin = (int) Math.floor((R / mR) * (double) nBins);
				if (thisBin == 0) {
					thisBin = 1;
				}
				thisBin = thisBin - 1;
				if (thisBin > nBins - 1) {
					thisBin = nBins - 1;
				}
				Accumulator[0][thisBin] = Accumulator[0][thisBin] + 1;
				Accumulator[1][thisBin] = Accumulator[1][thisBin] + ip.getPixelValue((int) i, (int) j);
			}
		}
		Calibration cal = imp.getCalibration();
		if (cal.getUnit() == "pixel") {
			useCalibration = false;
		}
		if (useCalibration) {
			for (int i = 0; i < nBins; i++) {
				Accumulator[1][i] = Accumulator[1][i] / Accumulator[0][i];
				Accumulator[0][i] = (float) (cal.pixelWidth * mR * ((double) (i + 1) / nBins));
			}
			//pw = new PlotWindow("Radial Profile Plot", "Radius ["+cal.getUnits()+"]", "Normalized Integrated Intensity",  Accumulator[0], Accumulator[1]);
		} else {
			for (int i = 0; i < nBins; i++) {
				Accumulator[1][i] = Accumulator[1][i] / Accumulator[0][i];
				Accumulator[0][i] = (float) (mR * ((double) (i + 1) / nBins));
			}
			//pw = new PlotWindow("Radial Profile Plot", "Radius [pixels]", "Normalized Integrated Intensity",  Accumulator[0], Accumulator[1]);
		}
		//pw.draw();

		float max = 0;
		float nBinmax = 0;
		for (int ii = 0; ii < nBins; ii = ii + 1) {
			if (Accumulator[1][ii] > max) {
				//{max=Accumulator[1][ii]; nBinmax=ii+1;}
				//IJ.write ("Max="+max + " Rad="+nBinmax);
				max = Accumulator[1][ii];
				nBinmax = Accumulator[0][ii];
			}
		}
		IJ.log("Max=" + max + " Rad=" + nBinmax);

	}


	/**
	 *  Description of the Method
	 */
	private void doDialog() {
		canceled = false;
		GenericDialog gd = new GenericDialog("Radial Distribution...", IJ.getInstance());
		gd.addNumericField("X center (pixels):", X0, 2);
		gd.addNumericField("Y center (pixels):", Y0, 2);
		gd.addNumericField("Radius (pixels):", mR, 2);
		//gd.addCheckbox("Normalize", doNormalize);
		gd.addCheckbox("Use Spatial Calibration", useCalibration);
		gd.showDialog();
		if (gd.wasCanceled()) {
			canceled = true;
			return;
		}
		X0 = gd.getNextNumber();
		Y0 = gd.getNextNumber();
		mR = gd.getNextNumber();
		//doNormalize = gd.getNextBoolean();
		useCalibration = gd.getNextBoolean();
		if (gd.invalidNumber()) {
			IJ.showMessage("Error", "Invalid input Number");
			canceled = true;
			return;
		}
	}
}
