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
import org.roiderh.gcodegeneratordialogs.GcodeGenerator;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class Roughing extends AbstractGenerator {

    public Roughing(PolyCirculinearCurve2D<CirculinearElement2D> _orig_contour, FunctionConf _fc, ArrayList<String> _values) {
        super(_orig_contour, _fc, _values);
     
    }

    @Override
    public String calculate() throws Exception {

        double depth = 0.5;
        double toolnoseradius = 0.4;
        double mat_allowance_x = 0.2;
        double mat_allowance_z = 0.1;
        double plunging_angle = 45.0 * Math.PI / 180.0;
        control = 0; // 0 for 840D, 1 for 810

        for (int i = 0; i < fc.arg.size(); i++) {
            if (fc.arg.get(i).name.compareTo("depth") == 0) {
                depth = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("toolnoseradius") == 0) {
                toolnoseradius = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("control") == 0) {
                control = Integer.parseInt(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("mat_allowance_x") == 0) {
                mat_allowance_x = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("mat_allowance_z") == 0) {
                mat_allowance_z = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("plunging_angle") == 0) {
                plunging_angle = Double.parseDouble(values.get(i).trim());
                plunging_angle *= Math.PI / 180;
            }

        }

        String output_gcode = "";

        GcodeGenerator gcode = new GcodeGenerator();
        gcode.control = control;
        output_gcode = gcode.rough(orig_contour, depth, toolnoseradius, new Point2D(mat_allowance_z, mat_allowance_x), plunging_angle);

        return output_gcode;
    }

}
