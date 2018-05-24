import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Map;

public class ChattyServerThread implements Runnable {
    private String username;
    private boolean running;
    private Socket client;
    private BufferedReader in;
    private BufferedWriter out;
    private ChattyServer server;
    private String serverHeader = "Server: ";

    public ChattyServerThread(Socket client, ChattyServer server) {
        this.client = client;
        this.server = server;
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
                        if (!running)
                            break;
                        for (Map.Entry<String, ChattyServerThread> user : server.getUsers().entrySet()) {
                            user.getValue().pushMessage(this.username + "> " + clientResponse, false);
                        }
                    }
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        this.pushMessage("Disconnected", true);
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
                if (server.getUsers().containsKey(clientResponse) || clientResponse.substring(0, 1).equals("/") || clientResponse.equals(" ")) {
                    this.pushMessage("Username already in use or is invalid", true);
                    createUser();
                }
                else{
                    this.username = clientResponse;
                    server.getUsers().put(this.username, this);
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
            case ("whisper"):
            case ("w"):
                    if (server.getUsers().containsKey(parsedResponse[1])) {
                        server.getUsers().get(parsedResponse[1]).pushMessage(this.username + " whispers " + String.join(" ",Arrays.copyOfRange(parsedResponse, 2, parsedResponse.length)), false);
                        break;
                    }
                    else {
                        this.pushMessage("User not found!", true);
                    }
                break;
            case ("changeusername"):
                server.getUsers().remove(this.username);
                this.username=null;
                createUser();
                break;
            case ("list"):
                String usersList = " ";
                for (Map.Entry<String, ChattyServerThread> user : server.getUsers().entrySet()) {
                    usersList = usersList + user.getKey();
                    this.pushMessage(usersList, true);
                }
                break;
            case ("k"):
            case ("kick"):
                if (server.isAdmin(this.username)){
                    if (server.getUsers().containsKey(parsedResponse[1])){
                        server.getUsers().get(parsedResponse[1]).running=false;
                        try {
                            server.getUsers().get(parsedResponse[1]).client.close();
                        }
                        catch(IOException ex){
                            ex.printStackTrace();
                        }
                    }
                }
                break;
            case ("ban"):
                this.server.ban(parsedResponse[1], false);
                chatProtocol("/k "+ parsedResponse[1]);
                break;
            case ("ipban"):
                this.server.ban(parsedResponse[1], true);
                chatProtocol("/k "+ parsedResponse[1]);
                break;
        }
    }

}

