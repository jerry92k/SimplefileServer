import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class SimpleFileServer {

    private final int port=7777;
    private final Charset charset = Charset.forName("UTF-8");
    private ServerSocket serverSocket;

    public static void main(String[] args){
       SimpleFileServer simpleFileServer=new SimpleFileServer();
       simpleFileServer.initServer();
       simpleFileServer.startServer();
    }

    private void closeServer() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket listenConnectRequet(ServerSocket serverSocket) throws IOException {
        System.out.println("server listen on port 7777");
        Socket socket = serverSocket.accept();
        System.out.println("[ " + socket.getInetAddress() + " ] client connected");
        return socket;
    }
    public void initServer(){
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                serverSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void startServer() {
        try {
            while(true) {
                try(Socket socket = listenConnectRequet(serverSocket)){
                    String requestMsg =readRequestMessage(socket);
                    handleFileRequest(requestMsg, socket);
                }
                catch (ArrayIndexOutOfBoundsException ex){
                    System.out.println("request 포맷 오류");
                }catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
        finally {
            closeServer();
        }
    }

    private void handleFileRequest(String requestFilePath,Socket socket) throws IOException, NoSuchAlgorithmException {

        String[] splits = requestFilePath.split(" ");

        String pathName = splits[1].substring(1); // 제일 앞에 / 제거
        String fileType = pathName.split("\\.")[1];

        byte[] filedata = readFiles(pathName);

        sendFile(socket,fileType,filedata);
    }

    private void sendFile(Socket socket, String fileType, byte[] filedata) throws IOException, NoSuchAlgorithmException {

        OutputStream output = socket.getOutputStream();

        String decodedData="\n";
        if (fileType.equals("txt") || fileType.equals("java") || fileType.equals("kt") || fileType.equals("kts")
                || fileType.equals("c") || fileType.equals("cpp") || fileType.equals("scala")) {
            decodedData+=new String(filedata);
        } else {
            decodedData+=bytesToStr(filedata);
        }
        decodedData+="\n";
        output.write(charset.encode(decodedData).array());
        sendCheckSum(filedata,output);
    }

    private void sendCheckSum(byte[] filedata, OutputStream output) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(filedata);
        byte[] checkSums=md.digest();

        String checkSumDecoded = bytesToStr(checkSums)+"\n";
        output.write(charset.encode(checkSumDecoded).array());
    }

    private byte[] readFiles(String pathName) throws IOException {
        try (InputStream fileInputStream = new FileInputStream(pathName)) {
            return fileInputStream.readAllBytes();
        }

    }

    private String readRequestMessage(Socket socket) throws IOException {
        InputStream input = socket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String requestMsg = reader.readLine();
        System.out.println(requestMsg);
        return requestMsg;
    }

    public String bytesToStr(byte[] bytes){
        StringBuilder sb= new StringBuilder();

        for(byte b : bytes){
            int i= (int)b;
            if(i<0){
                i=i+256;
            }
            sb.append(String.format("%X",i));
        }
        return sb.toString();
    }

}
