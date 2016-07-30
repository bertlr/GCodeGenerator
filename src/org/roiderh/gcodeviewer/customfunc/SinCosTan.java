/*
 * Copyright (C) 2015 by Herbert Roider <herbert.roider@utanet.at>
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
package org.roiderh.gcodeviewer.customfunc;

import java.util.Stack;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class SinCosTan extends PostfixMathCommand {

    SinCosTan.Function fn;

    public enum Function {

        SIN, COS, TAN, ASIN, ACOS, ATAN, ATAN2, SQRT, POT
    }

    public SinCosTan(SinCosTan.Function _fn) {

        this.fn = _fn;
        numberOfParameters = 1;
        if (_fn == SinCosTan.Function.ATAN2) {
            numberOfParameters = 2;
        }
    }

    /**
     * Runs the square root operation on the inStack. The parameter is popped
     * off the <code>inStack</code>, and the square root of it's value is pushed
     * back to the top of <code>inStack</code>.
     *
     */
    @SuppressWarnings("unchecked")
    public void run(Stack inStack) throws ParseException {

        // check the stack
        checkStack(inStack);

        // get the parameter from the stack
        Object param1 = inStack.pop();
        Object param2 = null;
        double p1 = 0;
        double p2 = 0;
        if (param1 instanceof Double) {
            p1 = (double) param1;
            //p1 *= Math.PI / 180.0;
        } else {
            throw new ParseException("Invalid parameter type");
        }

        if (this.numberOfParameters == 2) {
            // This is the first Parameter!!!
            param2 = inStack.pop();
            if (param2 instanceof Double) {
                p2 = (double) param2;
                //p2 *= Math.PI / 180.0;
            } else {
                throw new ParseException("Invalid parameter type");
            }

        }

        double r = 0.0;
        switch (this.fn) {
            case SIN:
                r = Math.sin(p1 * Math.PI / 180.0);

                break;
            case COS:
                r = Math.cos(p1 * Math.PI / 180.0);
                break;

            case TAN:
                r = Math.tan(p1 * Math.PI / 180.0);
                break;

            case ASIN:
                r = Math.asin(p1);
                r *= 180.0 / Math.PI;
                break;

            case ACOS:
                r = Math.acos(p1);
                r *= 180.0 / Math.PI;
                break;

            case ATAN:
                r = Math.atan(p1);
                r *= 180.0 / Math.PI;
                break;

            case ATAN2:
                // Achtung: vertauschte Parameter, siehe weiter oben.
                r = Math.atan2(p2, p1);
                r *= 180.0 / Math.PI;
                break;
                
            case SQRT:
                r = Math.sqrt(p1);
                break;

             case POT:
                r = Math.pow(p1, 2.0);
                break;

       }

        // push the result on the inStack
        inStack.push(new Double(r));

    }
}
