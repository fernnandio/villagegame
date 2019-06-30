package villagegameserver.server;

import villagegameserver.aesthetics.ConsoleColors;
import villagegameserver.game.Game;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {

    private ExecutorService clientThreadPool;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Game game;
    private PlayerHandler playerhandler;
    private ArrayList<String> votes;
    private boolean exit;
    private int countPlayers;
    private boolean isStartGame;
    private CopyOnWriteArrayList<PlayerHandler> playersList;
    private int numberPlayers;
    private int port;

    public Server(int port, int numberPlayers) {
        this.port = port;
        this.numberPlayers = numberPlayers;
        exit = false;
        isStartGame = false;
        this.playersList = new CopyOnWriteArrayList<>();
        countPlayers = 0;
        playersList = new CopyOnWriteArrayList<>();
        votes = new ArrayList<>();
    }

    public CopyOnWriteArrayList<PlayerHandler> getPlayersList() {
        return playersList;
    }

    public Game getGame() {
        return game;
    }

    public ArrayList<String> getVotes() {
        return votes;
    }

    public boolean getIsStartGame() {
        return isStartGame;
    }

    public synchronized void sendVote(String player) {
        System.out.println("player" + player);
        votes.add(player);
        System.out.println(votes.size() + " " + playersList.size());

        if (votes.size() == playersList.size()) {

            game.toNight = true;
            synchronized (this) {
                game.notifyDay();
            }
        }
        System.out.println(votes);
    }

    public synchronized void sendReadyStatus() {
        synchronized (this) {
            countPlayers++;
        }
        System.out.println("readystatus" + countPlayers);
        if (countPlayers >= 4 && isStartGame) {
            exit = true;
        }
    }

    public void start() {
        log("server started");
        game = new Game(this, playersList);

        try {
            serverSocket = new ServerSocket(port);
            clientThreadPool = Executors.newFixedThreadPool(numberPlayers);
            synchronized (this) {
                while (playersList.size() < numberPlayers) {
                    System.out.println("playerslistsize " + playersList.size() + "numberplayers " + numberPlayers);
                    log("Server waiting new connection");
                    clientSocket = serverSocket.accept();
                    playerhandler = new PlayerHandler(this, clientSocket);
                    playersList.add(playerhandler);
                    clientThreadPool.submit(playerhandler);
                    if (playersList.size() == numberPlayers) {
                        break;
                    }
                }
            }
            System.out.println("countplayers " + countPlayers);

            while (countPlayers != numberPlayers) {

                System.out.println(countPlayers + numberPlayers);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("countplayers2 " + countPlayers);
            game.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadCast(PlayerHandler playerHandler, String message) {

        for (PlayerHandler player : playersList) {
            if (playerHandler.equals(player)) {
                continue;
            }
            PrintWriter outMessage = null;
            try {
                outMessage = new PrintWriter(player.getClientSocket().getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            outMessage.println(message + ConsoleColors.RESET);
            log(playerHandler, message);
        }
    }

    public void broadCast(String message) {

        for (PlayerHandler player : playersList) {

            PrintWriter outMessage = null;
            try {
                outMessage = new PrintWriter(player.getClientSocket().getOutputStream(), true);
                //player.prompt.getUserInput(player.question1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            outMessage.println(message + ConsoleColors.RESET);
            log(message);
        }
    }

    public void setPlayerToKill(String playerNameKilledByWolf) {

        for (PlayerHandler player : playersList) {
            if (player.getAlias().equals(playerNameKilledByWolf)) {
                player.die();
                log("player to be killed by wolf chosen");
            }
        }
        game.toDay = true;
        game.notifyNight();
    }

    public void sendKilledMessage(PlayerHandler player) {
        String message = "you've been killed by the wolf";
        if (player.isWolf()) {
            message = "you've been killed by villagers";
        }
        PrintWriter outMessage = null;
        try {
            outMessage = new PrintWriter(player.getClientSocket().getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        outMessage.println(message + ConsoleColors.RESET);
        log(message);
    }

    public static void log(PlayerHandler playerHandler, String message) {
        System.out.println(ConsoleColors.RESET + playerHandler.getAlias() + " - " + message);
    }

    public static void log(String message) {
        System.out.println(ConsoleColors.RESET + message);
    }
}