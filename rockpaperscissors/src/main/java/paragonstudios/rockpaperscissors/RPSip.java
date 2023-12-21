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
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
public class RPSip implements Runnable{
    
    private String ip = "";
    private int port = 26262;
    private JFrame frame;
    private final int WIDTH = 500;
    private final int HEIGHT = 500;
    private Thread thread;
    
    private Painter painter;
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
          
    private ServerSocket serverSocket;
    
    private BufferedImage board;
    private BufferedImage rock;
    private BufferedImage paper;
    private BufferedImage scissor;
    private BufferedImage lostRoundImage;
    private BufferedImage wonRoundImage;
    private BufferedImage drawRoundImage;
    
    String roundNumString = "";
    private int roundTimeRemaining = 10; //time remaining in round
    private String roundTimeRemainingString = "";
    private boolean roundTimerStopped = false;
    private int roundNum = 1;
    private boolean roundEnded = false;
    
    private boolean serverStarter = true;
    private boolean accepted = false;
    private boolean justAccepted = true;
    private boolean unableToCommunicateWithOpponent = false;

    private boolean chose = false;

    private int choice = 0;
    private int enemyChoice = 0;
    
    private int wonRounds = 0;
    private int enemyWonRounds = 0;
    private int[] wonRoundsPlayers = new int[10]; // 1 -> win, 2 -> loss, 3 -> draw
    private int[] roundTimes = new int[]{10, 10, 15, 15, 20, 20, 25, 25, 30, 30};
    
    private boolean won = false;
    private boolean enemyWon = false;
    private boolean drawGame = false;
    
    private boolean overtime = false;
    
    private boolean gameEnd = false;
    
    private int errors = 0;
    private Font font = new Font("Verdana", Font.BOLD, 32);
    private Font smallerFont = new Font("Verdana", Font.BOLD, 20);
    private Font largerFont = new Font("Verdana", Font.BOLD, 50);
   
    private String waitingString = "Waiting for opponent...";
    private String unableToCommunicateWithOpponentString = "Unable to communicate with opponent.";
    private String wonString = "Won!";
    private String enemyWonString = "Opponent won!";
    private String drawGameString = "Game draw!";
    private String overtimeString = "Overtime!";
    
    private Timer roundTimer = new Timer(1000, null);
    ActionListener al = new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e){ 
            if (roundTimeRemaining-- <= 0) {
                
                if (!roundTimerStopped && !chose){
                    try {
                        chose = true;
                        choice = 1;
                        dos.writeInt(choice);
                        dos.flush();
                    } catch (IOException error){
                        errors++;
                        error.printStackTrace();
                    }
                }
                //roundTimer.setDelay(5000);
                roundTimer.stop();
            }
        }
    };
    
    public RPSip(){
        ip = JOptionPane.showInputDialog("Enter IP!");
        if (ip == null){
            RockPaperScissors.m.setVisible(true);
        } else {
            String portStr = JOptionPane.showInputDialog("Enter port!");
            if (portStr == null){
                RockPaperScissors.m.setVisible(true);
            } else {
                try {
                    port = Integer.parseInt(portStr);
                    while (port < 1 || port > 65535){
                        portStr = JOptionPane.showInputDialog("Enter valid port!");
                        port = Integer.parseInt(portStr);
                    }
                    loadImages();
        
                    painter = new Painter();
                    painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));

                    if (!connect()) initializeServer();

                    frame = new JFrame();
                    frame.setTitle("RPS by Paragon");
                    frame.setContentPane(painter);
                    frame.setSize(WIDTH, HEIGHT);
                    frame.setLocationRelativeTo(null);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setResizable(false);
                    frame.setVisible(true);

                    thread = new Thread(this, "Rockpaperscissors");
                    thread.start();
                } catch (NumberFormatException error) {
                    try {
                        port = Integer.parseInt(JOptionPane.showInputDialog("Port invalid. Enter valid port!"));
                    } catch (NumberFormatException error2){
                        RockPaperScissors.m.setVisible(true);
                    }
                }
            }
        }
        
    }
    
    public void loadImages(){
        try {
            board = ImageIO.read(getClass().getResourceAsStream("/board.png"));
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
        while (true){
            painter.repaint();
            tick();
            if (serverStarter && !accepted){
                listenForServerRequest();
            }
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
        if (unableToCommunicateWithOpponent){
            g.setColor(Color.RED);
            g.setFont(smallerFont);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(unableToCommunicateWithOpponentString);
            g.drawString(unableToCommunicateWithOpponentString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
            return;
        }
        if (accepted){
            g.setFont(font);
            
            //Round *num* display
            Graphics2D g2 = (Graphics2D)g;
            roundNumString = "Round "+roundNum;
            int stringWidth = g2.getFontMetrics().stringWidth(roundNumString);
            g.drawString(roundNumString, WIDTH / 2 - stringWidth / 2, HEIGHT / 10);
            
            //Round timer
            
            if (!roundEnded) {
                roundTimeRemainingString = "Time left: "+roundTimeRemaining;
                stringWidth = g2.getFontMetrics().stringWidth(roundTimeRemainingString);
                g.drawString(roundTimeRemainingString, WIDTH/2 - stringWidth, HEIGHT / 5);
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
                        
                        g.drawImage(rock, WIDTH / 2, 100, null);
                        break;
                    case 2:
                        g.drawImage(paper, WIDTH / 2, 100, null);
                        break;
                    case 3:
                        g.drawImage(scissor, WIDTH / 2, 100, null);
                        break;
                    default:
                        break;
                }
                g.setFont(smallerFont);
                switch (wonRoundsPlayers[roundNum-1]){
                    case 1:
                        if (serverStarter) g.drawString("Round won!", WIDTH / 2, 100);
                        else g.drawString("Round lost!", WIDTH / 2, 100);
                        break;
                    case 2:
                        if (serverStarter) g.drawString("Round lost!", WIDTH / 2, 100); 
                        else g.drawString("Round won!", WIDTH / 2, 100);
                        break;
                    case 3:
                        g.drawString("Draw!", WIDTH/2, 100);
                        break;
                }
            }
            for (int i = 0; i < wonRoundsPlayers.length; i++){
                switch (wonRoundsPlayers[i]) {
                    case 1:
                        if (serverStarter){
                            g.drawImage(wonRoundImage, (i * 30), 430, null);
                        } else {
                            g.drawImage(lostRoundImage, (i * 30), 430, null);
                        }   break;
                    case 2:
                        if (serverStarter){
                            g.drawImage(lostRoundImage, (i * 30), 430, null);
                        } else {
                            g.drawImage(wonRoundImage, (i * 30), 430, null);
                        }   break;
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
            if (won || enemyWon){
                g.setColor(Color.red);
                g.setFont(largerFont);
                if (won){
                    stringWidth = g2.getFontMetrics().stringWidth(wonString);
                    g.drawString(wonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                } else if (enemyWon){
                    stringWidth = g2.getFontMetrics().stringWidth(enemyWonString);
                    g.drawString(enemyWonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                }
            }
            if (drawGame){
                g.setColor(Color.red);
                g.setFont(largerFont);
                stringWidth = g2.getFontMetrics().stringWidth(drawGameString);
                g.drawString(wonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
            }
        } else {
            System.out.println("Waiting");
            g.setColor(Color.RED);
            g.setFont(font);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(waitingString);
            g.drawString(waitingString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
        }
    } 
    
    class readOpponentChoice extends Thread{
        @Override
        public void run(){
            try {
                enemyChoice = dis.readInt();
            } catch (Exception e){
                e.printStackTrace();
                errors++;
            }
            checkForWin();
        }
    }
    
    private boolean gettingEnemyChoice = false;
    public void tick(){
        if (errors >= 10)
            unableToCommunicateWithOpponent = true;
        
        if (chose && !unableToCommunicateWithOpponent && enemyChoice == 0 && !gettingEnemyChoice){
            startEnemyChoiceThread();
            gettingEnemyChoice = true;
        }
    }
    
    //the reason why this is needed is since dis.readInt() blocks the GUI thread, freezing it - timer doesn't decrement
    private void startEnemyChoiceThread(){
        SwingWorker sw1 = new SwingWorker(){
            @Override
            protected String doInBackground() throws Exception{
                try {
                    enemyChoice = dis.readInt();
                    checkForWin();
                } catch (IOException e) {
                    e.printStackTrace();
                    errors++;
                }
                return "";
            }
        };
        sw1.execute();
    }
    
    private void listenForServerRequest() {
        Socket socket = null;
        try {
            socket = serverSocket.accept(); //this freezes the app until someone joins
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            accepted = true;
            System.out.println("Client requested join, accepted request");
            roundTimer.addActionListener(al);
            roundTimer.start();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    
    private boolean connect(){
        try {
            socket = new Socket(ip, port);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            accepted = true;
            roundTimer.addActionListener(al);
            roundTimer.start();
        } catch (IOException e){
            System.out.println("Unable to connect to: "+ip+": "+ port+". Starting server...");
            return false;
        }
        System.out.println("Successfully connected!");
        return true;
    }
    
    private void initializeServer(){
        try{
            serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
        } catch (Exception e){
            e.printStackTrace();
        }
        serverStarter = true;
    }
    
    private boolean changeToOvertime = false;
    private void checkForWin(){
        /*System.out.println("test");
        System.out.println("choice: "+choice);
        System.out.println("enemychoice: "+enemyChoice);*/
        roundEnded = true;
        if (!overtime){
            methodInCheckWin();
            nextRound();
            if (wonRounds >= 3 || enemyWonRounds >= 3){
                if (wonRounds > enemyWonRounds) won = true;
                else enemyWon = true;
                gameEnd = true;
            } else if (roundNum >= 9){
                changeToOvertime = true;
            }
        } else {
            methodInCheckWin();
            if (wonRounds > enemyWonRounds) won = true;
            else if(enemyWonRounds > wonRounds) enemyWon = true;
            else drawGame = true;
            gameEnd = true;
        }
    }
    
    private void methodInCheckWin(){
        if ((choice == 2 && enemyChoice == 1) || (choice == 3 && enemyChoice == 2) || (choice == 1 && enemyChoice == 3)){
            wonRounds++;
            wonRoundsPlayers[roundNum-1] = 1;
        } else if (choice == enemyChoice){ 
            wonRoundsPlayers[roundNum-1] = 3;
        } else {
            enemyWonRounds++;
            wonRoundsPlayers[roundNum-1] = 2;
        }
    }
    
    private void nextRound(){
        //starts next round
        ActionListener timerActionListener = new ActionListener(){
            public void actionPerformed(ActionEvent event){
                chose = false;
                gettingEnemyChoice = false;
                choice = 0;
                roundNum++;
                enemyChoice = 0;
                roundTimeRemaining = roundTimes[roundNum-1];
                roundEnded = false;
                roundTimerStopped = false;
                if (changeToOvertime) overtime = true;
                roundTimer.start();
            }
        };
        Timer timer = new Timer(0, timerActionListener);
        timer.setInitialDelay(5000);
        timer.setRepeats(false);
        timer.start();
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
            if (accepted){
                if (!chose && !won && !enemyWon && !unableToCommunicateWithOpponent && !roundEnded){
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
                            dos.writeInt(choice);
                            dos.flush();
                        } catch (IOException error){
                            errors++;
                            error.printStackTrace();
                        }
                        System.out.println("data sent");
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
        RPSip o = new RPSip();
    }
}
