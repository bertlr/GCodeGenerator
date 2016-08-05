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
package org.roiderh.gcodegeneratordialogs.generators;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import math.geom2d.AffineTransform2D;
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import math.geom2d.conic.CircleArc2D;
import math.geom2d.domain.PolyOrientedCurve2D;
import math.geom2d.line.Line2D;
import org.roiderh.gcodegeneratordialogs.FunctionConf;
import org.roiderh.gcodeviewer.contourelement;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class AbstractGenerator {

    protected String g_code;
    protected FunctionConf fc;
    protected ArrayList<String> values;
    protected PolyCirculinearCurve2D orig_contour;
    protected int control = 0; // 0 = 840D, 1 = 810

    public AbstractGenerator(PolyCirculinearCurve2D _orig_contour, FunctionConf _fc, ArrayList<String> _values) {
        orig_contour = _orig_contour;
        fc = _fc;
        values = _values;

    }

    public String calculate() throws Exception {

        return "";
    }

    /**
     * transform and cleanup the contour, remove empty and zero length
     * clean_contour
     *
     * @param contour
     * @return
     */
    protected final PolyCirculinearCurve2D cleanup_contour(LinkedList<contourelement> contour) {
        PolyCirculinearCurve2D clean_contour = new PolyCirculinearCurve2D();
        for (contourelement current_ce : contour) {

            if (current_ce.curve == null) {
                continue;
            }
            if (current_ce.curve.length() == 0) {
                continue;
            }
            clean_contour.add(current_ce.curve);

            if (current_ce.transition_curve != null) {
                clean_contour.add(current_ce.transition_curve);
            }
        }
        return clean_contour;

    }

    public String convert2gcode(PolyCirculinearCurve2D new_curve) {

        String output_gcode = "";
        //PolyOrientedCurve2D new_curve = this.orig_contour.transform(mir_center);
        output_gcode += this.makeComment("Begin of generated contour") + "\n";
        ArrayList<CirculinearElement2D> el = (ArrayList<CirculinearElement2D>) new_curve.curves();
        for (int i = 0; i < el.size(); i++) {
            CirculinearElement2D curve = (CirculinearElement2D) el.get(i);
            if (i == 0) {
                output_gcode += "G1 " + format("X", curve.firstPoint().getY()) + format("Z", curve.firstPoint().getX()) + "\n";
            }
            if (curve.toString().contains("CircleArc2D")) {
                CircleArc2D c = (CircleArc2D) curve;
                if (c.isDirect()) {
                    output_gcode += "G3 ";
                } else {
                    output_gcode += "G2 ";
                }
                output_gcode += format("X", curve.lastPoint().getY()) + format("Z", curve.lastPoint().getX()) + format("R", c.supportingCircle().radius()) + "\n";
            } else {
                output_gcode += "G1 " + format("X", curve.lastPoint().getY()) + format("Z", curve.lastPoint().getX()) + "\n";
            }
        }
        output_gcode += this.makeComment("End of generated contour") + "\n";
        return output_gcode;
    }

    /**
     *
     * @param axis "X" or "Z"
     * @param d value
     * @return formatted String like: X2.52, the x-axis is converted from radius
     * to diameter.
     */
    private String format(String axis, double d) {
        if (axis == "x" || axis == "X") {
            d *= 2.0;
            return String.format(Locale.US, " X%.2f", d);
        } else if (axis == "R") { // Radius
            if (control == 1) { // 810
                return String.format(Locale.US, " B%.2f", d);
            } else {  // 840D
                return String.format(Locale.US, " CR=%.2f", d);
            }

        } else {
            return String.format(Locale.US, " Z%.2f", d);
        }

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

}