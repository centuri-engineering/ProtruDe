/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package video;

import ij.plugin.PlugIn;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.ResultsTable;
import java.util.*;

/**
 *
 **
 * /**
Copyright (C) 2008- 2011 Thomas Boudier
 *

 *
This file is part of mcib3d

mcib3d is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

 * @author thomas
/**
 * This is a template for a plugin that does not require one image
 * (be it that it does not require any, or that it lets the user
 * choose more than one image in a dialog).
 */
public class Analyse_Movie implements PlugIn {

    double nbmoy = 0;
    int nb0 = 0;
    int nb1 = 0;
    int nb2 = 0;
    int nb3 = 0;
    int nb4 = 0;
    float[] trans10;
    float[] trans20;
    float[] trans30;
    float[] trans40;
    int maxtime = 0;

    public void run(String arg) {
// analyse movie
        ImageStack movie = IJ.getImage().getStack();
        int sizex = movie.getWidth();
        int sizey = movie.getHeight();
        int nbframes = movie.getSize();


// analyse frame by frame
        ResultsTable rt = new ResultsTable();
        for (int f = 1; f <= nbframes; f++) {
            analyseFrame(movie.getProcessor(f), f);
            rt.incrementCounter();
            rt.setValue("Edge0", f - 1, nb0);
            rt.setValue("Edge1", f - 1, nb1);
            rt.setValue("Edge2", f - 1, nb2);
            rt.setValue("Edge3", f - 1, nb3);
            rt.setValue("Edge4", f - 1, nb4);
            rt.setValue("AverageEdge", f - 1, nbmoy);
        }
        rt.show("Edge Analysis");

// we want the time a particle is in state 2-4, so we look for pattern 0-[1-4]-0, and the time it stays in [1-4]
// the particle can change number if the neighborhood changes
// draw a timeline for each particle

        trans10 = new float[nbframes];
        trans20 = new float[nbframes];
        trans30 = new float[nbframes];
        trans40 = new float[nbframes];

        int part = 0;
        int[] tab;
        ArrayList particles = new java.util.ArrayList();
        for (int x = 0; x < sizex; x++) {
            IJ.showStatus("analysing " + x + " / " + sizex);
            for (int y = 0; y < sizey; y++) {
                tab = getTimeProfile(movie, x, y);
                // debug 
                //if((x==centerx)&&(y==centery)) for(i=0;i<tab.length;i++) IJ.log(i+" "+tab[i]);
                if (tabNotEmpty(tab)) {
                    particles.add(tab);
                    analyseTimeLine(tab, part);
                    part++;
                }
            }
        }
// statistics transition
        maxtime++;
        float[] xscale = new float[maxtime];
        for (int i = 0; i < maxtime; i++) {
            xscale[i] = i + 1;
        }
        float[] trans10bis = new float[nbframes - 1];
        System.arraycopy(trans10, 1, trans10bis, 0, trans10.length - 1);
        Plot plot10 = new Plot("Transition_1", "Time", "Number", xscale, trans10bis);
        plot10.show();
        float[] trans20bis = new float[nbframes - 1];
        System.arraycopy(trans20, 1, trans20bis, 0, trans20.length - 1);
        Plot plot20 = new Plot("Transition_2", "Time", "Number", xscale, trans20bis);
        plot20.show();
        float[] trans30bis = new float[nbframes - 1];
        System.arraycopy(trans30, 1, trans30bis, 0, trans30.length - 1);
        Plot plot30 = new Plot("Transition_3", "Time", "Number", xscale, trans30bis);
        plot30.show();
        float[] trans40bis = new float[nbframes - 1];
        System.arraycopy(trans40, 1, trans40bis, 0, trans40.length - 1);
        Plot plot40 = new Plot("Transition_4", "Time", "Number", xscale, trans40bis);
        plot40.show();


// draw TimeLine
        ByteProcessor timeline = new ByteProcessor(movie.getSize(), part);
        timeline.setMinAndMax(0, 255);
        Iterator it = particles.iterator();
        part = 0;
        while (it.hasNext()) {
            drawTimeLine(timeline, (int[]) it.next(), part);
            part++;
        }
// timeline
        ImagePlus timeplus = new ImagePlus("time", timeline);
        timeline.setMinAndMax(0, 255);
        timeplus.show();
        IJ.open(IJ.getDirectory("plugins") + "/MB.lut");


    }

    private void analyseFrame(ImageProcessor ip, int z) {
        int[] hist = ip.getHistogram();
        //IJ.log("analyse frame "+z);
        nb0 = hist[1];// IJ.log("nb0 "+nb0);
        nb1 = hist[2]; //IJ.log("nb1 "+nb1);
        nb2 = hist[3]; //IJ.log("nb2 "+nb2);
        nb3 = hist[4]; //IJ.log("nb3 "+nb3);
        nb4 = hist[5]; //IJ.log("nb4 "+nb4);
        int nbtot = nb0 + nb1 + nb2 + nb3 + nb4;
        //IJ.log("nbtotal "+nbtot);
        nbmoy = (double) (nb1 + 2 * nb2 + 3 * nb3 + 4 * nb4) / (double) (nbtot - nb0);
        //IJ.log("nbmoy "+nbmoy);
    }

//private int computeNeigh(ImageProcessor ip,int x,int y){
//	int co=0;
//	if(ip.getPixel(x+1,y)>0) co++;
//	if(ip.getPixel(x-1,y)>0) co++;
//	if(ip.getPixel(x,y+1)>0) co++;
//	if(ip.getPixel(x,y-1)>0) co++;
//
//	return co;
//}
    private int[] getTimeProfile(ImageStack movie, int x, int y) {
        int nbframe = movie.getSize();
        int[] tab = new int[nbframe];
        for (int t = 1; t <= nbframe; t++) {
            tab[t - 1] = movie.getProcessor(t).getPixel(x, y);
        }
        return tab;
    }

    private boolean tabNotEmpty(int[] tab) {
        boolean ok = false;
        int i = 0;
        int le = tab.length;
        while ((i < le) && (tab[i] == 0)) {
            i++;
        }
        if (i < le) {
            ok = true;
        }

        return ok;
    }

    private void drawTimeLine(ByteProcessor timeline, int[] tab, int y) {
        for (int x = 0; x < tab.length; x++) {
            timeline.putPixel(x, y, tab[x]);
        }
    }

    private void analyseTimeLine(int[] tab, int nb) {
        int i = 0;
        int pos0 = 0;
        int pos1 = 0;
        int state0 = 0;
        int state = 0;
        int le = tab.length;
        int ti;
        String res = "";
        ArrayList tablist = new ArrayList();
        int id = 0;
        IJ.log("///////////////////// " + nb);
        while ((pos0 < le) && (pos1 < le)) {
            // to different state
            i = pos0;
            while ((i < le) && (tab[i] == state0)) {
                i++;
            }
            pos1 = i;
            if (i < le) {
                state = tab[pos1];
            } else {
                state = -1;
            }
            ti = (pos1 - pos0);
            if (state >= 0) {
                tablist.add(new String("" + state0 + "-" + state + " " + ti));
                if (state == 0) {
                    if (state0 == 2) {
                        trans10[ti]++;
                    }
                    if (state0 == 3) {
                        trans20[ti]++;
                    }
                    if (state0 == 4) {
                        trans30[ti]++;
                    }
                    if (state0 == 5) {
                        trans40[ti]++;
                    }
                    if (ti > maxtime) {
                        maxtime = ti;
                    }
                }
            }
            state0 = state;
            pos0 = pos1;
            id++;
        }
        // results
        String s;
        for (i = 0; i < tablist.size(); i++) {
            s = (String) tablist.get(i);
            if (s.indexOf("0/") < 0) {
                if (s.indexOf("-0") >= 0) {
                    IJ.log("* " + s);
                } else {
                    IJ.log(" " + s);
                }
            }
        }
    }
}
