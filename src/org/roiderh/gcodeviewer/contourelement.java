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

import java.util.LinkedList;
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearElement2D;

/**
 *
 * @author Herbert Roider <herbert.roider@utanet.at>
 */
   
public class contourelement {
        // shape of the transition element:
        public enum Transition {
                CHAMFER, ROUND
        }
        // G0/G1 for lines and G2/G3 for arc
        public enum Shape {
                LINE, ARC
        }
        public enum Feed {
                RAPID, CUTTING
        }
       // holds the start and end point without transition element:
        public LinkedList<point> points = new LinkedList<>();
        // calculated start point with transition element:
        public Point2D start;
        // calculated end point with transition element:
        public Point2D end;
        // Transition Element chamfer or round between the current and the next element
        public Transition transistion_elem;
        // line or arc
        public Shape shape;
        public double transition_elem_size;
        // only rapid (G0) or cutting (G1)
        public Feed feed;
        // the real linenumber, not N-number or Lable
        public int linenumber;
        /**
         * If the shape was a arc (circle), the radius (CR or B)
         */
        public double radius; 
        // for arc, ccw (counter clockwise) is true for G3, ccw=false for G2
        public boolean ccw = true;
        
        // The element as curve (line or arc) with transition element
        public  CirculinearElement2D curve;
        // The transition element as curve (line or arc)
        public  CirculinearElement2D transition_curve;

}
