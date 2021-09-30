import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class BlockingIoFileServer {

    private final int port=7777;

    private ServerSocket serverSocket;

    private static final int THREAD_CNT = 100;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_CNT);

    public static void main(String[] args){

        BlockingIoFileServer blockingIoFileServer =new BlockingIoFileServer();
        try {
            blockingIoFileServer.initServer();
            blockingIoFileServer.startServer();
        }
        catch (Exception ex){
            System.out.println(ex);
        }
        finally {
            blockingIoFileServer.closeServer();
        }
    }

    public void closeServer() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Socket listenConnectRequet(ServerSocket serverSocket) throws IOException {
        System.out.println("server listen on port :"+port);
        Socket socket = serverSocket.accept();
        System.out.println("[ " + socket.getInetAddress() + " ] client connected");
        return socket;
    }
    public void initServer() throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void startServer() throws IOException {

        while(true) {

            try{
                Socket socket = listenConnectRequet(serverSocket);
                threadPool.execute(new SocketThread(socket));
            }
            catch (ArrayIndexOutOfBoundsException ex){
                System.out.println("request 포맷 오류");
            }
        }

    }


}
