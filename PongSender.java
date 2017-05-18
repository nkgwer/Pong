package work.java.pong;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

/* 送信用クラス */
public final class PongSender {
    private PongServer ps;
    private Socket s;
    private BufferedWriter bfw;

    private PongSender (PongServer nps, Socket ns, BufferedWriter nbfw) {
        this.ps = nps;
        this.s = ns;
        this.bfw = nbfw;
    }

    public static PongSender createSender(PongServer ps, Socket socket)
    throws IOException {
        OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream());
        BufferedWriter bfw = new BufferedWriter(osw); // データ送信用バッファの設定

        PongSender pongSender = new PongSender(ps, socket, bfw);
        System.out.println("送信用バッファ設定完了: " + socket);
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
            this.bfw.close();
        } catch (IOException ioe) {
            // Do Nothing.
        } finally {
            this.bfw = null;
        }
    }
}