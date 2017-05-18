import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Pong extends JFrame {
    Graphics g;
    final int N = 2, barV = 2; // barV: バーの横移動の速さ
    Dimension d, v;
    Rectangle ball;
    Rectangle[] bar = new Rectangle[N];
    boolean left, right, kz, kx;
    int[] lr = new int[N];
    // count[i]: ボールがbar[i]とぶつかった回数
    int[] count = new int[]{0, 0};
    int j;
    final int nKEY_LEFT = KeyEvent.VK_LEFT;
    final int nKEY_RIGHT = KeyEvent.VK_RIGHT;
    final int nKEY_Z = KeyEvent.VK_Z;
    final int nKEY_X = KeyEvent.VK_X;
    
    public Pong(){
        this.setTitle("Pong!");
        this.setSize(400, 566);
        d = getSize();
        v = new Dimension(1, 1);
        ball = new Rectangle(185, 268, 30, 30); // ボール 左上角の座標, 幅, 高さ
        bar[0] = new Rectangle(150, 461, 100, 10); // 下のバー
        bar[1] = new Rectangle(150, 95, 100, 10); // 上のバー
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
        this.g = this.getGraphics();
        this.init();
        g.dispose();
    }
    
    public static void main(String[] args) {
        new Pong();
    }
    
    public void init() {
        setBackground(Color.WHITE);
        for (int i = 0; i < N; i++) count[i] = 0;
        j = 1;
        addKeyListener(new KeyAdapter() {
            public void keyPressed (KeyEvent e) {
                switch (e.getKeyCode()) {
                    case nKEY_LEFT : left = true; break;
                    case nKEY_RIGHT : right = true; break;
                    case nKEY_Z : kz = true; break;
                    case nKEY_X : kx = true; break;
                }
            }
            public void keyReleased (KeyEvent e) {
                switch (e.getKeyCode()) {
                    case nKEY_LEFT : left = false; break;
                    case nKEY_RIGHT : right = false; break;
                    case nKEY_Z : kz = false; break;
                    case nKEY_X : kx = false; break;
                }
            }
        });
        new Timer(10, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // lr[i]: bar[i]の動く方向
                for (int i = 0; i < N; i++) lr[i] = 0;
                if (left) lr[0] = -barV;
                if (right) lr[0] = barV;
                if (kz) lr[1] = -barV;
                if (kx) lr[1] = barV;
                for (int i = 0; i < N; i++) bar[i].x += lr[i];
                if (isReboundx(ball)) v.width *= -1;
                if (isReboundy(ball)) v.height *= -1;
                // 2つのバーに5回ずつ当たると縦の速さが1段階速くなる
                if (Math.min(count[0], count[1]) >= 5 * j && Math.abs(v.height) < 8) {
                    v.height = (int) Math.signum(v.height) * (Math.abs(v.height) + 1);
                    j++;
                }
                if (v.width > 8) v.width = 8;
                if (v.width < -8) v.width = -8;
                ball.translate(v.width,v.height);
                repaint();
            }
        }).start();
    }
    
    public void paint(Graphics g) {
        // 描画
        synchronized(this) {
            g.clearRect(0, 0, d.width, d.height);
        }
        synchronized(this) {
            g.setColor(Color.RED);
            g.fillOval(ball.x, ball.y, ball.width, ball.height);
            g.setColor(Color.BLACK);
            for (int i = 0; i < N; i++) {
                g.fillRect(bar[i].x, bar[i].y, bar[i].width, bar[i].height);
            }
            //g.drawString("v_xの絶対値: " + Math.abs(v.width), 10, 50);
            //g.drawString("v_yの絶対値: " + Math.abs(v.height), 10, 65);
            //g.drawString("bar[0]とぶつかった回数: " + count[0], 10, 80);
            //g.drawString("bar[1]とぶつかった回数: " + count[1], 10, 95);
        }
    }
    
    boolean isReboundx (Rectangle bl) {
        boolean b = false;
        if (bl.x <= 0) b = true;
        if (bl.x + bl.width >= d.width - 1) b = true;
        Rectangle next = new Rectangle(bl.x + v.width, bl.y + v.height, bl.width, bl.height);
        for (int i = 0; i < N; i++)
            if (next.intersects(bar[i]) && (bl.x + bl.width <= bar[i].x || bl.x >= bar[i].x + bar[i].width)) {
                b = true; v.width = v.width - lr[i]; count[i]++;
            }
        return b;
    }
    
    boolean isReboundy (Rectangle bl) {
        boolean b = false;
        if (bl.y <= 0) b = true;
        if (bl.y + bl.height >= d.height - 1) b = true;
        Rectangle next = new Rectangle(bl.x + v.width, bl.y + v.height, bl.width, bl.height);
        for (int i = 0; i < N; i++)
            if (next.intersects(bar[i]) && (bl.y + bl.height <= bar[i].y || bl.y >= bar[i].y + bar[i].height)) {
                b = true; v.width = v.width + (int) Math.signum(lr[i]); count[i]++;
            }
        return b;
    }
}
