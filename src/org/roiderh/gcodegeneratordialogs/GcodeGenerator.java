/*
 * Copyright (C) 2015 Herbert Roider <herbert@roider.at>
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
import java.util.LinkedList;
import java.util.Locale;
import math.geom2d.Box2D;
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
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
     *
     * @param elements contour, y is the radius, x is the length
     * @param depth max depth in radius
     * @param toolNoseRadius
     * @param oversize Material allowance in x (z-axis) and y (x-axis)
     * @return
     */
    public String rough(LinkedList<CirculinearElement2D> elements, double depth, double toolNoseRadius, Point2D oversize) {
        PolyCirculinearCurve2D contour = new PolyCirculinearCurve2D(elements);
        Box2D bb = contour.boundingBox();

        System.out.println(bb.toString());
        double max_x = contour.firstPoint().getX(); // z-achse
        double max_y = contour.lastPoint().getY(); // x-achse
        double min_x = contour.lastPoint().getX(); // z-achse

        // Innencontour:
        if (contour.firstPoint().y() > contour.lastPoint().y()) {
            depth *= -1.0;
            oversize = oversize.scale(1.0, -1.0);
        }
        String gcode = makeComment("Abspantiefe: " + String.valueOf(depth)) + "\n";
        gcode += makeComment("Schneidenradius: " + String.valueOf(toolNoseRadius)) + "\n";
        gcode += makeComment("Aufmass x=" + String.valueOf(oversize.getY()) + " , z=" + String.valueOf(oversize.getX())) + "\n";

        Point2D startp = new Point2D(max_x, max_y);
        Point2D endp = new Point2D(min_x, max_y);
        //gcode = "G0 X" + 2 * (startp.getY()) + " Z" + ((startp.getX()) + 0.3) + "; Startpunkt\n";
        gcode += this.rough_part(elements, depth, toolNoseRadius, oversize, startp, endp);
        gcode += makeComment("End of generated code") + "\n";
        System.out.print(gcode);
        return gcode;
    }

    private String rough_part(LinkedList<CirculinearElement2D> elements, double depth, double toolNoseRadius, Point2D oversize, Point2D startpoint, Point2D endpoint) {
        PolyCirculinearCurve2D contour = new PolyCirculinearCurve2D(elements);
        //Box2D bb = contour.boundingBox();
        String gcode = "";
        String rad_comp = "42";// outside, right beside the contour
        if (contour.firstPoint().y() > contour.lastPoint().y()) {
            rad_comp = "41"; // inside, left beside the contour
        }
        double tnrc = toolNoseRadius * (1 + Math.tan(0.5 * 45.0)); // only for undercut elements to avoid a crash with the contour because of the tool nose radius
        System.out.println("##################Startpoint=" + startpoint.toString());
        double max_x = startpoint.getX(); // z-achse
        double max_y = startpoint.getY(); // x-achse
        double min_x = endpoint.getX(); // z-achse
        Point2D new_startpoint = null;
        //Point2D new_endpoint = null;
        for (int i = 1; i < 100; i++) {

            //gcode += "G0 X" + (startpoint.getY() - i*depth) + " Z" + (startpoint.getX()) + "\n";
            //gcode += "G1 X" + 2*(startpoint.getY() - i*depth) + "\n";
            Point2D start = null;
            Point2D end = null;
            LineSegment2D l = new LineSegment2D(max_x + 0.01, max_y - i * depth, min_x - 0.01, max_y - i * depth);
            LineSegment2D l_test;
            if (contour.firstPoint().y() > contour.lastPoint().y()) {
                l_test = l.parallel(-0.001); // inside, left beside the contour
            } else {
                l_test = l.parallel(0.001);
            }

            if (l.length() <= 0) {
                //break;
            }
            System.out.println("##### Line " + l.toString());
            ArrayList<Point2D> points = (ArrayList<Point2D>) contour.intersections(l);
            ArrayList<Point2D> points_test = (ArrayList<Point2D>) contour.intersections(l_test);

            System.out.println(points.toString());
            if (points.isEmpty()) {
                break;
            }
            if (points.size() != points_test.size()) {
                System.out.println("Fehler, Punkteanzahl nicht gleich!!! andere Schnitttiefe waehlen");
                break;
            }

            if ((points.size() % 2) == 0) {
                System.out.println("; inside");
                start = points.get(0).translate(-oversize.x(), oversize.y());
                if (points.size() > 1) {
                    end = points.get(1).translate(oversize.x(), oversize.y());
                } else {
                    end = points.get(0).translate(oversize.x(), oversize.y());
                }
                if (new_startpoint == null && points.size() > 2) {

                    double y = points.get(2).getY() + depth;
                    double x = points.get(2).getX();
                    new_startpoint = new Point2D(x, y);
                    System.out.println("new startpoint= " + new_startpoint.toString());
                }
                //gcode += "; inside\n";

            } else {
                start = l.firstPoint().translate(0, oversize.y());
                if (points.size() > 0) {
                    end = points.get(0).translate(oversize.x(), oversize.y());
                } else {
                    end = points.get(points.size() - 1).translate(oversize.x(), oversize.y());
                }

                if (new_startpoint == null && points.size() > 1) {

                    double y = points.get(1).getY() + depth;
                    double x = points.get(1).getX();
                    new_startpoint = new Point2D(x, y);

                    System.out.println("new startpoint= " + new_startpoint.toString());
                }

            }
            if (Math.abs(start.getX() - end.getX()) < tnrc) {
                System.out.println("Eintauchen in Hinterschnitt macht keinen Sinn, weil Schneidenradius zu groß ist");
                new_startpoint = null;
                break;
            }
            System.out.println(start.toString() + " -- " + end.toString());
            gcode += "G0 X" + this.format(2 * (start.getY() + depth + 0.1 * depth)) + " Z" + this.format(start.getX() + 1.1 * Math.abs(depth) - tnrc) + "\n";
            //gcode += "G0 X" + 2 * (start.getY() + depth) + "\n";

            gcode += "G1 X" + this.format(2 * (start.getY())) + " Z" + this.format(start.getX() - tnrc) + "\n"; // move to the depth of the cut
            gcode += "G1 Z" + this.format(end.getX()) + "\n";                                    // cut the layer
            //gcode += "G40 G1 X" + (2 * (start.getY() + 0.5*depth)) + "\n";
            gcode += "G1 X" + this.format(2 * (start.getY() + 0.5 * depth)) + " Z" + this.format(end.getX() + 0.3 * Math.abs(depth)) + "\n"; // drive away from contour
            //gcode += "G0 Z" + start.getX() + "\n";

        }

        if (new_startpoint != null) {
            gcode += makeComment("neue Senke Startpunkt") + "\n";
            gcode += "G0 X" + this.format(2 * (contour.lastPoint().y() + oversize.y())) + "\n";
            gcode += this.rough_part(elements, depth, toolNoseRadius, oversize, new_startpoint, endpoint);

        }
        gcode += "G0 X" + this.format(2 * (contour.lastPoint().y() + oversize.y())) + "\n";
        gcode += "G0 Z" + this.format(contour.firstPoint().x()) + "\n";
        return gcode;
    }

    private String format(double d) {

        return String.format(Locale.US, "%.2f", d);

    }

    private String makeComment(String text) {
        if (control == 1) {
            return " ( " + text + " ) ";
        } else {
            return " ; " + text;
        }
    }
}
