/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.roiderh.gcodegeneratordialogs.generators;

import java.util.ArrayList;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import org.roiderh.gcodegeneratordialogs.FunctionConf;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class Reverse extends AbstractGenerator {

    public Reverse(PolyCirculinearCurve2D<CirculinearElement2D> _orig_contour, FunctionConf _fc, ArrayList<String> _values) {
        super(_orig_contour, _fc, _values);

    }

    @Override
    public String calculate() throws Exception {

        
        control = 0; // 0 for 840D, 1 for 810

        for (int i = 0; i < fc.arg.size(); i++) {
            if (fc.arg.get(i).name.compareTo("control") == 0) {
                control = Integer.parseInt(values.get(i).trim());
            }

        }

        String output_gcode = "";
       

        PolyCirculinearCurve2D new_curve = this.orig_contour.reverse();
        
        //PolyCirculinearCurve2D new_curve_1 = new PolyCirculinearCurve2D(new_curve.curves());
        output_gcode = this.convert2gcode(new_curve);

        return output_gcode;
    }

}
