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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import org.roiderh.gcodeviewer.lexer.Gcodereader;
import org.roiderh.gcodeviewer.lexer.GcodereaderConstants;
import org.roiderh.gcodeviewer.lexer.Token;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import java.util.regex.*;
import java.util.LinkedList;
import org.roiderh.gcodeviewer.customfunc.IncAbs;
import math.geom2d.Point2D;
import math.geom2d.line.LineSegment2D;
import java.util.Collection;
import java.util.HashSet;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.polygon.LinearCurve2D;
import org.roiderh.gcodeviewer.customfunc.SinCosTan;

/**
 *
 * @author Herbert Roider <herbert.roider@utanet.at>
 */
public class gcodereader {

    public int linenumber_offset = 0;
    /**
     * Holds the Error messages since last function read call.
     *
     */
    public Collection<String> messages = null;

    public LinkedList<contourelement> read(InputStream is) throws Exception {
        //FileInputStream is;
        //FileOutputStream os;
        this.messages = new HashSet<>();
        boolean G0 = false;
        boolean G1 = false;
        boolean G2 = false;
        boolean G3 = false;
        double X = Double.MAX_VALUE;
        double Y = Double.MAX_VALUE;
        double Z = Double.MAX_VALUE;

        double X_prev = X;
        double Y_prev = Y;
        double Z_prev = Z;

        // circle Centerpoint coordinates relative to the startpoint
        double I = Double.MAX_VALUE;
        double J = Double.MAX_VALUE;
        double K = Double.MAX_VALUE;

        double B = Double.MAX_VALUE;
        double CR = Double.MAX_VALUE;
        double CHR = Double.MAX_VALUE;
        double RND = Double.MAX_VALUE;

        int machine = 0; // spinner = 0, emco = 1
        int linenumber = 0;

        geometry geo = new geometry();
        // R Parameters:
        Map<Integer, Double> R = new HashMap<>();
        //Pattern floatstring = Pattern.compile("(=)?(-)?([0-9])*.?([0-9])*");
        //Pattern integerstring = Pattern.compile("(=)?(-)?([0-9])+");
        point last_pos = null;
        point current_pos = new point();
        // All points to display:
        LinkedList<point> points = new LinkedList<>();
        LinkedList<contourelement> contour = new LinkedList<>();
        G0 = true;
        G1 = false;
        G2 = false;
        G3 = false;

        //try {
        //os = new FileOutputStream(new File("/home/herbert/NetBeansProjects/gcodeviewer/src/gcodeviewer/punkte.txt"));
        Token t;
        BufferedReader br;
        String line;
        InputStream istream;
        br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        //InputStream line = new ByteArrayInputStream(this.selectedText.getBytes());
        while ((line = br.readLine()) != null) {
            linenumber++;
            // remove comments, from semicolon to the line break.
            int semicolon_pos = line.indexOf(";");
            if (semicolon_pos >= 0) {
                line = line.substring(0, semicolon_pos);
            }
            // if the first character of a line is a brace, 
            // the complete line is interpreted as a comment. For old sinumerik 810
            int first_brace_pos = line.trim().indexOf("(");
            if (first_brace_pos == 0) {
                line = "";
            }
            // The parser needs an line break:
            line += '\n';
            System.out.println("line=" + line);
            // Parameters are not modal:
            B = 0.0;
            CHR = 0.0;
            RND = 0.0;
            CR = 0.0;

            X_prev = X;
            Y_prev = Y;
            Z_prev = Z;

            istream = new ByteArrayInputStream(line.getBytes());
            Gcodereader gr = new Gcodereader(istream);

            /*
             read one line
             */
            do {
                t = gr.getNextToken();
                if (t.kind == GcodereaderConstants.EOF) {
                    break;
                }
                System.out.println("Token: " + t.kind + ", " + t.image);
                Matcher m;
                parameter para = null;
                if (t.kind == GcodereaderConstants.G) {
                    String sNumber = t.image.substring(1, t.image.length());
                    int g_number = Integer.parseInt(sNumber);
                    if (g_number <= 3) {
                        G0 = false;
                        G1 = false;
                        G2 = false;
                        G3 = false;

                        switch (g_number) {
                            case 0:
                                G0 = true;
                                break;
                            case 1:
                                G1 = true;
                                break;
                            case 2:
                                G2 = true;
                                break;
                            case 3:
                                G3 = true;
                                break;

                        }
                    }

                }
                // All parameters except G-functions
                if (t.kind == GcodereaderConstants.PARAM || t.kind == GcodereaderConstants.SHORT_PARAM) {

                    para = new parameter();
                    para.parse(t.image);

                }
                if (para == null) {
                    continue;
                }
                if (para.name.compareTo("A") == 0) {
                    CalcException newExcept = new CalcException("\"A\" is not supported in line: " + String.valueOf(linenumber));
                    throw newExcept;
                }
                if (para.name.compareTo("R") == 0) {
                    org.nfunk.jep.JEP myParser = new org.nfunk.jep.JEP();
                    myParser.addStandardFunctions();
                    myParser.addStandardConstants();

                    for (Map.Entry<Integer, Double> entry : R.entrySet()) {
                        myParser.addVariable("R" + entry.getKey().toString(), entry.getValue());
                    }

                    myParser.parseExpression(para.strval);

                    double val = myParser.getValue();
                    R.put(para.index, val);

                }
                // Move functions (modal, valid until overridden):
                if (G0 || G1 || G2 || G3) {

                    if (para.name.compareTo("X") == 0
                            || para.name.compareTo("Y") == 0
                            || para.name.compareTo("Z") == 0
                            || para.name.compareTo("I") == 0
                            || para.name.compareTo("J") == 0
                            || para.name.compareTo("K") == 0
                            || para.name.compareTo("B") == 0
                            || para.name.compareTo("CR") == 0
                            || para.name.compareTo("CHR") == 0
                            || para.name.compareTo("RND") == 0) {

                        org.nfunk.jep.JEP myParser = new org.nfunk.jep.JEP();
                        myParser.addStandardFunctions();
                        myParser.addStandardConstants();

                        if (last_pos != null) {
                            switch (para.name) {
                                case "X":
                                    myParser.addFunction("IC", new IncAbs(X, true)); // Add the custom function
                                    //myParser.addFunction("AC", new IncAbs(X, false)); // Add the custom function

                                    break;

                                case "I":
                                    //myParser.addFunction("IC", new IncAbs(X, true)); // Add the custom function
                                    myParser.addFunction("AC", new IncAbs(X, false)); // Add the custom function

                                    break;
                                case "Y":
                                    //myParser.addFunction("IC", new IncAbs(0, true)); // Add the custom function
                                    //myParser.addFunction("AC", new IncAbs(Y, false)); // Add the custom function

                                    break;

                                case "J":
                                    //myParser.addFunction("IC", new IncAbs(Y, true)); // Add the custom function
                                    //myParser.addFunction("AC", new IncAbs(0, false)); // Add the custom function

                                    break;
                                case "Z":
                                    myParser.addFunction("IC", new IncAbs(Z, true)); // Add the custom function
                                    //myParser.addFunction("AC", new IncAbs(Z, false)); // Add the custom function

                                    break;

                                case "K":
                                    //myParser.addFunction("IC", new IncAbs(Z, true)); // Add the custom function
                                    myParser.addFunction("AC", new IncAbs(Z, false)); // Add the custom function

                                    break;

                            }
                        }
                        myParser.addFunction("SIN", new SinCosTan(SinCosTan.Function.SIN));
                        myParser.addFunction("COS", new SinCosTan(SinCosTan.Function.COS));
                        myParser.addFunction("TAN", new SinCosTan(SinCosTan.Function.TAN));
                        myParser.addFunction("ASIN", new SinCosTan(SinCosTan.Function.ASIN));
                        myParser.addFunction("ACOS", new SinCosTan(SinCosTan.Function.ACOS));
                        myParser.addFunction("ATAN", new SinCosTan(SinCosTan.Function.ATAN));
                        myParser.addFunction("ATAN2", new SinCosTan(SinCosTan.Function.ATAN2));
                        myParser.addFunction("SQRT", new SinCosTan(SinCosTan.Function.SQRT));
                        myParser.addFunction("POT", new SinCosTan(SinCosTan.Function.POT));

                        for (Map.Entry<Integer, Double> entry : R.entrySet()) {
                            myParser.addVariable("R" + entry.getKey().toString(), entry.getValue());
                        }

                        myParser.parseExpression(para.strval);

                        double val = myParser.getValue();

                        System.out.println("key " + para.name + " = " + val);
                        switch (para.name) {
                            case "X":
                                X = val;
                                break;
                            case "Y":
                                Y = val;
                                break;
                            case "Z":
                                Z = val;
                                break;

                            case "I":
                                I = val;
                                break;
                            case "J":
                                J = val;
                                break;
                            case "K":
                                K = val;
                                break;

                            case "B":
                                B = val;
                                machine = 1; // emco
                                break;
                            case "CR":
                                CR = val;
                                machine = 0; // spinner
                                break;

                            case "CHR":
                                CHR = val;
                                machine = 0;
                                break;
                            case "RND":
                                RND = val;
                                machine = 0;
                                break;

                        }

                    }
                }

            } while (!(t.kind == GcodereaderConstants.EOF));

            // when no movement, continue
            if (X_prev == X && Z_prev == Z) {
                continue;
            }
            //System.out.println("Break ");
            //if (G0 || G1 || G2 || G3) {
            System.out.println("Verfahrbefehl");
            if (Z == Double.MAX_VALUE || X == Double.MAX_VALUE) {
                CalcException newExcept = new CalcException("X and Z value are necessary for the first point");
                throw newExcept;
            }
            current_pos.x = Z;
            current_pos.y = X / 2.0;
            if (last_pos == null) {
                last_pos = current_pos.clone();
            }

            contourelement c_elem = new contourelement();
            c_elem.linenumber = linenumber;

            // Transition Element:
            if (machine == 0) {

                if (RND > 0.0) {
                    c_elem.transition_elem_size = RND;
                    c_elem.transistion_elem = contourelement.Transition.ROUND;
                } else if (CHR > 0.0) {
                    c_elem.transition_elem_size = CHR;
                    c_elem.transistion_elem = contourelement.Transition.CHAMFER;

                }

            } else if (machine == 1) {
                // only at G1 a transition element, not at G2/G3 because B is interpreted as radius
                if (G1) {
                    if (B > 0.0) {
                        c_elem.transition_elem_size = B;
                        c_elem.transistion_elem = contourelement.Transition.ROUND;
                    } else if (B < 0.0) {
                        c_elem.transition_elem_size = -B;
                        c_elem.transistion_elem = contourelement.Transition.CHAMFER;

                    }
                }
            }
            // Movement G0, G1, G2, G3
            if (G2 | G3) {
                boolean ccw = false;
                // clockwise or counterclockwise
                if (G2) {
                    ccw = false;
                } else {
                    ccw = true;
                }
                double r = 0.0;
                switch (machine) {
                    case 0:
                        r = CR;
                        break;
                    case 1:
                        r = B;
                        break;
                }

                if (r == 0.0) {
                    r = Math.sqrt(Math.pow((0.5 * I), 2.0) + Math.pow(K, 2.0));
                }

                //c_elem.points = geo.circle(last_pos, current_pos, r, ccw);
                c_elem.points.add(last_pos.clone());
                c_elem.points.add(current_pos.clone());
                c_elem.radius = r;
                c_elem.ccw = ccw;
                // no Transition Element at G2 or G3
                //c_elem.transition_elem_size = 0.0;
                c_elem.shape = contourelement.Shape.ARC;
                c_elem.feed = contourelement.Feed.CUTTING;

                //}
            } else if (G1) {
                //points.add(current_pos.clone());
                c_elem.points.add(last_pos.clone());
                c_elem.points.add(current_pos.clone());
                c_elem.shape = contourelement.Shape.LINE;
                c_elem.feed = contourelement.Feed.CUTTING;

            } else if (G0) {
                //points.add(current_pos.clone());
                c_elem.points.add(last_pos.clone());
                c_elem.points.add(current_pos.clone());
                c_elem.shape = contourelement.Shape.LINE;
                c_elem.feed = contourelement.Feed.RAPID;
                c_elem.transition_elem_size = 0.0;

            }
            // add point only when movement
            if (!last_pos.equals(current_pos) || contour.size() == 0) {
                contour.add(c_elem);
            }
            last_pos = current_pos.clone();
            //}

        }

        // Add a last Dummy Element with the same Coordinates as the last elements, so the way is 0:
        contourelement c_elem = new contourelement();
        c_elem.linenumber = Integer.MAX_VALUE;
        c_elem.points.add(last_pos.clone());
        c_elem.points.add(current_pos.clone());
        c_elem.shape = contourelement.Shape.LINE;
        c_elem.feed = contourelement.Feed.RAPID;
        c_elem.transition_elem_size = 0.0;
        c_elem.start = last_pos.createPoint2D();
        c_elem.end = current_pos.createPoint2D();

        contour.add(c_elem);

        // Create Contour points to display from Program Lines:
        boolean is_first = true;
        // points ready for display
        LinkedList<Point2D> disp = new LinkedList<>();
        contourelement current_ce = null;

        // calculate the transitions elements and the result vertexes and tangent points.
        // Also add points to the display contour, which is simple a chain of lines.
        for (contourelement next_ce : contour) {
            // First contour element:
            if (current_ce == null) {
                current_ce = next_ce;
                current_ce.start = current_ce.points.getFirst().createPoint2D();
                current_ce.end = current_ce.points.getLast().createPoint2D();
                next_ce.start = current_ce.end;
                continue;

            }
            current_ce.end = current_ce.points.getLast().createPoint2D();
            next_ce.start = current_ce.end;

            CirculinearElement2D current = null;
            CirculinearElement2D next = null;
            CirculinearElement2D transition = null;
            /*
             calculate the current and the next element without the transition element:
             */
            if (current_ce.shape == contourelement.Shape.ARC) {
                current = geo.createCircleArc(current_ce.points.getFirst().createPoint2D(), current_ce.points.getLast().createPoint2D(), current_ce.radius, current_ce.ccw);
            } else {
                current = new LineSegment2D(current_ce.points.getFirst().createPoint2D(), current_ce.points.getLast().createPoint2D());
            }
            if (current == null) {
                this.messages.add("cannot calculate the element in line: " + current_ce.linenumber);
                break;
            }
            if (next_ce.shape == contourelement.Shape.ARC) {
                next = geo.createCircleArc(next_ce.points.getFirst().createPoint2D(), next_ce.points.getLast().createPoint2D(), next_ce.radius, next_ce.ccw);

            } else {
                next = new LineSegment2D(next_ce.points.getFirst().createPoint2D(), next_ce.points.getLast().createPoint2D());

            }
            if (next == null) {
                this.messages.add("cannot calculate the element in line: " + next_ce.linenumber);
                break;

            }
            current_ce.curve = current;

            /*
             calculate the transition element and the new vertexes for the 2 elements:
             */
            if (current_ce.transition_elem_size != 0) {
                if (current_ce.transistion_elem == contourelement.Transition.ROUND) {
                    transition = geo.getRound(current, next, current_ce.transition_elem_size);
                } else if (current_ce.transistion_elem == contourelement.Transition.CHAMFER) {
                    transition = geo.getChamfer(current, next, current_ce.transition_elem_size);
                } else {

                }
                if (transition == null) {
                    this.messages.add("cannot calculate the transition in line: " + current_ce.linenumber);
                    break;
                }
                current_ce.end = transition.firstPoint();
                next_ce.start = transition.lastPoint();
                current_ce.transition_curve = transition;

            }

            // Calculate the element with transition element:
            if (current_ce.shape == contourelement.Shape.ARC) {
                current_ce.curve = geo.createCircleArc(current_ce.start, current_ce.end, current_ce.radius, current_ce.ccw);
            } else {
                current_ce.curve = new LineSegment2D(current_ce.start, current_ce.end);
            }

            current_ce = next_ce;
        }

        System.out.println("Contur without transition elements");
        for (contourelement ce : contour) {

            for (point p : ce.points) {
                System.out.println("x=" + p.x + ", y=" + p.y);

            }

        }
        if (this.messages.size() > 0) {
            for (String s : this.messages) {
                System.out.println(s);
            }

        }

        return contour;
    }

    /**
     * create a list with all edge points to display. Circles are returned as
     * multiple points.
     *
     * @param contour
     * @return list with all points
     */
    public LinkedList<Point2D> create_display_points(LinkedList<contourelement> contour) {
        geometry geo = new geometry();
        LinkedList<Point2D> disp = new LinkedList<>();

        // calculate the transitions elements and the result vertexes and tangent points.
        // Also add points to the display contour, which is simple a chain of lines.
        for (contourelement current_ce : contour) {

            if (current_ce.curve == null) {
                continue;
            }
            /*
             Add the current element to the contour as multiple lines.
             */
            if (current_ce.shape == contourelement.Shape.ARC) {
                LinearCurve2D c1 = current_ce.curve.asPolyline(10);
                //Polyline2D pl = c1;
                for (Point2D p : c1.vertices()) {
                    disp.add(p);
                }
            } else {
                LinearCurve2D c1 = current_ce.curve.asPolyline(1);
                //Polyline2D pl = c1;
                for (Point2D p : c1.vertices()) {
                    disp.add(p);
                }

            }
            /*
             Append the transition element to the current element for display: 
             */
            if (current_ce.transition_elem_size > 0) {
                if (current_ce.transistion_elem == contourelement.Transition.ROUND) {
                    LinearCurve2D pl = current_ce.transition_curve.asPolyline(10);
                    for (Point2D p : pl.vertices()) {
                        disp.add(p);
                    }

                } else if (current_ce.transistion_elem == contourelement.Transition.CHAMFER) {
                    LinearCurve2D pl = current_ce.transition_curve.asPolyline(1);
                    for (Point2D p : pl.vertices()) {
                        disp.add(p);
                    }

                }
            }

            //current_ce = next_ce;
        }

        System.out.println("Contur without transition elements");
        for (contourelement ce : contour) {

            for (point p : ce.points) {
                System.out.println("x=" + p.x + ", y=" + p.y);

            }

        }
        if (this.messages.size() > 0) {
            for (String s : this.messages) {
                System.out.println(s);
            }

        }
//                System.out.println("ready calculated Contur");
//                for (point p : disp) {
//                        System.out.println("x=" + p.x + ", y=" + p.y);
//
//                }
        return disp;

    }

}
