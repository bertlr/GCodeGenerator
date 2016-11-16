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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import math.geom2d.conic.CircleArc2D;
import org.roiderh.gcodegeneratordialogs.FunctionConf;
import org.roiderh.gcodeviewer.contourelement;

/**
 * Base Class for Generators
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class AbstractGenerator {

    protected String g_code;
    protected FunctionConf fc;
    protected ArrayList<String> values;
    protected PolyCirculinearCurve2D<CirculinearElement2D> orig_contour;
    protected int control = 0; // 0 = 840D, 1 = 810

    /**
     *
     * @param _orig_contour Contur
     * @param _fc Parameter description
     * @param _values Parameters
     */
    public AbstractGenerator(PolyCirculinearCurve2D<CirculinearElement2D> _orig_contour, FunctionConf _fc, ArrayList<String> _values) {
        orig_contour = _orig_contour;
        fc = _fc;
        values = _values;
    }

    /**
     *
     *
     * @return g-code
     * @throws Exception
     */
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
    protected final PolyCirculinearCurve2D<CirculinearElement2D> cleanup_contour(LinkedList<contourelement> contour) {
        PolyCirculinearCurve2D<CirculinearElement2D> clean_contour = new PolyCirculinearCurve2D<>();
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

    /**
     * convert the curve to a g-code
     *
     * @param new_curve
     * @return g-code
     */
    public String convert2gcode(PolyCirculinearCurve2D<CirculinearElement2D> new_curve) {

        String output_gcode = "";
        double prevX = 0.0;
        double prevY = 0.0;
        double curX;
        double curY;

        output_gcode += makeComment(fc.name.trim()) + "\n";
        for (int i = 0; i < fc.arg.size(); i++) {
            output_gcode += this.makeComment(fc.arg.get(i).name + "=" + values.get(i).trim()) + "\n";
        }

        output_gcode += this.makeComment("Begin of generated contour") + "\n";
        ArrayList<CirculinearElement2D> el = (ArrayList<CirculinearElement2D>) new_curve.curves();
        for (int i = 0; i < el.size(); i++) {
            CirculinearElement2D curve = el.get(i);
            if (i == 0) {
                curX = curve.firstPoint().getX();
                curY = curve.firstPoint().getY();

                output_gcode += "G1" + format("X", curY) + format("Z", curX) + "\n";
                prevX = curX;
                prevY = curY;

            }
            curX = curve.lastPoint().getX();
            curY = curve.lastPoint().getY();

            if (curve.toString().contains("CircleArc2D")) {
                CircleArc2D c = (CircleArc2D) curve;
                if (c.isDirect()) {
                    output_gcode += "G3";
                } else {
                    output_gcode += "G2";
                }
                output_gcode += format("R", c.supportingCircle().radius());

            } else {
                output_gcode += "G1";
            }
            if (Math.abs(curY - prevY) > 0.001) {
                output_gcode += format("X", curY);
            }
            if (Math.abs(curX - prevX) > 0.001) {
                output_gcode += format("Z", curX);
            }
            output_gcode += "\n";

            prevX = curX;
            prevY = curY;

        }
        output_gcode += this.makeComment("End of generated contour") + "\n";
        return output_gcode;
    }

    /**
     *
     * @param axis "X", "Z" or "R"
     * @param d value
     * @return formatted String like: X2.52, the x-axis is converted from radius
     * to diameter.
     */
    private String format(String axis, double d) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern("0.###");
        String ret = new String();

        if (axis == "x" || axis == "X") {
            d *= 2.0;
            ret = new String(" X" + df.format(d).toString());
        } else if (axis == "R") { // Radius
            if (control == 1) { // 810
                ret = new String(" B" + df.format(d).toString());
            } else {  // 840D
                ret = new String(" CR=" + df.format(d).toString());
            }

        } else {
            ret = new String(" Z" + df.format(d).toString());
        }
        return ret;
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
