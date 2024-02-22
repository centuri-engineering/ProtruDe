/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package video;

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
public class ParticleSet {

    ArrayList<Particle> particles = null;

    public ParticleSet() {
        particles = new ArrayList<Particle>();
    }

    public void addParticle(Particle pa) {
        particles.add(pa);
    }

    public Particle closest(Particle center, double rad) {
        return closest(center.xcoord, center.ycoord, rad);
    }

    public Particle closest(double cx, double cy, double rad) {
        Particle pa;
        double dist;
        double rad2 = rad * rad;
        double distmin = Double.MAX_VALUE;
        Particle close = null;
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            pa = it.next();
            dist = pa.distanceSquare(cx, cy);
            if ((dist < rad2) && (dist < distmin)) {
                distmin = dist;
                close = pa;
            }
        }

        return close;
    }

    public int getNbParticle() {
        if (particles != null) {
            return particles.size();
        } else {
            return 0;
        }
    }

    public Particle getParticle(int i) {
        if ((particles != null) && (i < getNbParticle())) {
            return particles.get(i);
        } else {
            return null;
        }
    }

    public Particle getLastParticle() {
        int nb = getNbParticle();
        if (nb > 0) {
            return getParticle(nb - 1);
        } else {
            return null;
        }
    }
}
