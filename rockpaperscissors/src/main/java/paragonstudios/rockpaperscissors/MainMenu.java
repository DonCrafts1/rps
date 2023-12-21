/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package paragonstudios.rockpaperscissors;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
/**
 *
 * @author user
 */
public class MainMenu extends javax.swing.JFrame {
    private LoginPage loginPage;
    private Image logoImg;
    private Image adjustedLogo;
    public static Socket s;
    public static String username = "";
    public clientConn cc;
    private Thread t;
    private connRestartThread r = new connRestartThread();;
    //, tryConnect;
    public byte usernameSentState = 0;
    public int wins = 0;
    
    private class connRestartThread extends Thread{
        @Override
        public void run(){ 
            try {
                
                cc = new clientConn();
                loginAccountButton.setEnabled(false);
                connectingLabel.setVisible(true);
                if (loginPage != null) loginPage.setConnectingLabel(true);
                Thread.sleep(300);
                if (!cc.getActive()){
                    while (!cc.getActive()){
                        Thread.sleep(1500);
                        cc = new clientConn();
                    }
                }
                connectingLabel.setVisible(false);
                loginAccountButton.setEnabled(true);
                if (loginPage != null) loginPage.setConnectingLabel(false);
            } catch (InterruptedException err){}
        }
    }

    public volatile boolean inMenu = true;
    
    public MainMenu() {
        loadImages();
        initComponents();
        getContentPane().setBackground(Color.WHITE);
        playButton.setEnabled(false);
        winsLabel.setHorizontalAlignment(JLabel.RIGHT);
        r.start();
        //162, 50
    }
    
    
    public class clientConn implements Runnable{
        private String ip = "REPLACE WITH YOUR OWN SERVER IP";
        private int port = 26262;
        private Socket socket;
        private DataOutputStream dos;
        private DataInputStream dis;
        private boolean active = true;
        
        public clientConn(){
            try{
                socket = new Socket("localhost",port);
                //ip, 
                dos = new DataOutputStream(socket.getOutputStream());
                dis = new DataInputStream(socket.getInputStream());
                System.out.println(socket.toString());
                System.out.println(dos.toString());
                System.out.println(dis.toString());
                t = new Thread(this);
                t.start();
            } catch (IOException e){
                System.out.println("Error in clientConn() constructor");
                active = false;
            }
            
        }
        
        @Override
        public void run(){
            while (active){
                try {
                    if (inMenu){
                        if (usernameSentState == 1){
                            dos.writeByte(-2);
                            dos.writeUTF(username);
                            dos.flush();
                            usernameSentState = 2;                        
                        }
                        byte typeOfMessage = dis.readByte();
                        System.out.println(typeOfMessage);
                        switch (typeOfMessage){
                            case -2:
                                wins = dis.readInt();
                                winsLabel.setText("Wins: "+wins);
                                break; //bruh don't forget breaks... this single thing wasted one hour of my time since it closed socket
                            case -1:
                                socket.close();
                                active = false;
                                break;
                            case 0:
                                boolean allow = dis.readBoolean();
                                System.out.println("Allow: "+allow);
                                if (allow){
                                    loginPage.setVisible(false);
                                    usernameTextField.setText(loginPage.u);
                                    username = loginPage.u;
                                    RockPaperScissors.m.setVisible(true);
                                    loginPage.dispose();
                                    playButton.setEnabled(true);
                                    winsLabel.setText("Wins: 0");
                                } else {
                                    System.out.println("check");
                                    loginPage.errorMessage();
                                }
                                break;
                            case 1:
                                boolean valid = dis.readBoolean();
                                System.out.println("Allow: "+valid);
                                if (valid){
                                    wins = dis.readInt();
                                    loginPage.setVisible(false);
                                    usernameTextField.setText(loginPage.u);
                                    username = loginPage.u;
                                    RockPaperScissors.m.setVisible(true);
                                    loginPage.dispose();
                                    playButton.setEnabled(true);
                                    winsLabel.setText("Wins: "+wins);
                                } else {
                                    System.out.println("check");
                                    loginPage.errorMessage();
                                }
                                break;
                            case 50:
                                break;
                            default:
                                break;
                        }
                        
                    }
                } catch (IOException e){

                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException er){}
            }
        }
        
        
        public void restartConnAfterGame(){
            try{
                socket = new Socket("localhost",port);
                dos = new DataOutputStream(socket.getOutputStream());
                dis = new DataInputStream(socket.getInputStream());
                System.out.println(socket.toString());
                System.out.println(dos.toString());
                System.out.println(dis.toString());
                RockPaperScissors.m.usernameSentState = 1;
                System.out.println("RESTARTED (after game end)");
            } catch (IOException e){
                System.out.println("Error in restartConnAfterGame()");
                active = false;
                if (!r.isAlive()){
                    r = new connRestartThread();
                    r.start();
                }
            }
        }
        
        public void createAccount(String user, String pass){
            try {
                System.out.println("Create acc");
                dos.writeByte(0);
                dos.writeUTF(user);
                dos.writeUTF(pass);
                System.out.println(user+" and "+pass);
                dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(MainMenu.class.getName()).log(Level.SEVERE, null, ex);
                active = false;
                if (!r.isAlive()){
                    r = new connRestartThread();
                    r.start();
                }
            }
        }
        
        public void loginAccount(String user, String pass){
            try {
                System.out.println("Login acc");
                dos.writeByte(1);
                dos.writeUTF(user);
                dos.writeUTF(pass);
                dos.flush();
            } catch (IOException ex) {
                Logger.getLogger(MainMenu.class.getName()).log(Level.SEVERE, null, ex);
                active = false;
                if (!r.isAlive()){
                    r = new connRestartThread();
                    r.start();
                }
            }
        }
        
        public void play(int mode){
            try {
                System.out.println(socket.toString());
                dos.writeByte(50);
                dos.flush();
                inMenu = false;
                //socket.shutdownOutput();
                System.out.println("play() run");
            } catch (IOException ex) {
                Logger.getLogger(MainMenu.class.getName()).log(Level.SEVERE, null, ex);
                active = false;
                r = new connRestartThread();
                r.start();
            }
        }
        
        public Socket getSocket(){
            return socket;
        }
        
        public DataInputStream getDIS(){
            return dis;
        }
        
        public DataOutputStream getDOS(){
            return dos;
        }
        
        public boolean getActive(){
            return active;
        }
    }
    
    public void loadImages(){
        try {
            logoImg = ImageIO.read(getClass().getResourceAsStream("/rpslogo.png"));
        } catch (IOException e){
            System.out.println("IOException in loadImages");
        }
    }

    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        logo = new javax.swing.JLabel();
        usernameTextField = new javax.swing.JTextField();
        playButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        playIPButton = new javax.swing.JButton();
        loginAccountButton = new javax.swing.JButton();
        winsLabel = new javax.swing.JLabel();
        connectingLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(225, 225, 225));
        setMaximumSize(new java.awt.Dimension(500, 500));

        logo.setText("n");
        adjustedLogo = logoImg.getScaledInstance(486, 150, java.awt.Image.SCALE_SMOOTH);
        logo.setIcon(new ImageIcon(adjustedLogo));
        logo.setText("");

        usernameTextField.setEditable(false);
        usernameTextField.setFont(new java.awt.Font("Verdana", 0, 14)); // NOI18N
        usernameTextField.setText("No username...");
        usernameTextField.setActionCommand("<Not Set>");
        usernameTextField.setDragEnabled(true);
        usernameTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                usernameTextFieldActionPerformed(evt);
            }
        });

        playButton.setFont(new java.awt.Font("Verdana", 1, 18)); // NOI18N
        playButton.setText("PLAY");
        playButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playButtonActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Verdana", 0, 14)); // NOI18N
        jLabel1.setText("By the epicc Paragon");

        playIPButton.setFont(new java.awt.Font("Verdana", 1, 18)); // NOI18N
        playIPButton.setText("PLAY using IP");
        playIPButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playIPButtonActionPerformed(evt);
            }
        });

        loginAccountButton.setFont(new java.awt.Font("Verdana", 1, 12)); // NOI18N
        loginAccountButton.setText("Login To Account");
        loginAccountButton.setEnabled(false);
        loginAccountButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginAccountButtonActionPerformed(evt);
            }
        });

        winsLabel.setFont(new java.awt.Font("Verdana", 1, 13)); // NOI18N

        connectingLabel.setText("Connecting...");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(logo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(40, 40, 40)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(usernameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(playButton, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(33, 33, 33)
                                        .addComponent(playIPButton, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(winsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(52, 52, 52)
                                .addComponent(loginAccountButton))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(175, 175, 175)
                                .addComponent(jLabel1)))
                        .addGap(0, 37, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(209, 209, 209)
                .addComponent(connectingLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(logo, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(33, 33, 33)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(connectingLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 47, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(usernameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(loginAccountButton, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(winsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(63, 63, 63)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(playIPButton, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(playButton, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(37, 37, 37))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void playIPButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playIPButtonActionPerformed
        this.setVisible(false);
        RPSip o = new RPSip();
    }//GEN-LAST:event_playIPButtonActionPerformed

    private void playButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playButtonActionPerformed
        this.setVisible(false);
        cc.play(0);
        
        RPS rps = new RPS();
    }//GEN-LAST:event_playButtonActionPerformed

    private void usernameTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_usernameTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_usernameTextFieldActionPerformed

    private void loginAccountButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginAccountButtonActionPerformed
        //this.setVisible(false);
        loginPage = new LoginPage();
        loginPage.setVisible(true);
        loginPage.setLocationRelativeTo(null);
        this.setVisible(false);
// TODO add your handling code here:
    }//GEN-LAST:event_loginAccountButtonActionPerformed

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
            java.util.logging.Logger.getLogger(MainMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                MainMenu m = new MainMenu();
                m.setSize(500,500);
                m.setLocationRelativeTo(null);
                m.setVisible(true);
                m.setResizable(false);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel connectingLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton loginAccountButton;
    private javax.swing.JLabel logo;
    private javax.swing.JButton playButton;
    private javax.swing.JButton playIPButton;
    private javax.swing.JTextField usernameTextField;
    private javax.swing.JLabel winsLabel;
    // End of variables declaration//GEN-END:variables
}
