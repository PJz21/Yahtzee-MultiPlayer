import java.io.*;
import java.net.*;
import java.util.regex.Pattern;

public class YahtzeeClient {
    public static void main(String[] args) throws IOException {


        BufferedReader yahtzeeClientInput = null;
        PrintWriter yahtzeeClientOutput = null;

        Socket yahtzeeClientSocket = null;
        int YahtzeeSocketNumber = 4545;
        String YahtzeeServerName = "localhost";
        String YahtzeeClientID = "YahtzeeClient";

        // Make the client socket
        try {
            yahtzeeClientSocket = new Socket(YahtzeeServerName, YahtzeeSocketNumber);
            yahtzeeClientOutput = new PrintWriter(yahtzeeClientSocket.getOutputStream(), true);
            yahtzeeClientInput = new BufferedReader(new InputStreamReader(yahtzeeClientSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + YahtzeeServerName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: "+ YahtzeeSocketNumber);
            System.exit(1);
        }

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String fromServer;
        String fromUser;

        System.out.println("Initialised " + YahtzeeClientID + " client and IO connections");
        System.out.println();

        while (yahtzeeClientSocket.isConnected()) {

            while ((fromServer = yahtzeeClientInput.readLine()) != null) {
                if (fromServer.equals("EOT")) {
                    break;
                } else {
                    System.out.println(fromServer);
                }
            }

            fromUser = stdIn.readLine();

            boolean correctInput = Pattern.matches("[0-9]|1[0-2]", fromUser);

            while (!correctInput) {
                correctInput = Pattern.matches("[0-9]|1[0-2]", fromUser);

                if (correctInput) {
                    break;
                }

                System.out.println("Please provide a valid input");
                fromUser = stdIn.readLine();
            }

                if (fromUser != null) {
                    yahtzeeClientOutput.println(fromUser);
                }
        }
        
        yahtzeeClientOutput.close();
        yahtzeeClientInput.close();
        stdIn.close();
        yahtzeeClientSocket.close();
    }
}
