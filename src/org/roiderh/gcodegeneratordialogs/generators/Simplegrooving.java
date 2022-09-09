/*
 * Copyright (C) 2022 Herbert Roider <herbert@roider.at>
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
//import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import org.roiderh.gcodegeneratordialogs.FunctionConf;
//import org.roiderh.gcodegeneratordialogs.GroovingGenerator;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class Simplegrooving extends AbstractGenerator {

    public Simplegrooving(PolyCirculinearCurve2D<CirculinearElement2D> _orig_contour, FunctionConf _fc, ArrayList<String> _values) {
        super(_orig_contour, _fc, _values);

    }

    @Override
    public String calculate() throws Exception {

        double start_x = 20.0;
        double start_z = 0.0;
        double ground_x = 0.0;
        double safety_dist = 0.2;
        double depth = 3.0;
        control = 0; // 0 for 840D, 1 for 810

        for (int i = 0; i < fc.arg.size(); i++) {

            if (fc.arg.get(i).name.compareTo("start_x") == 0) {
                start_x = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("start_z") == 0) {
                start_z = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("ground_x") == 0) {
                ground_x = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("safety_dist") == 0) {
                safety_dist = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("depth") == 0) {
                depth = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("control") == 0) {
                control = Integer.parseInt(values.get(i).trim());
            }

        }

        String output_gcode = "";
        output_gcode += this.makeComment("Begin of generated contour") + "\n";
        output_gcode += makeComment(fc.name.trim()) + "\n";
        for (int i = 0; i < fc.arg.size(); i++) {
            output_gcode += this.makeComment(fc.arg.get(i).name + "=" + values.get(i).trim()) + "\n";
        }
        double dCount = (int) Math.ceil((start_x - ground_x) / depth);
        depth = ((start_x - ground_x) / dCount);
        output_gcode += "G0 X" + (start_x + safety_dist) + " Z" + start_z + "\n";

        for (int i = 0; i < (int) dCount; i++) {
            double new_x = start_x - i * depth;
            output_gcode += "G0 X" + (new_x + safety_dist) + "\n";
            output_gcode += "G1 X" + (new_x - depth) + "\n";
            output_gcode += "G0 X" + (start_x + safety_dist) + "\n";
        }
        output_gcode += this.makeComment("End of generated contour") + "\n";
        return output_gcode;
    }
}
