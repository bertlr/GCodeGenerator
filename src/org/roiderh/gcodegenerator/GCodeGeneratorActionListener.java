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
package org.roiderh.gcodegenerator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import org.roiderh.gcodegeneratordialogs.FunctionConf;
import org.roiderh.gcodegeneratordialogs.DialogSelectGenerator;
import org.roiderh.gcodegeneratordialogs.DialogGenerateCode;

@ActionID(
        category = "File",
        id = "org.roiderh.gcodegenerator.GCodeGeneratorActionListener"
)
@ActionRegistration(
        iconBase = "org/roiderh/gcodegenerator/hi22-gcode-generator.png",
        displayName = "#CTL_GCodeGeneratorActionListener"
)
@ActionReference(path = "Toolbars/File", position = 0)
@Messages("CTL_GCodeGeneratorActionListener=generates g-code")
public final class GCodeGeneratorActionListener implements ActionListener {

    //private LineCookie context;
    //private JTextComponent editor;
    // private StyledDocument document;
    private String selectedText;
    //private String selectedGenerator;
    private String generatedGCode;

    @Override
    public void actionPerformed(ActionEvent e) {
        JTextComponent ed = org.netbeans.api.editor.EditorRegistry.lastFocusedComponent();
        if (ed == null) {
            JOptionPane.showMessageDialog(null, "Error: no open editor");
            return;
        }

        this.selectedText = ed.getSelectedText();
        if (selectedText == null) {
            selectedText = "";
        }

        Gson gson = new Gson();

        try {

            Reader reader = new InputStreamReader(GCodeGeneratorActionListener.class.getResourceAsStream("/resources/cycles.json"), "UTF-8");
            FunctionConf[] fc = gson.fromJson(reader, FunctionConf[].class);
            FunctionConf generator_conf = null;

            DialogSelectGenerator nf = new DialogSelectGenerator(fc, org.openide.windows.WindowManager.getDefault().getMainWindow(), true);
            nf.setLocationRelativeTo(org.openide.windows.WindowManager.getDefault().getMainWindow());
            nf.setVisible(true);
            generator_conf = nf.generator;
            if(generator_conf == null){
                return;
            }

            
            DialogGenerateCode btf = new DialogGenerateCode(selectedText, generator_conf, org.openide.windows.WindowManager.getDefault().getMainWindow(), true);
            btf.setLocationRelativeTo(org.openide.windows.WindowManager.getDefault().getMainWindow());
            btf.setVisible(true);
           
        } catch (Exception e1) {
            System.out.println("Error " + e1.toString());
            JOptionPane.showMessageDialog(null, "Error: " + e1.getMessage());
            return;

        }

    }
}
