import java.util.Random;
import java.util.Scanner;

public class Werewolfgame {

    static class Player {

        private int id;
        private String role;
        private boolean alive;

        public Player() {
            this.alive = true;
        }

        public Player(int id, String role) {
            this.id = id;
            this.role = role;
            this.alive = true;
        }

        public int getId(){
            return id;
        }

        public String getRole(){
            return role;
        }

        public boolean isAlive(){
            return alive;
        }

        public void kill(){
            alive = false;
        }

        public String getPublicInfo(){
            if (alive) {
                return "Player " + id + " [Alive]";
            }else{
                return "Player " + id + " [Dead]";
            }
        }
    }

    public static void main(String[] args){

        Scanner sc = new Scanner(System.in);
        Random rand = new Random();

        System.out.println("wolfGame");
        System.out.println("Enter number of players (4-10):");

        int n = sc.nextInt();
        sc.nextLine();

        while (n < 4 || n > 10) {
            System.out.println("Invalid number of players");
            n = sc.nextInt();
            sc.nextLine();
        }

        Player[] players = new Player[n];

        int wolfIndex = rand.nextInt(n);

        for (int i = 0; i < n; i++){
            if (i == wolfIndex) {
                players[i] = new Player(i+1, "Werewolf");
            }else{
                players[i] = new Player(i+1, "Villager");
            }
        }

        System.out.println();
        System.out.println("Role assignment start");
        System.out.println("Each player take turn to see role");

        for(int i = 0; i < n ; i++){

            System.out.println();
            System.out.println("Player " + (i + 1) + " Please press Enter");
            sc.nextLine();

            System.out.println("Your Role : " + players[i].getRole());

            System.out.println("Memorize your role, then press Enter");
            sc.nextLine();

            for(int line = 0; line < 30; line++ ){
                System.out.println();
            }
        }

        boolean gameOver = false;
        int round = 1;

        while(!gameOver){

            System.out.println("Round " + round);
            System.out.println();

            System.out.println("Night phase. Werewolf wakes up.");

            int aliveWerewolf = findAliveWerewolf(players);

            if (aliveWerewolf != -1) {

                System.out.println("Werewolf's turn.");

                System.out.println("Alive players:");
                printAlivePlayers(players);

                int targetId;

                while (true) {

                    System.out.println("Choose a player to kill:");

                    targetId = sc.nextInt();

                    if(isValidTarget(targetId,players,players[aliveWerewolf].getId())){
                        break;
                    }else{
                        System.out.println("Invalid target. Choose again.");
                    }
                }

                players[targetId-1].kill();

                System.out.println("Night results: Player " + targetId + " has been killed.");

            }else{
                System.out.println("No werewolf alive.");
            }

            if (checkKillvillagerwin(players)){
                System.out.println("Villagers win!");
                break;
            }

            if(checkKillwerewolfwin(players)){
                System.out.println("Werewolf win!");
                break;
            }

            System.out.println();
            System.out.println("Day phase. Voting begins.");

            printAlivePlayers(players);

            int voteId;

            while (true) {

                System.out.println("Vote a player to eliminate:");

                voteId = sc.nextInt();

                if(voteId>=1 && voteId<=players.length && players[voteId-1].isAlive()){
                    break;
                }else{
                    System.out.println("Invalid vote.");
                }
            }

            players[voteId-1].kill();

            System.out.println("Player " + voteId + " was voted out.");

            if (checkKillvillagerwin(players)){
                System.out.println("Villagers win!");
                break;
            }

            if(checkKillwerewolfwin(players)){
                System.out.println("Werewolf win!");
                break;
            }

            round++;
        }
    }

    public static int findAliveWerewolf(Player[] players){

        for(int i = 0; i < players.length; i++){

            if(players[i].isAlive() && players[i].getRole().equals("Werewolf")){
                return i;
            }
        }

        return -1;
    }

    public static void printAlivePlayers(Player[] players){

        for(int i=0; i<players.length; i++){

            if(players[i].isAlive()){
                System.out.println(players[i].getPublicInfo());
            }
        }
    }

    public static boolean isValidTarget(int targetId, Player[] players, int wolfId){

        if(targetId < 1 || targetId > players.length)
            return false;

        if(!players[targetId-1].isAlive())
            return false;

        if(targetId == wolfId)
            return false;

        return true;
    }

    public static boolean checkKillvillagerwin(Player[] players){

        for(Player p : players){

            if(p.isAlive() && p.getRole().equals("Werewolf")){
                return false;
            }
        }

        return true;
    }

    public static boolean checkKillwerewolfwin(Player[] players){

        int wolf = 0;
        int villager = 0;

        for(Player p : players){

            if(p.isAlive()){

                if(p.getRole().equals("Werewolf"))
                    wolf++;
                else
                    villager++;
            }
        }

        return wolf >= villager;
    }
}