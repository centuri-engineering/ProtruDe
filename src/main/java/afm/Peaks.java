package afm;

/**
 *  Description of the Class
 *
 * @author     thomas
 * @created    8 janvier 2006
 */
public class Peaks implements Comparable<Peaks> {
	/**
	 *  the coordinates
	 */
	int x, y;
	/**
	 *  the angle in degrees and the max correlation value and the internal symmetry
	 */
	float ang, corr, is;


	/**
	 *  Constructor for the Point2D object
	 */
	public Peaks() {
		clear();
	}


	/**
	 *  Constructor for the Point2D object
	 *
	 * @param  origpt  another peak
	 */
	public Peaks(Peaks origpt) {
		Copy(origpt);
	}


	/**
	 *  Constructor for the Point2D object
	 *
	 * @param  xx   x coordinate
	 * @param  yy   y coordinate
	 * @param  val  angle
	 * @param  co   Description of the Parameter
	 */
	public Peaks(int xx, int yy, float co, float val,float ii) {
		x = xx;
		y = yy;
		corr = co;
		ang = val;
		is=ii;
	}


	/**
	 *  reset the values
	 */
	public void clear() {
		x = 0;
		y = 0;
		corr = 0;
		ang = 0;
	}


	/**
	 *  copy from another peak
	 *
	 * @param  origpt  another peak
	 */
	public void Copy(Peaks origpt) {
		x = origpt.x;
		y = origpt.y;
		corr = origpt.corr;
		ang = origpt.ang;
		is=origpt.is;	}



	/**
	 *  convert to string so as to print
	 *
	 * @return    the string value
	 */
	public String toString() {
		return ("x=" + x + " y=" + y + " ang=" + ang + " corr=" + corr + " is=" + is + "\n");
	}


	/**
	 *  Gets the x attribute of the Peaks object
	 *
	 * @return    The x value
	 */
	public int getX() {
		return x;
	}


	/**
	 *  Gets the y attribute of the Peaks object
	 *
	 * @return    The y value
	 */
	public int getY() {
		return y;
	}


	/**
	 *  Gets the angle attribute of the Peaks object
	 *
	 * @return    The angle value
	 */
	public float getAngle() {
		return ang;
	}


	/**
	 *  Gets the corr attribute of the Peaks object
	 *
	 * @return    The corr value
	 */
	public float getCorr() {
		return corr;
	}
	
	public float getInternalSymmetry() {
		return is;
	}
	
	public int compareTo(Peaks obj) {
		double sum1 = this.getCorr() + this. getInternalSymmetry();
		double sum2 = obj.getCorr() + obj. getInternalSymmetry();
		if (sum1 < sum2) {return -1;}
		if (sum2 > sum1) {return 1;}
		else {return 0;}
	}
}

