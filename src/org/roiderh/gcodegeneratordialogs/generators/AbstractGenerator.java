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
import math.geom2d.circulinear.PolyCirculinearCurve2D;
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
}
