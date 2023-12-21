package paragonstudios.rockpaperscissors;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

/**
 *
 * @author user
 */
public class RPS implements Runnable{
    
    private clientSideConnection csc;
    
    private String ip = "42.2.170.217";
    
    private int port = 26262;
    private JFrame frame;
    private final int WIDTH = 500;
    private final int HEIGHT = 500;
    private Thread thread;
    
    private Painter painter;
              
    private BufferedImage board;
    private BufferedImage exitButton;
    private BufferedImage rock;
    private BufferedImage paper;
    private BufferedImage scissor;
    private BufferedImage lostRoundImage;
    private BufferedImage wonRoundImage;
    private BufferedImage drawRoundImage;
    
    String roundNumString = "";
    private int roundTimeRemaining = 15; //time remaining in round
    private String roundTimeRemainingString = "";
    private boolean roundTimerStopped = false;
    private int roundNum = 1;
    private volatile boolean roundEnded = false;
    
    private boolean gameStarted = false;
    private boolean unableToCommunicateWithServer = false;
    
    private byte playerID;
    private volatile String username = MainMenu.username;
    private volatile String opponentName = "";
    private volatile boolean opponentDisconnected = false;

    private volatile boolean chose = false;
    private byte choice = 0;
    private byte enemyChoice = 0;
    
    private byte[] roundResults = new byte[10]; // 1 -> win, 2 -> loss, 3 -> draw
    private byte[] roundTimes = new byte[]{15, 15, 15, 10, 10, 10, 8, 8, 8, 5};
    
    private byte gameResult = 0; //1 - win, 2 - loss, 3 - draw. (conversion to p2 handled in receiveRoundResults())
    
    private boolean changeToOvertime = false;
    private boolean overtime = false;
    
    private boolean gameEnd = false;
    
    private boolean quit = false;
    
    private int errors = 0;
    private Font font = new Font("Verdana", Font.BOLD, 32);
    private Font smallerFont = new Font("Verdana", Font.BOLD, 20);
    private Font largerFont = new Font("Verdana", Font.BOLD, 50);
   
    private String waitingString = "Waiting for opponent...";
    private String vsString = "VS";
    private String unableToCommunicateWithServerString = "Unable to communicate with server.";
    private String opponentDisconnectedString = "Opponent disconnected :/";
    private String wonString = "Won!";
    private String enemyWonString = "Opponent won!";
    private String drawGameString = "Game draw!";
    private String overtimeString = "Overtime!";
        
    
    private Timer roundTimer = new Timer(1000, null);
    ActionListener al = new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e){ 
            roundTimeRemaining--;
            if (roundTimeRemaining <= 0) {
                roundTimeRemaining = 0;
                if (!roundTimerStopped && !chose){
                    chose = true;
                    choice = 1;
                }
                //roundTimer.setDelay(5000);
                roundTimer.stop();
            }
        }
    };
    
    private int timeUntilTimeout = 10;
    ActionListener roundOverAl = new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e){ 
            timeUntilTimeout--;
            if (timeUntilTimeout == 0){
                unableToCommunicateWithServer = true;
            }
        }
    };
    private Timer roundOverTimer = new Timer(1000,roundOverAl);
    
    
    public RPS(){

        loadImages();
        
        painter = new Painter();
        painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        frame = new JFrame();
        frame.setTitle("RPS - First to 3 wins!");
        frame.setContentPane(painter);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                try {
                    quit = true;
                    if (csc.dos != null){
                        csc.dos.writeByte(-1);
                        csc.dos.flush();
                    }
                    
                } catch (IOException error){
                    System.out.println("IOExc error in windowClosing");
                }
                frame.dispose();
                System.exit(0);
            }
        });
        frame.setResizable(false);
        frame.setVisible(true);
        
        if (username.equals("")) username = "Guest";
        
        thread = new Thread(this, "Rockpaperscissors");
        thread.start();
    }
    
    public void loadImages(){
        try {
            board = ImageIO.read(getClass().getResourceAsStream("/board.png"));
            exitButton = ImageIO.read(getClass().getResourceAsStream("/exitbutton.png"));
            rock = ImageIO.read(getClass().getResourceAsStream("/rock.png"));
            paper = ImageIO.read(getClass().getResourceAsStream("/paper.png"));
            scissor = ImageIO.read(getClass().getResourceAsStream("/scissor.png"));
            wonRoundImage = ImageIO.read(getClass().getResourceAsStream("/won.png"));
            lostRoundImage = ImageIO.read(getClass().getResourceAsStream("/lost.png"));
            drawRoundImage = ImageIO.read(getClass().getResourceAsStream("/draw.png"));
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    
    public void run(){
        connectToServer();
        
        roundTimeRemaining = roundTimes[0];
        while (!quit){
            painter.repaint();
            tick();
            try {Thread.sleep(50);} catch (InterruptedException e){}
        }
    }
    
    private String choiceToString(int choice){
        String choiceString="";
        switch(choice){
            case 1:
                choiceString = "Rock";
                break;
            case 2:
                choiceString = "Paper";
                break;
            case 3:
                choiceString = "Scissors";
                break;
            default:
                break;
        }  
        return choiceString;
    }
    
    private void render(Graphics g){
        g.drawImage(board, 0, 0, null);
        if (unableToCommunicateWithServer){
            g.setColor(Color.RED);
            g.setFont(smallerFont);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(unableToCommunicateWithServerString);
            g.drawString(unableToCommunicateWithServerString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
            g.drawImage(exitButton, 400, 400, null);
            return;
        }
        if (gameStarted){
            g.setFont(font);
            
            //Round *num* display
            Graphics2D g2 = (Graphics2D)g;
            roundNumString = "Round "+roundNum;
            int stringWidth = g2.getFontMetrics().stringWidth(roundNumString);
            g.drawString(roundNumString, WIDTH / 2 - stringWidth / 2, HEIGHT / 10 + 8);
            
            g.setFont(smallerFont);
            g.drawString("Opponent: "+opponentName, 10, 20);
            //Round timer
            
            g.setFont(font);
            if (!(roundEnded || gameEnd)) {
                roundTimeRemainingString = "Time left: "+roundTimeRemaining;
                stringWidth = g2.getFontMetrics().stringWidth(roundTimeRemainingString);
                g.drawString(roundTimeRemainingString, WIDTH/2 - stringWidth, HEIGHT / 5 + 5);
            }
            
            //Display rock, paper, scissor images
            if (!(chose || roundEnded || gameEnd)){
                g.drawImage(rock, 113, 300, null);
                g.drawImage(paper, 213, 300, null);
                g.drawImage(scissor, 313, 300, null);
            }
            if ((chose || roundEnded) && !gameEnd){
                roundTimerStopped = true;
                g.setFont(smallerFont);
                String choiceString = choiceToString(choice);
                g.drawString("You chose: "+choiceString, WIDTH/2, HEIGHT/2);
                switch (choice){
                    case 1: 
                        g.drawImage(rock, 213, 300, null);
                        break;
                    case 2:
                        g.drawImage(paper, 213, 300, null);
                        break;
                    case 3:
                        g.drawImage(scissor, 213, 300, null);
                        break;
                }
            }
            if (roundEnded && !gameEnd){
                switch (enemyChoice){
                    case 1:
                        g.drawImage(rock, 213, 100, null);
                        break;
                    case 2:
                        g.drawImage(paper, 213, 100, null);
                        break;
                    case 3:
                        g.drawImage(scissor, 213, 100, null);
                        break;
                    default:
                        break;
                }
                g.setFont(smallerFont);
                switch (roundResults[roundNum-1]){
                    case 1:
                        stringWidth = g2.getFontMetrics().stringWidth("Round won!");
                        g.drawString("Round won!", WIDTH / 2 - stringWidth / 2, 100);
                        break;
                    case 2:
                        stringWidth = g2.getFontMetrics().stringWidth("Round lost!");
                        g.drawString("Round lost!", WIDTH / 2- stringWidth / 2, 100); 
                        break;
                    case 3:
                        stringWidth = g2.getFontMetrics().stringWidth("Draw!");
                        g.drawString("Draw!", WIDTH/2- stringWidth / 2, 100);
                        break;
                }
            }
            for (int i = 0; i < roundResults.length; i++){
                switch (roundResults[i]) {
                    case 1:
                        g.drawImage(wonRoundImage, (i * 30), 430, null);
                        break;
                    case 2:
                        g.drawImage(lostRoundImage, (i * 30), 430, null);
                        break;
                    case 3:
                        g.drawImage(drawRoundImage, (i * 30), 430, null);
                        break;
                    default:
                        break;
                }
                    
            }
            if (overtime){
                g.setColor(Color.red);
                g.setFont(font);
                
                stringWidth = g2.getFontMetrics().stringWidth(overtimeString);
                g.drawString(overtimeString, WIDTH / 2 - stringWidth / 2, HEIGHT/3);
            }
            if (gameEnd){
                if (opponentDisconnected){
                    g.setFont(smallerFont);
                    stringWidth = g2.getFontMetrics().stringWidth(opponentDisconnectedString);
                    g.drawString(opponentDisconnectedString, WIDTH / 2 - stringWidth / 2, HEIGHT/2 + 50);
                }
                g.setFont(largerFont);
                g.drawImage(exitButton, 400, 400, null);
                switch (gameResult) {
                    case 1:
                        g.setColor(Color.green);
                        stringWidth = g2.getFontMetrics().stringWidth(wonString);
                        g.drawString(wonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                        break;
                    case 2:
                        g.setColor(Color.red);
                        stringWidth = g2.getFontMetrics().stringWidth(enemyWonString);
                        g.drawString(enemyWonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                        break;
                    case 3:
                        g.setColor(Color.BLUE);
                        g.setFont(largerFont);
                        stringWidth = g2.getFontMetrics().stringWidth(drawGameString);
                        g.drawString(drawGameString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                        break;
                    default:
                        break;
                }
            }
            //opponent name
            
        } else {
            chose = false;
            g.setColor(Color.RED);
            g.setFont(font);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(waitingString);
            g.drawString(waitingString, WIDTH / 2 - stringWidth / 2, HEIGHT / 4);
            
            g.setFont(largerFont);
            stringWidth = g2.getFontMetrics().stringWidth(vsString);
            g.drawString(vsString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
            g.drawImage(exitButton, 400, 400, null);
            
            g.setColor(Color.BLACK);
            g.setFont(smallerFont);
            stringWidth = g2.getFontMetrics().stringWidth(username);
            g.drawString(username, WIDTH / 2 - stringWidth / 2, 200);
            
            if (!opponentName.equals("")){
                stringWidth = g2.getFontMetrics().stringWidth(opponentName);
                g.drawString(opponentName, WIDTH / 2 - stringWidth / 2, 275);
            } else {
                stringWidth = g2.getFontMetrics().stringWidth("?");
                g.drawString("?", WIDTH / 2 - stringWidth / 2, 275);
            }
        
            
            
        }
        try {Thread.sleep(50);} catch (InterruptedException error) {}
    }
    
    
    public void connectToServer(){
        csc = new clientSideConnection();
        try {
        Thread.sleep(500);} catch (InterruptedException e){}
        startReadingFromServer();
    }
    
    private void startReadingFromServer(){
        Thread t = new Thread(new Runnable(){
            @Override
            public void run(){
                System.out.println("hi");
                csc.readFromServer();
            }
        });
        t.start();
    }
    
    public void tick() {
        if (errors >= 4)
            unableToCommunicateWithServer = true;
        try{Thread.sleep(50);} catch (InterruptedException e){}
    }
    
    private class clientSideConnection{
        private Socket socket;
        private DataOutputStream dos;
        private DataInputStream dis;
        
        private clientSideConnection(){
            //try{
                socket = RockPaperScissors.m.cc.getSocket();
                System.out.println(socket.toString()+socket.isClosed());
                //ip, 
                
                dis = RockPaperScissors.m.cc.getDIS();
                System.out.println("dis: "+dis.toString());
                dos = RockPaperScissors.m.cc.getDOS();
                System.out.println("dos: "+dos.toString());
              
               
//            } catch (IOException e){
//                System.out.println("Error in clientSideConnection() constructor");
//                unableToCommunicateWithServer = true;
//            }
        }
        
        //the reason why this is needed is since dis.readInt() blocks the GUI thread, freezing it - timer doesn't decrement
        private void readFromServer(){
            try {
                dos.writeByte(-2);
                dos.writeUTF(username);
                dos.flush();
                while (!quit){
                    int typeOfMessage = dis.readByte();
                    System.out.println("type of message: "+typeOfMessage);
                    switch (typeOfMessage){
                        case -3:
                            opponentName = dis.readUTF();
                            System.out.println(opponentName);
                            break;
                        case -2:
                            playerID = dis.readByte();
                            System.out.println("Connected to game room as player "+playerID); 
                            break;
                        case -1:
                            opponentDisconnected = true;
                            break;
                        case 0:
                            checkGameStarted();
                            break;
                        case 1:
                            System.out.println("Receive");
                            gameEnd = dis.readBoolean();
                            gameResult = dis.readByte();
                            roundEnded = dis.readBoolean();
                            enemyChoice = dis.readByte();
                            roundResults[roundNum-1] = dis.readByte();
                            changeToOvertime = dis.readBoolean();
                            System.out.println("------------");
                            System.out.println("Game end: "+gameEnd);
                            System.out.println("Game result: "+gameResult);
                            System.out.println("RoundEnded: "+roundEnded);
                            System.out.println("Enemy choice: "+enemyChoice);
                            System.out.println("RoundResult -> "+roundResults[roundNum-1]);
                            System.out.println("changeToOvertime? -> "+changeToOvertime);
                            if (gameResult != 0 && playerID == 2){
                                if (gameResult == 1) gameResult = 2;
                                else if (gameResult == 2) gameResult = 1;
                            }
                            if (!gameEnd) roundOverTimer.start();
                            break;
                        case 2:
                            nextRound();
                            break;
                        }
                }
            } catch (IOException e) {
                System.out.println("RPS closed socket (readFromServer exception)");
                //e.printStackTrace();
                //errors++;
                unableToCommunicateWithServer = true;
            }
            
        }
        
        private void checkGameStarted(){
            try{
                gameStarted = dis.readBoolean();
                roundTimer.addActionListener(al);
                roundTimer.start();
                /*
                dos.writeBoolean(true);
                dos.flush(); //simply for sSC run() method*/
            } catch (IOException e) {
                System.out.println("Exception in checkGameStarted()");
                errors++;
            }
        }
        
        public void sendChoice(byte choice){
            try {
                dos.writeByte(1);
                dos.writeByte(choice);
                dos.flush();
            } catch (IOException e){
                System.out.println("Exception in sendChoice()");
                errors++;
            }
        }
    }
    
    private void nextRound(){
        //starts next round
        try{
            roundEnded = csc.dis.readBoolean();
            roundOverTimer.stop();
            timeUntilTimeout = 10;
        } catch (IOException e){
            errors++;
        }
        choice = 0;
        enemyChoice = 0;
        roundNum++;
        roundTimeRemaining = roundTimes[roundNum-1];
        roundTimerStopped = false;
        roundTimer.start();
        if (changeToOvertime) overtime = true;
        System.out.println(roundNum);
        chose = false; //break out of loop
    }

    
    public class Painter extends JPanel implements MouseListener{
        public Painter(){
            setFocusable(true);
            requestFocus();
            setBackground(Color.WHITE);
            addMouseListener(this);
        }
        @Override
        public void paintComponent(Graphics g){
            super.paintComponent(g);
            render(g);
        }
        @Override
        public void mouseClicked(MouseEvent e) {
            //System.out.println(gameStarted+"."+unableToCommunicateWithServer+"."+gameEnd);
            if (!gameStarted || unableToCommunicateWithServer || gameEnd){
                int x = e.getX();
                int y = e.getY();
                System.out.println("X: "+x+". Y: "+y);
                if (y > 400 && y < 476 && x > 400 && x < 476){
                    quit = true;
                    try {
                        csc.dos.writeByte(-1);
                        csc.dos.flush();

                        csc.socket.close();
                        
                        //RockPaperScissors.m.cc.
                        
                        
                        
                    } catch (IOException | NullPointerException err){}
                    
                    frame.dispose();
                    
                    RockPaperScissors.m.setVisible(true);
                    RockPaperScissors.m.cc.restartConnAfterGame();
                    RockPaperScissors.m.inMenu = true;
                }
            } else {
                if (!chose && !roundEnded){
                    System.out.println("Activated");
                    int x = e.getX();
                    int y = e.getY();
                    if (y > 300 && y < 375){
                        if (x > 113 && x < 198){
                            chose = true;
                            choice = 1;
                        } else if (x > 213 && x < 298){
                            chose = true;
                            choice = 2;
                        } else if (x > 313 && x < 398){
                            chose = true;
                            choice = 3;
                        }
                    }
                    if (chose){
                        try {
                            csc.sendChoice(choice);
                        } catch (Exception error){
                            errors++;
                            error.printStackTrace();
                        }
                        System.out.println("data sent. choice: "+choice);
                    }
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }
        
    }
    
    public static void main(String[] args) {
        RPS rps = new RPS();
    }
}
