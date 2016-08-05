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
import math.geom2d.AffineTransform2D;
import math.geom2d.Point2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import math.geom2d.domain.PolyOrientedCurve2D;
import math.geom2d.line.Line2D;
import org.roiderh.gcodegeneratordialogs.FunctionConf;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class Mirror extends AbstractGenerator {

   

    public Mirror(PolyCirculinearCurve2D _orig_contour, FunctionConf _fc, ArrayList<String> _values) {
        super(_orig_contour, _fc, _values);

    }

    @Override
    public String calculate() throws Exception {

        int vertical = 0;
        control = 0; // 0 for 840D, 1 for 810

        for (int i = 0; i < fc.arg.size(); i++) {
            if (fc.arg.get(i).name.compareTo("vertical") == 0) {
                vertical = Integer.parseInt(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("control") == 0) {
                control = Integer.parseInt(values.get(i).trim());
            }

        }

        String output_gcode = "";
        AffineTransform2D mir_center;
        if (vertical == 0) {
            mir_center = AffineTransform2D.createLineReflection(new Line2D(new Point2D(0, 0), new Point2D(0, 1)));
        } else {
            mir_center = AffineTransform2D.createLineReflection(new Line2D(new Point2D(0, 0), new Point2D(1, 0)));
        }

        PolyOrientedCurve2D new_curve = this.orig_contour.transform(mir_center);
        PolyCirculinearCurve2D new_curve_1 = new PolyCirculinearCurve2D(new_curve.curves());
        output_gcode = this.convert2gcode(new_curve_1);
       
        return output_gcode;
    }

  
}
