/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package HuellaDigital;

import com.digitalpersona.onetouch.*;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.event.*;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.verification.DPFPVerification;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.GroupLayout;

/**
 *
 * @author Sergio Reynaga
 */
public class CapturaHuella extends javax.swing.JFrame {

    //Nos sirve para identificar al dispositivo
    private DPFPCapture Lector = DPFPGlobal.getCaptureFactory().createCapture();
    //Nos sirve para leer a modo de enrrolar, y crear una plantilla nueva, a base de 4 huellas.
    private DPFPEnrollment Reclutador = DPFPGlobal.getEnrollmentFactory().createEnrollment();
    //Nos sirve para leer a modo de verificar o comparar, a base de una plantilla creada anteriormente
    private DPFPVerification Verificador = DPFPGlobal.getVerificationFactory().createVerification();
    //La plantilla, nueva o rescatada
    private DPFPTemplate template;
    //A modo de CONSTANTE para crear plantillas
    public String TEMPLATE_PROPERTY = "template";
    //Para leer la huella, y definirla como un enrrolamiento
    public DPFPFeatureSet featureSetInscripcion;
    //Para leer la huella, y definirla como una verificación
    public DPFPFeatureSet featureSetVerificacion;

    /**
     * Creates new form CapturaHuella
     */
    public CapturaHuella() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "No es posible cambiar tema visual", "LookAndFeel inválido", JOptionPane.ERROR_MESSAGE);
        }

        initComponents();
        Iniciar();
        start();
        EstadoHuellas();
        btnGuardar.setEnabled(false);
        btnIdentificar.setEnabled(false);
        btnVerificar.setEnabled(false);
        btnSalir.grabFocus();
        txtMensaje.setEditable(false);
    }

    protected void Iniciar() {
        Lector.addDataListener(new DPFPDataAdapter() {
            @Override
            public void dataAcquired(final DPFPDataEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EnviarTexto("La huella ha sido capturada");
                        ProcesarCaptura(e.getSample());
                    }
                });
            }
        });
        Lector.addReaderStatusListener(new DPFPReaderStatusAdapter() {
            @Override
            public void readerConnected(final DPFPReaderStatusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EnviarTexto("El sensor de huella dactilar se encuentra Activado");
                    }
                });
            }

            @Override
            public void readerDisconnected(final DPFPReaderStatusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EnviarTexto("El sensor de huella dactilar se encuentra Desactivado");
                    }
                });
            }
        });
        Lector.addErrorListener(new DPFPErrorAdapter() {
            public void errorReader(final DPFPErrorEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EnviarTexto("Error: " + e.getError());
                    }
                });
            }
        });
    }

    public void EnviarTexto(String mensaje) {
        txtMensaje.append(mensaje + "\n");
    }

    public void start() {
        Lector.startCapture();
        EnviarTexto("Utilizando lector de huella dactilar");
    }

    public void stop() {
        Lector.stopCapture();
        EnviarTexto("Lector detenido");
    }

    public void EstadoHuellas() {
        EnviarTexto("Muestra de huellas necesarias para guardar plantilla: " + Reclutador.getFeaturesNeeded());
    }

    public void ProcesarCaptura(DPFPSample muestra) {
        featureSetInscripcion = extraerCaracteristicas(muestra, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
        featureSetVerificacion = extraerCaracteristicas(muestra, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
        if (featureSetInscripcion != null) {
            try {
                System.out.println("Las características de la huella han sido creadas");
                Reclutador.addFeatures(featureSetInscripcion);
                DibujarHuella(CrearImagenHuella(muestra));
                btnVerificar.setEnabled(true);
                btnIdentificar.setEnabled(true);
            } catch (DPFPImageQualityException e) {
                System.err.println("Error: " + e.getMessage());
            } finally {
                EstadoHuellas();
                switch (Reclutador.getTemplateStatus()) {
                    case TEMPLATE_STATUS_READY:
                        stop();
                        setTemplate(Reclutador.getTemplate());
                        EnviarTexto("La plantilla de huella ha sido creada. Puede Verificar o Identificarla");
                        btnIdentificar.setEnabled(false);
                        btnVerificar.setEnabled(false);
                        btnGuardar.setEnabled(true);
                        btnGuardar.grabFocus();
                        break;
                    case TEMPLATE_STATUS_FAILED:
                        Reclutador.clear();
                        stop();
                        EstadoHuellas();
                        setTemplate(null);
                        JOptionPane.showMessageDialog(CapturaHuella.this, "La plantilla de la huella no pudo ser creada. Repita el proceso", "Inscripción de Huellas Dactilares", JOptionPane.ERROR_MESSAGE);
                        start();
                        break;
                }
            }
        }
    }

    public DPFPFeatureSet extraerCaracteristicas(DPFPSample muestra, DPFPDataPurpose motivo) {
        DPFPFeatureExtraction extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
        try {
            return extractor.createFeatureSet(muestra, motivo);
        } catch (DPFPImageQualityException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public Image CrearImagenHuella(DPFPSample muestra) {
        return DPFPGlobal.getSampleConversionFactory().createImage(muestra);
    }

    public void DibujarHuella(Image image) {
        lblHuella.setIcon(
                new ImageIcon(
                        image.getScaledInstance(
                                lblHuella.getWidth(),
                                lblHuella.getHeight(),
                                Image.SCALE_DEFAULT
                        )
                )
        );
    }

    public void setTemplate(DPFPTemplate template) {
        DPFPTemplate antigua = this.template;
        this.template = template;
        firePropertyChange(TEMPLATE_PROPERTY, antigua, template);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        PanHuella = new javax.swing.JPanel();
        lblHuella = new javax.swing.JLabel();
        PanBtns = new javax.swing.JPanel();
        btnVerificar = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnIdentificar = new javax.swing.JButton();
        btnSalir = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtMensaje = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        PanHuella.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), "Huellas Digitales", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        javax.swing.GroupLayout PanHuellaLayout = new javax.swing.GroupLayout(PanHuella);
        PanHuella.setLayout(PanHuellaLayout);
        PanHuellaLayout.setHorizontalGroup(
            PanHuellaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanHuellaLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblHuella, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(101, 101, 101))
        );
        PanHuellaLayout.setVerticalGroup(
            PanHuellaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanHuellaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblHuella, javax.swing.GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE)
                .addContainerGap())
        );

        PanBtns.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), "Acciones", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        btnVerificar.setText("Verificar");

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnIdentificar.setText("Identificar");

        btnSalir.setText("Salir");
        btnSalir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalirActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout PanBtnsLayout = new javax.swing.GroupLayout(PanBtns);
        PanBtns.setLayout(PanBtnsLayout);
        PanBtnsLayout.setHorizontalGroup(
            PanBtnsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanBtnsLayout.createSequentialGroup()
                .addGap(41, 41, 41)
                .addGroup(PanBtnsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnVerificar, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnIdentificar, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(50, 50, 50)
                .addGroup(PanBtnsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSalir, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(52, Short.MAX_VALUE))
        );
        PanBtnsLayout.setVerticalGroup(
            PanBtnsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanBtnsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PanBtnsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnVerificar)
                    .addComponent(btnGuardar))
                .addGap(18, 18, 18)
                .addGroup(PanBtnsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnIdentificar)
                    .addComponent(btnSalir))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        txtMensaje.setColumns(20);
        txtMensaje.setRows(5);
        jScrollPane1.setViewportView(txtMensaje);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(PanHuella, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(PanBtns, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(PanHuella, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(PanBtns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        JOptionPane.showMessageDialog(null, "Boton Guardar");
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnSalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalirActionPerformed
        System.exit(0);
    }//GEN-LAST:event_btnSalirActionPerformed

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
            java.util.logging.Logger.getLogger(CapturaHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CapturaHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CapturaHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CapturaHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CapturaHuella().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel PanBtns;
    private javax.swing.JPanel PanHuella;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnIdentificar;
    private javax.swing.JButton btnSalir;
    private javax.swing.JButton btnVerificar;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblHuella;
    private javax.swing.JTextArea txtMensaje;
    // End of variables declaration//GEN-END:variables
}
