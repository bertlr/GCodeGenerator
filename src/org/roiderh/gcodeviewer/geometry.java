/*
 * Copyright (C) 2014 by Herbert Roider <herbert.roider@utanet.at>
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
package org.roiderh.gcodeviewer;

import java.awt.Graphics2D;
import java.util.Collection;
import java.util.LinkedList;
import math.geom2d.Point2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.conic.CircleArc2D;
import math.geom2d.line.Line2D;
import math.geom2d.line.LineSegment2D;
import math.geom2d.line.Ray2D;
import math.geom2d.line.StraightLine2D;
import java.util.HashSet;
import math.geom2d.circulinear.CirculinearElement2D;

/**
 *
 * @author Herbert Roider <herbert.roider@utanet.at>
 */
public class geometry {

        public Graphics2D g = null;

        /**
         * calculate multiple points to display a circle.
         * Use instead:
         * 
         * <code><pre>
         * CircleArc2D c1 = this.createCircleArc(begin, end, r, ccw);
         * Polyline2D pl = c1.asPolyline(4);
         * </pre></code>
         * 
         * @see #createCircleArc(math.geom2d.Point2D, math.geom2d.Point2D, double, boolean) 
         *
         * @param begin first Point2D
         * @param end second Point2D
         * @param r radius
         * @param ccw counterclockwise or clockwise
         * @return a list with the points
         */
        @Deprecated
        public LinkedList<Point2D> circle(Point2D begin, Point2D end, double r, boolean ccw) {
                LinkedList<Point2D> points = new LinkedList<>();
                //Point2D center = new Point2D();

                double distance;
                double dist_center;
                //points.push(begin);
                //points.push(end);

                // Abstand zwischen den Punkten:
                distance = Math.sqrt(Math.pow((end.x() - begin.x()), 2.0) + Math.pow((end.y() - begin.y()), 2.0));

                // Mittelpunkt der Strecke zwischen den Punkten:
                Point2D middle = new Point2D((begin.x() + end.x()) / 2.0, (begin.y() + end.y()) / 2.0);
                //middle.y() = ;

                // Mittelpunkt vom Kreis (im und gegen Uhrzeigersinn:
                double x;
                double y;
                if (ccw) {
                        x = middle.x() + Math.sqrt(Math.pow(r, 2.0) - Math.pow((distance / 2.0), 2.0)) * (begin.y() - end.y()) / distance;
                        y = middle.y() + Math.sqrt(Math.pow(r, 2.0) - Math.pow((distance / 2.0), 2.0)) * (end.x() - begin.x()) / distance;
                } else {
                        x = middle.x() - Math.sqrt(Math.pow(r, 2.0) - Math.pow((distance / 2.0), 2.0)) * (begin.y() - end.y()) / distance;
                        y = middle.y() - Math.sqrt(Math.pow(r, 2.0) - Math.pow((distance / 2.0), 2.0)) * (end.x() - begin.x()) / distance;

                }
                Point2D center = new Point2D(x, y);

                try {
                        points.add(begin.clone());

                        //Point2D new_point = new Point2D();               
                        Point2D p1 = getPointOnArc(begin, end, center, r);
                        points.add(1, p1);

                        Point2D p2 = getPointOnArc(begin, p1, center, r);
                        points.add(1, p2);

                        Point2D p3 = getPointOnArc(p1, end, center, r);
                        points.add(3, p3);

                        points.add(4, end.clone());

                } catch (Exception e) {
                        System.out.println(e.getMessage());
                }

                return points;
        }

        /**
         * Calculate a Point2D on a circle between 2 points. Don't use this,
         * @see #circle(math.geom2d.Point2D, math.geom2d.Point2D, double, boolean)
         *
         *
         * @param begin startpoint
         * @param end endpoint
         * @param center centerpoint of the circle
         * @param r radius
         * @return the Point2D between startpoint and endpoint.
         */
        @Deprecated
        public Point2D getPointOnArc(Point2D begin, Point2D end, Point2D center, double r) {

                //Point2D middle = new Point2D();
                double x;
                double y;
                // Mittelpunkt der Strecke zwischen den Punkten:
                x = (begin.x() + end.x()) / 2.0;
                y = (begin.y() + end.y()) / 2.0;
                Point2D middle = new Point2D(x, y);
                // Abstand Kreismittelpunkt zu Mittelpunkt der 2 Punkte
                double dist_center = Math.sqrt(Math.pow((middle.x() - center.x()), 2.0) + Math.pow((middle.y() - center.y()), 2.0));

                // der neue Punkt der Zwischen den anfangs und Endpunkt liegt:
                x = r * (middle.x() - center.x()) / dist_center + center.x();
                y = r * (middle.y() - center.y()) / dist_center + center.y();
                Point2D new_point = new Point2D(x, y);
                return new_point;

        }

        /**
         * calculate the Tangentpoint where a circle with a radius r touches the
         * lines.
         *
         *
         *
         * @param _A first Point2D
         * @param _B second Point2D
         * @param _C the corner where A and B are connected
         * @param r radius of the circle
         * @return a list with 2 points, one Point2D for each line
         */
        @Deprecated
        public LinkedList<Point2D> getTangentialPoints(Point2D _A, Point2D _B, Point2D _C, double r) {

                // Verschieben vom Koordinatensystem ins Berechnungssystem mit Mittelpunkt B 
                //Point2D A = new Point2D();
                //Point2D B = new Point2D();
                //Point2D C = new Point2D();
                double x;
                double y;

                // translate points
                Point2D A = _A.minus(_B);
                Point2D C = _C.minus(_B);
                // Lange AB
                double AB = Math.sqrt(Math.pow(A.x(), 2) + Math.pow(A.y(), 2));
                //Länge BC
                double BC = Math.sqrt(Math.pow(C.x(), 2) + Math.pow(C.y(), 2));

                //Tangentenpunkte, wenn Abstand = 1
                x = A.x() / AB;
                y = A.y() / AB;
                Point2D E = new Point2D(x, y);
                x = C.x() / BC;
                y = C.y() / BC;
                Point2D D = new Point2D(x, y);

                x = (E.x() + D.x()) / 2;
                y = (E.y() + D.y()) / 2;
                Point2D F = new Point2D(x, y);

                double EF = Math.sqrt(Math.pow((E.x() - F.x()), 2) + Math.pow((E.y() - F.y()), 2));
                double BF = Math.sqrt(Math.pow(F.x(), 2) + Math.pow(F.y(), 2));

                double GF = Math.pow(EF, 2) / BF;

                x = F.x() * (BF + GF) / BF;
                y = F.y() * (BF + GF) / BF;
                Point2D G = new Point2D(x, y);
                double BG = Math.sqrt(Math.pow(G.x(), 2) + Math.pow(G.y(), 2));

                // disp("Radius bei Abstand = 1")
                double GE = EF / BF;
                //disp("Abstand bei r= ")
                //disp( r )
                double HB = r / GE;

                //disp("Koordinaten der Tangentenpunkte")
                x = HB * E.x();
                y = HB * E.y();
                Point2D H = new Point2D(x, y);
                x = HB * D.x();
                y = HB * D.y();
                Point2D I = new Point2D(x, y);
                // disp("Punkte ins normale Ausgangskoordinatensystem verschieben")
                //H.x() = H.x() + _B.x();
                //H.y() = H.y() + _B.y();
                //H.translate(_B.x(), _B.y());

                //I.x() = I.x() + _B.x();
                //I.y() = I.y() + _B.y();
                LinkedList<Point2D> ret = new LinkedList<>();
                ret.add(H.plus(_B));
                ret.add(I.plus(_B));
                return ret;

        }

        /**
         * chamfer
         @see #getChamfer(math.geom2d.curve.AbstractSmoothCurve2D, math.geom2d.curve.AbstractSmoothCurve2D, double) 
         *
         * @param _A
         * @param _B
         * @param _C
         * @param EB size of the chamfer
         * @return 2 Points: begin and end Point2D of the chamfer
         */
        @Deprecated
        public LinkedList<Point2D> getChamferPoints(Point2D _A, Point2D _B, Point2D _C, double EB) {
                // CHR  Fase
                // Verschieben vom Koordinatensystem ins Berechnungssystem mit Mittelpunkt B 
                Point2D A = _A.minus(_B);
                //Point2D B = new Point2D();
                Point2D C = _C.minus(_B);

                double x;
                double y;
                // Lange AB
                double AB = Math.sqrt(Math.pow(A.x(), 2) + Math.pow(A.y(), 2));
                // Länge BC
                double BC = Math.sqrt(Math.pow(C.x(), 2) + Math.pow(C.y(), 2));

                // Eckpunkte, wenn Abstand = CHR (EB oder DB)
                x = A.x() * EB / AB;
                y = A.y() * EB / AB;
                Point2D E = new Point2D();
                x = C.x() * EB / BC;
                y = C.y() * EB / BC;
                Point2D D = new Point2D();

                // Punkte ins normale Ausgangskoordinatensystem verschieben"
                LinkedList<Point2D> ret = new LinkedList<>();
                ret.add(E.plus(_B));
                ret.add(D.plus(_B));
                return ret;

        }

        /**
         * Test if 3 Points are counterclockwise
         *
         * @param p0
         * @param p1
         * @param p2
         * @return +1 is counterclockwise, -1 if clockwise, 0 if kolinear
         */
        int ccw(Point2D p0, Point2D p1, Point2D p2) {
                double dx1, dx2, dy1, dy2;
                dx1 = p1.x() - p0.x();
                dy1 = p1.y() - p0.y();
                dx2 = p2.x() - p0.x();
                dy2 = p2.y() - p0.y();
                if (dx1 * dy2 > dy1 * dx2) {
                        return 1;
                }
                if (dx1 * dy2 < dy1 * dx2) {
                        return -1;
                }
                if ((dx1 * dx2 < 0) || (dy1 * dy2 < 0)) {
                        return -1;
                }
                if ((dx1 * dx1 + dy1 * dy1) < (dx2 * dx2 + dy2 * dy2)) {
                        return 1;
                }

                return 0;

        }

        /**
         * calculate the center point of the arc (circle)
         *
         * @param begin first Point2D
         * @param end second Point2D
         * @param r radius
         * @param ccw counterclockwise or clockwise
         * @return the center point of the arc (circle)
         */
        public Point2D getArcCenterPoint(Point2D begin, Point2D end, double r, boolean ccw) {

                double distance;
                double dist_center;
                //points.push(begin);
                //points.push(end);

                // Abstand zwischen den Punkten:
                distance = Math.sqrt(Math.pow((end.x() - begin.x()), 2.0) + Math.pow((end.y() - begin.y()), 2.0));

                // Mittelpunkt der Strecke zwischen den Punkten:
                Point2D middle = new Point2D((begin.x() + end.x()) / 2.0, (begin.y() + end.y()) / 2.0);
                //middle.y() = ;

                // Mittelpunkt vom Kreis (im und gegen Uhrzeigersinn:
                double x;
                double y;
                if (ccw) {
                        x = middle.x() + Math.sqrt(Math.pow(r, 2.0) - Math.pow((distance / 2.0), 2.0)) * (begin.y() - end.y()) / distance;
                        y = middle.y() + Math.sqrt(Math.pow(r, 2.0) - Math.pow((distance / 2.0), 2.0)) * (end.x() - begin.x()) / distance;
                } else {
                        x = middle.x() - Math.sqrt(Math.pow(r, 2.0) - Math.pow((distance / 2.0), 2.0)) * (begin.y() - end.y()) / distance;
                        y = middle.y() - Math.sqrt(Math.pow(r, 2.0) - Math.pow((distance / 2.0), 2.0)) * (end.x() - begin.x()) / distance;

                }
                Point2D center = new Point2D(x, y);

                return center;
        }

        /**
         * The Arc
         *
         * @param begin
         * @param end
         * @param r
         * @param ccw
         * @return
         */
        public CircleArc2D createCircleArc(Point2D begin, Point2D end, double r, boolean ccw) {
                Point2D center = getArcCenterPoint(begin, end, r, ccw);

                double angleBegin = new Line2D(center, begin).horizontalAngle();
                double angleEnd = new Line2D(center, end).horizontalAngle();
                double angleBegin_0 = angleBegin - angleBegin;  // =0
                double angleEnd_0 = angleEnd - angleBegin;

                double extent = 0;
                angleBegin_0 = angleBegin - angleBegin;  // =0
                angleEnd_0 = angleEnd - angleBegin;

                extent = angleEnd_0;

                if (ccw) {
                        if (extent < 0) {
                                extent = 2 * Math.PI + extent;
                        }
                } else {
                        if (extent > 0) {
                                extent = extent - 2 * Math.PI;
                        }
                }

                if (Math.abs(extent) > 2 * Math.PI) {
                        if (extent > 2 * Math.PI) {
                                extent -= 2 * Math.PI;
                        } else if (extent < 2 * Math.PI) {
                                extent += 2 * Math.PI;
                        }
                }

                //extent = angleEnd - angleBegin;
                CircleArc2D b = new CircleArc2D(center, r, angleBegin, extent);
                return b;

        }
        /** @see #getRound(math.geom2d.curve.AbstractSmoothCurve2D, math.geom2d.curve.AbstractSmoothCurve2D, double) 
         * 
         * @param b1
         * @param b2
         * @param r
         * @return 
         */
        @Deprecated
        public CircleArc2D getRound_old(CircleArc2D b1, CircleArc2D b2, double r) {
                Point2D center1 = b1.supportingCircle().center();
                Point2D center2 = b2.supportingCircle().center();

                CircleArc2D b1i = b1.parallel(-r);
                CircleArc2D b1o = b1.parallel(r);

                CircleArc2D b2i = b2.parallel(-r);
                CircleArc2D b2o = b2.parallel(r);
                if (this.g != null) {
                        b1i.draw(this.g);
                        b1o.draw(this.g);

                        b2i.draw(this.g);
                        b2o.draw(this.g);
                }

                Circle2D c1i = b1i.supportingCircle();
                Circle2D c1o = b1o.supportingCircle();

                Circle2D c2i = b2i.supportingCircle();
                Circle2D c2o = b2o.supportingCircle();

                /*
                 Je nach dem, ob cw - ccw oder cw-cw ist:
                 cw-cw:  beide inner Kreise verschnitten
                 cw-ccw: jeweils ein inner mit einen outer Kreis verschnitten
                 If a circle is a inner or outer circle depends on the extent, if it is negative.
                 */
                Collection<Point2D> inter_i;
                Collection<Point2D> inter_o;
//                if ((b1.getAngleExtent() < 0 && b2.getAngleExtent() < 0) || (b1.getAngleExtent() > 0 && b2.getAngleExtent() > 0)) {
//                        inter_i = Circle2D.circlesIntersections(c1i, c2i);
//                        inter_o = Circle2D.circlesIntersections(c1o, c2o);
//
//                } else {
//                        inter_i = Circle2D.circlesIntersections(c1i, c2o);
//                        inter_o = Circle2D.circlesIntersections(c1o, c2i);
//
//                }
                inter_i = Circle2D.circlesIntersections(c1i, c2i);
                inter_o = Circle2D.circlesIntersections(c1o, c2o);
                inter_i.addAll(inter_o);

                System.out.println(c1i.toString());
                System.out.println(c2i.toString());
                System.out.println(c1o.toString());
                System.out.println(c2o.toString());

                CircleArc2D result = null;
                Point2D res_start = null;
                Point2D res_end = null;
                Point2D res_center = null;

                // Walk thru all possible intersections points (centerpoints of the transition arc)
                for (Point2D point : inter_i) {
                        if (this.g != null) {
                                point.draw(this.g, 2);
                        }

                        Ray2D r1 = new Ray2D(center1, point);

                        Collection<Point2D> points_1 = b1.intersections(r1);

                        Ray2D r2 = new Ray2D(center2, point);
                        Collection<Point2D> points_2 = b2.intersections(r2);

                        if (points_1.size() > 0 && points_2.size() > 0) {
                                System.out.println("found Tangent points");
                                System.out.println(point.toString());
                                System.out.println(points_1.toString());
                                System.out.println(points_2.toString());
                                res_start = points_1.iterator().next();
                                res_end = points_2.iterator().next();
                                res_center = point;
                                int iccw = this.ccw(res_start, res_end, res_center);
                                boolean ccw = false;
                                if (iccw > 0) {
                                        ccw = true;
                                }
                                result = this.createCircleArc(res_start, res_end, r, ccw);
                                if (this.g != null) {
                                        res_start.draw(this.g, 2);
                                        res_end.draw(this.g, 2);
                                        result.draw(this.g);
                                }

                                return result;

                        }

                }
                return null;

        }

        /** Calculate a chamfer between 2 Elements
         *
         * @param b1 LineSegment2D or CircleArc2D
         * @param b2 LineSegment2D or CircleArc2D
         * @param length size of the chamfer
         * @return a LineSegment2D which is the chamfer
         */
        public LineSegment2D getChamfer(CirculinearElement2D b1, CirculinearElement2D b2, double length) {
                Point2D vertex = b1.lastPoint();
                Circle2D circle = new Circle2D(vertex, length);
                Collection<Point2D> inter_1;
                Collection<Point2D> inter_2;
                Point2D first = null;
                Point2D last = null;
                if (this.g != null) {
                        b1.draw(this.g);
                        b2.draw(this.g);
                        circle.draw(this.g);
                }
                System.out.println(b1.toString());
                System.out.println(b2.toString());

                if (b1.toString().contains("CircleArc2D") ) {
                        CircleArc2D geo = (CircleArc2D) b1;
                        inter_1 = Circle2D.circlesIntersections(geo.supportingCircle(), circle);
                } else {
                        LineSegment2D geo = (LineSegment2D) b1;
                        inter_1 = Circle2D.lineCircleIntersections(geo.supportingLine(), circle);

                }

                if (b2.toString().contains("CircleArc2D") ) {
                        CircleArc2D geo = (CircleArc2D) b2;
                        inter_2 = Circle2D.circlesIntersections(geo.supportingCircle(), circle);
                } else {
                        LineSegment2D geo = (LineSegment2D) b2;
                        inter_2 = Circle2D.lineCircleIntersections(geo.supportingLine(), circle);

                }

                for (Point2D p : inter_1) {
                        if (b1.contains(p)) {
                                first = p;
                                break;
                        }

                }
                for (Point2D p : inter_2) {
                        if (b2.contains(p)) {
                                last = p;
                                break;
                        }

                }
                if (first == null || last == null) {
                        return null;
                }
                if (this.g != null) {
                        first.draw(this.g, 2);
                        last.draw(this.g, 2);
                }

                LineSegment2D chamfer = new LineSegment2D(first, last);
                if (this.g != null) {
                        chamfer.draw(this.g);
                }

                return chamfer;

        }

        /** Calculate a round between 2 elements.
         *
         * @param b1 LineSegment2D or CircleArc2D
         * @param b2 LineSegment2D or CircleArc2D
         * @param radius size of the round
         * @return a CircleArc2D which is the round
         */
        public CircleArc2D getRound(CirculinearElement2D b1, CirculinearElement2D b2, double radius) {
                //Point2D vertex = b1.lastPoint();
                //Circle2D circle = new Circle2D(vertex, radius);
                //Collection<Point2D> inter_1;
                //Collection<Point2D> inter_2;
                Collection<CirculinearElement2D> parallelLines_1 = new HashSet<>();
                Collection<CirculinearElement2D> parallelLines_2 = new HashSet<>();

                if (b1.toString().contains("CircleArc2D") ) {
                        CircleArc2D geo = (CircleArc2D) b1;
                        CircleArc2D b1i = geo.parallel(-radius);
                        CircleArc2D b1o = geo.parallel(radius);
                        parallelLines_1.add(b1i);
                        parallelLines_1.add(b1o);

                        //inter_1 = Circle2D.circlesIntersections(geo.supportingCircle(), circle);
                } else {
                        LineSegment2D geo = (LineSegment2D) b1;
                        LineSegment2D b1i = geo.parallel(-radius);
                        LineSegment2D b1o = geo.parallel(radius);
                        parallelLines_1.add(b1i);
                        parallelLines_1.add(b1o);

                        //inter_1 = Circle2D.lineCircleIntersections(geo.supportingLine(), circle);
                }

                if (b2.toString().contains("CircleArc2D") ) {
                        CircleArc2D geo = (CircleArc2D) b2;
                        CircleArc2D b1i = geo.parallel(-radius);
                        CircleArc2D b1o = geo.parallel(radius);
                        parallelLines_2.add(b1i);
                        parallelLines_2.add(b1o);

                        //inter_1 = Circle2D.circlesIntersections(geo.supportingCircle(), circle);
                } else {
                        LineSegment2D geo = (LineSegment2D) b2;
                        LineSegment2D b1i = geo.parallel(-radius);
                        LineSegment2D b1o = geo.parallel(radius);
                        parallelLines_2.add(b1i);
                        parallelLines_2.add(b1o);

                        //inter_1 = Circle2D.lineCircleIntersections(geo.supportingLine(), circle);
                }

                /*
                 Find all possible intersections of the parallel lines
                 */
                Collection<Point2D> inter = new HashSet<>();
                for (CirculinearElement2D c1 : parallelLines_1) {
                        for (CirculinearElement2D c2 : parallelLines_2) {
                                Collection<Point2D> inter_new;

                                if (c1.toString().contains("CircleArc2D") ) {
                                        CircleArc2D geo1 = (CircleArc2D) c1;
                                        if (c2.toString().contains("CircleArc2D") ) {
                                                CircleArc2D geo2 = (CircleArc2D) c2;
                                                inter_new = Circle2D.circlesIntersections(geo1.supportingCircle(), geo2.supportingCircle());

                                        } else {
                                                LineSegment2D geo2 = (LineSegment2D) c2;
                                                inter_new = Circle2D.lineCircleIntersections(geo2.supportingLine(), geo1.supportingCircle());

                                        }

                                } else {
                                        LineSegment2D geo1 = (LineSegment2D) c1;
                                        if (c2.toString().contains("CircleArc2D") ) {
                                                CircleArc2D geo2 = (CircleArc2D) c2;
                                                inter_new = Circle2D.lineCircleIntersections(geo1.supportingLine(), geo2.supportingCircle());

                                        } else {
                                                LineSegment2D geo2 = (LineSegment2D) c2;
                                                inter_new = geo2.intersections(geo1);

                                        }
                                }
                                inter.addAll(inter_new);

                        }
                }
                /*
                 Possible intersection Points of parallel Lines, only Points on the lines.
                 */
                Collection<Point2D> inter_1 = new HashSet<>();
                for (Point2D p : inter) {
                        int found = 0;
                        for (CirculinearElement2D c : parallelLines_1) {
                                if (c.contains(p)) {
                                        found++;
                                        break;
                                }

                        }
                        for (CirculinearElement2D c : parallelLines_2) {
                                if (c.contains(p)) {
                                        found++;
                                        break;
                                }

                        }
                        if (found >= 2) {
                                inter_1.add(p);
                        }

                }

                if (inter_1.size() != 1) {
                        return null;
                }
                // the centerpoint of the round
                Point2D center = inter_1.iterator().next();
                // point where the round touches the first contour element:
                Point2D tangent_point_1 = null;
                // point where the round touches the second contour element:
                Point2D tangent_point_2 = null;

                /*
                 Intersection of a ray from the contour element center to the round center point with the contour element.
                 This is the tangent point.
                 */
                if (b1.toString().contains("CircleArc2D") ) {

                        CircleArc2D geo = (CircleArc2D) b1;
                        Point2D geo_center = geo.supportingCircle().center();
                        Collection<Point2D> tangentpoints = geo.intersections(new Ray2D(geo_center, center));
                        for (Point2D p : tangentpoints) {
                                if (geo.contains(p)) {
                                        tangent_point_1 = p;
                                        break;
                                }
                        }

                        //inter_1 = Circle2D.circlesIntersections(geo.supportingCircle(), circle);
                } else {
                        LineSegment2D geo = (LineSegment2D) b1;
                        StraightLine2D n = geo.perpendicular(center);
                        tangent_point_1 = geo.intersection(n);

                }

                if (b2.toString().contains("CircleArc2D") ) {

                        CircleArc2D geo = (CircleArc2D) b2;
                        Point2D geo_center = geo.supportingCircle().center();
                        Collection<Point2D> tangentpoints = geo.intersections(new Ray2D(geo_center, center));
                        for (Point2D p : tangentpoints) {
                                if (geo.contains(p)) {
                                        tangent_point_2 = p;
                                        break;
                                }
                        }

                        //inter_1 = Circle2D.circlesIntersections(geo.supportingCircle(), circle);
                } else {
                        LineSegment2D geo = (LineSegment2D) b2;
                        StraightLine2D n = geo.perpendicular(center);
                        tangent_point_2 = geo.intersection(n);

                }
                if (tangent_point_1 == null || tangent_point_2 == null) {
                        return null;
                }
                int iCcw = this.ccw(tangent_point_1, tangent_point_2, center);
                boolean ccw = true;
                if (iCcw < 0) {
                        ccw = false;
                }
                CircleArc2D result = this.createCircleArc(tangent_point_1, tangent_point_2, radius, ccw);
                if (this.g != null) {
                        result.draw(this.g);
                }

                return result;
        }

}
