import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NonBlockingIoFileServer {

    private Selector selector = null;
    private List<SelectionKey> room = new ArrayList<>();

    private final int port=7777;
    private final Charset charset = Charset.forName("UTF-8");
    private ServerSocketChannel serverSocketChannel;

    private final char CR = (char) 0x0D;
    private final char LF = (char) 0x0A;

    public static void main(String[] args){

        NonBlockingIoFileServer nonBlockingIoFileServer =new NonBlockingIoFileServer();
        try {
            nonBlockingIoFileServer.initServer();
            nonBlockingIoFileServer.startServer();
        }
        catch (Exception ex){
            System.out.println(ex);
        }
        finally {
            nonBlockingIoFileServer.closeServer();
        }
    }

    public void initServer() throws IOException {

        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(port));

        // 채널에 Selector를 등록한다.
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("server initialized on port :"+port);

    }

    public void closeServer(){
        try {
            System.out.println("server closed");
            selector.close();
            serverSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void startServer() throws IOException, NoSuchAlgorithmException {

        while (true) {
            // 발생한 이벤트가 있는지 확인한다.
            System.out.println("server is waiting");

                selector.select();
                Set<SelectionKey> selectionKeySet = selector.selectedKeys();

                for (SelectionKey key : selectionKeySet) {
                    if (key.isAcceptable()) {
                        accept(key);
                    }
                    else if (key.isReadable()) {
                        read(key);
                    }
                }

        }
    }

    private void accept(SelectionKey key) throws IOException {
        SocketChannel socketChannel = serverSocketChannel.accept();

        if (null == socketChannel) {
            return;
        }

        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);

        room.add(key); // 연결된 클라이언트 추가하기.
        System.out.println("client is connected :"+socketChannel.toString());
    }

    private void read(SelectionKey key) throws IOException, NoSuchAlgorithmException  {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024); // buffer 생성

        try (SocketChannel socketChannel = (SocketChannel) key.channel()) {
            socketChannel.read(byteBuffer); // 클라이언트 소켓으로부터 데이터를 읽음

            String receiveData=requestDataToStr(byteBuffer);

            System.out.println("Received Data : " + receiveData);
            handleFileRequest(receiveData, socketChannel);
        } finally {
            byteBuffer.clear();
        }
    }

    private String requestDataToStr(ByteBuffer byteBuffer) {
        byteBuffer.flip();
        String receiveData=charset.decode(byteBuffer).toString();

        if (receiveData.length() > 2 &&
                receiveData.charAt(receiveData.length() - 2) == CR
                && receiveData.charAt(receiveData.length() - 1) == LF) {

            receiveData = receiveData.substring(0,receiveData.length()-2);
        }
        return receiveData;
    }

    private void handleFileRequest(String requestFilePath,SocketChannel socket) throws IOException, NoSuchAlgorithmException {

        String[] splits = requestFilePath.split(" ");

        String pathName = splits[1].substring(1); // 제일 앞에 / 제거
        String fileType = pathName.split("\\.")[1];

        byte[] filedata = readFiles(pathName);

        sendFile(socket,fileType,filedata);
    }

    private void sendFile(SocketChannel socketChannel, String fileType, byte[] filedata) throws IOException, NoSuchAlgorithmException {

        String decodedData="\n";
        if (fileType.equals("txt") || fileType.equals("java") || fileType.equals("kt") || fileType.equals("kts")
                || fileType.equals("c") || fileType.equals("cpp") || fileType.equals("scala")) {
            decodedData+=new String(filedata);
        } else {
            decodedData+=bytesToStr(filedata);
        }
        decodedData+="\n";
        socketChannel.write(ByteBuffer.wrap(charset.encode(decodedData).array()));
        sendCheckSum(filedata,socketChannel);

    }

    private void sendCheckSum(byte[] filedata, SocketChannel socketChannel) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(filedata);
        byte[] checkSums=md.digest();

        String checkSumDecoded = bytesToStr(checkSums)+"\n";
        socketChannel.write(ByteBuffer.wrap(charset.encode(checkSumDecoded).array()));
    }

    private byte[] readFiles(String pathName) throws IOException {
        try (InputStream fileInputStream = new FileInputStream(pathName)) {
            return fileInputStream.readAllBytes();
        }
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
