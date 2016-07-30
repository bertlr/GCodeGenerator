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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Herbert Roider <herbert.roider@utanet.at>
 */
public class parameter {

        public String name;
        public int index;
        //public double value;
        public String strval;

        public void parse_key(String key) {
                String trimmed_key = key.trim();
                Matcher m = Pattern.compile("\\d").matcher(trimmed_key);
                int firstDigitLocation = m.find() ? m.start() + 1 : 0;
                if (firstDigitLocation > 0) {
                        this.name = trimmed_key.substring(0, firstDigitLocation-1);
                        this.index = Integer.parseInt(trimmed_key.substring(firstDigitLocation-1, trimmed_key.length()));

                } else {
                        this.name = trimmed_key;
                        this.index = -1;
                }

        }

        public void parse(String expr) {
                String trimmed_expr = expr.trim();
                int assign_pos = trimmed_expr.indexOf('=');
                if (assign_pos > 0) {
                        String key = trimmed_expr.substring(0, assign_pos);
                        this.strval = trimmed_expr.substring(assign_pos+1, trimmed_expr.length());
                        this.parse_key(key);

                } else {

                        Matcher m = Pattern.compile("(-)?([0-9])").matcher(trimmed_expr);
                        int firstDigitLocation = 0;
                        if(m.find()){
                                firstDigitLocation = m.start();
                        }
                       
                        if (firstDigitLocation > 0) {
                                this.name = trimmed_expr.substring(0, firstDigitLocation);
                                this.index = -1;
                                this.strval = trimmed_expr.substring(firstDigitLocation, trimmed_expr.length());

                        }

                }

        }

}
