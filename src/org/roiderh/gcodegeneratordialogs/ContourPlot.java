/*
 * Copyright (C) 2016 by Herbert Roider <herbert@roider.at>
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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import math.geom2d.AffineTransform2D;
import math.geom2d.Box2D;
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.line.Line2D;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class ContourPlot extends JPanel {

    /**
     * The curves to display
     */
    public PolyCirculinearCurve2D<CirculinearElement2D> origCurve;
    public PolyCirculinearCurve2D<CirculinearElement2D> newCurve;

    private math.geom2d.Box2D getBoundingBox(PolyCirculinearCurve2D<CirculinearElement2D> c_el) {

        //c_el.add(new Line2D(c_el.getLast().lastPoint(), c_el.getFirst().firstPoint()));
        math.geom2d.Box2D bb = null;
        math.geom2d.Box2D bb1 = null;
        for (int i = 0; i < c_el.size(); i++) {
            CirculinearElement2D el = c_el.get(i);
            bb1 = el.boundingBox();
            if (bb1 == null) {
                return null;
            }
            if (i == 0) {
                bb = bb1;

            } else {
                bb = bb.union(bb1);
            }

        }
        return bb;

    }

    private void doDrawing(Graphics g) {
        if (this.origCurve == null) {
            return;
        }
        if (newCurve == null) {
            return;
        }

        PolyCirculinearCurve2D<CirculinearElement2D> new_curve = newCurve;
        PolyCirculinearCurve2D<CirculinearElement2D> orig_curve = origCurve;

        int canvas_width = this.getWidth();
        int canvas_height = this.getHeight();

        Graphics2D g2d = (Graphics2D) g;

        Box2D bb = this.getBoundingBox(orig_curve);
        Box2D bb_new = this.getBoundingBox(new_curve);
        bb = bb.union(bb_new);

        double max_x = bb.getMaxX();
        double min_x = bb.getMinX();
        double max_y = bb.getMaxY();
        double min_y = bb.getMinY();

        double middle_x = (max_x + min_x) / 2.0;
        double middle_y = (max_y + min_y) / 2.0;
        double width = bb.getWidth();
        double height = bb.getHeight();

        double x_trans = (double) canvas_width / 2.0 - middle_x;
        double y_trans = (double) canvas_height / 2.0 - middle_y;

        double x_fact = (double) canvas_width * 0.9 / width;
        double y_fact = (double) canvas_height * 0.9 / height;
        double fact = Math.min(x_fact, y_fact);

        AffineTransform2D sca = AffineTransform2D.createScaling(new Point2D(middle_x, middle_y), fact, fact);
        AffineTransform2D tra = AffineTransform2D.createTranslation(x_trans, y_trans);
        AffineTransform2D mir = AffineTransform2D.createLineReflection(new Line2D(new Point2D(0, middle_y), new Point2D(1, middle_y)));

        g2d.setColor(Color.green);
        orig_curve.transform(mir).transform(sca).transform(tra).draw(g2d);

        g2d.setColor(Color.RED);
        new_curve.transform(mir).transform(sca).transform(tra).draw(g2d);
        // draw a circle at the end of the curve
        double radius = 3;
        Circle2D ende = new Circle2D(new_curve.lastPoint(), radius / fact);
        ende.transform(mir).transform(sca).transform(tra).draw(g2d);

    }

    @Override
    public void paintComponent(Graphics g
    ) {

        super.paintComponent(g);
        doDrawing(g);
    }

}
