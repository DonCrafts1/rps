/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package paragonstudios.rockpaperscissorsSERVER;

import java.util.logging.Level;
import java.util.logging.Logger;
import paragonstudios.rockpaperscissorsSERVER.mainServer.serverConn;

/**
 *
 * @author user
 */
public class Launcher {
    public static mainServer mS;
    public static void main(String[] args){
        mS = new mainServer();
        mS.startAccepting();
        
    }
}
