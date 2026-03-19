import java.util.Random;
import java.util.Scanner;

public class Werewolfgame2 {

    // ==================== Camp Constants ====================
    static final String CAMP_GOOD = "Good";
    static final String CAMP_BAD = "Bad";
    static final String CAMP_NEUTRAL = "Neutral";

    // ==================== Player Class ====================
    static class Player {

        private int id;
        private String role;
        private String camp;
        private boolean alive;
        private boolean protected_;  // Whether protected by Guard this night
        private boolean poisoned;    // Whether poisoned by Witch this night
        private boolean healed;      // Whether healed by Witch this night

        public Player() {
            this.alive = true;
            this.protected_ = false;
            this.poisoned = false;
            this.healed = false;
        }

        public Player(int id, String role, String camp) {
            this.id = id;
            this.role = role;
            this.camp = camp;
            this.alive = true;
            this.protected_ = false;
            this.poisoned = false;
            this.healed = false;
        }

        public int getId() {
            return id;
        }

        public String getRole() {
            return role;
        }

        public String getCamp() {
            return camp;
        }

        public boolean isAlive() {
            return alive;
        }

        public void kill() {
            alive = false;
        }

        public boolean isProtected() {
            return protected_;
        }

        public void setProtected(boolean val) {
            this.protected_ = val;
        }

        public boolean isPoisoned() {
            return poisoned;
        }

        public void setPoisoned(boolean val) {
            this.poisoned = val;
        }

        public boolean isHealed() {
            return healed;
        }

        public void setHealed(boolean val) {
            this.healed = val;
        }

        public String getPublicInfo() {
            if (alive) {
                return "Player " + id + " [Alive]";
            } else {
                return "Player " + id + " [Dead]";
            }
        }

        public String getRoleInfo() {
            return "Player " + id + " | Role: " + role + " | Camp: " + camp;
        }
    }

    // ==================== Global Variables ====================
    static Scanner sc = new Scanner(System.in);
    static Random rand = new Random();
    static boolean witchHasHeal = true;   // Witch heal potion (one-time use)
    static boolean witchHasPoison = true; // Witch poison potion (one-time use)

    // ==================== Main Method ====================
    public static void main(String[] args) {

        System.out.println("=== Werewolf Game 2 (Assignment 2) ===");
        System.out.println("Enter number of players (8-15):");

        int n = sc.nextInt();
        sc.nextLine();

        while (n < 8 || n > 15) {
            System.out.println("Invalid number. Please enter a number between 8 and 15:");
            n = sc.nextInt();
            sc.nextLine();
        }

        // --- Configure role counts ---
        System.out.println();
        System.out.println("=== Role Configuration ===");

        int werewolfCount = getConfiguredCount("Werewolf (Bad camp)", 1, n / 2);
        int seerCount = getConfiguredCount("Seer (Good camp)", 0, 1);
        int witchCount = getConfiguredCount("Witch (Good camp)", 0, 1);
        int hunterCount = getConfiguredCount("Hunter (Good camp)", 0, 1);
        int guardCount = getConfiguredCount("Guard (Good camp)", 0, 1);
        int foolCount = getConfiguredCount("Fool (Neutral camp)", 0, 1);

        int specialCount = werewolfCount + seerCount + witchCount + hunterCount + guardCount + foolCount;

        while (specialCount > n) {
            System.out.println("Total roles (" + specialCount + ") exceed player count (" + n + "). Please reconfigure.");
            werewolfCount = getConfiguredCount("Werewolf (Bad camp)", 1, n / 2);
            seerCount = getConfiguredCount("Seer (Good camp)", 0, 1);
            witchCount = getConfiguredCount("Witch (Good camp)", 0, 1);
            hunterCount = getConfiguredCount("Hunter (Good camp)", 0, 1);
            guardCount = getConfiguredCount("Guard (Good camp)", 0, 1);
            foolCount = getConfiguredCount("Fool (Neutral camp)", 0, 1);
            specialCount = werewolfCount + seerCount + witchCount + hunterCount + guardCount + foolCount;
        }

        int villagerCount = n - specialCount;

        System.out.println();
        System.out.println("Role Summary:");
        System.out.println("  Werewolf: " + werewolfCount);
        System.out.println("  Seer:     " + seerCount);
        System.out.println("  Witch:    " + witchCount);
        System.out.println("  Hunter:   " + hunterCount);
        System.out.println("  Guard:    " + guardCount);
        System.out.println("  Fool:     " + foolCount);
        System.out.println("  Villager: " + villagerCount);
        System.out.println();

        // --- Build role array and shuffle ---
        String[] roles = new String[n];
        String[] camps = new String[n];
        int idx = 0;

        for (int i = 0; i < werewolfCount; i++) {
            roles[idx] = "Werewolf";
            camps[idx] = CAMP_BAD;
            idx++;
        }
        for (int i = 0; i < seerCount; i++) {
            roles[idx] = "Seer";
            camps[idx] = CAMP_GOOD;
            idx++;
        }
        for (int i = 0; i < witchCount; i++) {
            roles[idx] = "Witch";
            camps[idx] = CAMP_GOOD;
            idx++;
        }
        for (int i = 0; i < hunterCount; i++) {
            roles[idx] = "Hunter";
            camps[idx] = CAMP_GOOD;
            idx++;
        }
        for (int i = 0; i < guardCount; i++) {
            roles[idx] = "Guard";
            camps[idx] = CAMP_GOOD;
            idx++;
        }
        for (int i = 0; i < foolCount; i++) {
            roles[idx] = "Fool";
            camps[idx] = CAMP_NEUTRAL;
            idx++;
        }
        for (int i = 0; i < villagerCount; i++) {
            roles[idx] = "Villager";
            camps[idx] = CAMP_GOOD;
            idx++;
        }

        // Shuffle using Fisher-Yates
        for (int i = n - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            String tmpRole = roles[i];
            roles[i] = roles[j];
            roles[j] = tmpRole;
            String tmpCamp = camps[i];
            camps[i] = camps[j];
            camps[j] = tmpCamp;
        }

        // --- Create players ---
        Player[] players = new Player[n];
        for (int i = 0; i < n; i++) {
            players[i] = new Player(i + 1, roles[i], camps[i]);
        }

        // --- Role reveal phase ---
        System.out.println("=== Role Assignment ===");
        System.out.println("Each player take turns to see your role.");

        for (int i = 0; i < n; i++) {
            System.out.println();
            System.out.println("Player " + (i + 1) + ", please press Enter to see your role.");
            sc.nextLine();

            System.out.println("Your Role: " + players[i].getRole() + " | Camp: " + players[i].getCamp());

            System.out.println("Memorize your role, then press Enter to continue.");
            sc.nextLine();

            clearScreen();
        }

        // ==================== Game Loop ====================
        boolean gameOver = false;
        int round = 1;

        while (!gameOver) {

            System.out.println("========================================");
            System.out.println("           Round " + round);
            System.out.println("========================================");
            System.out.println();

            // Reset nightly status
            for (int i = 0; i < n; i++) {
                players[i].setProtected(false);
                players[i].setPoisoned(false);
                players[i].setHealed(false);
            }

            int wolfKillTarget = -1; // Player ID killed by wolves (1-based), -1 if none

            // ===== Night Phase =====
            System.out.println("--- Night Phase ---");
            System.out.println();

            // 1. Guard's turn
            if (guardCount > 0) {
                int guardIdx = findAliveRole(players, "Guard");
                if (guardIdx != -1) {
                    System.out.println("[Guard] Player " + players[guardIdx].getId() + ", it's your turn.");
                    System.out.println("Alive players:");
                    printAlivePlayers(players);
                    System.out.println("Choose a player to protect (enter player number, or 0 to skip):");

                    int protectId = sc.nextInt();
                    sc.nextLine();

                    while (protectId != 0 && !isValidAliveTarget(protectId, players)) {
                        System.out.println("Invalid target. Choose again (or 0 to skip):");
                        protectId = sc.nextInt();
                        sc.nextLine();
                    }

                    if (protectId != 0) {
                        players[protectId - 1].setProtected(true);
                        System.out.println("You chose to protect Player " + protectId + ".");
                    } else {
                        System.out.println("You chose not to protect anyone.");
                    }

                    System.out.println("Press Enter to continue.");
                    sc.nextLine();
                    clearScreen();
                }
            }

            // 2. Werewolves' turn
            System.out.println("[Werewolf] Werewolves, wake up.");
            System.out.println("Alive werewolves:");

            boolean anyWolfAlive = false;
            for (int i = 0; i < n; i++) {
                if (players[i].isAlive() && players[i].getRole().equals("Werewolf")) {
                    System.out.println("  " + players[i].getRoleInfo());
                    anyWolfAlive = true;
                }
            }

            if (anyWolfAlive) {
                System.out.println();
                System.out.println("Alive players:");
                printAlivePlayers(players);

                System.out.println("Werewolves, choose a player to kill:");

                int targetId = sc.nextInt();
                sc.nextLine();

                while (!isValidWolfTarget(targetId, players)) {
                    System.out.println("Invalid target. Choose again:");
                    targetId = sc.nextInt();
                    sc.nextLine();
                }

                wolfKillTarget = targetId;
                System.out.println("Werewolves chose to kill Player " + targetId + ".");
            } else {
                System.out.println("No werewolf alive.");
            }

            System.out.println("Press Enter to continue.");
            sc.nextLine();
            clearScreen();

            // 3. Witch's turn
            if (witchCount > 0) {
                int witchIdx = findAliveRole(players, "Witch");
                if (witchIdx != -1) {
                    System.out.println("[Witch] Player " + players[witchIdx].getId() + ", it's your turn.");

                    // Show who was killed
                    if (wolfKillTarget != -1) {
                        System.out.println("Tonight, Player " + wolfKillTarget + " was attacked by werewolves.");

                        // Heal option
                        if (witchHasHeal) {
                            System.out.println("Do you want to use your HEAL potion to save Player " + wolfKillTarget + "? (yes/no):");
                            String healChoice = sc.nextLine().trim().toLowerCase();
                            if (healChoice.equals("yes")) {
                                players[wolfKillTarget - 1].setHealed(true);
                                witchHasHeal = false;
                                System.out.println("You used the heal potion on Player " + wolfKillTarget + ".");
                            }
                        } else {
                            System.out.println("You have already used your heal potion.");
                        }
                    } else {
                        System.out.println("No one was attacked tonight.");
                    }

                    // Poison option
                    if (witchHasPoison) {
                        System.out.println("Do you want to use your POISON potion? (yes/no):");
                        String poisonChoice = sc.nextLine().trim().toLowerCase();
                        if (poisonChoice.equals("yes")) {
                            System.out.println("Alive players:");
                            printAlivePlayers(players);
                            System.out.println("Choose a player to poison:");

                            int poisonTarget = sc.nextInt();
                            sc.nextLine();

                            while (!isValidAliveTarget(poisonTarget, players) || poisonTarget == players[witchIdx].getId()) {
                                System.out.println("Invalid target. Choose again:");
                                poisonTarget = sc.nextInt();
                                sc.nextLine();
                            }

                            players[poisonTarget - 1].setPoisoned(true);
                            witchHasPoison = false;
                            System.out.println("You poisoned Player " + poisonTarget + ".");
                        }
                    } else {
                        System.out.println("You have already used your poison potion.");
                    }

                    System.out.println("Press Enter to continue.");
                    sc.nextLine();
                    clearScreen();
                }
            }

            // 4. Seer's turn
            if (seerCount > 0) {
                int seerIdx = findAliveRole(players, "Seer");
                if (seerIdx != -1) {
                    System.out.println("[Seer] Player " + players[seerIdx].getId() + ", it's your turn.");
                    System.out.println("Alive players:");
                    printAlivePlayers(players);
                    System.out.println("Choose a player to investigate:");

                    int checkId = sc.nextInt();
                    sc.nextLine();

                    while (!isValidAliveTarget(checkId, players) || checkId == players[seerIdx].getId()) {
                        System.out.println("Invalid target. Choose again:");
                        checkId = sc.nextInt();
                        sc.nextLine();
                    }

                    System.out.println("Player " + checkId + "'s camp is: " + players[checkId - 1].getCamp());

                    System.out.println("Press Enter to continue.");
                    sc.nextLine();
                    clearScreen();
                }
            }

            // ===== Resolve Night Results =====
            System.out.println("--- Night Results ---");

            boolean someoneDied = false;

            // Process wolf kill
            if (wolfKillTarget != -1) {
                Player target = players[wolfKillTarget - 1];
                if (target.isHealed()) {
                    System.out.println("Player " + wolfKillTarget + " was saved by the Witch!");
                } else if (target.isProtected()) {
                    System.out.println("Player " + wolfKillTarget + " was protected by the Guard!");
                } else {
                    target.kill();
                    System.out.println("Player " + wolfKillTarget + " was killed by werewolves.");
                    someoneDied = true;

                    // Hunter's last shot
                    if (target.getRole().equals("Hunter")) {
                        hunterLastShot(players, target);
                    }
                }
            }

            // Process poison kill
            for (int i = 0; i < n; i++) {
                if (players[i].isPoisoned() && players[i].isAlive()) {
                    players[i].kill();
                    System.out.println("Player " + (i + 1) + " was poisoned by the Witch.");
                    someoneDied = true;

                    // Hunter's last shot
                    if (players[i].getRole().equals("Hunter")) {
                        hunterLastShot(players, players[i]);
                    }
                }
            }

            if (!someoneDied) {
                System.out.println("It was a peaceful night. No one died.");
            }

            System.out.println();

            // Check win conditions after night
            String nightWinner = checkWinCondition(players);
            if (nightWinner != null) {
                announceWinner(nightWinner, players);
                break;
            }

            // ===== Day Phase =====
            System.out.println("--- Day Phase ---");
            System.out.println("Surviving players:");
            printAlivePlayers(players);
            System.out.println();

            // Voting
            System.out.println("Each alive player votes to eliminate someone.");
            System.out.println();

            int aliveCount = countAlive(players);
            int[] votes = new int[n]; // vote count for each player (0-indexed)

            for (int i = 0; i < n; i++) {
                if (!players[i].isAlive()) continue;

                System.out.println("Player " + (i + 1) + ", vote to eliminate (enter player number, or 0 to abstain):");

                int voteId = sc.nextInt();
                sc.nextLine();

                while (voteId != 0 && (!isValidAliveTarget(voteId, players) || voteId == (i + 1))) {
                    System.out.println("Invalid vote. Choose again (or 0 to abstain):");
                    voteId = sc.nextInt();
                    sc.nextLine();
                }

                if (voteId != 0) {
                    votes[voteId - 1]++;
                    System.out.println("Player " + (i + 1) + " voted for Player " + voteId + ".");
                } else {
                    System.out.println("Player " + (i + 1) + " abstained.");
                }
            }

            // Find player with most votes
            int maxVotes = 0;
            int eliminatedId = -1;
            boolean tie = false;

            for (int i = 0; i < n; i++) {
                if (votes[i] > maxVotes) {
                    maxVotes = votes[i];
                    eliminatedId = i;
                    tie = false;
                } else if (votes[i] == maxVotes && votes[i] > 0) {
                    tie = true;
                }
            }

            System.out.println();
            System.out.println("--- Voting Results ---");

            for (int i = 0; i < n; i++) {
                if (votes[i] > 0) {
                    System.out.println("Player " + (i + 1) + ": " + votes[i] + " vote(s)");
                }
            }

            if (tie || maxVotes == 0) {
                System.out.println("It's a tie or no votes. No one is eliminated.");
            } else {
                Player eliminated = players[eliminatedId];
                eliminated.kill();
                System.out.println("Player " + (eliminatedId + 1) + " was voted out. Role: " + eliminated.getRole());

                // Fool win condition: if Fool is voted out, Fool wins
                if (eliminated.getRole().equals("Fool")) {
                    System.out.println();
                    System.out.println("The Fool has been voted out and wins the game!");
                    System.out.println("=== Game Over ===");
                    printAllRoles(players);
                    break;
                }

                // Hunter's last shot
                if (eliminated.getRole().equals("Hunter")) {
                    hunterLastShot(players, eliminated);
                }
            }

            System.out.println();

            // Check win conditions after day
            String dayWinner = checkWinCondition(players);
            if (dayWinner != null) {
                announceWinner(dayWinner, players);
                break;
            }

            round++;
        }

        sc.close();
    }

    // ==================== Helper Methods ====================

    /**
     * Get configured count for a role from user input.
     */
    public static int getConfiguredCount(String roleName, int min, int max) {
        System.out.println("Enter number of " + roleName + " (" + min + "-" + max + "):");
        int count = sc.nextInt();
        sc.nextLine();

        while (count < min || count > max) {
            System.out.println("Invalid. Please enter a number between " + min + " and " + max + ":");
            count = sc.nextInt();
            sc.nextLine();
        }

        return count;
    }

    /**
     * Find the index of the first alive player with a given role.
     * Returns -1 if not found.
     */
    public static int findAliveRole(Player[] players, String role) {
        for (int i = 0; i < players.length; i++) {
            if (players[i].isAlive() && players[i].getRole().equals(role)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Print all alive players' public info.
     */
    public static void printAlivePlayers(Player[] players) {
        for (int i = 0; i < players.length; i++) {
            if (players[i].isAlive()) {
                System.out.println("  " + players[i].getPublicInfo());
            }
        }
    }

    /**
     * Check if a target ID is a valid alive player.
     */
    public static boolean isValidAliveTarget(int targetId, Player[] players) {
        if (targetId < 1 || targetId > players.length) return false;
        return players[targetId - 1].isAlive();
    }

    /**
     * Check if a target is valid for werewolf kill (alive and not a werewolf).
     */
    public static boolean isValidWolfTarget(int targetId, Player[] players) {
        if (targetId < 1 || targetId > players.length) return false;
        if (!players[targetId - 1].isAlive()) return false;
        if (players[targetId - 1].getRole().equals("Werewolf")) return false;
        return true;
    }

    /**
     * Count alive players.
     */
    public static int countAlive(Player[] players) {
        int count = 0;
        for (Player p : players) {
            if (p.isAlive()) count++;
        }
        return count;
    }

    /**
     * Check win condition.
     * Returns "Good" if all werewolves are dead.
     * Returns "Bad" if werewolves >= good-camp alive players.
     * Returns null if game continues.
     */
    public static String checkWinCondition(Player[] players) {
        int wolfAlive = 0;
        int goodAlive = 0;

        for (Player p : players) {
            if (p.isAlive()) {
                if (p.getRole().equals("Werewolf")) {
                    wolfAlive++;
                } else if (p.getCamp().equals(CAMP_GOOD)) {
                    goodAlive++;
                }
                // Neutral camp players (Fool) don't count for either side
            }
        }

        // Good wins: all werewolves dead
        if (wolfAlive == 0) {
            return CAMP_GOOD;
        }

        // Bad wins: werewolves >= good camp alive
        if (wolfAlive >= goodAlive) {
            return CAMP_BAD;
        }

        return null;
    }

    /**
     * Announce the winner and print all roles.
     */
    public static void announceWinner(String winnerCamp, Player[] players) {
        System.out.println();
        if (winnerCamp.equals(CAMP_GOOD)) {
            System.out.println("Good camp (Villagers) wins!");
        } else if (winnerCamp.equals(CAMP_BAD)) {
            System.out.println("Bad camp (Werewolves) wins!");
        }
        System.out.println("=== Game Over ===");
        printAllRoles(players);
    }

    /**
     * Print all players' roles (end-of-game reveal).
     */
    public static void printAllRoles(Player[] players) {
        System.out.println();
        System.out.println("=== All Player Roles ===");
        for (Player p : players) {
            String status = p.isAlive() ? "Alive" : "Dead";
            System.out.println("  Player " + p.getId() + " | " + p.getRole() + " (" + p.getCamp() + ") | " + status);
        }
    }

    /**
     * Hunter's last shot: when Hunter dies, they can shoot one alive player.
     */
    public static void hunterLastShot(Player[] players, Player hunter) {
        System.out.println();
        System.out.println("[Hunter] Player " + hunter.getId() + " (Hunter) has died!");
        System.out.println("Hunter can take a last shot. Alive players:");
        printAlivePlayers(players);
        System.out.println("Choose a player to shoot (or 0 to skip):");

        int shootId = sc.nextInt();
        sc.nextLine();

        while (shootId != 0 && (!isValidAliveTarget(shootId, players) || shootId == hunter.getId())) {
            System.out.println("Invalid target. Choose again (or 0 to skip):");
            shootId = sc.nextInt();
            sc.nextLine();
        }

        if (shootId != 0) {
            players[shootId - 1].kill();
            System.out.println("Hunter shot Player " + shootId + "! Player " + shootId + " is dead. Role: " + players[shootId - 1].getRole());
        } else {
            System.out.println("Hunter chose not to shoot anyone.");
        }
    }

    /**
     * Clear screen by printing blank lines.
     */
    public static void clearScreen() {
        for (int i = 0; i < 30; i++) {
            System.out.println();
        }
    }
}
