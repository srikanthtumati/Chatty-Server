import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChattyServerThread implements Runnable {
    private String username;
    private boolean running;
    private Socket client;
    private BufferedReader in;
    private BufferedWriter out;
    private HashMap<String, ChattyServerThread> users;
    private String serverHeader = "Server: ";
    private String SEPERATOR = "/*/";

    public ChattyServerThread(Socket client, HashMap<String, ChattyServerThread> users) {
        this.client = client;
        this.users = users;
        try {
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            this.out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
        } catch (IOException ex) {
            System.err.println("Fatal Error!");
            ex.printStackTrace();
        }
        System.out.println("New User");
        this.running = true;
    }

    public void run() {
        createUser();
        while (running) {
            try{
                String clientResponse = this.in.readLine();
                if (clientResponse != null) {
                    System.out.println(clientResponse);
                    if (clientResponse.substring(0, 1).equals("/")){
                        chatProtocol(clientResponse.substring(1));
                    }
                    else {
                        for (Map.Entry<String, ChattyServerThread> user : users.entrySet()) {
                            user.getValue().pushMessage(this.username + ": " + clientResponse, false);
                        }
                    }
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
        }
        }
    }

    public void pushMessage(String message, Boolean serverMessage) {
        try {
            this.out.write(((serverMessage) ? serverHeader : "") + message + "\n");
            this.out.flush();
        } catch (SocketException ex) {
            this.running = false;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void createUser() {
        try {
            if (username == null) {
                this.pushMessage("Please enter a username", true);
                String clientResponse = this.in.readLine();
                if (users.containsKey(clientResponse) || clientResponse.substring(0, 1).equals("/")) {
                    this.pushMessage("Username already in use or is invalid", true);
                    createUser();
                }
                else{
                    this.username = clientResponse;
                    users.put(this.username, this);
                    this.pushMessage("Username successfully set to " + this.username, true);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void chatProtocol(String clientResponse){
        String[] parsedResponse = clientResponse.split(" ");
        switch(parsedResponse[0].toLowerCase()) {
            case ("w"):
                for (Map.Entry<String, ChattyServerThread> user : users.entrySet()) {
                    if (user.getKey().toLowerCase().equals(parsedResponse[1].toLowerCase())) {
                        user.getValue().pushMessage(this.username + " whispers " + String.join(" ",Arrays.copyOfRange(parsedResponse, 2, parsedResponse.length)), false);
                        break;
                    }
                }
                this.pushMessage("User not found!", true);
                break;
            case ("changeusername"):
                users.remove(this.username);
                this.username=null;
                createUser();
                break;
            case ("list"):
                String usersList = " ";
                for (Map.Entry<String, ChattyServerThread> user : users.entrySet()) {
                    usersList = usersList + user.getKey();
                    this.pushMessage(usersList, true);
                }
        }
    }
}

