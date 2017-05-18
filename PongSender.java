import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

/* 送信用クラス */
public final class PongSender {
    private PongServer ps;
    private Socket s;
    private BufferedWriter bfw;
    private int i;

    private PongSender (PongServer nps, Socket ns, BufferedWriter nbfw, int ni) {
        this.ps = nps;
        this.s = ns;
        this.bfw = nbfw;
        this.i = ni;
    }

    public static PongSender createSender(PongServer ps, Socket socket, int i)
    throws IOException {
        OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream());
        BufferedWriter bfw = new BufferedWriter(osw); // データ送信用バッファの設定

        PongSender pongSender = new PongSender(ps, socket, bfw, i);
        System.out.println("Complete setting : Sender[" + i + "] = " + pongSender);
        System.out.println("Complete setting : Sending Buffered Writer[" + i + "] = " + bfw);
        return pongSender;
    }

    public boolean send(String string) {
        if (this.bfw == null) {
            return false;
        }

        boolean isNormalWork = true;

        System.out.println("送信: \"" + string + "\" to " + this.s.getRemoteSocketAddress());
        PrintWriter out = new PrintWriter(this.bfw, true);
        out.println(string); // データの送信
        out.close();

        return isNormalWork;
    }

    // 送信用バッファを閉じる
    public void terminate() {
        if (this.bfw == null) {
            return;
        }
        try {
            System.out.println("Closing : Sending Buffered Writer[" + i + "] = " + this.bfw);
            this.bfw.close();
        } catch (IOException ioe) {
            // Do Nothing.
        } finally {
            this.bfw = null;
        }
    }
}