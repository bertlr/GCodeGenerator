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
package org.roiderh.gcodeviewer.customfunc;

//import org.roiderh.gcodeviewer.customfunc.*;
import java.util.Stack;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 *
 * @author Herbert Roider <herbert.roider@utanet.at>
 */
public class IncAbs extends PostfixMathCommand {

        double current_pos = 0.0;
        boolean incremental = true;  // Incrementeller Wert wird übergeben, und der Abs. Wert zurückgegeben IC(), sonst umgekehrt:  otherwise AC()

        /**
         * 
         * @param _current_pos
         * @param inc if true, the run function need a incremental value and returned the abs. value ("IC()")
         *            if false, the run function need a absolute value and returned the incremental value  ("AC()")
         */
        public IncAbs(double _current_pos, boolean inc) {
                this.current_pos = _current_pos;
                
                this.incremental = inc;

                numberOfParameters = 1;
        }

        /**
         * Runs the square root operation on the inStack. The parameter is
         * popped off the <code>inStack</code>, and the square root of it's
         * value is pushed back to the top of <code>inStack</code>.
         * 
         */
        @SuppressWarnings("unchecked")
        public void run(Stack inStack) throws ParseException {
                if(this.current_pos ==  Double.MAX_VALUE){
                        throw new ParseException("Invalid current position");
                }
                // check the stack
                checkStack(inStack);

                // get the parameter from the stack
                Object param = inStack.pop();

                // check whether the argument is of the right type
                if (param instanceof Double) {
                        // calculate the result
                        double r = 0.0;
                        if (this.incremental) {
                                r = this.current_pos + ((Double) param).doubleValue();
                        } else {
                                r = ((Double) param).doubleValue() - this.current_pos;
                        }

                        // push the result on the inStack
                        inStack.push(new Double(r));
                } else {
                        throw new ParseException("Invalid parameter type");
                }
        }
}
