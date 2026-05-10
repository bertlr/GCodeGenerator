/*
 * Copyright (C) 2026 Herbert Roider <herbert@roider.at>
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
import java.util.Collection;
import java.util.List;
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.conic.CircleArc2D;
import math.geom2d.line.LineSegment2D;
import math.geom2d.polygon.Polyline2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.operation.buffer.OffsetCurve;
import org.roiderh.gcodegeneratordialogs.FunctionConf;
import org.roiderh.gcodeviewer.geometry;

/**
 * Stable offset generator based on JTS.
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class Offset extends AbstractGenerator {

    private static final double EPSILON = 1e-6;
    private static final double ARC_TOLERANCE = 0.005;
    private static final double MIN_ARC_RADIUS = 0.05;
    private static final int MIN_ARC_POINTS = 5;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private final geometry geo = new geometry();

    /**
     * Creates a JTS-backed offset generator.
     *
     * @param _orig_contour source contour in internal lathe coordinates
     * @param _fc generator configuration from cycles.json
     * @param _values generator argument values from the dialog
     */
    public Offset(PolyCirculinearCurve2D<CirculinearElement2D> _orig_contour, FunctionConf _fc, ArrayList<String> _values) {
        super(_orig_contour, _fc, _values);
    }

    /**
     * Calculates the offset contour and returns it as G-code.
     *
     * @return generated G-code
     * @throws Exception if the offset contour cannot be created
     */
    @Override
    public String calculate() throws Exception {

        double dist = 0.0;
        control = 0; // 0 for 840D, 1 for 810

        for (int i = 0; i < fc.arg.size(); i++) {
            if (fc.arg.get(i).name.compareTo("distance") == 0) {
                dist = Double.parseDouble(values.get(i).trim());
            } else if (fc.arg.get(i).name.compareTo("control") == 0) {
                control = Integer.parseInt(values.get(i).trim());
            }
        }

        PolyCirculinearCurve2D<CirculinearElement2D> new_curve = this.offsetContour(orig_contour, dist);
        return this.convert2gcode(new_curve);
    }

    /**
     * Runs the JTS offset operation and converts the selected result back to the
     * repository's curve model.
     *
     * @param contour source contour
     * @param distance signed offset distance
     * @return offset contour as circulinear elements
     * @throws Exception if no usable offset contour can be created
     */
    private PolyCirculinearCurve2D<CirculinearElement2D> offsetContour(PolyCirculinearCurve2D<CirculinearElement2D> contour, double distance) throws Exception {
        if (Math.abs(distance) < EPSILON) {
            return new PolyCirculinearCurve2D<>(contour.curves());
        }

        LineString lineString = this.toLineString(contour);
        BufferParameters params = new BufferParameters();
        params.setEndCapStyle(BufferParameters.CAP_FLAT);
        params.setJoinStyle(BufferParameters.JOIN_ROUND);
        params.setQuadrantSegments(12);
        params.setSimplifyFactor(0.0);

        OffsetCurve offsetCurve = new OffsetCurve(lineString, distance, params);
        offsetCurve.setJoined(true);
        Geometry geometry = offsetCurve.getCurve();
        if (geometry == null || geometry.isEmpty()) {
            throw new Exception("cannot calculate offset contour");
        }

        LineString bestLine = this.selectBestLine(geometry, lineString);
        return this.toPolyCurve(bestLine);
    }

    /**
     * Converts the repository's contour representation into a JTS line string.
     * Existing arcs are sampled to points before offsetting.
     *
     * @param contour contour to convert
     * @return JTS line string for the offset operation
     * @throws Exception if the contour does not contain enough points
     */
    private LineString toLineString(PolyCirculinearCurve2D<CirculinearElement2D> contour) throws Exception {
        ArrayList<Coordinate> coordinates = new ArrayList<>();
        Collection<? extends CirculinearElement2D> curves = contour.curves();

        for (CirculinearElement2D curve : curves) {
            if (curve instanceof LineSegment2D) {
                LineSegment2D line = (LineSegment2D) curve;
                this.appendCoordinate(coordinates, line.firstPoint());
                this.appendCoordinate(coordinates, line.lastPoint());
            } else if (curve instanceof CircleArc2D) {
                CircleArc2D arc = (CircleArc2D) curve;
                Polyline2D polyline = arc.asPolyline(this.getArcSegmentCount(arc));
                Collection<LineSegment2D> edges = polyline.edges();
                for (LineSegment2D edge : edges) {
                    this.appendCoordinate(coordinates, edge.firstPoint());
                    this.appendCoordinate(coordinates, edge.lastPoint());
                }
            } else {
                this.appendCoordinate(coordinates, curve.firstPoint());
                this.appendCoordinate(coordinates, curve.lastPoint());
            }
        }

        Coordinate[] clean = this.removeDuplicateCoordinates(coordinates);
        if (clean.length < 2) {
            throw new Exception("offset contour needs at least two points");
        }
        return GEOMETRY_FACTORY.createLineString(clean);
    }

    /**
     * Converts the selected JTS offset result into line and fitted arc segments.
     *
     * @param line offset line from JTS
     * @return contour made of line and arc elements
     * @throws Exception if the result is empty after conversion
     */
    private PolyCirculinearCurve2D<CirculinearElement2D> toPolyCurve(LineString line) throws Exception {
        Coordinate[] coordinates = this.removeDuplicateCoordinates(line.getCoordinates());
        if (coordinates.length < 2) {
            throw new Exception("offset contour result is empty");
        }

        PolyCirculinearCurve2D<CirculinearElement2D> curve = new PolyCirculinearCurve2D<>();
        int index = 0;
        while (index < coordinates.length - 1) {
            ArcFit fit = this.findBestArcFit(coordinates, index);
            if (fit != null) {
                curve.add(fit.arc);
                index = fit.endIndex;
                continue;
            }

            Coordinate prev = coordinates[index];
            Coordinate current = coordinates[index + 1];
            if (!this.sameCoordinate(prev, current)) {
                curve.add(new LineSegment2D(new Point2D(prev.x, prev.y), new Point2D(current.x, current.y)));
            }
            index++;
        }

        if (curve.size() == 0) {
            throw new Exception("offset contour result has no segments");
        }
        return curve;
    }

    /**
     * Finds the longest valid arc run starting at the given coordinate index.
     *
     * @param coordinates sampled offset coordinates
     * @param startIndex start index of the candidate run
     * @return the best fitted arc, or {@code null} if the run should stay linear
     */
    private ArcFit findBestArcFit(Coordinate[] coordinates, int startIndex) {
        ArcFit bestFit = null;
        for (int endIndex = coordinates.length - 1; endIndex >= startIndex + MIN_ARC_POINTS - 1; endIndex--) {
            ArcFit candidate = this.tryCreateArcFit(coordinates, startIndex, endIndex);
            if (candidate != null) {
                bestFit = candidate;
                break;
            }
        }
        return bestFit;
    }

    /**
     * Attempts to fit one arc over a contiguous coordinate range.
     *
     * @param coordinates sampled offset coordinates
     * @param startIndex start index of the candidate run
     * @param endIndex end index of the candidate run
     * @return fitted arc metadata, or {@code null} when the run is not stable enough
     */
    private ArcFit tryCreateArcFit(Coordinate[] coordinates, int startIndex, int endIndex) {
        int pointCount = endIndex - startIndex + 1;
        if (pointCount < MIN_ARC_POINTS) {
            return null;
        }

        int midIndex = startIndex + pointCount / 2;
        if (midIndex <= startIndex || midIndex >= endIndex) {
            return null;
        }

        Point2D start = this.toPoint(coordinates[startIndex]);
        Point2D middle = this.toPoint(coordinates[midIndex]);
        Point2D end = this.toPoint(coordinates[endIndex]);
        if (this.areCollinear(start, middle, end)) {
            return null;
        }

        Circle2D circle;
        try {
            circle = Circle2D.circumCircle(start, middle, end);
        } catch (Exception ex) {
            return null;
        }
        if (circle == null || Double.isNaN(circle.radius()) || Double.isInfinite(circle.radius())) {
            return null;
        }
        if (circle.radius() < MIN_ARC_RADIUS) {
            return null;
        }

        Point2D center = circle.center();
        Boolean ccw = null;
        double previousAngle = 0.0;
        boolean previousAngleSet = false;
        for (int i = startIndex; i <= endIndex; i++) {
            Point2D point = this.toPoint(coordinates[i]);
            double radiusError = Math.abs(point.distance(center) - circle.radius());
            if (radiusError > ARC_TOLERANCE) {
                return null;
            }

            if (i > startIndex && i < endIndex) {
                double orientation = this.orientation(start, point, end);
                if (Math.abs(orientation) < EPSILON) {
                    return null;
                }
                boolean currentCcw = orientation > 0.0;
                if (ccw == null) {
                    ccw = currentCcw;
                } else if (ccw.booleanValue() != currentCcw) {
                    return null;
                }
            }

            double angle = Math.atan2(point.y() - center.y(), point.x() - center.x());
            if (previousAngleSet && i > startIndex) {
                double delta = this.normalizeAngle(angle - previousAngle);
                if (Math.abs(delta) < EPSILON) {
                    return null;
                }
                boolean stepCcw = delta > 0.0;
                if (ccw == null) {
                    ccw = stepCcw;
                } else if (ccw.booleanValue() != stepCcw) {
                    return null;
                }
            }
            previousAngle = angle;
            previousAngleSet = true;
        }

        if (ccw == null) {
            return null;
        }

        CircleArc2D arc;
        try {
            arc = geo.createCircleArc(start, end, circle.radius(), ccw.booleanValue());
        } catch (Exception ex) {
            return null;
        }

        if (!this.matchesArc(arc, coordinates, startIndex, endIndex)) {
            return null;
        }

        ArcFit fit = new ArcFit();
        fit.arc = arc;
        fit.endIndex = endIndex;
        return fit;
    }

    /**
     * Verifies that all intermediate coordinates stay close enough to the
     * reconstructed arc.
     *
     * @param arc fitted arc candidate
     * @param coordinates sampled offset coordinates
     * @param startIndex start of the run
     * @param endIndex end of the run
     * @return {@code true} when the run matches the arc within tolerance
     */
    private boolean matchesArc(CircleArc2D arc, Coordinate[] coordinates, int startIndex, int endIndex) {
        for (int i = startIndex + 1; i < endIndex; i++) {
            Point2D point = this.toPoint(coordinates[i]);
            if (arc.distance(point) > ARC_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    /**
     * Chooses the most useful JTS line result and orients it so it follows the
     * original contour direction.
     *
     * @param geometry raw JTS offset result
     * @param originalLine source line used for the offset operation
     * @return best oriented line string
     * @throws Exception if JTS returned no usable line geometry
     */
    private LineString selectBestLine(Geometry geometry, LineString originalLine) throws Exception {
        ArrayList<LineString> lines = new ArrayList<>();
        this.collectLineStrings(geometry, lines);
        if (lines.isEmpty()) {
            throw new Exception("offset contour result has no line string");
        }

        LineString best = lines.get(0);
        double bestLength = best.getLength();
        for (int i = 1; i < lines.size(); i++) {
            LineString candidate = lines.get(i);
            double candidateLength = candidate.getLength();
            if (candidateLength > bestLength) {
                best = candidate;
                bestLength = candidateLength;
            }
        }

        Coordinate[] coordinates = best.getCoordinates();
        if (coordinates.length >= 2) {
            Coordinate inputStart = originalLine.getCoordinateN(0);
            double firstDistance = coordinates[0].distance(inputStart);
            double lastDistance = coordinates[coordinates.length - 1].distance(inputStart);
            if (lastDistance < firstDistance) {
                coordinates = this.reverseCoordinates(coordinates);
                best = GEOMETRY_FACTORY.createLineString(coordinates);
            }
        }
        return best;
    }

    /**
     * Recursively flattens JTS geometry collections into line strings.
     *
     * @param geometry geometry to inspect
     * @param lines destination list for usable line strings
     */
    private void collectLineStrings(Geometry geometry, List<LineString> lines) {
        if (geometry instanceof LineString) {
            LineString line = (LineString) geometry;
            if (line.getNumPoints() >= 2 && line.getLength() > EPSILON) {
                lines.add(line);
            }
            return;
        }

        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            this.collectLineStrings(geometry.getGeometryN(i), lines);
        }
    }

    /**
     * Chooses how finely an existing arc should be sampled before it is passed to JTS.
     *
     * @param arc source arc
     * @return polyline segment count
     */
    private int getArcSegmentCount(CircleArc2D arc) {
        int byAngle = (int) Math.ceil(Math.abs(arc.getAngleExtent()) / (Math.PI / 18.0));
        int byLength = (int) Math.ceil(arc.length() / 1.0);
        return Math.max(8, Math.min(180, Math.max(byAngle, byLength)));
    }

    /**
     * Appends one point to the coordinate list while suppressing immediate duplicates.
     *
     * @param coordinates destination list
     * @param point point to append
     */
    private void appendCoordinate(ArrayList<Coordinate> coordinates, Point2D point) {
        Coordinate coordinate = new Coordinate(point.getX(), point.getY());
        if (!coordinates.isEmpty() && this.sameCoordinate(coordinates.get(coordinates.size() - 1), coordinate)) {
            return;
        }
        coordinates.add(coordinate);
    }

    /**
     * Removes consecutive duplicate coordinates from a collection.
     *
     * @param coordinates source coordinates
     * @return deduplicated coordinate array
     */
    private Coordinate[] removeDuplicateCoordinates(Collection<Coordinate> coordinates) {
        ArrayList<Coordinate> clean = new ArrayList<>();
        for (Coordinate coordinate : coordinates) {
            if (!clean.isEmpty() && this.sameCoordinate(clean.get(clean.size() - 1), coordinate)) {
                continue;
            }
            clean.add(new Coordinate(coordinate.x, coordinate.y));
        }
        return clean.toArray(new Coordinate[clean.size()]);
    }

    /**
     * Removes consecutive duplicate coordinates from an array.
     *
     * @param coordinates source coordinates
     * @return deduplicated coordinate array
     */
    private Coordinate[] removeDuplicateCoordinates(Coordinate[] coordinates) {
        ArrayList<Coordinate> clean = new ArrayList<>();
        for (Coordinate coordinate : coordinates) {
            if (!clean.isEmpty() && this.sameCoordinate(clean.get(clean.size() - 1), coordinate)) {
                continue;
            }
            clean.add(new Coordinate(coordinate.x, coordinate.y));
        }
        return clean.toArray(new Coordinate[clean.size()]);
    }

    /**
     * Reverses the coordinate order of a line string.
     *
     * @param coordinates source coordinates
     * @return reversed copy
     */
    private Coordinate[] reverseCoordinates(Coordinate[] coordinates) {
        Coordinate[] reversed = new Coordinate[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            Coordinate coordinate = coordinates[coordinates.length - i - 1];
            reversed[i] = new Coordinate(coordinate.x, coordinate.y);
        }
        return reversed;
    }

    /**
     * Tests whether two coordinates are equal within the local epsilon.
     *
     * @param a first coordinate
     * @param b second coordinate
     * @return {@code true} when both coordinates are effectively identical
     */
    private boolean sameCoordinate(Coordinate a, Coordinate b) {
        return Math.abs(a.x - b.x) < EPSILON && Math.abs(a.y - b.y) < EPSILON;
    }

    /**
     * Converts one JTS coordinate into a javaGeom point.
     *
     * @param coordinate coordinate to convert
     * @return converted point
     */
    private Point2D toPoint(Coordinate coordinate) {
        return new Point2D(coordinate.x, coordinate.y);
    }

    /**
     * Checks whether three points are effectively collinear.
     *
     * @param a first point
     * @param b second point
     * @param c third point
     * @return {@code true} when the points lie on the same line within epsilon
     */
    private boolean areCollinear(Point2D a, Point2D b, Point2D c) {
        return Math.abs(this.orientation(a, b, c)) < EPSILON;
    }

    /**
     * Returns the signed orientation of the triangle defined by three points.
     *
     * @param a first point
     * @param b second point
     * @param c third point
     * @return positive for counterclockwise, negative for clockwise
     */
    private double orientation(Point2D a, Point2D b, Point2D c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }

    /**
     * Normalizes an angle delta into the {@code (-pi, pi]} range.
     *
     * @param angle raw angle delta
     * @return normalized angle delta
     */
    private double normalizeAngle(double angle) {
        while (angle <= -Math.PI) {
            angle += 2.0 * Math.PI;
        }
        while (angle > Math.PI) {
            angle -= 2.0 * Math.PI;
        }
        return angle;
    }

    /**
     * Holds a fitted arc together with the last consumed coordinate index.
     */
    private static class ArcFit {

        private CircleArc2D arc;
        private int endIndex;
    }
}
