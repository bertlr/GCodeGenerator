/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.roiderh.gcodegeneratordialogs.generators;

import java.util.ArrayList;
import java.util.Locale;
import math.geom2d.AffineTransform2D;
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import math.geom2d.conic.CircleArc2D;
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
