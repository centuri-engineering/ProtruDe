/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package video;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import mcib3d.image3d.legacy.IntImage3D;
import mcib3d.image3d.processing.FastFilters3D;

import java.awt.*;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Iterator;

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
 */
public class Analyse_Coord implements PlugIn {

    // directions
    private double ax = 11.84;
    private double ay = -6.72;
    private double bx = 9.09;
    private double by = 8.93;
    private int rad = 6;
    // image_size
    private int wi = 512;
    private int he = 256;
    private int nbf = 452;
    private double time = 250; // ms 
    private boolean visu = false;

    public void run(String arg) {
        if (!Dialogue()) {
            return;
        }

        OpenDialog op = new OpenDialog("coordinates", "");
        String dir = op.getDirectory();
        String coorfile = op.getFileName();

        double firstframe = 1;

        ArrayList<PopParticle> PopParticles = new ArrayList<PopParticle>();
        PopParticle pop = new PopParticle(firstframe);

        try {
            BufferedReader bf = new java.io.BufferedReader(new java.io.FileReader(dir + coorfile));
            // first line, header
            String line = bf.readLine();
            //IJ.log("header : "+line);
            // reading all frames and build a movie
            line = bf.readLine();

            String[] data;
            double frame;
            double xcoord;
            double ycoord;

            while (line != null) {
                data = line.split(",");
                frame = Double.parseDouble(data[0]);
                xcoord = Double.parseDouble(data[1]);
                ycoord = Double.parseDouble(data[2]);
                if (frame != firstframe) {
                    //IJ.log("Reading frame " + firstframe);
                    PopParticles.add(pop);
                    //analyseParticleNeigh(particles1,particles0,firstframe,rad);
                    firstframe = frame;
                    pop = new PopParticle(frame - 1);
                } else {
                    pop.addParticle(new Particle(xcoord, ycoord, frame - 1));
                }
                line = bf.readLine();
            }
        } catch (java.io.IOException e) {
        }
        ArrayList<PathParticle> paths = new ArrayList<PathParticle>();
        for (int f = 0; f < 1; f++) {
            constructPath(paths, PopParticles, f);
        }
        IJ.log("nb path=" + paths.size());
        WritePath(paths);
        // create image
        IntImage3D pathimage = createImagePath(paths);
        new ImagePlus("paths", pathimage.getStack()).show();
        if (visu) {
            IntImage3D pathimage2 = new IntImage3D(wi, he, nbf);
            pathimage.filterGeneric(pathimage2, 2, 2, 0, 0, nbf, FastFilters3D.MAX);
            new ImagePlus("paths_visu", pathimage2.getStack()).show();
        }
    }

    private void WritePath(ArrayList<PathParticle> paths) {
        IJ.log("X Y frame time neigh");
        for (PathParticle path : paths) {
            IJ.log("************* Path " + path.id + " *********************************");
            for (int p = 0; p < path.getNbParticle(); p++) {
                Particle pa = path.getParticle(p);
                IJ.log(pa.xcoord + " " + pa.ycoord + " " + (int) (pa.frame) + " " + (pa.frame * time) + " " + pa.nbneigh);
            }
        }
    }

    private IntImage3D createImagePath(ArrayList<PathParticle> paths) {
        IntImage3D ima = new IntImage3D(wi, he, nbf);
        Iterator<PathParticle> it = paths.iterator();
        int x0, y0;
        while (it.hasNext()) {
            PathParticle path = it.next();
            x0 = (int) path.getParticle(0).xcoord;
            y0 = (int) path.getParticle(0).ycoord;
            for (int p = 0; p < path.getNbParticle(); p++) {
                Particle pa = path.getParticle(p);
                ima.putPixel(x0, y0, (int) pa.frame, pa.nbneigh + 1);
            }
        }


        return ima;
    }

    private void constructPath(ArrayList<PathParticle> paths, ArrayList<PopParticle> pops, int t) {
        int nb = paths.size();
        // init paths from frame
        if (t >= pops.size()) {
            return;
        }

        PopParticle pop = pops.get(t);
        for (int p = 0; p < pop.getNbParticle(); p++) {
            Particle pa = pop.getParticle(p);
            pa.nbneigh = pop.getNbNeigh(pa, ax, ay, bx, by, rad);
            if (pa.path == null) {
                PathParticle path = new PathParticle();
                path.addParticle(pa);
                paths.add(path);
            }
        }
        // try to extend each  path
        for (int p = nb; p < paths.size(); p++) {
            PathParticle path = paths.get(p);
            boolean nextp = true;
            while (nextp) {
                nextp = false;
                Particle pa = path.getLastParticle();
                if (pa != null) {
                    double fra = pa.frame;
                    if (fra + 1 < pops.size()) {
                        PopParticle nextPop = pops.get((int) (fra + 1));
                        if (nextPop != null) {
                            Particle close = nextPop.closest(pa, rad);
                            if (close != null) {
                                path.addParticle(close);
                                nextp = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean Dialogue() {
        GenericDialog gd = new GenericDialog("Coord Parameter");
        gd.addMessage("Vector_A", new Font("Arial", Font.BOLD, 12));
        gd.addNumericField("ax", ax, 2);
        gd.addNumericField("ay", ay, 2);
        gd.addMessage("Vector_B", new Font("Arial", Font.BOLD, 12));
        gd.addNumericField("bx", bx, 2);
        gd.addNumericField("by", by, 2);
        gd.addMessage("Parameters", new Font("Arial", Font.BOLD, 12));
        gd.addNumericField("radius search", rad, 0, 5, "pix");
        gd.addNumericField("TimeFrame", time, 2, 10, "ms");
        gd.addCheckbox("Create visu", false);
        gd.showDialog();
        ax = gd.getNextNumber();
        ay = gd.getNextNumber();
        bx = gd.getNextNumber();
        by = gd.getNextNumber();
        rad = (int) gd.getNextNumber();
        time = gd.getNextNumber();
        visu = gd.getNextBoolean();

        return gd.wasOKed();
    }
}
/*

function analyseParticleNeigh
(part
,partold
,frame
,rad


){
if (partold == null) {
return;
}
var it = partold.iterator();
var p;
var p0;
var pp = 0;
while (it.hasNext()) {
p = it.next();
pp = 0;
rt.incrementCounter();
rt.addValue("XPart", p.getX());
rt.addValue("YPart", p.getY());
rt.addValue("Frame", frame);
p0 = nbParticlePresent(partold, p.getX() + ax, p.getY() + ay, rad);
if (p0 > 0) {
rt.addValue("N0", 1);
pp++;
} else {
rt.addValue("N0", 0);
}
p0 = nbParticlePresent(partold, p.getX() - ax, p.getY() - ay, rad);
if (p0 > 0) {
rt.addValue("N1", 1);
pp++;
} else {
rt.addValue("N1", 0);
}
p0 = nbParticlePresent(partold, p.getX() + bx, p.getY() + by, rad);
if (p0 > 0) {
rt.addValue("N2", 1);
pp++;
} else {
rt.addValue("N2", 0);
}
p0 = nbParticlePresent(partold, p.getX() - bx, p.getY() - by, rad);
if (p0 > 0) {
rt.addValue("N3", 1);
pp++;
} else {
rt.addValue("N3", 0);
}
rt.addValue("NbN", pp);
if (part == null) {
rt.addValue("Next", -1);
} else {
rt.addValue("Next", nbParticlePresent(part, p.getX(), p.getY(), rad));
}
}
}

function nbParticlePresent
(part
,cx
,cy
,r


){
var it = part.iterator();
var p, d2, nn = 0;
while (it.hasNext()) {
p = it.next();
d2 = (p.getX() - cx) * (p.getX() - cx) + (p.getY() - cy) * (p.getY() - cy);
if (d2 < r * r) {
nn++;
}
}
return nn;
}


 */
