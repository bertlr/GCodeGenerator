/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.roiderh.gcodegeneratordialogs.generators;

import java.util.ArrayList;
import java.util.Collection;
import math.geom2d.AffineTransform2D;
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearCurve2D;
import math.geom2d.circulinear.CirculinearCurves2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import math.geom2d.circulinear.buffer.BufferCalculator;
import math.geom2d.circulinear.buffer.RoundCapFactory;
import math.geom2d.circulinear.buffer.RoundJoinFactory;
import math.geom2d.conic.CircleArc2D;
import math.geom2d.domain.PolyOrientedCurve2D;
import math.geom2d.line.Line2D;
import math.geom2d.line.LineSegment2D;
import org.roiderh.gcodegeneratordialogs.FunctionConf;
import org.roiderh.gcodeviewer.geometry;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class Parallel extends AbstractGenerator {

    public Parallel(PolyCirculinearCurve2D _orig_contour, FunctionConf _fc, ArrayList<String> _values) {
        super(_orig_contour, _fc, _values);

    }

    @Override
    public String calculate() throws Exception {

        double dist = 0;
        control = 0; // 0 for 840D, 1 for 810

        for (int i = 0; i < fc.arg.size(); i++) {
            if (fc.arg.get(i).name.compareTo("distance") == 0) {
                dist = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("control") == 0) {
                control = Integer.parseInt(values.get(i).trim());
            }

        }

        String output_gcode = "";
      

        PolyOrientedCurve2D new_curve = this.parallel(orig_contour, dist);
        PolyCirculinearCurve2D new_curve_1 = new PolyCirculinearCurve2D(new_curve.curves());
        output_gcode = this.convert2gcode(new_curve_1);

        return output_gcode;
    }
    PolyCirculinearCurve2D parallel(PolyCirculinearCurve2D set, double offset) {
        geometry geo = new geometry();
        RoundJoinFactory jf = new RoundJoinFactory();
        RoundCapFactory bcf = new RoundCapFactory();

        //PolyCirculinearCurve2D set = new PolyCirculinearCurve2D(elements);
        BufferCalculator bufferc = new BufferCalculator(jf, bcf);
        CirculinearCurve2D cc = bufferc.createParallel(set, offset);

        Collection<Point2D> points = CirculinearCurves2D.findSelfIntersections(cc);
        ArrayList<Point2D> points_arr = new ArrayList<>();

        for (Point2D p : points) {
            double dist = set.distance(p);
            System.out.println("dist: " + dist + ": " + (dist - offset));

            if (Math.abs((Math.abs(dist) - Math.abs(offset))) < 0.00001) {
                points_arr.add(p);
            }
        }

        ArrayList<CirculinearElement2D> el = (ArrayList<CirculinearElement2D>) cc.continuousCurves();

        PolyCirculinearCurve2D el_2 = (PolyCirculinearCurve2D) el.get(0);

        ArrayList<CirculinearElement2D> el_3 = new ArrayList<>();
        PolyCirculinearCurve2D el_4 = new PolyCirculinearCurve2D();

        for (int i = 0; i < el_2.size(); i++) {
            CirculinearElement2D elem = (CirculinearElement2D) el_2.get(i);
            el_3.add(elem);

        }

        Point2D begin = null;
        Point2D end = null;
        //ArrayList<Point2D> points_arr_clean = points_arr;
        ArrayList<Point2D> points_arr_used = new ArrayList<>();
        for (int i = 0; i < el_3.size(); i++) {
            CirculinearElement2D elem = (CirculinearElement2D) el_3.get(i);
            boolean contains_begin = false;
            boolean contains_end = false;
            if (begin != null && elem.contains(begin)) {
                contains_begin = true;

            }
            for (Point2D f : points_arr) {
                if (points_arr_used.indexOf(f) >= 0) {
                    continue;
                }
                if (elem.contains(f)) {
                    end = f;
                    contains_end = true;
                    points_arr_used.add(f);
                    break;

                }

            }

            if (contains_begin && contains_end) {
                if (elem.toString().contains("CircleArc2D")) {
                    CircleArc2D c_orig = (CircleArc2D) elem;
                    CircleArc2D c_new = geo.createCircleArc(begin, end, c_orig.supportingCircle().radius(), c_orig.isDirect());
                    el_4.add(c_new);
                } else {
                    LineSegment2D c_new = new LineSegment2D(begin, end);
                    el_4.add(c_new);

                }
                begin = end;
                end = null;

            } else if (contains_begin && !contains_end) {
                if (elem.toString().contains("CircleArc2D")) {
                    CircleArc2D c_orig = (CircleArc2D) elem;
                    CircleArc2D c_new = geo.createCircleArc(begin, elem.lastPoint(), c_orig.supportingCircle().radius(), c_orig.isDirect());
                    el_4.add(c_new);
                } else {
                    LineSegment2D c_new = new LineSegment2D(begin, ((LineSegment2D) elem).lastPoint());
                    el_4.add(c_new);

                }
                begin = null;

            } else if (!contains_begin && contains_end) {
                if (elem.toString().contains("CircleArc2D")) {
                    CircleArc2D c_orig = (CircleArc2D) elem;
                    CircleArc2D c_new = geo.createCircleArc(elem.firstPoint(), end, c_orig.supportingCircle().radius(), c_orig.isDirect());
                    el_4.add(c_new);
                } else {
                    LineSegment2D c_new = new LineSegment2D(elem.firstPoint(), end);
                    el_4.add(c_new);

                }
                begin = end;
            } else {
                // Kein Zwischenelement zwischen Intersection Points:
                if (begin == null) {
                    el_4.add(elem);
                }
            }

        }

        return el_4;

    }
    

}
