/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package video;

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
public class PopParticle extends ParticleSet {

    double time;

    public PopParticle(double time) {
        super();
        this.time = time;
    }

    public int getNbNeigh(Particle pa, double ax, double ay, double bx, double by, double rad) {
        int nb = 0;
        Particle close;
        close = this.closest(pa.xcoord + ax, pa.ycoord + ay, rad);
        if (close != null) {
            nb++;
        }
        close = this.closest(pa.xcoord - ax, pa.ycoord - ay, rad);
        if (close != null) {
            nb++;
        }
        close = this.closest(pa.xcoord + bx, pa.ycoord + by, rad);
        if (close != null) {
            nb++;
        }
        close = this.closest(pa.xcoord - bx, pa.ycoord - by, rad);
        if (close != null) {
            nb++;
        }

        return nb;
    }
}
