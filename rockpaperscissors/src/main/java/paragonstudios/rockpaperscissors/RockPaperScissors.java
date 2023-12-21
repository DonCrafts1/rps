/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package paragonstudios.rockpaperscissors;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author user
 */
public class RockPaperScissors {
    //THIS IS THE LAUNCHER CLASS.
    public static MainMenu m;
    public static void main(String args[]){
        m = new MainMenu();
        m.setTitle("Rock Paper Scissors - THE GAME");
        m.setSize(500,500);
        m.setLocationRelativeTo(null);
        m.setVisible(true);
        m.setResizable(false);
//        while (true){
//            try {
//                Thread.sleep(5000);
//                System.out.println("inMenu "+m.inMenu);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(RockPaperScissors.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
    }
}
