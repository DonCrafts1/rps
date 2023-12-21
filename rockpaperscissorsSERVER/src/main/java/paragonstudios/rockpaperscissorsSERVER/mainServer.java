/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package paragonstudios.rockpaperscissorsSERVER;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 *
 * @author user
 */
public class mainServer {
    public static ServerSocket ss;
    public static String ip = "localhost";
    public static DatabaseConnection db;
    public static Connection conn;
    public ExecutorService es = Executors.newFixedThreadPool(30);
    public rpsServer rpsServer = new rpsServer();
    public HashMap serverConnections = new HashMap();
    public int count = 0;
    
    //private static boolean stop = false;
    
    public mainServer(){
        try{
            ss = new ServerSocket(26262);
            //, 8, InetAddress.getByName(ip)
            db = new DatabaseConnection();
            conn = db.getConnection();
            
//            es.execute(() -> {
//                while (true){
//                    try {
//                        Thread.sleep(5000);
//                        System.out.println("------------- mS serverConnections... ");
//                            serverConnections.forEach((key, value) -> {
//                                System.out.println("Key: "+key+". ServerConn: "+value.toString());
//                            });
//
//                    } catch (InterruptedException ex) {
//                        System.out.println("mainServer serverconnections read went wrong");
//                    }
//                }
//            });
                
        } catch (IOException e){
            System.out.println("IOException from server() constructor");
        }
    }
    
    public void startAccepting(){
        while (true){
            try {
                Socket s = ss.accept();
                System.out.println("Player opened app");
                count++;
                serverConn sc = new serverConn(count, s);
                serverConnections.put(count, sc);
                es.execute(sc);
            } catch (IOException ex) {
                Logger.getLogger(mainServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public class serverConn implements Runnable{
        private String username;
        private Socket socket;
        private int number;
        private DataInputStream dis;
        private DataOutputStream dos;
        private boolean active = true;
        private boolean inMenu = true;
        
        public serverConn(int c, Socket s){
            number = c;
            active = true;
            socket = s;
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException | NullPointerException e ){
                Logger.getLogger(mainServer.class.getName()).log(Level.SEVERE, null, e);
            }
            System.out.println("New server connection, number: "+number);
        }
        
        public void executeUpdate(String update){
            try (Statement s = conn.createStatement()){
                System.out.println("Executing: "+update);
                s.executeUpdate(update);
            } catch (SQLException ex) {
                Logger.getLogger(mainServer.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
        
        @Override
        public void run(){
            while (active){
                try {
                    if (inMenu){
                        byte typeOfMessage = dis.readByte();
                        System.out.println(typeOfMessage);
                        switch (typeOfMessage){
                            case -2: //sending username (for people who arealready playing)
                                username = dis.readUTF();
                                System.out.println("Got username: "+username);
                                dos.writeByte(-2);
                                dos.writeInt(getWins());
                                dos.flush();
                                break;
                            case -1: //disconnect
                                active = false;
                                socket.close();
                                serverConnections.remove(number);
                                break;
                            case 0: //create account
                                String user = dis.readUTF();
                                String pass = dis.readUTF();
                                boolean exists = checkIfUserExists(user);
                                System.out.println("Exists: "+exists);
                                if (!exists){
                                    executeUpdate("INSERT into UserAccounts (username, password, totalWins) VALUES ('"+user+"','"+pass+"', 0)");
                                }
                                dos.writeByte(0);
                                dos.writeBoolean(!exists); //if exist, don't allow. if not exist, allow
                                dos.flush();
                                break;
                            case 1: //login to account
                                username = dis.readUTF();
                                pass = dis.readUTF();
                                boolean valid = checkIfUserPassValid(username,pass);
                                System.out.println("Valid: "+valid);
                                dos.writeByte(1);
                                dos.writeBoolean(valid);
                                if (valid) dos.writeInt(getWins());
                                dos.flush();
                                break;
                            case 50:
                                rpsServer.playerJoin(socket, dis, dos);
                                //socket.shutdownInput();
                                inMenu = false;
                                break;

                            default:
                                break;
                        }
                    }
                    Thread.sleep(50);
                } catch (IOException | InterruptedException e){
                    System.out.println("EOFException from serverConnection "+number);
                    active = false;
                    serverConnections.remove(number);
                }
            }
        }
        public int getWins(){
            int wins = 0;
            String query = "select totalWins from UserAccounts where username = '"+username+"'";
            try (ResultSet r = conn.createStatement().executeQuery(query)){
                if (r.next()){
                    wins = r.getInt(1);
                }
            } catch (SQLException e){
                e.printStackTrace();
            }
            return wins;
        }
        
        public boolean checkIfUserExists(String user){
            boolean result = false;
            System.out.println(result);
            String query = "select username from UserAccounts where username = '"+user+"'";
            
            try (Statement s = conn.createStatement()){
                ResultSet r = s.executeQuery(query);
                r.next();
                String get = r.getString(1);
                System.out.println("Database name: "+get);
                if (get.equals(user))
                    result = true; //return true if not null
            } catch (SQLException ex) {
                Logger.getLogger(mainServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println(result);
            return result;
        }
        
        
        public boolean checkIfUserPassValid(String user, String pass){
            boolean result = false;
            String query = "select username from UserAccounts where username = '"+user+"' and password = '"+pass+"'";
            try (ResultSet r = conn.createStatement().executeQuery(query)){
                if (r.next()){
                    String get = r.getString(1);
                    if (get.equals(user)) result = true; //return true if not null
                }                
            } catch (SQLException ex) {
                Logger.getLogger(mainServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            return result;
        }
    }
}
