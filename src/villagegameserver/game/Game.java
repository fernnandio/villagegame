package villagegameserver.game;

import villagegameserver.aesthetics.AsciiArt;
import villagegameserver.server.PlayerHandler;
import villagegameserver.server.Server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Game {

    private CopyOnWriteArrayList<PlayerHandler> inGamePlayersList;
    private Server server;
    private Map<String, Integer> votes;
    private PlayerHandler wolf;
    public volatile boolean toNight;
    public volatile boolean toDay;
    private volatile boolean voteTime;

    public Game(Server server, CopyOnWriteArrayList inGamePlayersList) {
        this.server = server;
        this.inGamePlayersList = inGamePlayersList;
        this.votes = new HashMap<>();
        toNight = false;
        toDay = false;
        voteTime = false;
    }

    public void start() {

        System.out.println("game started");
        server.broadCast(AsciiArt.gameStartString());
        Server.log("game started");
        assignRoles();
        start2();
    }

    private synchronized void start2() {

        while (!wolf.isDead() || inGamePlayersList.size() > 1) {
            while (!toNight) {
                try {
                    dayTime();
                    wait();
                } catch (InterruptedException e) {
                    System.out.println("erro");
                }
            }
            System.out.println("end of daytime");
            while (!voteTime) {
                try {
                    voteTime();
                    wait();
                } catch (InterruptedException e) {
                    System.out.println("erro");
                }
            }
            while (!toDay) {
                try {
                    nightTime();
                    wait();
                } catch (InterruptedException e) {
                    System.out.println("erro");
                }
            }
            toDay = false;
            toNight = false;
        }
    }

    public synchronized void notifyDay() {
        System.out.println(toNight);
        notifyAll();
    }

    public synchronized void notifyNight() {
        notifyAll();
    }

    private synchronized void dayTime() {

        Server.log("daytime started");
        server.broadCast(AsciiArt.dayTimeMessage());
        server.broadCast("Waiting for all players to vote. Type /help to see commands.");
        System.out.println("day begin");
    }

    private synchronized void voteTime() {
        String result = votingDecisions(); // waits for voting decisions

        System.out.println("resultado" + result);

        for (PlayerHandler player : inGamePlayersList) {
            System.out.println("player option" + player.getAlias() + result);
            if (player.getAlias().equals(result)) {
                System.out.println(player.getAlias());
                server.broadCast("There will be BLOOD tonight.");
                player.die();
                notifyDay();

                System.out.println("player killed" + player.getAlias());
                //  inGamePlayersList.remove(player);
                break;
            }
        }
        System.out.println("player kill continue loop");
        votes.clear();
    }

    public synchronized void votingTime() {
        this.voteTime = true;
        notifyAll();
    }

    private synchronized void nightTime() {
        synchronized (this) {
            Server.log("night time started");
            server.broadCast(AsciiArt.nightTimeMessage());
        }
    }

    private void assignRoles() {

        int rand = (int) (Math.random() * inGamePlayersList.size());
        wolf = inGamePlayersList.get(rand);
        wolf.makeWolf();
        Server.log("wolf chosen.");
        System.out.println("wolf is" + wolf.getAlias());
    }

    private Map<String, Integer> votesStatistic() {

        for (String vote : server.getVotes()) {

            if (votes.containsKey(vote)) {
                votes.replace(vote, votes.get(vote) + 1);
                continue;
            }
            votes.put(vote, 1);
        }
        return votes;
    }

    private String votingDecisions() {

        int max = 0;
        String mostVoted = "";

        for (String key : votesStatistic().keySet()) {

            if (max < votesStatistic().get(key)) {

                max = votesStatistic().get(key);
                mostVoted = key;
            }
        }
        return votesStatistic().containsValue(max) ? "Tied" : mostVoted;
    }
}