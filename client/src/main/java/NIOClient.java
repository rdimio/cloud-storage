import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

public class NIOClient implements Runnable {

    private SocketChannel sc;
    private ByteBuffer buffer;

    public NIOClient() throws IOException {
        sc = SocketChannel.open();

        buffer = ByteBuffer.allocate(256);
    }

    @Override
    public void run() {
        try {
            sc.connect(new InetSocketAddress("localhost",8189));
            while (sc.isOpen()) {
                System.out.println("Connection Set:  " + sc.getRemoteAddress());
                Path path = Paths.get("C:\\Users\\Vadim\\Documents\\CloudStorage\\client\\1.txt");
                FileChannel fileChannel = FileChannel.open(path,
                        EnumSet.of(StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING,
                                StandardOpenOption.WRITE)
                    );
                while(sc.read(buffer) > 0) {
                    buffer.flip();
                    fileChannel.write(buffer);
                    buffer.clear();
                }
                fileChannel.close();
                sc.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
