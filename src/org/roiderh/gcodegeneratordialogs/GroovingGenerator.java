/*
 * Copyright (C) 2016 Herbert Roider <herbert@roider.at>
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
package org.roiderh.gcodegeneratordialogs;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import math.geom2d.domain.PolyOrientedCurve2D;
import math.geom2d.line.LineSegment2D;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class GroovingGenerator {

    /**
     * only for the format of the commments Siemens Sinumerik 840D=0, 810=1
     */
    public int control = 0;

    /**
     *
     * @param _elements contour elements
     * @param depth depth of grooving in radius negative value for outside and
     * positive for inside
     * @param begin x-value at start
     * @param end x-value at end
     * @param starty y-value at start in radius
     * @param oversize material allowance for x and y (absolute values)
     * @param width width of the cutter
     * @param left zero point of the cutter left or right
     * @param overlap factor for overlaping
     * @return
     * @throws Exception
     */
    public String calcToolpath(PolyCirculinearCurve2D<CirculinearElement2D> _elements, double depth, double begin, double end, double starty, Point2D oversize, double width, boolean left, double overlap) throws Exception {
        String gcode = "";
        PolyOrientedCurve2D contour = new PolyOrientedCurve2D(_elements);
        ArrayList<Point2D> intersections = new ArrayList<>();
        double curr_pos_x = begin - oversize.getX() - 0.001;
        boolean ready = false;
        for (int j = 0; j < 200; j++) {
            if (ready == true) {
                break;
            }
            if (j >= 199) {
                Exception newExcept = new Exception("too much iterations in length");
                throw newExcept;
            }
            if (curr_pos_x < end + width) {
                curr_pos_x = end + width + oversize.getX() + 0.001;
                ready = true;
            }

            double current_depth_step = depth;
            Point2D pos = new Point2D(curr_pos_x, starty);
            if (left == false) {
                pos = pos.translate(-width, 0.0);
            }
            gcode += "G0 " + this.format("X", pos.getY()) + " " + this.format("Z", pos.getX()) + "\n";
            int i = 0;
            for (i = 0; i < 100; i++) {
                if (i >= 99) {
                    Exception newExcept = new Exception("too much iterations in depth");
                    throw newExcept;
                }
                if (Math.abs(current_depth_step) < 0.001) {
                    gcode += "G1 " + this.format("X", pos.getY()) + "\n";
                    gcode += "G0 " + this.format("X", starty) + "\n";
                    break;
                }               
                pos = pos.translate(0, current_depth_step);
                LineSegment2D mainside;
                LineSegment2D secondside;
                LineSegment2D ground;

                double sword_length = 1000.0;
                double oversize_y = -oversize.getY();
                if (depth > 0) {
                    // for inside
                    sword_length *= -1.0;
                    oversize_y *= -1.0;
                }

                if (left) {
                    mainside = new LineSegment2D(pos.getX() + oversize.getX(), pos.getY() + oversize_y, pos.getX() + oversize.getX(), sword_length);
                    secondside = new LineSegment2D(pos.getX() - width - oversize.getX(), pos.getY() + oversize_y, pos.getX() - width - oversize.getX(), sword_length);

                } else {
                    mainside = new LineSegment2D(pos.getX() - oversize.getX(), pos.getY() + oversize_y, pos.getX() - oversize.getX(), sword_length);
                    secondside = new LineSegment2D(pos.getX() + width + oversize.getX(), pos.getY() + oversize_y, pos.getX() + width + oversize.getX(), sword_length);

                }
                ground = new LineSegment2D(mainside.firstPoint(), secondside.firstPoint());

                intersections = (ArrayList<Point2D>) contour.intersections(mainside);
                if (intersections.isEmpty()) {
                    intersections = (ArrayList<Point2D>) contour.intersections(secondside);
                }
                if (intersections.isEmpty()) {
                    intersections = (ArrayList<Point2D>) contour.intersections(ground);
                }
                // no intersection at full allowed depth, break with max. depth:
                if(i==0 && intersections.isEmpty()){
                    double y = starty + depth;
                    gcode += "G1 " + this.format("X", y) + "\n";
                    gcode += "G0 " + this.format("X", starty) + "\n";
                    break;
                    
                }
                
                
                current_depth_step /= 2.0;
                if (intersections.isEmpty()) {
                    if (depth < 0) {
                        if (current_depth_step > 0) {
                            current_depth_step *= -1.0;
                        }
                    } else if (current_depth_step < 0) {
                        current_depth_step *= -1.0;
                    }

                } else if (depth < 0) {
                    if (current_depth_step < 0) {
                        current_depth_step *= -1.0;
                    }
                } else if (current_depth_step > 0) {
                    current_depth_step *= -1.0;
                }

            }

            curr_pos_x -= width - overlap * width;

        }
        
        return gcode;
    }

    /**
     *
     * @param text
     * @return commented text like: "( I am a comment )"
     */
    private String makeComment(String text) {
        if (control == 1) {
            return " ( " + text + " ) ";
        } else {
            return " ; " + text;
        }
    }

    /**
     *
     * @param axis "X" or "Z"
     * @param d value
     * @return formatted String like: X2.52, the x-axis is converted from radius
     * to diameter.
     */
    private String format(String axis, double d) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern("0.###");
        if (axis == "x" || axis == "X") {
            d *= 2.0;
            return "X" + df.format(d);
        }
        return "Z" + df.format(d);

    }

}
