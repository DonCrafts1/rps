/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package paragonstudios.rockpaperscissorsSERVER;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
/**
 *
 * @author user
 */
public class rpsServer {
    private volatile static ArrayList<rpsGameRoom> rpsGameRooms = new ArrayList<>();
    private static ArrayList<Integer> roomsToRemove = new ArrayList<>();
    private boolean isAdding = false;
    
    //private static boolean stop = false;
    
    public rpsServer(){
        
    }
    

    
    public void playerJoin(Socket s1, DataInputStream dis, DataOutputStream dos){
        isAdding = true;
        /// REMOVES INACTIVE rpsGameRooms
        for (int i = 0; i<rpsGameRooms.size(); i++){
            if (rpsGameRooms.get(i).gameRoomActive == false){
                roomsToRemove.add(i);
            }
        }
        Collections.reverse(roomsToRemove); //makes it so it removes the inactive indexes
        //WITHOUT IT: remove rpsGameRooms(1) -> 1 gets removed, remove rpsGameRooms(3) -> removes 4th element... NOT GOOD

        for (int i = 0; i<roomsToRemove.size(); i++){
            System.out.print(roomsToRemove.get(i)+" ");
            rpsGameRooms.remove((int)roomsToRemove.get(i));
        }
        roomsToRemove.clear();
        
        if (rpsGameRooms.isEmpty()){
            ///CREATES GAMEROOM WHEN NO ROOMS EXIST
            System.out.println("No game room. Creating new room and joining...");
            rpsGameRooms.add(new rpsGameRoom());
            rpsGameRooms.get(0).addPlayer(s1, dis, dos);
        } else {
            
            ///FINDS rpsGameRooms WITH A PLAYER IN IT
            for (int i = 0; i<rpsGameRooms.size(); i++){ 
                System.out.println("gameRoom "+(i+1)+": "+rpsGameRooms.get(i).toString()+". Active: "+rpsGameRooms.get(i).gameRoomActive);
                if (rpsGameRooms.get(i).numPlayers < 2 && rpsGameRooms.get(i).gameRoomActive ){
                    System.out.println("Room with 0/1 player found. Joining...");
                    rpsGameRooms.get(i).addPlayer(s1, dis, dos);
                    return;
                }
            }
                ///CREATES GAMEROOM
            System.out.println("No available game rooms. Creating new room and joining...");
            rpsGameRooms.add(new rpsGameRoom());
            rpsGameRooms.get(rpsGameRooms.size()-1).addPlayer(s1, dis, dos);
            /*
            if (s1 != null){
                for (rpsGameRoom g : rpsGameRooms) { //for-each loop - iterate using specified variable type in array
                    if (g.gameRoomActive == false){
                        System.out.println("Inactive game room found. Joining.");
                        g = null;
                        g = new rpsGameRoom();
                        System.out.println("Active: "+g.gameRoomActive);
                        g.addPlayer(s1);
                        s1 = null;
                        break;
                    }
                }
            */

            
        }
        isAdding = false;
    }
}
