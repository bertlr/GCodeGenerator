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
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import org.roiderh.gcodegeneratordialogs.FunctionConf;
import org.roiderh.gcodegeneratordialogs.GroovingGenerator;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class Grooving extends AbstractGenerator {

    public Grooving(PolyCirculinearCurve2D<CirculinearElement2D> _orig_contour, FunctionConf _fc, ArrayList<String> _values) {
        super(_orig_contour, _fc, _values);

    }

    @Override
    public String calculate() throws Exception {

        double depth = 50.0;
        double start_z = 0.0;
        double start_x = 20.0;
        double end_z = -10.0;
        double width = 2.0;
        double mat_allowance_x = 0.1;
        double mat_allowance_z = 0.1;
        double overlap = 0.7;
        int left = 0;
        control = 0; // 0 for 840D, 1 for 810

        for (int i = 0; i < fc.arg.size(); i++) {
            if (fc.arg.get(i).name.compareTo("start_z") == 0) {
                start_z = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("start_x") == 0) {
                start_x = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("end_z") == 0) {
                end_z = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("depth") == 0) {
                depth = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("width") == 0) {
                width = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("left") == 0) {
                left = Integer.parseInt(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("overlap") == 0) {
                overlap = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("control") == 0) {
                control = Integer.parseInt(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("mat_allowance_x") == 0) {
                mat_allowance_x = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("mat_allowance_z") == 0) {
                mat_allowance_z = Double.parseDouble(values.get(i).trim());
            }

        }

        String output_gcode = "";

        GroovingGenerator gcode = new GroovingGenerator();
        gcode.control = control;
        Boolean is_left = true;
        if(left == 0){
            is_left = false;
        }
        start_x /= 2.0;
        output_gcode = gcode.calcToolpath(orig_contour, depth, start_z, end_z, start_x, new Point2D(mat_allowance_z, mat_allowance_x), width, is_left, overlap);
        return output_gcode;
    }

}
