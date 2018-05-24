import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChattyServer {
    private ServerSocket serverSocket;
    private Socket client;
    private HashMap<String, ChattyServerThread> users = new HashMap<>();
    private ArrayList<String> admins = new ArrayList<>();
    private ArrayList<String> banned_ips = new ArrayList<>();
    private ArrayList<String> banned_users = new ArrayList<>();


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
            client = this.serverSocket.accept();
            if (banned_ips.contains(client.getInetAddress().getHostAddress())){
                throw new ServerException("IP is banned");
            }
            return client;
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
        return null;
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

    public void init(){
        loadFile();
        while (true){
            try {
                Thread temp = new Thread(new ChattyServerThread(this.acceptIncomingConnections(), this));
                temp.start();
            }
            catch (ServerException ex){
                try {
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                    out.write(("You are banned on this server!" + "\n"));
                    out.flush();
                } catch (IOException xe) {
                    xe.printStackTrace();
                }
            }
        }
    }

    public void fileParser(BufferedReader fileLoader, String keyword, ArrayList<String> container){
        try{
            String line;
            while (true) {
                line = fileLoader.readLine();
                if (line==null || (line = line.toLowerCase()).equals(keyword))
                    break;
                container.add(line);
            }
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }

    public void loadFile(){
        try {
            BufferedReader fileLoader = new BufferedReader(new FileReader("userData.txt"));
            fileParser(fileLoader, "banned_ips", admins);
            fileParser(fileLoader, "banned_users", banned_ips);
            fileParser(fileLoader, "", banned_users);
            fileLoader.close();
        }
        catch (FileNotFoundException ex){
            try {
                String template = "admins" + "\n" + "banned_ips" + "\n" + "banned_users";
                BufferedWriter fileWriter = new BufferedWriter(new FileWriter("userData.txt"));
                fileWriter.write(template);
                fileWriter.close();
            }
            catch (IOException xe){
                xe.printStackTrace();
            }
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }

    public void ban(String username, boolean banIP){
        banned_users.add(username);
        updateFile(username, "banned_users");
        if (banIP) {
            banned_ips.add(username);
            updateFile(username, "banned_ips");
        }
    }

    public void updateFile(String update, String keyword){
        ArrayList<String> temp = new ArrayList<>();
        String line;
        try {
            BufferedReader fileLoader = new BufferedReader(new FileReader("userData.txt"));
            Pattern p = Pattern.compile("\\b"+keyword+"\\b", Pattern.CASE_INSENSITIVE);
            while ( (line = fileLoader.readLine()) != null) {
                temp.add(line);
            }
            for (int i = 0; i < temp.size(); i++) {
                Matcher m = p.matcher(temp.get(i));
                if (m.find()){
                    temp.add(i, update+"\n");
                }
            }
            File file = new File("userData.txt");
            file.delete();
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter("userData.txt"));
            for (int i = 0; i < temp.size(); i ++){
                fileWriter.write(temp.get(i));
            }
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }


    public boolean isAdmin(String username){
        return true;
        //return admins.contains(username);
    }

    public HashMap<String, ChattyServerThread> getUsers(){
        return this.users;
    }
}