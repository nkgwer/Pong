import java.util.StringTokenizer;
import java.io.*;
import java.net.*;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import javax.swing.*;

public class PongServer {
    public static final int PORT = 8080; // ポート番号を設定する．
    private static final int MIN_PORT = 1024; // 設定できる最小のポート番号
    private static final int MAX_PORT = 65535; // 設定できる最大のポート番号
    private static final int INVALID_PORT_NUMBER = -1;
    private boolean isInitialized;
    StartFrameS sf;
    GameFrameS gf;
    String usrname;
    Integer Number;
    private SocketConnector sConnector;
    private Socket[] socket;
    private PongReceiver[] pongReceiver;
    private PongSender[] pongSender;

    PongServer() {
        this.sf = new StartFrameS(this);
        this.isInitialized = false;
    }

    public static void main(String[] args) throws Exception {
        PongServer ps = new PongServer();
        ps.initialize();
        ps.waitBtnPushed();

        ps.usrname = ps.sf.textField1.getText(); // ユーザーネーム
        ps.Number = Integer.parseInt((String) ps.sf.textField2.getSelectedItem()); // 対戦人数

        // サーバーソケットの作成
        ps.startServer(args, ps.Number - 1);

        try {
            ps.sf.label.setText("対戦相手を受付中...");
            while (ps.sConnector.getNumberOfSocket() < ps.Number - 1) {
                Thread.sleep(10);
            }
            System.out.println("あと" + 0 + "人");
            ps.sf.isAccept = false;
            ps.sf.label.setText("ゲームの準備中...");

            String[] str = new String[ps.Number - 1];
            int i;

            try {
                for (i = 0; i < ps.Number - 1; i++) {

                }
                ps.sf.logAppendln("ゲームの準備中...");
                for (i = 0; i < ps.Number - 1; i++) {
                    ps.pongSender[i].send(ps.Number.toString());
                }
                Thread.sleep(1000);
                for (i = 0; i < ps.Number - 1; i++) {
                    ps.pongSender[i].send("Close Start Frame");
                }

                ps.gf = new GameFrameS();
                System.out.println("Closing: スタート画面");
                ps.sf.setVisible(false);
                System.out.println("Opening: ゲーム画面");
                ps.gf.setVisible(true);

                // スタートボタンが押されて無効になるまで待つ
                while (ps.gf.btn.isEnabled()) {
                    Thread.sleep(10);
                }
                while (true) {
                    int x = 0, vx = 0, vy = 0;

                    // ボールが自分のフィールドから出ない間待つ。
                    while (ps.gf.isBallHere) {
                        Thread.sleep(10);
                    }

                    x = ps.gf.ball.x;
                    vx = ps.gf.ball.getVX();
                    vy = ps.gf.ball.getVY();

                    for (i = 0; i < ps.Number - 1; i++) {
                        int[] place = new int[3];
                        // 上に行ったボールの位置と速度を送信する。
                        ps.sendPlaceVelocity(i, x, vx, vy);

                        // str[i] = null; ps.pongReceiver[i].line = null;
                        // while ( str[i] == null ) {
                        //     str[i] = ps.pongReceiver[i].line;
                        // }
                        System.out.println("受信: \"" + str[i] + "\" from " + ps.socket[i].getRemoteSocketAddress());
                        StringTokenizer st = new StringTokenizer(str[i], " ");
                        for (int j = 0; j < 3; j++) {
                            place[j] = Integer.parseInt(st.nextToken());
                        }
                        x = place[0];
                        vx = place[1];
                        vy = place[2];
                    }
                    ps.gf.ball.setLocation(ps.gf.d.width - (x - ps.gf.ball.width), 1);
                    ps.gf.ball.setVX(- vx);
                    ps.gf.ball.setVY((int) Math.abs(vy));
                    ps.gf.isBallHere = true;
                }

                // Thread.sleep(10000);
            } finally {
                System.out.println("closing...");
                for (i = 0; i < ps.Number - 1; i++)
                    ps.closeSocketStream(i);
            }
        } finally {
            ps.sConnector.terminate();
        }
        // System.exit(0);
    }

    // 初期化
    public void initialize () {
        if (this.isInitialized) {
            return;
        }

        this.isInitialized = true;
        System.out.println("Opening: スタート画面");
        this.sf.setVisible(true);
    }

    // 文字列portSからポート番号を取得
    private int getPortNumber(String portS) {
        int portNumber = INVALID_PORT_NUMBER;
        try {
            portNumber = Integer.parseInt(portS);

            if ((portNumber < MIN_PORT) || (portNumber > MAX_PORT)) {
                portNumber = INVALID_PORT_NUMBER;
            }
        } catch (NumberFormatException nfe) {
            System.err.println("数値への変換に失敗");
        }

        return portNumber;
    }

    // n個以下のクライアントソケットと接続できるサーバーソケットを起動する
    public void startServer(String[] args, int n) {
        int portNumber = PORT;
        if (args.length > 0) {
            portNumber = this.getPortNumber(args[0]); // 引数のポート番号を取得する
        }

        if (portNumber == INVALID_PORT_NUMBER) {
            String msg = "ポート番号は " + MIN_PORT + " から " + MAX_PORT + " までの整数値。";
            System.err.println(msg);
            return;
        }

        try {
            this.sConnector = SocketConnector.createConnector(this, portNumber, n);
        } catch (IOException ioe) {
            String msg = "ポートの確保に失敗しました。";
            System.out.println(msg);

            return;
        }
        this.socket = new Socket[n];
        this.pongReceiver = new PongReceiver[n];
        this.pongSender = new PongSender[n];
        Thread thread = new Thread(this.sConnector);
        thread.start();


        this.sf.isAccept = true;
    }

    // スタート画面のボタンが押されるまで待つ
    public void waitBtnPushed() {
        try {
            while (!this.sf.isBtnPushed) {
                Thread.sleep(10);
            }
        } catch (InterruptedException ire) {
            // Do Nothing.
        }
    }

    // ボールの位置と速度をThread[n % Number]へ送信する。
    public synchronized void sendPlaceVelocity (int n, int x, int vx, int vy) {
        n = n % Number;
        if (n == Number - 1) {
            return;
        }
        pongSender[n].send("Place");
        pongSender[n].send(x + " " + vx + " " + vy);
    }

    public synchronized void terminateConnection (int i) {
        try {
            this.socket[i].close();
        } catch (IOException ioe) {
            // Do Nothing.
        }
    }

    // PongReceiverで受信した文字列に対する処理
    public synchronized void receive(String s, int i) {
        if (this.sf.isAccept == true) {
            this.sf.logAppendln(s);
            this.pongSender[i].send(this.usrname);
        }
    }

    private void closeSocketStream(int i) {
        if (this.pongSender[i] != null) {
            this.pongSender[i].send("END");
            this.pongSender[i].terminate();
            this.pongSender[i] = null;
        }

        if (this.pongReceiver[i] != null) {
            this.pongReceiver[i].terminate();
            this.pongReceiver[i] = null;
        }

        if (this.socket[i] != null) {
            try {
                this.socket[i].close();
                this.sConnector.transNumberOfSocket(-1);
            } catch (IOException ioe) {
                // Do Nothing.
            } finally {
                this.socket[i] = null;
            }
        }
    }

    public boolean acceptSocket(Socket nsocket) {
        boolean isNormalWork = true;
        int i = 0;
        try {
            while (i >= this.Number) {
                if (this.socket[i] != null) break;
                i++;
            }
            if (i >= this.Number) {
                System.err.println("接続数が上限に達しているため, 接続できません。");
                isNormalWork = false;
                return false;
            }
            this.socket[i] = nsocket;
            this.pongReceiver[i] = PongReceiver.createReceiver(this, nsocket, i); // 受信の設定
            this.pongSender[i] = PongSender.createSender(this, nsocket); // 送信の設定
        } catch (IOException ioe) {
            this.socket[i] = null;
            this.pongReceiver[i] = null;
            this.pongSender[i] = null;
            isNormalWork = false;

            return false;
        }

        // 受信中に設定
        this.sConnector.setReceivedNow(true);
        this.sConnector.transNumberOfSocket(1);

        Thread thread = new Thread(this.pongReceiver[i]);
        thread.start();

        return true;
    }
}

// スタート画面: 自分の名前と、対戦人数を指定する。
class StartFrameS extends JFrame implements ActionListener {
    /* スタート画面のタイトル */
    private static final String FRAME_TITLE = "Pong!";
    /* スタート画面のサイズ */
    private static final Dimension FRAME_SIZE = new Dimension(640, 360);
    PongServer ps;

    Container cont;
    JPanel p1, p2, p3;
    JLabel label, label1, label2;
    final JTextField textField1;
    final JComboBox<String> textField2;
    final JTextArea log;
    final JScrollPane scrollpane;
    JButton btn;
    String[] combodata = {"2", "3", "4", "5", "6", "7", "8"};
    Dimension lsize = new Dimension(120, 30);
    boolean isBtnPushed;
    boolean isAccept; // 対戦相手を受付中

    public StartFrameS (PongServer nps) {
        this.ps = nps;
        isBtnPushed = false;
        isAccept = false;

        setTitle(FRAME_TITLE); // タイトルの設定
        setSize(FRAME_SIZE); // サイズの設定
        setResizable(false); //サイズを固定
        setLocationRelativeTo(null); // 位置を中央に設定
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ×を押すとプログラムが終了

        // ラベル
        label = new JLabel("自分の名前と対戦人数を入力してください:");
        label1 = new JLabel("Player's Name:");
        label2 = new JLabel("対戦人数:");
        label1.setPreferredSize(lsize);
        label2.setPreferredSize(lsize);

        // 名前のテキストフィールド, 対戦人数のコンボボックス
        textField1 = new JTextField(); // 名前(ユーザーネーム)のテキストフィールド
        textField2 = new JComboBox<String>(combodata); // 自分も含めた対戦人数のコンボボックス
        textField1.setPreferredSize(lsize);
        textField2.setPreferredSize(lsize);

        // ログ: 通信状態, 参加者を表示
        log = new JTextArea();
        log.setEditable(false);
        scrollpane = new JScrollPane(log);
        scrollpane.setPreferredSize(new Dimension(400, 180));

        // 入力完了ボタン
        btn = new JButton("完了");
        btn.setPreferredSize(new Dimension(60, 30));
        btn.addActionListener(this);


        p3 = new JPanel();
        p3.setLayout(new GridLayout(2, 2));
        p3.add(label1);
        p3.add(textField1);
        p3.add(label2);
        p3.add(textField2);

        p2 = new JPanel();
        p2.setLayout(null);
        p2.add(p3);
        p2.add(scrollpane);
        p3.setBounds(0, 0, 400, 60);
        scrollpane.setBounds(0, 60, 400, 200);

        p1 = new JPanel();
        p1.setLayout(null);
        p1.add(label);
        p1.add(p2);
        p1.add(btn);
        label.setBounds(120, 0, 400, 40);
        p2.setBounds(120, 30, 400, 260);
        btn.setBounds(290, 300, 60, 30);

        cont = getContentPane();
        cont.add(p1, BorderLayout.CENTER);
    }

    // ボタンが押されたときの動作
    public void actionPerformed(ActionEvent e) {
        btn.setEnabled(false);
        isBtnPushed = true;
        label.setText("対戦相手を募集中...");
        textField1.setEditable(false);
        textField2.setEnabled(false);
    }

    // ログに文字列を表示する
    public void logAppendln (String str) {
        System.out.println("ログに表示: " + str);
        this.log.append(str + "\n");
    }
}

class GameFrameS extends JFrame implements ActionListener {
    Container cont;
    JPanel p;
    JButton btn;

    Graphics g;
    final int barV = 2; // barV: バーの横移動の速さ
    Dimension d, v;
    Ball ball; // Rectangle型のサブクラス
    Bar bar;
    boolean left, right, kz, kx;
    // count: ボールがbarとぶつかった回数
    int count = 0, j;
    final int nKEY_LEFT = KeyEvent.VK_LEFT;
    final int nKEY_RIGHT = KeyEvent.VK_RIGHT;

    boolean isBallHere = true;

    public GameFrameS () {
        try {
            setTitle("Pong!"); // タイトルの設定
            setSize(400, 566); // サイズの設定
            setResizable(false); //サイズを固定
            setLocationRelativeTo(null); // 位置を中央に設定
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ×を押すとプログラムが終了

            // 背景は白
            setBackground(Color.WHITE);

            // スタートボタン
            btn = new JButton("Start!");
            btn.setPreferredSize(new Dimension(60, 30));
            btn.addActionListener(this);

            p = new JPanel();
            p.setLayout(null);
            p.add(btn);
            btn.setBounds(110, 200, 180, 120);

            cont = getContentPane();
            cont.add(p, BorderLayout.CENTER);

            d = getSize();

            // ボール 左上角の座標, 幅, 高さ
            ball = new Ball(185, 268, 30, 30);
            ball.setV(new Dimension(1, 1));

            bar = new Bar(150, 461, 100, 10); // 下のバー

            g = getGraphics();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // init();
    }

    public void actionPerformed(ActionEvent e) {
        btn.setEnabled(false);
        btn.setVisible(false);
        init();
    }

    public void init() {
        count = 0; j = 1;
        addKeyListener(new KeyAdapter() {
            public void keyPressed (KeyEvent e) {
                switch (e.getKeyCode()) {
                case nKEY_LEFT : left = true; break;
                case nKEY_RIGHT : right = true; break;
                }
            }
            public void keyReleased (KeyEvent e) {
                switch (e.getKeyCode()) {
                case nKEY_LEFT : left = false; break;
                case nKEY_RIGHT : right = false; break;
                }
            }
        });
        new Timer(10, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // barの動く方向の設定
                bar.setVX(0);
                if (left && !right) bar.setVX(-barV);
                if (right && !left) bar.setVX(barV);
                bar.translate();

                if (isBallHere) {
                    isBallHere = !isCeiling(ball);
                    if (isReboundLeft(ball)) ball.setVX(Math.abs(ball.getVX()));
                    if (isReboundRight(ball)) ball.setVX(- Math.abs(ball.getVX()));
                    if (isReboundx(ball)) ball.BoundX();
                    if (isReboundy(ball)) ball.BoundY();
                    // バーに5回当たると縦の速さが1段階速くなる
                    if (count >= 5 * j && Math.abs(ball.getVY()) < 8) {
                        ball.setVY((int) Math.signum(ball.getVY()) * (Math.abs(ball.getVY()) + 1));
                        j++;
                    }
                    if (ball.getVX() > 8) ball.setVX(8);
                    if (ball.getVX() < -8) ball.setVX(-8);
                    ball.translate();
                }
                repaint();
            }
        }).start();
    }

    public void paint(Graphics g) {
        // 描画
        synchronized (this) {
            g.clearRect(0, 0, d.width, d.height);
        }
        synchronized (this) {
            if (isBallHere) {
                g.setColor(Color.RED);
                g.fillOval(ball.x, ball.y, ball.width, ball.height);
            }
            g.setColor(Color.BLACK);
            g.fillRect(bar.x, bar.y, bar.width, bar.height);
        }
    }

    boolean isCeiling (Ball bl) {
        Rectangle ceiling = new Rectangle(-d.width, -d.height, 3 * d.width, d.height);
        return bl.Next().intersects(ceiling);
    }

    boolean isFloor (Ball bl) {
        Rectangle floor = new Rectangle(-d.width, d.height, 3 * d.width, d.height);
        return bl.Next().intersects(floor);
    }

    boolean isReboundLeft (Ball bl) {
        Rectangle left = new Rectangle(-d.width, -d.height, d.width, 3 * d.height);
        return bl.Next().intersects(left);
    }

    boolean isReboundRight (Ball bl) {
        Rectangle right = new Rectangle(d.width, -d.height, d.width, 3 * d.height);
        return bl.Next().intersects(right);
    }

    boolean isReboundx (Ball bl) {
        boolean b = false;
        if (bl.Next().intersects(bar) && (bl.x + bl.width <= bar.x || bl.x >= bar.x + bar.width)) {
            b = true; ball.setVX(ball.getVX() - bar.getVX()); count++;
        }
        return b;
    }

    boolean isReboundy (Ball bl) {
        boolean b = (bl.y + bl.height >= d.height - 1);
        if (bl.Next().intersects(bar) && (bl.y + bl.height <= bar.y || bl.y >= bar.y + bar.height)) {
            b = true; ball.setVX(ball.getVX() + (int) Math.signum(bar.getVX())); count++;
        }
        return b;
    }
}

/* 受信用クラス */
final class PongReceiver implements Runnable {
    private PongServer ps;

    private BufferedReader bfr;
    private boolean isReading;
    private int i;
    PrintWriter out;

    private PongReceiver (PongServer nps, BufferedReader nbfr, int ni) {
        this.ps = nps;
        this.bfr = nbfr;
        this.i = ni;
        this.isReading = true;
    }

    public static PongReceiver createReceiver(PongServer s, Socket socket, int i)
    throws IOException {
        BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream())); // データ受信用バッファの設定
        PongReceiver pongReceiver = new PongReceiver(s, in, i);
        System.out.println("受信用バッファ設定完了: " + socket);
        return pongReceiver;
    }

    public void run() {

        String line = null;

        while (this.getReading()) {
            if (this.bfr == null) {
                this.ps.terminateConnection(i);
                break;
            }

            try {
                line = this.bfr.readLine(); // データの受信
                System.out.println("受信: \"" + line/* + "\" from " + socket.getRemoteSocketAddress()*/);
            } catch (IOException ie) {
                this.ps.terminateConnection(i);
                break;
            }

            // 相手から接続が切れた場合
            if (line == null) {
                this.ps.terminateConnection(i);
                break;
            }

            // "END"を受信した場合
            if (line.equals("END")) {
                this.ps.terminateConnection(i);
                break;
            }
            this.ps.receive(line, this.i);
        }

        // 終了処理
        if (this.bfr != null) {
            try {
                System.out.println("closing..." + this.bfr);
                this.bfr.close();
            } catch (IOException ioe) {
                // Do nothing.
            } finally {
                this.bfr = null;
            }
        }
    }

    // 終了状態に設定する。
    public synchronized void terminate() {
        this.isReading = false;
    }

    // 受信しているか(true)終了状態か(false)調べる。
    public synchronized boolean getReading() {
        return this.isReading;
    }
}