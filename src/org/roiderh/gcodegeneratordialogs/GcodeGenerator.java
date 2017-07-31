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
package org.roiderh.gcodegeneratordialogs;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import math.geom2d.domain.PolyOrientedCurve2D;
import math.geom2d.line.Line2D;
import math.geom2d.line.LineSegment2D;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class GcodeGenerator {

    /**
     * only for the format of the commments Siemens Sinumerik 840D=0, 810=1
     */
    public int control = 0;
    /**
     * only for debugging:
     */
    public ArrayList< ArrayList<Point2D>> intersect_p = new ArrayList<>();
    private PolyCirculinearCurve2D<CirculinearElement2D> orig_contour;

    /**
     *
     * @param _elements contour, y is the radius, x is the length
     * @param depth max depth in radius
     * @param toolNoseRadius
     * @param oversize Material allowance in x (z-axis) and y (x-axis)
     * @param plunging_angle in rad! 1 to 90 degree
     * @return G-code
     */
    public String rough(PolyCirculinearCurve2D<CirculinearElement2D> _elements, double depth, double toolNoseRadius, Point2D oversize, double plunging_angle) throws Exception {
        orig_contour = _elements;
        String gcode = makeComment("Abspantiefe: " + String.valueOf(depth)) + "\n";

        // Innencontour:
        if (_elements.firstPoint().y() > _elements.lastPoint().y()) {
            depth *= -1.0;
            oversize = oversize.scale(1.0, -1.0);
        }

        // create a copy
        PolyCirculinearCurve2D<CirculinearElement2D> elements = new PolyCirculinearCurve2D<>(_elements.curves());

        // Am Anfang eine Linie anhängen, damit die Contour vorne geschlossen ist:
        elements.add(0, new LineSegment2D(new Point2D(elements.firstPoint().getX(), elements.lastPoint().y()), elements.firstPoint()));

        gcode += makeComment("Schneidenradius: " + String.valueOf(toolNoseRadius)) + "\n";
        gcode += makeComment("Aufmass x=" + String.valueOf(oversize.getY()) + " , z=" + String.valueOf(oversize.getX())) + "\n";
        gcode += makeComment("Eintauchwinkel " + String.format(Locale.US, "%.2f", plunging_angle * 180.0 / Math.PI)) + "\n";

        ArrayList<LineSegment2D> lines = this.getLines(elements, depth);

        gcode += this.rough_part(elements, depth, toolNoseRadius, oversize, plunging_angle, lines);
        gcode += makeComment("End of generated code") + "\n";
        return gcode;
    }

    /**
     *
     * @param elements contour
     * @param depth a negative number is for inside roughing
     * @param toolNoseRadius
     * @param oversize material allowance
     * @param plunging_angle
     * @param lines the layers to roughing
     * @return G-code
     * @throws Exception
     */
    private String rough_part(PolyCirculinearCurve2D<CirculinearElement2D> elements, double depth, double toolNoseRadius, Point2D oversize, double plunging_angle, ArrayList<LineSegment2D> lines) throws Exception {
        String gcode = "";

        double tnrc = toolNoseRadius * (1 + Math.tan(0.5 * plunging_angle)); // only for undercut elements to avoid a crash with the contour because of the tool nose radius

        Point2D prev_p2 = null; // calculated with oversize, ..
        Point2D prev_orig_p2 = null; // Point directly from original line (layer)

        for (int i = 0; i < lines.size(); i++) {
            Point2D p1 = null;
            Point2D p2 = null;
            Point2D p3 = null;
            Point2D p4 = null;

            LineSegment2D l = lines.get(i);

            p2 = l.firstPoint();
            p3 = l.lastPoint();

            // this would be the next undercut
            if (prev_orig_p2 != null) {
                // Inner
                if (depth < 0) {
                    if (prev_orig_p2.getY() >= p2.getY()) {
                        gcode += makeComment("Undercut") + "\n";
                        gcode += "G0 " + this.format("X", (elements.lastPoint().getY()) + 0.1 * depth + oversize.getY()) + "\n";
                    }
                } else if (prev_orig_p2.getY() <= p2.getY()) {
                    gcode += makeComment("Undercut") + "\n";
                    gcode += "G0 " + this.format("X", (elements.lastPoint().getY()) + 0.1 * depth + oversize.getY()) + "\n";

                }
            }
            prev_orig_p2 = p2;

            p1 = p2.translate(Math.abs(1.1 * depth) / Math.tan(plunging_angle), (1.1 * depth));

            // Startpunkte für eine Schicht nach hinten verschieben, damit nicht mit G0 ins Mat. gefahren wird:
            if (prev_p2 != null && p2.getX() < elements.firstPoint().getX()) {
                double a = prev_p2.getX() - p1.getX();
                if (a < 0) {
                    p2 = p2.translate(a, 0);
                    p1 = p1.translate(a, 0);
                }
            }

            // auf kollision testen, nur bei Hinterschnitt           
            for (int j = 0; j < 100; j++) {
                Line2D eintauchweg = new Line2D(p1.translate(+oversize.getX() + tnrc, 0), p2.translate(+oversize.getX() + tnrc, 0));
                //eintauchweg = eintauchweg
                java.util.Collection<Point2D> inters = this.orig_contour.intersections(eintauchweg);
                if (inters.size() <= 0) {
                    break;
                }
                p2 = p2.translate(-oversize.getX() - 0.05, 0);
                p1 = p1.translate(-oversize.getX() - 0.05, 0);

            }

            p1 = p1.translate(0, oversize.getY());
            p2 = p2.translate(0, oversize.getY());
            p3 = p3.translate(oversize.getX(), oversize.getY());
            p4 = p3.translate(Math.abs(0.7 * depth) / Math.tan(plunging_angle), (0.7 * depth));

            if (p2.getX() - p3.getX() < 0) {
                //System.out.println("Eintauchen in Hinterschnitt macht keinen Sinn, weil Schneidenradius zu groß ist");
                break;
            }
            System.out.println(p2.toString() + " -- " + p3.toString());
            gcode += "G0 " + this.format("X", p1.getY()) + " " + this.format("Z", p1.getX()) + "\n";
            gcode += "G1 " + this.format("X", p2.getY()) + " " + this.format("Z", p2.getX()) + "\n"; // move to the depth of the cut
            gcode += "G1 " + this.format("Z", p3.getX()) + "\n";                                    // cut the layer
            gcode += "G1 " + this.format("X", p4.getY()) + " " + this.format("Z", p4.getX()) + "\n"; // drive away from contour
            prev_p2 = p2;

        }
        gcode += "G0 " + this.format("X", elements.lastPoint().getY() + 0.1 * depth + oversize.getY()) + "\n";
        gcode += "G0 " + this.format("Z", elements.firstPoint().getX()) + "\n";

        return gcode;
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

    /**
     * Sorter for the cutting layers
     */
    public class LineCompare implements Comparator<LineSegment2D> {

        /**
         * true if outside of the workpice
         */
        boolean outer = false;

        public LineCompare(boolean _outer) {

            outer = _outer;
        }

        /**
         * Outside contour:
         * <pre>
         * |-----------1------------|
         * |---4-----|    |----2----|
         * |---5----|     |----3----|
         * </pre>
         *
         * @param a
         * @param b
         * @return
         */
        @Override
        public int compare(LineSegment2D a, LineSegment2D b) {
            if (a.lastPoint().getX() > b.firstPoint().getX()) {
                return -1;
            }
            if (b.lastPoint().getX() > a.firstPoint().getX()) {
                return 1;
            }
            if (outer == false) {
                if (a.firstPoint().getY() < b.firstPoint().getY()) {
                    return -1;
                } else {
                    return 1;
                }
            } else if (a.firstPoint().getY() < b.firstPoint().getY()) {
                return 1;
            } else {
                return -1;
            }

        }
    }

    /**
     * calculate the cutting layers and sort them
     *
     * @param elements
     * @param depth a negative number is for inside roughing
     * @return
     * @throws Exception
     */
    private ArrayList<LineSegment2D> getLines(PolyCirculinearCurve2D<CirculinearElement2D> elements, double depth) throws Exception {

        ArrayList<LineSegment2D> lines = new ArrayList<>();

        PolyOrientedCurve2D contour = new PolyOrientedCurve2D(elements);
        double max_x = elements.firstPoint().getX(); // z-achse
        double max_y = elements.lastPoint().getY(); // x-achse
        double min_x = elements.lastPoint().getX();
        double y_layer = max_y - depth;

        for (int i = 0; i < 100; i++) {
            ArrayList<Point2D> points = null;
            double offset = 0.001;
            if (depth < 0) {
                offset *= -1;
            }
            for (int j = 0; j < 3; j++) {
                LineSegment2D l = new LineSegment2D(max_x + 0.01, y_layer - i * depth + offset * j, min_x - 0.01, y_layer - i * depth + offset * j);
                points = (ArrayList<Point2D>) contour.intersections(l);
                if (points.isEmpty()) {
                    break;
                }
                if ((points.size() % 2) == 0) {
                    break;
                }
            }
            if (points == null) {
                Exception newExcept = new Exception("list of intersection points is null");
                throw newExcept;
            }
            if (points.isEmpty()) {
                break;
            }

            if ((points.size() % 2) != 0) {
                Exception newExcept = new Exception("size of intersection points is odd: " + String.valueOf(points.size()) + ", line number: " + String.valueOf(i) + ", try to change the depth");
                throw newExcept;
            }

            this.intersect_p.add(points);

            for (int j = 0; j < 100; j += 2) {
                if (points.size() <= (j + 1)) {
                    break;
                }
                Point2D p1 = points.get(j);
                Point2D p2 = points.get(j + 1);
                LineSegment2D line = new LineSegment2D(p1, p2);
                lines.add(line);

            }

        }
        boolean outer = true;
        if (depth < 0) {
            outer = false;
        }
        Collections.sort(lines, new LineCompare(outer));

        for (LineSegment2D lout : lines) {
            System.out.println(lout.firstPoint().getX() + "   :   " + lout.firstPoint().getY());
        }

        return lines;
    }

    /**
     *
     * @param axis "X" or "Z"
     * @param d value
     * @return formatted String like: X2.52, the x-axis is converted from radius
     * to diameter.
     */
    private String format(String axis, double d) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern("0.###");
        if (axis == "x" || axis == "X") {
            d *= 2.0;
            return "X" + df.format(d);
        }
        return "Z" + df.format(d);

    }

}
