import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.JOptionPane;

public class Client extends Frame implements ActionListener {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private TextField textField;
    private TextArea textArea;
    private Button sent;
    private Panel p;

    // 是否登录成功
    private boolean ent = false;
    // 交互阶段标识：1：输入ID 2：输入密码 3：业务选择
    static private int a = 1;
    // 保存当前用户输入的信息
    static public String message0 = "";

    public Client() {
        super("客户端");

        sent = new Button("sent");
        sent.addActionListener(this);

        p = new Panel();

        textField = new TextField(30);

        textArea = new TextArea(20, 50);
        textArea.append("欢迎使用ATM客户端！(输入“0”登出)" + "\n" + "请输入用户id： \n");
        textArea.setEditable(false);

        // 设置较大的字体，字号20
        Font font = new Font("Dialog", Font.PLAIN, 20);
        textField.setFont(font);
        textArea.setFont(font);
        sent.setFont(font);

        p.add(textField, BorderLayout.CENTER);
        p.add(sent, BorderLayout.EAST);
        add(textArea, BorderLayout.CENTER);
        add(p, BorderLayout.SOUTH);

        // 调整窗口大小
        pack();
        setSize(1000, 800);
        setVisible(true);

        String ip = JOptionPane.showInputDialog(this, "请输入服务器IP（留空则默认 172.20.10.9）：");
        if (ip == null || ip.trim().isEmpty()) {
            ip = "172.20.10.9";
        }
        String portStr = JOptionPane.showInputDialog(this, "请输入服务器端口（留空则默认 2525）：");
        int port;
        if (portStr == null || portStr.trim().isEmpty()) {
            port = 2525;
        } else {
            try {
                port = Integer.parseInt(portStr.trim());
            } catch (NumberFormatException e) {
                port = 2525;
            }
        }

        // 连接服务端
        try {
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            // 使用独立线程读取服务器消息，避免阻塞GUI线程
            new Thread(() -> {
                try {
                    while (true) {
                        // 每次循环开始清空textField内容
                        textField.setText("");
                        String response = in.readLine();
                        System.out.println(response);

                        if (response == null) {
                            break;
                        }

                        // 退出本次连接
                        if (response.equals("BYE")) {
                            ent = false;
                            textArea.append("您已退出登录，欢迎下次使用！\n");
                            textArea.append("欢迎使用ATM客户端！（输入“0”登出）\n");
                            response = "";
                        }
                        // 请求用户密码
                        if (!ent && response.equals("500 AUTH REQUIRE")) {
                            textArea.append("您输入的用户id：" + message0 + "\n");
                            textArea.append("请输入密码\n");
                            response = "";
                            a++;
                        }
                        // 登录成功，进入业务选择阶段
                        if (!ent && response.equals("525 OK!")) {
                            textArea.append("登陆成功！请选择您要办理的业务：\n");
                            textArea.append("查询余额输入“1”；\n");
                            textArea.append("取钱输入“2”+“（空格）”+“（您要取出的金额）”；\n");
                            ent = true;
                            response = "";
                            a++;
                        }
                        // 登录失败
                        if (!ent && response.equals("401 ERROR!")) {
                            textArea.append("用户id或者密码错误！\n请重新输入id：\n");
                            response = "";
                            a = 1;
                        }
                        // 余额查询
                        if (ent && response.startsWith("AMNT:")) {
                            String temp = response.substring(5);
                            textArea.append("您账户余额：" + temp + "\n");
                            response = "";
                        }
                        // 取钱成功
                        if (ent && response.equals("525 OK!")) {
                            textArea.append("取钱成功！\n");
                            response = "";
                        }
                        // 取钱失败
                        if (ent && response.equals("401 ERROR!")) {
                            textArea.append("取钱失败，可能账户余额不足！\n");
                            response = "";
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            System.exit(1);
        }

        // 窗口关闭时的回调
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    dispose();
                    System.exit(0);
                }
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        try {
            message0 = textField.getText();
            String message = "";
            if (message0 == null || message0.isEmpty()) {
                return;
            } else if (message0.equals("0")) {
                message = "BYE";
            } else if (a == 1) {
                message = "HELO " + message0;
            } else if (a == 2) {
                message = "PASS " + message0;
            } else if (a == 3) {
                if (message0.equals("1")) {
                    message = "BALA";
                } else if (message0.startsWith("2 ")) {
                    message = "WDRA " + message0.substring(2);
                } else {
                    throw new IOException();
                }
            }
            out.write(message);
            out.newLine();
            out.flush();
        } catch (IOException ex) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}