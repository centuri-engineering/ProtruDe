package tracking;

/**
 * 
 */
//import java.util.Random;
import ij.*;
import java.util.ArrayList;
/**
 * @author Perrine
 *
 */
public class SimulateAnnealing{

    /**
     * The tracking owner.
     */
    protected Tracking owner;
    /** the current temperature
     *
     */
    protected double temperature;
    /** the current energy
     *
     */
    protected double pathlength;
    protected double pathlengthnew;
    /**
     * The actual minimal energy found
     */
    protected double minimallength;
    /**
     * The actual order : gives for each frame the old label at the position newtag.
     * example: order[3][2] is the old tag of object in the original file at frame 3+1.
     *  2 means that this object will be associated at object not modified at tag 2+1(first tag=0) in frame 0.
     */
    
    protected int order[][];
    /** the order leading to the minimal energy 
     *
     */
    protected int nbtrajectories;
    /**
     *  will be use to reduce order at the end
     */
    protected int minimalorder[][];
    
    protected ArrayList<ArrayList <ObjecttoTrack>> listofTrajectories;
    
    /**
     * Constructor
     * <p>
     * Initialize order and minimalorder.
     * @param owner The Tracking class that owns this object using the owner frames count and particles count.
     */
    SimulateAnnealing(Tracking owner) {
        this.owner = owner;
        order = new int[owner.getFrames_COUNT()][owner.getParticles_COUNT_Max()];
        nbtrajectories=0;
        minimalorder = new int[owner.getFrames_COUNT()][owner.getParticles_COUNT_Max()];
        listofTrajectories = new ArrayList<ArrayList <ObjecttoTrack>>();
    }

    /**
     * Called to determine if annealing should take place.
     * <p>
     * when d is >0 then the probability of this assertion to be true is very low (exp(negative)<1),
     *  so when the new association cost more than the former, then it is unlikely that annealing will be performed,
     * but not impossible
     * <p>
     *  when it's <0 (new association cost less) , then exp(d+1) is > 1 then always true
     *  if the temperature is high, then the probability is decreasing slower, allowing for more changes than when the temperature is low.
     * @param d The distance.
     * @return True if annealing should take place.
     */
    public boolean anneal(double d) {
        if (temperature < 10) {
            if (d < 0.0) // then new association cost less than the former
            {
                return true;
            } else {
                return false;
            }
        }
        if (Math.random() < Math.exp(-d / temperature)) /** when d is >0 then the probability of this assertion to be true is very low (exp(negative)<1),
         * // so when the new association cost more than the former, then it is unlikely that annealing will be performed,
         * //but not impossible
         * // when it's <0 (new association cost less) , then exp(d+1) is > 1 then always true
         * // if the temperature is high, then the probability is decreasing slower, allowing for more changes than when the temperature is low.
         */
        {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Used to ensure that the passed in integer is within the particle range.
     *
     * @param i A particle index.
     * @return A particle index that will be less than Particles_COUNT
     */
    //  public int mod(int i)
    //  {
    //    return i % owner.Particles_COUNT;
    //  }
    /**
     * Called to get the energy between two objects in two frames.
     *
     * @param i The position of the first object to be compared at frame f1.
     * The object tag is obtained by order[frame][position]
     * @param j The position of the second object at frame f2
     * @param f1 The first frame
     * @param f2 The second frame
     * @return The distance (energy) between the two objects.
     */
    public double distance(int i, int j, int f1, int f2) {
        int c1 = order[f1][i];
        int c2 = order[f2][j];
        return owner.getFrames()[f1].getObject(c1).energyALL(owner.getFrames()[f2].getObject(c2));
    }

    /**
     * Run as a background thread. This method is called to
     * perform the simulated annealing.
     * <p>
     * cycle is initialized at 1, temperature at the static value start_temperature.
     * order and minimal order are initialized according to the initial tag.
     * example: all tag 1 are associated together.
     * <p>
     * The initial object are plotted
     * <p> The initial energy is computed
     * <p> For each frame,
     */
    //@SuppressWarnings("null")
	public void run() {
    	IJ.showStatus("Tracking start");
    	//IJ.showMessage("test");
        int cycle = 0;
        int sameCount =0;
        temperature = Tracking.START_TEMPERATURE;
        int nbiteration=Tracking.nbiteration;
       
        inittrajslinked(listofTrajectories);
       
        nbtrajectories=listofTrajectories.size();
        
        ArrayList<ArrayList <ObjecttoTrack>> listofTrajectoriestested=new ArrayList<ArrayList <ObjecttoTrack>>();;
        updatetraj(listofTrajectoriestested);
        //owner.WriteObjectsTracked_plugin("Original objects");
        //owner.plotObjects();
       // pathlength = energy();
        pathlength= energytrajs(listofTrajectoriestested);
        minimallength = pathlength;

        while (sameCount < nbiteration) {
            
        	
        
            for (int f = 0; f < owner.getFrames_COUNT() - 1; f++) {
                // make adjustments to particles association(annealing)
                //for (int j2 = 0; j2 <   owner.Particles_COUNT; j2++) {
                for (int j2 = 0; j2 < 100 ; j2++) {
                    // will be only pooled among the points within the max authorised euclidean distance.
                	
                    int i1tag = (int) Math.floor((double) owner.getFrames()[f].getParticles_COUNT() * Math.random());// in frame f
                    
                    ObjecttoTrack objecttomodify=owner.getFrames()[f].getObject(i1tag);
                    
                    ObjecttoTrack proposedobject;
                    int tau=0;
                    if (objecttomodify.getNext()==null) {// this object is tha last of one trajectory make sense to look at disapearance allowed
                    	 tau= (int) Math.floor(Tracking.Max_disapearance*Math.random());
                    } 
                    
                    if(f +1+ tau<owner.getFrames_COUNT())
                    {
                    	 proposedobject = objecttomodify.getinAuthorizedPool(owner.getFrames()[f +1+ tau], f+1+tau, tau+1);
                    	 
                    }
                    else
                      {
				j2=j2-1;
                    	continue;
		
                      }
                  //  IJ.showMessage("PROPOSED frame"+ Integer.toString(f)+" / tau "+Integer.toString(tau)+ " object"+Integer.toString(i1tag));
                    
                    // compute d the cost difference of associating particles i1 with particles i1+1 in frame f+t and j1 with j1+1 against the former association
                    if (objecttomodify.getNext()==proposedobject)
                    	continue;//already same trajectory
                   
                    // we compute the difference between associating i1 with j1 and the former association (having i1 with the next tag in its trajectory, and j1 with the previous tag in its trajectory)
                    //double d = distance(i1_trajnumber_in_f, j1_trajnumber_in_fplus1, f, f + 1) - (distance(j1_trajnumber_in_fplus1, j1_trajnumber_in_fplus1, f, f + 1) + distance(i1_trajnumber_in_f, i1_trajnumber_in_f, f, f + 1))/2;
                    double d;
                    if (proposedobject==null)
                    {// then we have to comapre the cost of no association against current association
                    		d=objecttomodify.energynoassociation()-objecttomodify.energyALL(objecttomodify.getNext());
                    }
                    else
                    { 
                    	 
                    	if (proposedobject.getPrevious()==null)//this object is free
                    	// then we compare the cost of assocaiting both object against the former association
                    		{
                    		if (objecttomodify.getNext()==null)
                    		{
                    			
                    			d= objecttomodify.energyALL(proposedobject)-objecttomodify.energynoassociation(); // was a fixed number
                    			//System.out.println("d "+d);
                    		}
                    		else
                    			{
                    		d=(objecttomodify.energyALL(proposedobject))-objecttomodify.energyALL(objecttomodify.getNext());
                    			}
                    		}
                    	else 
                    		//the object was not free: comparing the cost object to modify with proposed object to modify association
                    		if (objecttomodify.getNext()!=null){
                    			d=(objecttomodify.energyALL(proposedobject)+proposedobject.getPrevious().energyALL(objecttomodify.getNext()))-(objecttomodify.energyALL(objecttomodify.getNext())+proposedobject.energyALL(proposedobject.getPrevious()));
                    		}
                    		else 
                    			d=(objecttomodify.energyALL(proposedobject)+proposedobject.getPrevious().energynoassociation())-(objecttomodify.energynoassociation()+proposedobject.energyALL(proposedobject.getPrevious()));
                    }
                    
                    if (anneal(d)) {
                    	/*if (proposedobject!=null)
                    		IJ.log("cycle "+ sameCount+" modified: "+objecttomodify.getlabel()+ " at frame "+f+"newly associated with "+proposedobject.getlabel());
                    	else
                    		IJ.log("cycle "+ sameCount+" modified: "+objecttomodify.getlabel()+ " at frame "+f+" to be the last of the trajectory");*/
                        // then the new association is chosen is kept : in f they keep the same order
                    	//IJ.showMessage("ANNEALED frame"+ Integer.toString(f)+" / tau "+Integer.toString(tau)+ " object"+Integer.toString(i1tag));
                    	 if (objecttomodify.getNext()!=null)
                         {	
                        	 if (proposedobject!=null)
                        	 {
                        		 objecttomodify.getNext().setPrevious(proposedobject.getPrevious());
                        		 
                        		 if (proposedobject.getPrevious()!=null)
                        			 proposedobject.getPrevious().setNext(objecttomodify.getNext());
                        		 
                        		 objecttomodify.setNext(proposedobject);
                        		 proposedobject.setPrevious(objecttomodify);
                        	 }
                        	 else
                        	 {
                    		 objecttomodify.getNext().setPrevious(null);
                    		 objecttomodify.setNext(null);
                    		 
                        	 }
                        	 break;
                      }
                     else{
                    	 if (proposedobject!=null)
                    	 {
                    		 objecttomodify.setNext(proposedobject);
                    		 if (proposedobject.getPrevious()!=null)
                    			 proposedobject.getPrevious().setNext(null);
                    		 proposedobject.setPrevious(objecttomodify);
                    	 }
                    	 //else nothing happened: proposed object null, object to modify get next already at null
                    		 
                    	 
                     }
                   
                     
                        
                    }
                }
                //	}
            }
            updatetraj(listofTrajectoriestested);





            // See if this improved anything
            pathlength = energytrajs(listofTrajectoriestested);
            if (pathlength < minimallength) {
                minimallength = pathlength;
                updatetraj(listofTrajectories);
                cycle++;
                sameCount = 0;
            } else {
                sameCount++;
                reinitlinks(listofTrajectories);
            }
            temperature = (1/nbiteration) * temperature; // in order to go down proportionally to the number of iteration
           
            IJ.showStatus("TEST..."+Integer.toString(sameCount)+"/"+Integer.toString(nbiteration));
        }
     
     
        IJ.showMessage("Changes accepted in " + cycle + " cycles.");
      
        return;
    }

    private void reinitlinks(
			ArrayList<ArrayList<ObjecttoTrack>> traj) {
		// set back the next and previous as they were (all iniatlized at null, and then filled by list of trajectories.
    	for (int f=0;f<owner.getFrames_COUNT();f++)
    	{
    		for(int p=0;p<owner.getFrames()[f].getParticles_COUNT();p++)
    		{
    			owner.getFrames()[f].getObject(p).setPrevious(null);
    			owner.getFrames()[f].getObject(p).setNext(null);
    		}
    	}
    	int p;
    	for (int tr=0;tr<traj.size();tr++)
    	{
    		if (traj.get(tr).size()>1){
    		traj.get(tr).get(0).setNext(traj.get(tr).get(1));
			
    		for(p=1;p<traj.get(tr).size()-1;p++)
    		{
    			traj.get(tr).get(p).setNext(traj.get(tr).get(p+1));
    			traj.get(tr).get(p).setPrevious(traj.get(tr).get(p-1));
    		}
    		traj.get(tr).get(traj.get(tr).size()-1).setPrevious(traj.get(tr).get(traj.get(tr).size()-2));
    		traj.get(tr).get(traj.get(tr).size()-1).setNext(null);
    		}
    	}
	}

	private void updatetraj(
			ArrayList<ArrayList<ObjecttoTrack>> Traj) {
		//Traj=new ArrayList<ArrayList <ObjecttoTrack>>(); //remove everything instead to keep the java ref
		//while (Traj.size()>0)
		//	Traj.remove(Traj.size()-1);
		Traj.clear();
    	ArrayList <ObjecttoTrack> currenttrajectory = new ArrayList<ObjecttoTrack>();
    	ObjecttoTrack NearestNeighbor=null;
		for (int f=0;f<owner.getFrames_COUNT();f++) // for each frame
		{
			
			for (int p=0;p<owner.getFrames()[f].getParticles_COUNT();p++)
			{
				if (owner.getFrames()[f].getObject(p).getPrevious()==null)
				{//save the previous trajectory and start a new trajectory
					 if (currenttrajectory.size()>0)
					 {
			    	    	// end of trajectory
			    	    	Traj.add(currenttrajectory);
					
					}
					 currenttrajectory = new ArrayList<ObjecttoTrack>();
				NearestNeighbor=owner.getFrames()[f].getObject(p);
				
				while(NearestNeighbor!=null)
					{
						
						
						currenttrajectory.add(NearestNeighbor);
						//IJ.log(p+" "+NearestNeighbor.getframe()+" "+NearestNeighbor.getlabel());
						NearestNeighbor=NearestNeighbor.getNext();
						
					}
			}
			}
				
					
		}
		
		//just for the last one
		if (currenttrajectory.size()>0)
	    	// end of trajectory
	    	Traj.add(currenttrajectory);
		
	}

	

    /**
     * Return the energy of the current association through
     * the particles.
     *
     * @return  the energy of the current association through
     * the particles.
     */
    public double energy() {
        double d = 0.0;
        for (int f = 1; f < owner.getFrames_COUNT(); f++) {
            for (int i = 0; i < owner.getFrames()[f].getParticles_COUNT(); i++) {
                d += distance(i, i, f - 1, f);
            }
        }
        return d;
    }
    public double energytrajs(ArrayList<ArrayList<ObjecttoTrack>> traj) {
        double d = 0.0;
        for (int t=0;t<traj.size();t++){
        	for (int p=0;p<traj.get(t).size()-1;p++){
        		d=d+traj.get(t).get(p).energyALL(traj.get(t).get(p+1));
        		
        		
        	}
        }
        return d+traj.size()*Tracking.MaxNeighbourood;//in order to favorize less trajectories
    }
    /**
     * Set the specified array to have a list of the particles in
     * order of association for each frame.
     *
     * @param an An array to hold the particles.
     */

    
public void inittrajs(ArrayList<ArrayList<ObjecttoTrack >> initTraj) {
	TrackFrame frametested;
	for (int i=0;i<owner.getParticles_COUNT_Max();i++)
	{
		ArrayList <ObjecttoTrack> currenttrajectory = new ArrayList<ObjecttoTrack>();
		currenttrajectory.add(owner.getFrames()[0].getObject(i));
		for (int f=1;f<owner.getFrames_COUNT();f++)
		{
    		frametested=owner.getFrames()[f];
    		ObjecttoTrack objectinpreviousframe=currenttrajectory.get(currenttrajectory.size()-1)  ;
    		ObjecttoTrack NearestNeighbor=null;
    		Boolean NNfound=false;
    		float distold=Tracking.MaxNeighbourood;
    	    // find the neigbors in next frame
    	    for (int i1 = 0; i1 < frametested.getParticles_COUNT(); i1++) 
    	    {
    	    	float dist=objectinpreviousframe.energyDistance(owner.getFrames()[f].getObject(i1));
    	    	if (dist<distold)
    	    	{
    	    		if (this.notalreadylink(owner.getFrames()[f].getObject(i1),initTraj))
    	    		{
    	    		NearestNeighbor=owner.getFrames()[f].getObject(i1);
    	        	NNfound=true;
    	    		distold=dist;
    	    		}
    	    	}
    	    	
			}
    	    if (NNfound)
    	    	
    	    		currenttrajectory.add(NearestNeighbor);
		}
    	    
    	    if (currenttrajectory.size()>0)
    	    	// end of trajectory
    	    	initTraj.add(currenttrajectory);
             
		
	}
}
public void inittrajslinked(ArrayList<ArrayList<ObjecttoTrack >> initTraj) {
	ObjecttoTrack NearestNeighbor=null;
	for (int f=0;f<owner.getFrames_COUNT()-1;f++) // for each frame
	{
		
		for (int p=0;p<owner.getFrames()[f].getParticles_COUNT();p++)
		{// for each particle in this frame
			
			ObjecttoTrack objectinpreviousframe=owner.getFrames()[f].getObject(p);
			NearestNeighbor=null;
    		Boolean NNfound=false;
    		float distold=Tracking.MaxNeighbourood;
    	    // find the neigbors in next frame
    	    for (int i1 = 0; i1 < owner.getFrames()[f+1].getParticles_COUNT(); i1++) 
    	    {
    	    	float dist=objectinpreviousframe.energyDistance(owner.getFrames()[f+1].getObject(i1));
    	    	if (dist<distold)
    	    	{
    	    		if (owner.getFrames()[f+1].getObject(i1).getPrevious()==null) // not linked yet
    	    		{
    	    		NearestNeighbor=owner.getFrames()[f+1].getObject(i1);
    	        	NNfound=true;
    	    		distold=dist;
    	    		}
    	    		else 
    	    		{
    	    			float distconcurrent=owner.getFrames()[f+1].getObject(i1).energyDistance(owner.getFrames()[f+1].getObject(i1).getPrevious());
    	    			if (distconcurrent>dist) 
    	    			{
    	    			//brink the link 
    	    				//owner.getFrames()[f+1].getObject(i1).getPrevious().setNext(null);
    	    				
    	    				//owner.getFrames()[f+1].getObject(i1).setPrevious(null);
    	    				NearestNeighbor=owner.getFrames()[f+1].getObject(i1);
    	    				distold=dist;
    	    				NNfound=true;
    	    			}// we do not change anything otherwise: the previous association was nearest
    	    		}
    	    		
    	    	}
    	    	
    	    	
			}
    	    
    	    if (NNfound){
    	    	//create the new link and break the previous link if any
    	    	if (NearestNeighbor.getPrevious()!=null)
    	    		NearestNeighbor.getPrevious().setNext(null);
				
				
    	    	NearestNeighbor.setPrevious(objectinpreviousframe);
    	    	objectinpreviousframe.setNext(NearestNeighbor);
    	    }
		
    	    
    	    
    	    
		}
	}
	
	// check the non complete trajectories and link them
	for (int f=0;f<owner.getFrames_COUNT();f++) // for eacf frame
	{
		
		for (int p=0;p<owner.getFrames()[f].getParticles_COUNT();p++)
		{
			Boolean NNfound=false;
    		float distold=Tracking.MaxNeighbourood;
			if (owner.getFrames()[f].getObject(p).getNext()==null)
			{//save the previous trajectory and start a new trajectory
				if (f!=owner.getFrames_COUNT()-1)
				{
					//incomplete trajectory
					//check if there is any NN among starting directory in the next frame:
					for (int p2=0;p2<owner.getFrames()[f+1].getParticles_COUNT();p2++)
					{
						if (owner.getFrames()[f+1].getObject(p2).getPrevious()==null)
						{
							//check if it's matching
							float dist=owner.getFrames()[f].getObject(p).energyDistance(owner.getFrames()[f+1].getObject(p2));
			    	    	if (dist<distold)
			    	    	{
			    	    		
			    	    		NearestNeighbor=owner.getFrames()[f+1].getObject(p2);
			    	        	NNfound=true;
			    	    		distold=dist;			    	    
			    	    	}
						}
					}
					 if (NNfound)
					 {
			    	    	//create the new link and break the previous link if any
			    	    	if (NearestNeighbor.getPrevious()!=null)
			    	    		NearestNeighbor.getPrevious().setNext(null);
							
							
			    	    	NearestNeighbor.setPrevious(owner.getFrames()[f].getObject(p));
			    	    	owner.getFrames()[f].getObject(p).setNext(NearestNeighbor);
			    	    }
					
				}
		}
		
	}
	}
	//create the trajectory
	updatetraj(initTraj);
// now create the trajectory (display purpose here only)	
	/*ArrayList <ObjecttoTrack> currenttrajectory = new ArrayList<ObjecttoTrack>();
		for (int f=0;f<owner.getFrames_COUNT();f++) // for eacf frame
		{
			
			for (int p=0;p<owner.getFrames()[f].getParticles_COUNT();p++)
			{
				if (owner.getFrames()[f].getObject(p).getPrevious()==null){//save the previous trajectory and start a new trajectory
					 if (currenttrajectory.size()>0)
			    	    	// end of trajectory
			    	    	initTraj.add(currenttrajectory);
					currenttrajectory = new ArrayList<ObjecttoTrack>();
					NearestNeighbor=owner.getFrames()[f].getObject(p);
					while(NearestNeighbor!=null)
					{
						currenttrajectory.add(NearestNeighbor);
						NearestNeighbor=NearestNeighbor.getNext();
					}
				}
				
					
			}
		}
		//just for the last one
		if (currenttrajectory.size()>0)
	    	// end of trajectory
	    	initTraj.add(currenttrajectory); */
		
}
private boolean notalreadylink(ObjecttoTrack object,
		ArrayList<ArrayList<ObjecttoTrack>> initTraj)
{
	//check if the object ios aleardy linked to another object.
	return false;
}
}


                //IJ.showMessage(IJ.d2s(an[f][i]));
         
    


    

