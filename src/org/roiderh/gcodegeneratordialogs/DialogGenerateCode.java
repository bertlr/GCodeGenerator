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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.swing.text.*;
import javax.swing.JLabel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import org.roiderh.gcodegeneratordialogs.generators.Mirror;
import org.roiderh.gcodegeneratordialogs.generators.Parallel;
import org.roiderh.gcodegeneratordialogs.generators.Reverse;
import org.roiderh.gcodegeneratordialogs.generators.Roughing;
import org.roiderh.gcodegeneratordialogs.generators.Translate;
import org.roiderh.gcodeviewer.contourelement;
import org.roiderh.gcodeviewer.gcodereader;

/**
 *
 * @author Herbert Roider <herbert@roider.at>
 */
public class DialogGenerateCode extends javax.swing.JDialog implements ActionListener, FocusListener {

    private FunctionConf fc = null;
    /**
     * Field with the generated g-Code:
     */
    public String g_code;
    private java.util.ArrayList<JTextField> jFormattedFields;
    int machine = 0;

    /**
     * Creates new form DialogBackTranslationFunction
     */
    public DialogGenerateCode(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

    }

    public DialogGenerateCode(String _g_code, FunctionConf _fc, java.awt.Frame parent, boolean modal) throws Exception {
        super(parent, modal);
        initComponents();
        this.g_code = _g_code;

        java.util.ArrayList<String> values = new java.util.ArrayList<>();

        jButtonCancel.setActionCommand("cancel");
        jButtonCancel.addActionListener(this);

        jButtonOk.setActionCommand("ok");
        jButtonOk.addActionListener(this);

        descriptionArea.setContentType("text/html");
        descriptionArea.setEditable(false);
        HTMLEditorKit kit = new HTMLEditorKit();
        descriptionArea.setEditorKit(kit);
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body {color:#0000ff; font-family:times; margin: 0px; font: 10px; }");
        styleSheet.addRule("pre {font-family: monospace; color : black; background-color : #f0f0f0; }");
        styleSheet.addRule("td, th {padding: 1px; border: 1px solid #ddd; }");
        Document doc = kit.createDefaultDocument();
        descriptionArea.setDocument(doc);

        jFormattedFields = new java.util.ArrayList<>();

        ContourPlot panelContour = new ContourPlot();
        this.tabOutput.addTab("Plot", panelContour);

        JTextArea panelGCode = new JTextArea();
        JScrollPane areaGCodeScrollPane = new JScrollPane(panelGCode);
        areaGCodeScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        this.tabOutput.addTab("G-Code", areaGCodeScrollPane);

        //gr.read(is);
        fc = _fc;
        if (this.fc == null) {
            throw new Exception("no Config");

        }

        this.setTitle(fc.title + " " + fc.name);

        for (int i = 0; i < fc.arg.size(); i++) {

            values.add(fc.arg.get(i).defaultval);

        }

        for (int i = 0; i < fc.arg.size(); i++) {

            JTextField f = new JTextField();
            f.setText(values.get(i));
            jFormattedFields.add(f);
        }

        for (int i = 0; i < fc.arg.size(); i++) {
            jFormattedFields.get(i).addFocusListener(this);
            jFormattedFields.get(i).addActionListener(this);
            jFormattedFields.get(i).setPreferredSize(new Dimension(80, 16));
            jFormattedFields.get(i).setMinimumSize(new Dimension(60, 16));
        }

        for (int i = 0; i < fc.arg.size(); i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = i;

            listPane.add(new JLabel(fc.arg.get(i).name), c);
            c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.gridy = i;
            c.insets = new Insets(0, 5, 0, 5);  // padding
            listPane.add(jFormattedFields.get(i), c);

            String desc = fc.arg.get(i).desc;
            if (desc.length() > 35) {
                desc = desc.substring(0, 35);
            }
            int breakpos = desc.indexOf('\n');
            if (breakpos > 0) {
                desc = desc.substring(0, breakpos);
            }

            if (fc.arg.get(i).desc.length() > desc.length()) {
                desc += "...";
            }
            c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 2;
            c.gridy = i;
            listPane.add(new JLabel(desc), c);
        }

        pack();
    }

    /**
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if ("ok".equals(e.getActionCommand())) {
            java.util.ArrayList<String> args = new java.util.ArrayList<>();
            String txtGcode = "";

            for (int i = 0; i < fc.arg.size(); i++) {
                args.add(i, jFormattedFields.get(i).getText().trim());

            }

            PolyCirculinearCurve2D origCurve;
            PolyCirculinearCurve2D newCurve;

            try {
                gcodereader gr = new gcodereader();

                InputStream is = new ByteArrayInputStream(this.g_code.getBytes());
                LinkedList<contourelement> origPath = gr.read(is);
                origCurve = this.cleanup_contour(origPath);

                if (fc.name.compareTo("roughing") == 0) {
                    Roughing r = new Roughing(origCurve, fc, args);
                    txtGcode = r.calculate();

                }else if(fc.name.compareTo("mirror") == 0) {
                    Mirror m = new Mirror(origCurve, fc, args);
                    txtGcode = m.calculate();

                }else if(fc.name.compareTo("reverse") == 0) {
                    Reverse m = new Reverse(origCurve, fc, args);
                    txtGcode = m.calculate();
                }else if(fc.name.compareTo("parallel") == 0) {
                    Parallel m = new Parallel(origCurve, fc, args);
                    txtGcode = m.calculate();
                
                }else if(fc.name.compareTo("translate") == 0) {
                    Translate m = new Translate(origCurve, fc, args);
                    txtGcode = m.calculate();
                }else{
                    JOptionPane.showMessageDialog(null, "Error: no valid generator" );
                    return;
                }

                JTextArea panelGCode = (JTextArea) (((JViewport) (((JScrollPane) this.tabOutput.getComponentAt(1)).getViewport()))).getView();
                panelGCode.setText(txtGcode);

                InputStream is_new = new ByteArrayInputStream(txtGcode.getBytes());
                LinkedList<contourelement> newPath = gr.read(is_new);

                newCurve = this.cleanup_contour(newPath);

                ContourPlot panelContour = (ContourPlot) this.tabOutput.getComponentAt(0);
                panelContour.origCurve = origCurve;
                panelContour.newCurve = newCurve;
                panelContour.repaint();

            } catch (Exception e1) {
                System.out.println("Error " + e1.toString());
                JOptionPane.showMessageDialog(null, "Error: " + e1.toString());
                return;

            }

        } else if ("cancel".equals(e.getActionCommand())) {
            this.setVisible(false);
        } else {

        }

    }

    @Override
    public void focusGained(FocusEvent e) {
        System.out.println("focusGained");
        JTextField source = (JTextField) e.getSource();
        for (int i = 0; i < fc.arg.size(); i++) {
            if (source == jFormattedFields.get(i)) {
                System.out.println("found: " + i);
                descriptionArea.setText(fc.arg.get(i).desc);
                return;
            }

        }

    }

    @Override
    public void focusLost(FocusEvent e) {
        //System.out.println("focusLost");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButtonOk = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        listPane = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        descriptionArea = new javax.swing.JEditorPane();
        tabOutput = new javax.swing.JTabbedPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        org.openide.awt.Mnemonics.setLocalizedText(jButtonOk, org.openide.util.NbBundle.getMessage(DialogGenerateCode.class, "DialogGenerateCode.jButtonOk.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jButtonCancel, org.openide.util.NbBundle.getMessage(DialogGenerateCode.class, "DialogGenerateCode.jButtonCancel.text")); // NOI18N
        jButtonCancel.setActionCommand(org.openide.util.NbBundle.getMessage(DialogGenerateCode.class, "DialogGenerateCode.jButtonCancel.actionCommand")); // NOI18N

        listPane.setLayout(new java.awt.GridBagLayout());

        jScrollPane1.setViewportView(descriptionArea);

        tabOutput.setName("tabOutput"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tabOutput)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCancel))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(listPane, javax.swing.GroupLayout.PREFERRED_SIZE, 567, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 685, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(listPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabOutput, javax.swing.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonOk))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DialogGenerateCode.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DialogGenerateCode.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DialogGenerateCode.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DialogGenerateCode.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                DialogGenerateCode dialog = new DialogGenerateCode(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    /**
     * transform and cleanup the contour
     *
     * @param contour
     * @return
     */
    private PolyCirculinearCurve2D cleanup_contour(LinkedList<contourelement> contour) {
        PolyCirculinearCurve2D elements = new PolyCirculinearCurve2D();
        for (contourelement current_ce : contour) {

            if (current_ce.curve == null) {
                continue;
            }
            if (current_ce.curve.length() == 0) {
                continue;
            }
            elements.add(current_ce.curve);

            if (current_ce.transition_curve != null) {
                elements.add(current_ce.transition_curve);
            }
        }
        return elements;

    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JEditorPane descriptionArea;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOk;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel listPane;
    private javax.swing.JTabbedPane tabOutput;
    // End of variables declaration//GEN-END:variables
}
