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

public class PongClient {
	StartFrameC sf;
	GameFrame gf;
	int Number;

	PongClient() {
		sf = new StartFrameC();
	}

	public static void main(String[] args) throws Exception {
		PongClient pc = new PongClient();
		System.out.println("Opening: スタート画面");
		pc.sf.setVisible(true);
		while (pc.sf.btn.isEnabled()) {
			Thread.sleep(50);
		}

		String usrname = pc.sf.textField1.getText(); // ユーザーネーム
		String name = pc.sf.textField2.getText(); // 10.9.81.128 など
		pc.sf.textField1.setEditable(false);
		pc.sf.textField2.setEditable(false);

		try {
			InetAddress addr = InetAddress.getByName(name); // IPアドレスへの変換
			System.out.println("addr = " + addr);

			// ソケットの作成
			Socket socket;
			if (args.length > 0) socket = new Socket(addr, Integer.parseInt(args[0]));
			else socket = new Socket(addr, PongServer.PORT);

			try {
				System.out.println("socket = " + socket);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // データ受信用バッファの設定
				PrintWriter out =
				    new PrintWriter(new BufferedWriter(
				                        new OutputStreamWriter(
				                            socket.getOutputStream())
				                    ), true); // 送信バッファ設定

				String str = null;
				System.out.println("Connection accepted: " + socket + "\n接続されました。");

				System.out.println("送信: \"" + usrname + "さんが参加しました。\"");
				out.println(usrname + "さんが参加しました。"); // データ送信

				str = in.readLine();
				System.out.println("ログに表示: " + str + "さんに接続しました。");
				pc.sf.log.append(str + "さんに接続しました。" + "\n");
				System.out.println("ログに表示: 待機中...");
				pc.sf.log.append("待機中...\n");

				str = in.readLine();
				System.out.println("受信: \"" + str + "\" from " + socket.getRemoteSocketAddress());
				pc.Number = Integer.parseInt(str);
				System.out.println("対戦人数: " + pc.Number + "人");

				while (true) {
					int[] place = new int[3];
					str = in.readLine(); // データの受信
					System.out.println("受信: \"" + str + "\" from " + socket.getRemoteSocketAddress());
					if (str.equals("END")) break;
					if (str.equals("Close Start Frame")) {
						pc.gf = new GameFrame();

						System.out.println("Closing: スタート画面");
						pc.sf.setVisible(false);
						System.out.println("Opening: ゲーム画面");
						pc.gf.setVisible(true);
					}
					if (str.equals("Place")) {
						str = in.readLine();
						System.out.println("受信: \"" + str + "\" from " + socket.getRemoteSocketAddress());
						StringTokenizer st = new StringTokenizer(str, " ");
						for (int i = 0; i < 3; i++) {
							place[i] = Integer.parseInt(st.nextToken());
						}
						pc.gf.ball.setLocation(pc.gf.d.width - (place[0] - pc.gf.ball.width), 1);
						pc.gf.ball.setVX(- place[1]);
						pc.gf.ball.setVY((int) Math.abs(place[2]));
						pc.gf.isBallHere = true;

						// ボールが自分のフィールドから出ない間待つ。
						while (pc.gf.isBallHere) {
							Thread.sleep(10);
						}

						// サーバーに上に行ったボールの位置と速度を送信する。
						System.out.println("送信: \"" + pc.gf.ball.x + " " + pc.gf.ball.getVX() + " " + pc.gf.ball.getVY() + "\" to " + socket.getRemoteSocketAddress());
						out.println(pc.gf.ball.x + " " + pc.gf.ball.getVX() + " " + pc.gf.ball.getVY());
					}
					// System.out.println(str);
				}
				out.println("END");
			} catch (IOException ie) {
				ie.printStackTrace();
			} finally {
				try {
					if (socket != null) {
						System.out.println("closing...");
						socket.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("切断されました: " + socket.getRemoteSocketAddress());
			}
		} catch (Exception e) {}
		// pc.sf.setVisible(false);
		// System.exit(0);

		// 画面

	}
}

// スタート画面: 自分の名前とServer(親機)のホスト名/IPアドレスを指定する。
class StartFrameC extends JFrame implements ActionListener {
	/* スタート画面のタイトル */
	private static final String FRAME_TITLE = "Pong!";
	/* スタート画面のサイズ */
	private static final Dimension FRAME_SIZE = new Dimension(640, 360);
	Container cont;
	JPanel p1, p2, p3;
	JLabel label, label1, label2;
	final JTextField textField1, textField2;
	final JTextArea log;
	final JScrollPane scrollpane;
	JButton btn;
	Dimension lsize = new Dimension(120, 30);

	public StartFrameC () {
		setTitle(FRAME_TITLE); // タイトルの設定
		setSize(FRAME_SIZE); // サイズの設定
		setLocationRelativeTo(null); // 位置を中央に設定
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ×を押すとプログラムが終了
		label = new JLabel("自分の名前とホスト名を入力してください:");
		label1 = new JLabel("Player's Name:");
		label2 = new JLabel("ホスト名:");
		label1.setPreferredSize(lsize);
		label2.setPreferredSize(lsize);
		textField1 = new JTextField(); // 名前(ユーザーネーム)のテキストフィールド
		textField2 = new JTextField("localhost"); // 自分も含めた対戦人数のコンボボックス
		textField1.setPreferredSize(lsize);
		textField2.setPreferredSize(lsize);
		log = new JTextArea();
		log.setEditable(false);
		scrollpane = new JScrollPane(log);
		scrollpane.setPreferredSize(new Dimension(400, 180));
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

	public void actionPerformed(ActionEvent e) {
		btn.setEnabled(false);
	}
}

class GameFrame extends JFrame implements ActionListener {
	Container cont;
	JPanel p;
	JButton btn;

	Graphics g;
	final int barV = 2; // barV: バーの横移動の速さ
	Dimension d;
	Ball ball; // Rectangle型のサブクラス
	Bar bar; // Rectangle型のサブクラス
	boolean left, right, kz, kx;
	// count: ボールがbarとぶつかった回数
	int count = 0, j;
	final int nKEY_LEFT = KeyEvent.VK_LEFT;
	final int nKEY_RIGHT = KeyEvent.VK_RIGHT;

	boolean isBallHere = false;

	public GameFrame () {
		try {
			setTitle("Pong!"); // タイトルの設定
			setSize(400, 566); // サイズの設定
			setResizable(false); //サイズを固定
			setLocationRelativeTo(null); // 位置を中央に設定
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ×を押すとプログラムが終了

			// 背景は白
			setBackground(Color.WHITE);

			// スタートボタン
			// btn = new JButton("Start!");
			// btn.setPreferredSize(new Dimension(60, 30));
			// btn.addActionListener(this);

			p = new JPanel();
			p.setLayout(null);
			// p.add(btn);
			// btn.setBounds(110, 200, 180, 120);

			cont = getContentPane();
			cont.add(p, BorderLayout.CENTER);

			d = getSize();

			// ボール 左上角の座標, 幅, 高さ
			ball = new Ball(-30, -30, 30, 30);
			ball.setV(new Dimension(0, 0));

			// バー
			bar = new Bar(150, 461, 100, 10);

			g = getGraphics();
		} catch (Exception e) {
			e.printStackTrace();
		}
		init();
	}

	public void actionPerformed(ActionEvent e) {
		// btn.setEnabled(false);
		// btn.setVisible(false);
		// init();
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
		g.clearRect(0, 0, d.width, d.height);

		if (isBallHere) {
			g.setColor(Color.RED);
			g.fillOval(ball.x, ball.y, ball.width, ball.height);
		}
		g.setColor(Color.BLACK);
		g.fillRect(bar.x, bar.y, bar.width, bar.height);
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
