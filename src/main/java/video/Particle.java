/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package video;

/**
 *
 **
 * /**
 * Copyright (C) 2008- 2011 Thomas Boudier
 *
 *
 *
 * This file is part of mcib3d
 *
 * mcib3d is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author thomas
 */
public class Particle {

    double xcoord;
    double ycoord;
    int nbneigh;
    PathParticle path = null;
    static int nbpart = -1;
    int id;
    public double frame;

    public Particle(double xcoord, double ycoord, double time) {
        this.xcoord = xcoord;
        this.ycoord = ycoord;
        this.frame = time;
        nbpart++;
        id = nbpart;
    }

    public int getNbneigh() {
        return nbneigh;
    }

    public void setNbneigh(int nbneigh) {
        this.nbneigh = nbneigh;
    }

    public PathParticle getPath() {
        return path;
    }

    public void setPath(PathParticle path) {
        this.path = path;
    }

    double distanceSquare(Particle pa) {
        return distanceSquare(pa.xcoord, pa.ycoord);
    }

    double distanceSquare(double cx, double cy) {
        return ((cx - this.xcoord) * (cx - this.xcoord) + (cy - this.ycoord) * (cy - this.ycoord));
    }
}
