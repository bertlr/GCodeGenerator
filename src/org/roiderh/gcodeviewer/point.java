/*
 * Copyright (C) 2014 by Herbert Roider <herbert.roider@utanet.at>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.roiderh.gcodeviewer;

import math.geom2d.Point2D;

/**
 *
 * @author Herbert Roider <herbert.roider@utanet.at>
 */
public class point implements Cloneable {

        public double x;
        public double y;

        @Override
        public point clone() throws CloneNotSupportedException {
                return (point) super.clone();
        }

        public boolean equals(point p) {
                if (this == p) {
                        return true;
                }
                if (!(p instanceof point)) {
                        return false;
                }
                return (x == p.x)
                        && (y == p.y);

        }

        public Point2D createPoint2D() {
                return new Point2D(this.x, this.y);
        }

}
