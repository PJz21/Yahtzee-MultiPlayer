import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class YahtzeeServer {
  public static void main(String[] args) throws IOException {

    ServerSocket yahtzeeServerSocket = null;
    String YahtzeeServerName = "YahtzeeServer";
    int YahtzeeServerPort = 4545;

    int arrayElements = 2;
    ArrayList<ArrayList<Integer>> globalScoreboard = new ArrayList<>(arrayElements);

    ArrayList<Integer> blankScore = new ArrayList<>(arrayElements);
    blankScore.add(1);
    blankScore.add(0);

    //Create the shared object in the global scope...
    SharedYahtzeeState yahtzeeScoreBoardObject = new SharedYahtzeeState(globalScoreboard);

    // Make the server socket
    try {
      yahtzeeServerSocket = new ServerSocket(YahtzeeServerPort);
      System.out.println("Yahtzee server details: " + InetAddress.getLocalHost() + " Port: " + YahtzeeServerPort);
    } catch (IOException e) {
      System.err.println("Could not start " + YahtzeeServerName + " on port " + YahtzeeServerPort);
      System.exit(-1);
    }

    System.out.println(YahtzeeServerName + " started");

    int i = 0;
    while (!yahtzeeServerSocket.isClosed()) {
      new YahtzeeServerThread(yahtzeeServerSocket.accept(), i, yahtzeeScoreBoardObject).start();
      globalScoreboard.add(new ArrayList<>(blankScore));
      i++;
    }

    yahtzeeServerSocket.close();
  }
}