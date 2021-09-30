import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SocketThread extends Thread{

    private Socket socket;
    private final Charset charset = Charset.forName("UTF-8");

    public SocketThread(Socket socket) {
        this.socket = socket;
    }

    public void run(){

        while(true){

            try {
                handleFileRequest(readRequestMessage(socket), socket);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
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
