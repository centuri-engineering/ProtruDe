package tracking;

public class TrackFrame {

    /**
     * list of objects to be tracked in this frame
     */
    ObjecttoTrack[] objects;
    /**
     * A virtual object which does not exist: used to represent no association at this frame
     */
    ObjecttoTrack noobject;
    /**
     * Number of real object (or particle) in this frame
     */
    int Particles_COUNT;
    /**
     * Frame number
     */
    int frame;

    /**
     * Constructor.
     *
     *@param myobject a list of object associated
     *
     */
    TrackFrame(ObjecttoTrack[] myobjects, int myframe) {
        objects = myobjects;
        Particles_COUNT = objects.length;
        frame = myframe;
        noobject = this.CreateEmpyObject();
    }

    /**
     *
     * @return an empty object to track with a label -1
     */
    private ObjecttoTrack CreateEmpyObject() {
        ObjecttoTrack emptyobject = new ObjecttoTrack(-1, -1, this.frame, -1, -1, -1, objects[0].getSigma(), -1);
        return emptyobject;
    }

    /**
     * Return's the number of particles is this frame
     *
     * @return The number of particles is this frame
     */
    int getParticles_COUNT() {
        return Particles_COUNT;
    }

    ObjecttoTrack getObject(int indx) {
        if (indx > -1) {
            return objects[indx];
        } else {
            return noobject;
        }
    }
}
