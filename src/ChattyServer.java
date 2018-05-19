import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

//IP Detection

public class ChattyServer {
    private ServerSocket serverSocket;
    private HashMap<String, ChattyServerThread> users = new HashMap<>();

    public ChattyServer(String port) throws ServerException{
        try{
            this.serverSocket= new ServerSocket(Integer.parseInt(port));
        }
        catch(NumberFormatException ex){
            throw new ServerException("Error: Port number must be numerical!");
        }
        catch (IOException ex){
            throw new ServerException("Error: Server could not be created!");
        }
    }

    public Socket acceptIncomingConnections() throws ServerException{
        try {
            return this.serverSocket.accept();
        }
        catch(IOException ex){
            throw new ServerException("Server connection error!");
        }
    }



    public static void main(String[] args) throws ServerException{
        if (args.length>1){
            throw new ServerException("ChattyServer (port #)");
        }
        try{
            ChattyServer chattyServer = new ChattyServer(args[0]);
            chattyServer.init();
        }
        catch(NumberFormatException ex){
            throw new ServerException("Error: Port number must be numerical!");
        }
    }

    public void init() throws ServerException{
        while (true){
            Thread temp = new Thread(new ChattyServerThread(this.acceptIncomingConnections(), users));
            temp.start();
        }
    }
}