package afm2;

import afm2.image.Register;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;

import java.io.IOException;
import java.util.stream.IntStream;

public class TEST {

    public static void main(String[] args) {
        try {
            // open clusters
            String dir = "/home/thomas/AMU/Projects/AFMVIDEO/Data/RealData/FullTest/";
            ResultsTable rt = ResultsTable.open(dir+"clusters.csv");
            rt.show("rt");
            double[] clusters = rt.getColumn("cluster");
            double[] clustersFit = rt.getColumn("value");
            int nbClusters = 5;
            int nbFrames = clusters.length;
            // open image
            ImagePlus plus = IJ.openImage(dir+"Aligned2.tif");
            plus.show();
            // create avg images
            ImageStack[] avgs = new ImageStack[nbClusters];
            IntStream.range(0, nbClusters).forEach(i -> avgs[i] = new ImageStack(plus.getWidth(), plus.getHeight()));
            // loop over clusters and add to the avg
            IntStream.range(0,nbFrames).forEach(f->{
                System.out.println(""+clusters[f]+" "+clustersFit[f]);
                ImageProcessor imageProcessor = plus.getStack().getProcessor(f+1);
                if(clustersFit[f]>0)
                    avgs[(int) (clusters[f]-1)].addSlice(imageProcessor);
            });
            // display the avgs, and create a stack with all avg
            ImageStack finalAvg = new ImageStack(plus.getWidth(), plus.getHeight());
            IntStream.range(0, nbClusters).forEach(i-> {
                ImagePlus plusAvg = new ImagePlus("cluster_"+i,avgs[i]);
                ImagePlus avgProj = Register.projectionAvg(plusAvg);
                finalAvg.addSlice("cluster_"+(i+1),avgProj.getProcessor());
            });
            ImagePlus finalPlus = new ImagePlus("Avg clusters",finalAvg);
            finalPlus.show();
            IJ.saveAs(finalPlus,"tiff",dir+"AVGClusters.tif");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
