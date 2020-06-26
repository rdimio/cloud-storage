import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class NIOServer implements Runnable {

    private ServerSocketChannel ssc;
    private Selector selector;
    private ByteBuffer buffer = ByteBuffer.allocate(256);
    private static int clientCount = 0;

    public NIOServer() throws IOException {
        ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(8189));
        ssc.configureBlocking(false);
        selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        try {
            System.out.println("Server started on port: 8189.");
            Iterator<SelectionKey> iterator;
            SelectionKey key;
            while (ssc.isOpen()) {
                int eventsCount = selector.select();
                System.out.println("Selected " + eventsCount + " events.");
                iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        handleAccess(key);
                    }
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                    if(key.isWritable()) {
                        handleWrite(key);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder message = new StringBuilder();
        int read = 0;
        buffer.rewind();
        while ((read = channel.read(buffer)) > 0) {
            buffer.flip();
            byte[] bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            message.append(new String(bytes));
            buffer.rewind();
        }
        if (read < 0) {
            System.out.println(key.attachment() + ": leave!");
            for (SelectionKey send : key.selector().keys()) {
                if (send.channel() instanceof SocketChannel && send.isReadable()) {
                    ((SocketChannel) send.channel()).write(ByteBuffer.wrap((key.attachment() + ": leave!").getBytes()));
                }
            }
            channel.close();
        } else {
            //message.deleteCharAt(message.length()-1);
            System.out.println(key.attachment() + ": " + message);
            String msg = key.attachment() + ": " + message;
            for (SelectionKey send : key.selector().keys()) {
                if (send.channel() instanceof SocketChannel && send.isReadable()) {
                    ((SocketChannel) send.channel()).write(ByteBuffer.wrap(msg.getBytes()));
                }
            }
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        System.out.println("open file");
        Path path = Paths.get("C:\\Users\\Vadim\\Documents\\CloudStorage\\server\\data\\1.txt");
        FileChannel fileChannel = FileChannel.open(path);
        System.out.println("start sending");
        while(fileChannel.read(buffer) > 0) {
            buffer.flip();
            sc.write(buffer);
            buffer.clear();
        }
        fileChannel.close();
        sc.close();
        System.out.println("file is sent");
    }

    private void handleAccess(SelectionKey key) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        clientCount++;
        String userName = "user#" + clientCount;
        channel.configureBlocking(false);
//        channel.register(selector, SelectionKey.OP_READ, userName);
//        channel.write(ByteBuffer.wrap(("Hello " + userName + "!").getBytes()));
        System.out.println("Client " + userName + " connected from ip: " + channel.getLocalAddress());
        channel.register(selector, SelectionKey.OP_WRITE, userName);
    }

    public void stop() {
        try {
            ssc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
