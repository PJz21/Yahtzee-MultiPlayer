import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class YahtzeeServerThread extends Thread {

    private Socket yahtzeeSocket;
    private SharedYahtzeeState mySharedYahtzeeStateObject;
    private int myYahtzeeServerThreadNumber;

    //Setup the thread
    public YahtzeeServerThread(Socket yahtzeeSocket, int YahtzeeServerThreadNumber, SharedYahtzeeState SharedObject) {
        this.yahtzeeSocket = yahtzeeSocket;
        mySharedYahtzeeStateObject = SharedObject;
        myYahtzeeServerThreadNumber = YahtzeeServerThreadNumber;
    }

    public void run() {
        try {
            System.out.println("Thread " + myYahtzeeServerThreadNumber + " initialising.");
            PrintWriter out = new PrintWriter(yahtzeeSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(yahtzeeSocket.getInputStream()));
            out.println("Welcome to multi player Yahtzee!");
            out.println("You are Player " + myYahtzeeServerThreadNumber + "!");

            int round = 1;
            int[][] currentScoreRecord = new int[][]{{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}};

            int numberOfRounds = 13;

            while (round < numberOfRounds + 1) {

                try {
                    int[] theDice = new int[]{0, 0, 0, 0, 0};
                    int[][] canScoreThisRound;

                    // Show scoreboard
                    String currentScoreBoard;
                    currentScoreBoard = showCurrentScoreBreakdown(currentScoreRecord);
                    out.println("Round: " + round);
                    out.println(currentScoreBoard);

                    // Roll the dice
                    for (int i = 0; i < 5; i++) {
                        theDice[i] = die();
                    }

                    // See what you have rolled
                    out.println(showDice(theDice));

                    out.println("Three chances to re-roll");

                    int noRolls = 0;
                    int temp;
                    boolean reroll = true;
                    int[] rerollDice = new int[5];
                    int rerollDie;


                    rerollDie = 1;
                    while (reroll) {
                        noRolls++;
                        if (rerollDie > 0) {
                            out.println("How many dice do you want to re-roll? (1-5 - 0 for no dice)");
                            out.println("EOT");
                            rerollDie = Integer.parseInt(in.readLine().trim());
                            while (!checkInput(rerollDie, 0, 5)) {
                                out.println("Please choose a number between 0 and 5");
                                out.println("EOT");
                                rerollDie = Integer.parseInt(in.readLine().trim());
                            }

                            if (rerollDie > 0) {
                                for (int i = 0; i < rerollDie; i++) {
                                    out.println("Select a die (1-5)");
                                    out.println("EOT");
                                    temp = Integer.parseInt(in.readLine().trim());
                                    while (!checkInput(temp, 1, 5)) {
                                        out.println("Please choose a number between 1 and 5");
                                        out.println("EOT");
                                        temp = Integer.parseInt(in.readLine().trim());
                                    }
                                    rerollDice[i] = temp - 1; //adjust for array index
                                }
                                for (int i = 0; i < rerollDie; i++) {
                                    theDice[rerollDice[i]] = die();
                                }
                                out.println(showDice(theDice));
                            }
                        } else {
                            reroll = false;
                        }
                        if (noRolls == 3) {
                            reroll = false;
                        }
                    }

                    canScoreThisRound = whatCanBeScored(currentScoreRecord, theDice);
                    out.println(showWhatToScore(currentScoreRecord, canScoreThisRound));
                    out.println("Choose one choice: ");
                    out.println("EOT");

                    currentScoreRecord = chooseWhatToScore(canScoreThisRound, Integer.parseInt(in.readLine().trim()));

                    mySharedYahtzeeStateObject.acquireLock();
                    mySharedYahtzeeStateObject.updateScoreBoard(myYahtzeeServerThreadNumber, showCurrentOverallScore(currentScoreRecord), round);
                    mySharedYahtzeeStateObject.releaseLock();

                    out.println("Waiting for opponents to finish round...");
                    out.println();

                    mySharedYahtzeeStateObject.roundCompleted();

                    if (round == numberOfRounds) {
                        out.println("Final scoreboard");
                        out.println("Winner is player " + mySharedYahtzeeStateObject.winner() + "!!!");
                    }

                    out.println(formatScoreBoard(mySharedYahtzeeStateObject.getScoreBoard()));

                    round = mySharedYahtzeeStateObject.getScoreBoard().get(myYahtzeeServerThreadNumber).get(0);
                }
                catch(InterruptedException e) {
                    System.err.println("Failed to get lock when reading:"+e);
                }
            }

            out.close();
            in.close();
            yahtzeeSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int die() {
        Random r = new Random();
        return r.nextInt(6)+1;
    }

    private String showDice(int[] theseDice) {
        return ("You rolled: " + theseDice[0] + " " + theseDice[1] + " " + theseDice[2] + " " + theseDice[3] + " " + theseDice[4]);
    }

    private String showCurrentScoreBreakdown(int[][] currentScoreRecord) {
        //Scoring Y FH LS SS 4K 3K On Tw Th Fo Fi Si C

        StringBuilder output = new StringBuilder();

        String[] options = {"Yahtzee", "Full-House", "Long-Straight", "Short-Straight", "Quad", "Triple", "Ones", "Twos", "Threes", "Fours", "Fives", "Sixes", "Chance"};

        output.append("Your current scoring status is:").append(System.lineSeparator());

        //Show what's been scored
        for (int i=0; i<13; i++) {
            output.append(options[i]).append(" scoring ").append(currentScoreRecord[i][1]).append(" points").append(System.lineSeparator());
        }

        return output.toString();
    }

    private synchronized int showCurrentOverallScore(int[][] currentScoreRecord) {

        int score = 0;

        //Calculate current score
        for (int[] ints : currentScoreRecord) {
            score = score + ints[1];
        }

        return score;
    }

    private int[][] whatCanBeScored(int[][] currentScoreRecord, int[] theDice) {

        //Scoring Y FH LS SS 4K 3K On Tw Th Fo Fi Si C

        int[][] canScoreThisRound = new int[13][2];
        int count;
        int score;
        Arrays.sort(theDice);

        //Check the number scores
        //Check first if the number has been scored
        //If there is a score possible then note it

        // Check for Yahtzee
        if (currentScoreRecord[0][0] == 0) {

            count = 0;
            for (int value : theDice) {
                if (theDice[0] != value) {
                    count++;
                }
            }

            if (count == 0){
                canScoreThisRound[0][0] = 1;
                canScoreThisRound[0][1] = 50;
            }
        }

        // Check full house
        if (currentScoreRecord[1][0] == 0) {

            if (checkDuplicates(theDice, 2) == 3) {

                if (checkDuplicates(theDice, 1) == 2 || checkDuplicates(theDice, 3) == 2) {
                    canScoreThisRound[1][0] = 1;
                    canScoreThisRound[1][1] = 25;
                }
            }
        }

        // Check long straight
        if (currentScoreRecord[2][0] == 0) {

            if (checkStraights(theDice) == 4){
                canScoreThisRound[2][0] = 1;
                canScoreThisRound[2][1] = 40;
            }
        }

        // Check short straight
        if (currentScoreRecord[3][0] == 0) {

            if(checkStraights(theDice) == 3) {
                canScoreThisRound[3][0] = 1;
                canScoreThisRound[3][1] = 30;
            }
        }


        //4 of a kind
        if (currentScoreRecord[4][0] == 0) {

            if (checkDuplicates(theDice, 2) == 4) {
                score = 0;
                for (int value : theDice) {
                    score = score + value;
                }
                canScoreThisRound[4][0] = 1;
                canScoreThisRound[4][1] = score;
            }

        }

        //3 of a kind
        if (currentScoreRecord[5][0] == 0) {

            if (checkDuplicates(theDice, 2) == 3) {
                score = 0;
                for (int value : theDice) {
                    score = score + value;
                }
                canScoreThisRound[5][0] = 1;
                canScoreThisRound[5][1] = score;
            }

        }

        //Check 1s to 6s
        for (int i = 6; i < 12; i++) {
            if (currentScoreRecord[i][0] == 0) {

                int diceNumber = i - 5;

                canScoreThisRound[i][0] = 1;
                canScoreThisRound[i][1] = checkSameNumber(diceNumber, theDice);

            }
        }

        //Check chance
        if (currentScoreRecord[12][0] == 0) {
            canScoreThisRound[12][0] = 1;
            canScoreThisRound[12][1] = 0;
            for (int value : theDice) {
                canScoreThisRound[12][1] = canScoreThisRound[12][1] + value;
            }
        }

        return canScoreThisRound;
    }

    private int checkSameNumber(int numberToCheck, int[] theDice) {
        int diceNumber = 0;

        for (int value : theDice) {
            if (value == numberToCheck) {
                diceNumber = diceNumber + numberToCheck;
            }

        }

        return diceNumber;
    }

    private int checkDuplicates(int[] theDice, int positionInArray) {

        int duplicate = 0;

        for (int value : theDice) {
            if (theDice[positionInArray] == value) {
                duplicate += 1;

            }
        }
        return duplicate;
    }

    private int checkStraights(int[] theDice) {

        int consecutiveNumber = 1;
        int tmp = 0;

        for (int i = 0; i < theDice.length - 1; i++) {
            if (theDice[i + 1] - theDice[i] == 1) {
                consecutiveNumber += 1;
                if (consecutiveNumber > 2) {
                    tmp = consecutiveNumber;
                }
            } else if (theDice[i + 1] - theDice[i] > 1) {
                consecutiveNumber = 1;
            }
        }

        if (tmp > 2){
            consecutiveNumber = tmp;
        }

        return consecutiveNumber;
    }

    private String showWhatToScore(int[][] currentScoreRecord, int [][]canScoreThisRound) {
        //Scoring Y FH LS SS 4K 3K On Tw Th Fo Fi Si C

        String[] options = {"Yahtzee", "Full-House", "Long-Straight", "Short-Straight", "Quad", "Triple", "Ones", "Twos", "Threes", "Fours", "Fives", "Sixes", "Chance"};
        StringBuilder output = new StringBuilder();

        output.append("With your roll you can select...").append(System.lineSeparator());
        //Present choices - check if it has been scored and if it can be scored
        for (int i=0; i<13; i++) {
            if ((currentScoreRecord[i][0] == 0) && (canScoreThisRound[i][0] == 1)){
                output.append("Select ").append(i).append(" for ").append(options[i]).append(" scoring ").append(canScoreThisRound[i][1]).append(" points").append(System.lineSeparator());
            }
        }

        return output.toString();
    }

    private int[][] chooseWhatToScore(int[][] canScoreThisRound, int userChoice) {

        int[][] newScoreRecord = new int[13][2];

        //Choose and update score
        newScoreRecord[userChoice][0] = 1;
        newScoreRecord[userChoice][1] = canScoreThisRound[userChoice][1];

        return newScoreRecord;
    }

    private String formatScoreBoard(ArrayList<ArrayList<Integer>> scoreBoard) {

        StringBuilder output = new StringBuilder();

        output.append("--------------").append("--------------".repeat(scoreBoard.size())).append(System.lineSeparator());
        output.append("|   " + "Player" + "   |");

        for (int i = 0; i < scoreBoard.size(); i++) {
            if (i == myYahtzeeServerThreadNumber) {
                output.append(fixedLengthString(Integer.toString(i + 1), 7)).append(" *    |");
            } else {
                output.append(fixedLengthString(Integer.toString(i + 1), 7)).append("      |");
            }
        }

        output.append(System.lineSeparator()).append("--------------").append("--------------".repeat(scoreBoard.size())).append(System.lineSeparator());
        output.append("|   " + "Score" + "    |");

        for (ArrayList<Integer> integers : scoreBoard) {
            output.append(fixedLengthString(Integer.toString(integers.get(1)), 7)).append("      |");
        }

        output.append(System.lineSeparator()).append("--------------").append("--------------".repeat(scoreBoard.size())).append(System.lineSeparator());

        return output.toString();

    }

    private String fixedLengthString(String string, int length) {
        return String.format("%1$"+length+ "s", string);
    }

    private boolean checkInput(int input, int lowerBound, int upperBound) {

        return (lowerBound <= input && upperBound >= input);

    }

}