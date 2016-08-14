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

import java.util.ArrayList;
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
    public ArrayList< ArrayList<Point2D>> intersect_p = new ArrayList<>();

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
        double max_x = _elements.firstPoint().getX(); // z-achse
        double max_y = _elements.lastPoint().getY(); // x-achse

        String gcode = makeComment("Abspantiefe: " + String.valueOf(depth)) + "\n";

        // Innencontour:
        if (_elements.firstPoint().y() > _elements.lastPoint().y()) {
            depth *= -1.0;
            oversize = oversize.scale(1.0, -1.0);
        }

        // create a copy
        PolyCirculinearCurve2D<CirculinearElement2D> elements = new PolyCirculinearCurve2D<>(_elements.curves());

        //elements;
//        for(CirculinearElement2D e : _elements){
//            elements.add(e);
//        }
        // Am Anfang eine Linie anhängen, damit die Contour vorne geschlossen ist:
        elements.add(0, new Line2D(new Point2D(elements.firstPoint().x(), elements.lastPoint().y()), elements.firstPoint()));

        gcode += makeComment("Schneidenradius: " + String.valueOf(toolNoseRadius)) + "\n";
        gcode += makeComment("Aufmass x=" + String.valueOf(oversize.getY()) + " , z=" + String.valueOf(oversize.getX())) + "\n";
        gcode += makeComment("Eintauchwinkel " + String.format(Locale.US, "%.2f", plunging_angle * 180.0 / Math.PI)) + "\n";

        //gcode = "G0 X" + 2 * (startp.getY()) + " Z" + ((startp.getX()) + 0.3) + "; Startpunkt\n";
        gcode += this.rough_part(elements, depth, toolNoseRadius, oversize, max_x, max_y - depth, plunging_angle);
        gcode += "G0 " + this.format("X", elements.lastPoint().getY() + 0.1 * depth + oversize.getY()) + "\n";
        gcode += "G0 " + this.format("Z", elements.firstPoint().getX()) + "\n";

        gcode += makeComment("End of generated code") + "\n";
        System.out.print(gcode);
        return gcode;
    }

    /**
     *
     * @param elements contour
     * @param depth
     * @param toolNoseRadius
     * @param oversize material allowance
     * @param max_x at the beginning it is the x-value of the first point of the
     * contour
     * @param y_layer the layer for roughing, at the beginning it is the y-value
     * of the last point minus the depth
     * @param plunging_angle
     * @return G-code
     * @throws Exception
     */
    private String rough_part(PolyCirculinearCurve2D<CirculinearElement2D> elements, double depth, double toolNoseRadius, Point2D oversize, double max_x, double y_layer, double plunging_angle) throws Exception {
        String gcode = "";

        double tnrc = toolNoseRadius * (1 + Math.tan(0.5 * plunging_angle)); // only for undercut elements to avoid a crash with the contour because of the tool nose radius

        Point2D new_startpoint = null;

        PolyOrientedCurve2D contour = new PolyOrientedCurve2D(elements);

        double min_x = elements.lastPoint().getX();
        Point2D prev_p2 = null;

        for (int i = 0; i < 100; i++) {
            Point2D p1 = null;
            Point2D p2 = null;
            Point2D p3 = null;
            Point2D p4 = null;
            LineSegment2D l = new LineSegment2D(max_x + 0.01, y_layer - i * depth, min_x - 0.01, y_layer - i * depth);

            ArrayList<Point2D> points = (ArrayList<Point2D>) contour.intersections(l);
            this.intersect_p.add(points);

            if (points.isEmpty()) {
                break;
            }
            if ((points.size() % 2) != 0) {
                Exception newExcept = new Exception("size of intersection points is odd: " + String.valueOf(points.size()) + ", line number: " + String.valueOf(i) + ", try to change the depth");
                throw newExcept;
            }

            if (new_startpoint == null && points.size() > 2) {
                new_startpoint = points.get(2);
                min_x = points.get(2).getX() + 0.1;
            }

            p2 = points.get(0);
            p3 = points.get(1);
            p1 = p2.translate(Math.abs(1.1 * depth) / Math.tan(plunging_angle), (1.1 * depth));
            //p4 = p3.translate(Math.abs(0.7 * depth) / Math.tan(plunging_angle), (0.7 * depth));

            // Startpunkte für eine Schicht nach hinten verschieben, damit nicht mit G0 ins Mat. gefahren wird:
            if (prev_p2 != null && p2.getX() < elements.firstPoint().getX()) {
                double a = prev_p2.getX() - p1.getX();
                if (a < 0) {
                    p2 = p2.translate(a, 0);
                    p1 = p1.translate(a, 0);
                }
            }

            // auf kollision testen, nur bei Hinterschnitt           
            if (p2.getX() < elements.firstPoint().getX()) {
                for (int j = 0; j < 100; j++) {
                    Line2D eintauchweg = new Line2D(p1.translate(+oversize.getX()+tnrc, 0), p2.translate(+oversize.getX()+tnrc, 0));
                    //eintauchweg = eintauchweg
                    java.util.Collection<Point2D> inters = elements.intersections(eintauchweg);
                    if (inters.size() <= 0) {
                        break;
                    }
                    p2 = p2.translate(-oversize.getX() - 0.05, 0);
                    p1 = p1.translate(-oversize.getX() - 0.05, 0);

                }
            }
            // Punkt liegt bereits weit genug hinten:
//            if(p2.getX() < points.get(0).getX() -oversize.getX() - tnrc){
//                p2 = p2.translate(0, oversize.getY());
//            }else{
//                p2 = p2.translate(-oversize.getX() - tnrc, oversize.getY());
//            }
            
            p1 = p1.translate(0, oversize.getY());
            p2 = p2.translate(0, oversize.getY());
            
            p3 = p3.translate(oversize.getX(), oversize.getY());
            //p1 = p2.translate(Math.abs(1.1 * depth) / Math.tan(plunging_angle), (1.1 * depth));
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

        if (new_startpoint != null) {
            gcode += makeComment("neue Senke Startpunkt") + "\n";
            gcode += "G0 " + this.format("X", (elements.lastPoint().getY()) + 0.1 * depth + oversize.getY()) + "\n";
            gcode += this.rough_part(elements, depth, toolNoseRadius, oversize, new_startpoint.getX(), new_startpoint.getY(), plunging_angle);

        }
        return gcode;
    }

    /**
     *
     * @param axis "X" or "Z"
     * @param d value
     * @return formatted String like: X2.52, the x-axis is converted from radius
     * to diameter.
     */
    private String format(String axis, double d) {
        if (axis == "x" || axis == "X") {
            d *= 2.0;
            return String.format(Locale.US, "X%.2f", d);
        }
        return String.format(Locale.US, "Z%.2f", d);

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
}
