package afm2.plugins;

import afm2.image.Register;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import mcib3d.geom.Vector3D;
import mcib3d.geom.Voxel3D;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.measurements.MeasureCentroid;
import mcib3d.geom2.measurements.MeasureEllipsoid;
import mcib3d.geom2.measurements.MeasureFeret;
import mcib3d.image3d.ImageHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

// alignment based on object detection
// should have only one object in field of view
public class Alignment1_ implements PlugIn {
    int imaRaw, imaSeg;
    boolean doRotation = true;

    @Override
    public void run(String s) {
        if (Dialogue()) {
            final ImagePlus rawPlus = WindowManager.getImage(imaRaw);
            final ImagePlus segPlus = WindowManager.getImage(imaSeg);
            final int nFrames = rawPlus.getNFrames();
            // calibration
            Calibration cal = rawPlus.getCalibration();

            // centers + orientations
            final Map<Integer, Voxel3D> mapCenters = new HashMap<>();
            final Map<Integer, Vector3D> mapOrientations = new HashMap<>();
            IntStream.rangeClosed(1, nFrames).forEach(f -> {
                IJ.log("\\Update:Processing " + f + " / " + nFrames + "    ");
                ImagePlus plus2 = Register.extractCurrentStack(segPlus, 1, f);
                ImageHandler seg = ImageHandler.wrap(plus2);
                // object detection
                Object3DInt object3DInt = new Object3DInt(seg);
                // measure centroid
                MeasureCentroid center = new MeasureCentroid(object3DInt);
                double cx = center.getValueMeasurement(MeasureCentroid.CX_PIX);
                double cy = center.getValueMeasurement(MeasureCentroid.CY_PIX);
                Voxel3D V = new Voxel3D(cx, cy, f, 1);
                mapCenters.put(f, V);
                // feret for orientation
                if(doRotation){
                    MeasureFeret feret = new MeasureFeret(object3DInt);
                    Vector3D vect = new Vector3D(feret.getFeret1(), feret.getFeret2());
                    // test Ellipsoid
                    //MeasureEllipsoid ellipsoid = new MeasureEllipsoid(object3DInt);
                    //vect = ellipsoid.getAxis1();
                    if (vect.x < 0) vect = vect.multiply(-1);
                    mapOrientations.put(f, new Vector3D(vect.getNormalizedVector()));
                }
            });
            IJ.log("Processing done");
            // center
            final Voxel3D center = new Voxel3D(rawPlus.getWidth() / 2, rawPlus.getHeight() / 2, 0, 1);
            // translate + rotate
            ImageStack stackAligned1 = new ImageStack(rawPlus.getWidth(), rawPlus.getHeight());
            Vector3D axisX = new Vector3D(1, 0, 0);
            IntStream.rangeClosed(1, nFrames).forEach(f -> {
                IJ.log("\\Update:Aligning " + f + " / " + nFrames + "    ");
                ImagePlus plus = Register.extractCurrentStack(rawPlus, 1, f);
                ImageProcessor processor = plus.getProcessor();
                // translate
                Voxel3D V = mapCenters.get(f);
                processor.translate(center.x - V.x, center.y - V.y);
                // rotate
                if(doRotation){
                    Vector3D vect = mapOrientations.get(f);
                    double ang = vect.angleDegrees(axisX);
                    if (vect.y > 0) ang *= -1;
                    processor.setBackgroundValue(0);
                    processor.setInterpolationMethod(ImageProcessor.BICUBIC);
                    processor.rotate(ang);
                }
                stackAligned1.addSlice(processor);
            });
            IJ.log("Alignment done");
            ImagePlus plusTrans = new ImagePlus("Aligned1", stackAligned1);
            IJ.run(plusTrans, "Properties...", "channels=1 slices=1 frames=" + nFrames);
            plusTrans.setCalibration(cal);
            plusTrans.show();
        }
    }

    private boolean Dialogue() {
        int nbima = WindowManager.getImageCount();
        String[] names = new String[nbima];
        for (int i = 0; i < nbima; i++) {
            names[i] = WindowManager.getImage(i + 1).getShortTitle();
        }
        imaSeg = 0;
        imaRaw = nbima > 1 ? nbima - 1 : 0;

        GenericDialog dia = new GenericDialog("AFM ALIGN 1");
        dia.addChoice("BinaryMask", names, names[imaSeg]);
        dia.addChoice("RawMovie", names, names[imaRaw]);
        dia.addCheckbox("Do Rotation", doRotation);

        dia.showDialog();
        imaSeg = dia.getNextChoiceIndex() + 1;
        imaRaw = dia.getNextChoiceIndex() + 1;
        doRotation = dia.getNextBoolean();

        return dia.wasOKed();
    }
}
