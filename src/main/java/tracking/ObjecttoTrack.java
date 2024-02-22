package tracking;

import Jama.*;

public class ObjecttoTrack {

    private float xpos;
    private float ypos;
    private int oframe;
    private float odata1;
    private float odata2;
    private float odata3;
    private int trajectoryNumber;
    private Matrix osigma;
    private int olabel;
    private int[] alreadygenerated;
    private int[][] candidates;
    private ObjecttoTrack next;
    private ObjecttoTrack previous;
    

    /**
     * Constructor.
     *
     * @param x The object's x position
     * @param y The object's y position.
     */
    ObjecttoTrack(float x, float y, int frame, float data1, float data2, float data3, Matrix sigma, int label) {
        xpos = x;
        ypos = y;
        oframe = frame;
        odata1 = data1;
        odata2 = data2;
        odata3 = data3;
        osigma = sigma;
        olabel = label;
        alreadygenerated = new int[Tracking.Max_disapearance+1];
        candidates=new int[Tracking.Max_disapearance+1][];
        next=null;
        previous=null;
    }
    
    
    void setNext(ObjecttoTrack objectnext){
    	this.next=objectnext;
    }
    void setPrevious(ObjecttoTrack objectprevious){
    	this.previous=objectprevious;
    }
    
    ObjecttoTrack getNext(){
    	return this.next;
    }
    ObjecttoTrack getPrevious(){
    	return this.previous;
    }
    
   /**
    * Set/get for the trajectory number
 * @return 
    */
    void setTrajectoryNumber(int trajnum){
    	this.trajectoryNumber=trajnum;
    }
    int getTrajectoryNumber(){
    	return this.trajectoryNumber;
    }
    /**
     * Return's the object's x position.
     *
     * @return The object's x position.
     */
    float getx() {
        return this.xpos;
    }

    /**
     * Returns the object's y position.
     *
     * @return The object's y position.
     */
    float gety() {
        return this.ypos;
    }

    /**
     * Returns the object's frame number.
     *
     * @return The object's frame number.
     */
    int getframe() {
        return this.oframe;
    }

    /**
     * Returns the object's data1.
     *
     * @return The object's data1.
     */
    float getdata1() {
        return this.odata1;
    }

    /**
     * Returns the object's data2.
     *
     * @return The object's data2.
     */
    float getdata2() {
        return this.odata2;
    }

    /**
     * Returns the object's data3.
     *
     * @return The object's data3.
     */
    float getdata3() {
        return this.odata3;
    }

    int getlabel() {
        // TODO Auto-generated method stub
        return this.olabel;
    }

    /**
     * Returns the object's covariance matrix osigma.
     *
     * @return The object's covariance matrix osigma.
     */
    Matrix getSigma() {
        return this.osigma;
    }

    /**
     * Returns the object's vector for direct computation of the energy.
     *
     * @return The object's vector (x,y,data1,data2).
     */
    Matrix getVector() {
        double[][] V = new double[4][1];
        V[0][0] = this.xpos;
        V[1][0] = this.ypos;
        V[2][0] = this.odata1;
        V[3][0] = this.odata2;
        return new Matrix(V);
    }

    /**
     * Returns the energy cost for a association to another particle
     * E(o1,O2)=(Vo1-Vo2)T sigma (-1/2) (Vo1-Vo2)  as defined in V. racine PhD thesis p44
     * The Energy is the current object is empty (noobject) or if the object oother is empty
     * will be computed using the max_neihboorood
     * @param oother The other object.
     * @return An energy.
     */
    double energyALL(ObjecttoTrack oother) {
        if (oother== null) { // we are testing the possibility of this of not being associated
            return this.energynoassociation();
        
        } else {
            return energy(oother.getVector()).get(0, 0); // the output matrix should be a 1 1 matrix
        }
    }

    /**
     * This method compute the cost of no association of this object, ie associating it with an empty object.
     * It is computed by considering an object at the distance max distance*110%
     * @return the energy
     */
    double energynoassociation() {
        float x = (float) (this.getx() + Tracking.MaxNeighbourood * 1.1);
        float y = (float) (this.gety() + Tracking.MaxNeighbourood * 1.1);
        int label = -1;
        float data1 = this.getdata1();
        float data2 = this.getdata2();
        float data3 = this.getdata3();
        ObjecttoTrack VirtualObject = new ObjecttoTrack(x, y, -1, data1, data2, data3, this.osigma, label);
        return energy(VirtualObject.getVector()).get(0, 0);
    }

    /**
     * Returns the energy cost for a association to another particle
     * E(o1,O2)=(Vo1-Vo2)T sigma (-1/2) (Vo1-Vo2)  as defined in V. racine PhD thesis p44
     * @param otherVector The other object vector.
     * @return a 1x1 matrix .
     */
    Matrix energy(Matrix otherVector) {
        Matrix Difference = this.getVector().minus(otherVector);

        return Difference.transpose().times(osigma.times(Difference));

    }

    float energyDistance(ObjecttoTrack oother) {
    	
    		return energy(oother.getx(), oother.gety());
    }

    /**
     * Returns how far this point is from a specific point.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param data1 data1
     * @param data2 data2
     * @param data3 data3
     * @return The distance.
     */
    float energy(float x, float y, float data1, float data2, float data3) {
        float euclideanDistance = energy(x, y);
        return euclideanDistance;
    }

    /**
     * Returns how far this point is from a specific point.

     *
     * @param x The x coordinate
     * @param y The y coordinate

     * @return The distance.
     */
    float energy(float x, float y) {
        float xdiff = xpos - x;
        float ydiff = ypos - y;
        return (float) Math.sqrt(xdiff * xdiff + ydiff * ydiff);
    }

    /** From an array generated during the initialization of possibilities
     * in the neighborhood of its own position in frame f+1
     * this method pool a random choice
     * @param frametested the TrackFrame containing all the object in frame tested.
     * @return the object tag randomly chosen in the pool of authorized candidates.
     */
    public ObjecttoTrack getinAuthorizedPool(TrackFrame frametested, int fnumber, int tau) {


        // TODO Auto-generated method stub
        //(int)Math.floor( * Math.random())
        if (this.alreadygenerated[tau] == 0) {
            this.candidateArray(frametested, fnumber, tau);
        }
        if (this.candidates[tau].length != 0) {
            int indx = (int) Math.floor((this.candidates[tau].length+1) * Math.random());
            //+1 in order to test no association sometimes
            if (indx==this.candidates[tau].length)
            	return null;
            else
            		
            	return frametested.getObject(this.candidates[tau][indx]);
        }// to be modified to a [currentframe+nbframesblinking][int] later
        else {
            return null; // if there is no potential candidate, then we return -1= no association
        }
    }

    /**
     * This method generate a pool of candidate
     * @param frametested the TrackFrame containing all the object in frame tested.
     * @return
     */
    private int[][] candidateArray(TrackFrame frametested, int f, int tau) {
        // TODO Auto-generated method stub

        int[] array = new int[frametested.getParticles_COUNT()];
        int j = 0;
        for (int i = 0; i < frametested.getParticles_COUNT(); i++) {
            if (frametested.getObject(i).energyDistance(this) < Tracking.MaxNeighbourood) {
                array[j] = i;
                j++;
            }
        }
        //if Tracking.divisionAllowed
        // if Tracking. disappearanceAllowed
        //if Tracking.fusionAllowed

        //for (int i=j; j<array.length; i++)
        //array[i]=-1;

        candidates[tau] = new int[j];
        System.arraycopy(array, 0, candidates[tau], 0, j);
        this.alreadygenerated[tau]= 1;// to be modified: alreadygenerated valable only for one frame
        return candidates;
    }
    
}
