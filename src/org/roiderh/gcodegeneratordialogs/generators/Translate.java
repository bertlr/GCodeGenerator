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
import java.util.Iterator;
import math.geom2d.AffineTransform2D;
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import math.geom2d.domain.PolyOrientedCurve2D;
import math.geom2d.line.Line2D;
import org.roiderh.gcodegeneratordialogs.FunctionConf;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class Translate extends AbstractGenerator {

    /**
     *
     * @param _orig_contour
     * @param _fc
     * @param _values
     */
    public Translate(PolyCirculinearCurve2D<CirculinearElement2D> _orig_contour, FunctionConf _fc, ArrayList<String> _values) {
        super(_orig_contour, _fc, _values);

    }

    @Override
    public String calculate() throws Exception {

        double y = 0; // x-axis in maschine
        double x = 0;  // z-axis in maschine
        control = 0; // 0 for 840D, 1 for 810
        int repeated = 0; // for multible translations
        for (int i = 0; i < fc.arg.size(); i++) {
            if (fc.arg.get(i).name.compareTo("x") == 0) {
                y = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("z") == 0) {
                x = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("repeated") == 0) {
                repeated = Integer.parseInt(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("control") == 0) {
                control = Integer.parseInt(values.get(i).trim());
            }

        }

        String output_gcode = "";

        PolyCirculinearCurve2D<CirculinearElement2D> new_curve_1 = new PolyCirculinearCurve2D<>();
        Point2D conn_line_first_point = new Point2D();
        Point2D conn_line_last_point = new Point2D();
        for (int i = 0; i <= repeated; i++) {
            AffineTransform2D tra = AffineTransform2D.createTranslation((i + 1) * x, (i + 1) * y);

            PolyOrientedCurve2D new_curve = this.orig_contour.transform(tra);

            conn_line_last_point = new_curve.firstPoint();
            if (i > 0) {
                new_curve_1.add(new Line2D(conn_line_first_point, conn_line_last_point));
            }
            for (Iterator iterator = new_curve.curves().iterator(); iterator.hasNext();) {
                new_curve_1.add((CirculinearElement2D) iterator.next());
            }
            conn_line_first_point = new_curve.lastPoint();

        }
        output_gcode += this.convert2gcode(new_curve_1);

        return output_gcode;
    }

}
