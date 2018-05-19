public class ServerException extends Exception{
    private String msg;

    public ServerException(String msg){
        super(msg);
        this.msg=msg;
    }

    @Override
    public String toString() {
        return (msg);
    }
}
