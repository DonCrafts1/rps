/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package paragonstudios.rockpaperscissorsSERVER;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import javax.swing.Timer;

/**
 *
 * @author user
 */
public class rpsGameRoom implements Runnable{
    private ExecutorService es;
    //private Thread thread;
    public volatile byte numPlayers;
    private serverSideConnection p1;
    private serverSideConnection p2;
    private volatile String p1Name = "";
    private volatile String p2Name = "";
    
    private volatile boolean p1Offline = false;
    private volatile boolean p2Offline = false;
    
    private boolean gameStartedSent = false;
    private volatile boolean gameStarted = false;
    private volatile boolean roundEnded = false;
    private volatile boolean overtime = false;
    private volatile boolean gameEnd = false;
    public volatile boolean gameRoomActive;
    
    private volatile int roundNum = 1;
    private final int[] roundTimes = new int[]{15, 15, 15, 10, 10, 10, 8, 8, 8, 5};
    private volatile boolean roundTimerStarted = false;
    private int roundTimeRemaining = 15;
    
    private volatile byte result = 0;
    private byte p1RoundsWon = 0;
    private byte p2RoundsWon = 0;
    private byte gameResult = 0; //1, 2, 3 -> p1 win, p2 win, draw
    
    private volatile boolean p1Chose = false;
    private volatile boolean p2Chose = false;
    private volatile int p1Choice = 0;
    private volatile int p2Choice = 0;
    private volatile boolean chose = false;
    
    private Timer roundTimer = new Timer(1000, null);
    ActionListener al = new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e){ 
            roundTimeRemaining--;
            if (roundTimeRemaining < 0) {
                System.out.println("Ran out of time!");
                roundTimeRemaining = 0;
                if (!chose){
                    if (p1Choice == 0) p1Choice = 1;
                    if (p2Choice == 0) p2Choice = 1;
                    chose = true;
                }
                //roundTimer.setDelay(5000);
                roundEnded = true;
                roundTimer.stop();
            }
        }
    };
    
    ActionListener testAl = new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e){
            System.out.println("---------");
            System.out.println("roundNum: "+roundNum);
            System.out.println("p1Choice: "+p1Choice+". p2Choice: "+p2Choice);
            System.out.println("p1Chose: "+p1Chose+". p2Chose: "+p2Chose);
        }
    };
    private Timer testTimer = new Timer(1000, testAl);
   
    private volatile int p1FinalReply = 0;
    private volatile int p2FinalReply = 0;
    ActionListener endAl = new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e){
            try {
                System.out.println("Hmm...");
                if (p1Offline || p2Offline) shutdown();
                es.execute(()->{
                    try {
                        p1FinalReply = p1.dis.readByte();
                        if (p1FinalReply == -1) shutdown();
                        p1.socket.close();
                    } catch (IOException | NullPointerException | RejectedExecutionException err){
                        shutdown();
                    }});
                p2FinalReply = p2.dis.readByte();
                if (p2FinalReply == -1) shutdown();
                p2.socket.close();
            } catch (IOException | NullPointerException err ){
                shutdown();
            }
        }
    };
    private Timer timeOutTimer;
    
    private void shutdown(){
        System.out.println("I'm out! gameRoomActive = false");
        roundTimer.stop();
        
        gameRoomActive = false;
        timeOutTimer.stop();
        es.shutdown();
        
    }
    
    public rpsGameRoom(){
        System.out.println("Game room started");
        es = Executors.newFixedThreadPool(6);
        p1 = null;
        p2 = null;
        numPlayers = 0;
        gameStarted = false;
        roundEnded = false;
        overtime = false;
        gameEnd = false;
        gameRoomActive = true;
        roundNum = 1;
        result = 0;
        p1RoundsWon = 0;
        p2RoundsWon = 0;
        gameResult = 0;
        p1Choice = 0;
        p2Choice = 0;
        chose = false;
        timeOutTimer = new Timer(0, endAl);
        timeOutTimer.setInitialDelay(5000);
        timeOutTimer.setRepeats(false);
        es.execute(this);
        
        
    }
    
    public void addPlayer(Socket s, DataInputStream dis, DataOutputStream dos){
        System.out.println(numPlayers);
        numPlayers++;
        System.out.println(numPlayers);
        System.out.println("Player #" + numPlayers + "joined");
        serverSideConnection ssc = new serverSideConnection(s, dis, dos, numPlayers);
        if (numPlayers == 1) p1 = ssc;
        else if (numPlayers == 2) p2 = ssc;
        es.execute(ssc);
    }
    
    boolean updatedWins = false;
    @Override
    public void run(){
        while (gameRoomActive){
            if (!gameStarted){
                if (p1Offline && numPlayers == 1){
                    /*
                    p1Offline = false;
                    numPlayers = 0;
                    p1 = null;*/
                    gameRoomActive = false;
                    
                }
                if (numPlayers == 2 && p2 != null && !p2Name.equals("")){
                    p1.sendName();
                    p2.sendName();
                    try {Thread.sleep(2000);} catch (InterruptedException e){}
                    if (p1Offline){
                        p1Offline = false;
                        numPlayers--;
                        p1 = p2;
                        System.out.println("numplayers"+ numPlayers);
                        p1.playerID = 1;
                        p1.sendPlayerID();
                        p2 = null;
                        p1Name = p2Name;
                        p2Name = "";
                        p1.sendName();
                    } else if (p2Offline){
                        p2Offline = false;
                        numPlayers--;
                        p2 = null;
                        p2Name = "";
                        p1.sendName();
                    } else gameStarted = true;
                }
            }
            if (!gameStartedSent && gameStarted){
                sendStart();
                //testTimer.start();
                gameStartedSent = true;
            }
            if (gameStarted && gameStartedSent){
                if (!gameEnd){
                    if (p1Offline || p2Offline){
                        roundEnded = true;
                        if (p1Offline) gameResult = 2;
                        else if (p2Offline) gameResult = 1;
                        //finalRoundCheckWin();
                        gameEnd = true;
                        chose = true; //without chose = true, does not send
                        sendRoundResultToPlayers();
                        //updateplayerwins is dealt with at the else statement
                    }
                    if (!roundEnded){
                        if (!roundTimerStarted){
                            roundTimer.addActionListener(al);
                            roundTimeRemaining = roundTimes[roundNum-1];
                            roundTimer.start();
                            
                            roundTimerStarted = true;
                        }
                        if (p1Choice != 0 && p2Choice != 0){
                            System.out.println("Valid, can check");
                            roundTimeRemaining = 9999;
                            chose = true;
                            roundEnded = true;
                            
                        }
                    } else {
                        roundTimer.stop();
                        if (chose) {
                            checkForWin();
                            chose = false;
                        }
                        try {Thread.sleep(5000);} catch (InterruptedException ex) {}
                    }
                } else {
                    updatePlayerWins();
                    timeOutTimer.start();
                }
            }
            try {Thread.sleep(50);} catch (InterruptedException e){}
        }
        
    }
    
    private void updatePlayerWins(){
        if (!updatedWins){
            try (Statement s = Launcher.mS.conn.createStatement()){
                String up = "";
                switch (gameResult){
                    case 1:
                        up = "UPDATE UserAccounts SET totalWins = totalWins + 1 where username = '"+p1Name+"'";
                        System.out.println("Just to be clear... result is "+gameResult+" and added win to "+p1Name);
                        break;
                    case 2:
                        up = "UPDATE UserAccounts SET totalWins = totalWins + 1 where username = '"+p2Name+"'";
                        System.out.println("Just to be clear... result is "+gameResult+" and added win to "+p2Name);
                        break;
                    default:
                        break;
                }
                s.executeUpdate(up);

            } catch (SQLException e){
                //e.printStackTrace();
                System.out.println("Something went wrong in adding to player total wins");
            }
            updatedWins = true;
        }
    }
    
    private void sendStart(){
        try {
            p1.dos.writeByte(0);
            p1.dos.writeBoolean(true);
            p2.dos.writeByte(0);
            p2.dos.writeBoolean(true);
            p1.dos.flush();
            p2.dos.flush();
        } catch (IOException e){
            System.out.println("IOExc. in sendStart()");
        }
    }
    
    private void checkForWin(){
        methodInCheckWin();
        if (!overtime){
            if (p1RoundsWon >= 3 || p2RoundsWon >= 3){
                if (p1RoundsWon > p2RoundsWon) gameResult = 1;
                else gameResult = 2;
                gameEnd = true;
                sendRoundResultToPlayers();
                return;
            }
            if (roundNum >= 9){
                overtime = true;
            }
            sendRoundResultToPlayers();
            nextRound();
        } else {
            finalRoundCheckWin();
        }
    }
    
    private void finalRoundCheckWin(){
        if (p1RoundsWon > p2RoundsWon) gameResult = 1;
        else if(p2RoundsWon > p1RoundsWon) gameResult = 2;
        else gameResult = 3;
        gameEnd = true;
        chose = true; //without chose = true, does not send
        sendRoundResultToPlayers();
    }
    
    private void sendRoundResultToPlayers(){
        System.out.println("sendroundresulttoplayers run");
        if (!p1Offline) p1.sendRoundResult(p2Choice, result);
        
        if (result == 1) result = 2;
        else if (result == 2) result = 1;
        
        if (!p2Offline) p2.sendRoundResult(p1Choice, result);
    }
    
    private void methodInCheckWin(){
        if ((p1Choice == 2 && p2Choice == 1) || (p1Choice == 3 && p2Choice == 2) || (p1Choice == 1 && p2Choice == 3)){
            result = 1;
            p1RoundsWon++;
        } else if (p1Choice == p2Choice){ 
            result = 3;
        } else {
            result = 2;
            p2RoundsWon++;
        }
    }
    
    private void nextRound(){
        //starts next round
        ActionListener timerActionListener = new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent event){
                chose = false;
                p1Choice = 0;
                p2Choice = 0;
                
                roundNum++;
                roundTimeRemaining = roundTimes[roundNum-1];
                roundEnded = false;
                if (gameRoomActive){
                    try {
                        p1.dos.writeByte(2);
                        p1.dos.writeBoolean(roundEnded);
                        p2.dos.writeByte(2);
                        p2.dos.writeBoolean(roundEnded);
                        
                        p1.dos.flush();
                        p2.dos.flush();
                    } catch (IOException e) {
                        System.out.println("Weird in nextRound()");
                    }
                    roundTimer.start();
                    System.out.println("Round start");
                }
                //syncRoundStart();
            }
        };
        Timer timer = new Timer(0, timerActionListener);
        timer.setInitialDelay(5000);
        timer.setRepeats(false);
        timer.start();
    }
    
    
    private class serverSideConnection implements Runnable{
        private Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;
        private int playerID;
        private int typeOfMessage;
        
        public serverSideConnection(Socket s, DataInputStream inputDIS, DataOutputStream inputDOS, int id){
            socket = s;
            playerID = id;
            gameStartedSent = false;
            dis = inputDIS;
            dos = inputDOS;
//            try {
//                dis = new DataInputStream(socket.getInputStream());
//                dos = new DataOutputStream(socket.getOutputStream());
//            } catch (IOException e){
//                System.out.println("IOException from sSC constructor");
//            }
        }
        
        @Override
        public void run(){
            try {
                //Thread.sleep(1000);
                System.out.println("START RUN FOR player "+playerID);
                dos.writeByte(50); //simply to get the readInt() in MainMenu to stop interfering
                dos.writeByte(-2);
                dos.writeByte(playerID);
                //System.out.println(dos.size());
                dos.flush();
                
                while (gameRoomActive){
                    typeOfMessage = dis.readByte();
                    System.out.println("Type of message: "+typeOfMessage);
                    switch (typeOfMessage){
                        case -2:
                            if (playerID == 1 && p1Name.equals("")) p1Name = dis.readUTF();
                            else if (playerID == 2 && p2Name.equals("")) p2Name = dis.readUTF();
                            break;
                        case -1:
                            if (playerID == 1) p1Offline = true;
                            else p2Offline = true;
                            System.out.println("p1Offline: "+p1Offline+". p2Offline: "+p2Offline);
                            socket.close();
                            break;
                        case 1:
                            try {
                                if (p1Choice == 0 && playerID == 1){
                                    p1Choice = dis.readByte();
                                    System.out.println("Player 1 choice: "+p1Choice);
                                }
                                if (p2Choice == 0 && playerID == 2){
                                    p2Choice = dis.readByte();
                                    System.out.println("Player 2 choice: "+p2Choice);
                                }
                            } catch (IOException err){
                                System.out.println("IOException in getting player choices ");
                                if (playerID == 1) p1Offline = true;
                                else p2Offline = true;
                            }
                            break;
                        default:
                            break;
                    }
                    
                    try {Thread.sleep(50);} catch (InterruptedException error){}
                }
                System.out.println("End for player "+playerID);
            } catch (IOException e){
                System.out.println("IOException from run() in sSC");
                if (playerID == 1) {p1Offline = true;}
                else {p2Offline = true;}
            }
        }
        
        private void sendName(){
            try {
                dos.writeByte(-3);
                if (playerID == 1) dos.writeUTF(p2Name);
                else dos.writeUTF(p1Name);
                System.out.println("p1Name: "+p1Name+". p2Name: "+p2Name);
                dos.flush();
                
            } catch (IOException e){
                System.out.println("IOException in sendNames");
                if (playerID == 1) p1Offline = true;
                else p2Offline = true;
            }
        }
        
        private void sendPlayerID(){
            try {
            dos.writeByte(-2);
            dos.writeByte(playerID);
            dos.flush();
            } catch (IOException e){
                System.out.println("IOExc. in sendPlayerID");
            }
        }
        
        private void sendRoundResult(int enemyChoice, int result){
            if (chose){
                try {
                    dos.writeByte(1);
                    dos.writeBoolean(gameEnd);
                    dos.writeByte(gameResult);
                    dos.writeBoolean(roundEnded);
                    dos.writeByte(enemyChoice);
                    dos.writeByte(result);
                    dos.writeBoolean(overtime);
                    dos.flush();
                    System.out.println("Sent it!");
                    if (p1Offline){
                        p2.dos.write(-1);
                        p2.dos.flush();
                    } else if (p2Offline){
                        p1.dos.write(-1);
                        p1.dos.flush();
                    }
                } catch (IOException e) {
                    System.out.println("Exception in sendRoundResult()");
                    if (!timeOutTimer.isRunning()) timeOutTimer.start();
                }
                
            } else {
                //System.out.println("Weird glitch where even without choosing, still sends results from nowhere");
                if (playerID == 1) p1Offline = true;
                else p2Offline = true;
                timeOutTimer.start();
                finalRoundCheckWin();
            }
        }
        
        
        
        /*private void syncRoundStart(){
            try {
            dos.writeInt(roundNum);
            dos.writeBoolean(overtime);
            } catch (IOException e){
                System.out.println("Exception in syncRoundStart() <- nextRound()");
            }
        }*/
    }
}
