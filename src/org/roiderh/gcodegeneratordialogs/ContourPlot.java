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
import java.util.LinkedList;
import javax.swing.JPanel;
import math.geom2d.AffineTransform2D;
import math.geom2d.Box2D;
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import math.geom2d.line.Line2D;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class ContourPlot extends JPanel {

    /**
     * The points to display
     */
    
    public LinkedList<CirculinearElement2D> origElements;
    public LinkedList<CirculinearElement2D> newElements;

    private void doDrawing(Graphics g) {
        if(this.origElements == null){
            return;
        }
        
        int canvas_width = this.getWidth();
        int canvas_height = this.getHeight();

        Graphics2D g2d = (Graphics2D) g;
        
        PolyCirculinearCurve2D orig_curve = new PolyCirculinearCurve2D(origElements);
        Box2D bb = new Box2D(origElements.getFirst().firstPoint(), origElements.getLast().lastPoint());
        //Box2D bb = orig_curve.boundingBox();
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

        //g2d.setColor(Color.blue);
        //bb.transform(mir).transform(sca).transform(tra).draw(g2d);

        g2d.setColor(Color.green);
        orig_curve.transform(mir).transform(sca).transform(tra).draw(g2d);
        
        if(newElements == null){
            return;
        }
        PolyCirculinearCurve2D new_curve = new PolyCirculinearCurve2D(newElements);
        g2d.setColor(Color.RED);
        new_curve.transform(mir).transform(sca).transform(tra).draw(g2d);

    }

    @Override
    public void paintComponent(Graphics g
    ) {

        super.paintComponent(g);
        doDrawing(g);
    }

}
