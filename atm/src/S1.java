import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class S1 {

    static String id;//用户id
    static String Pass_w;//用户密码
    static int Balance;//用户余额
    public static final String url = "jdbc:mysql://127.0.0.1/users";//数据库地址
	private final static String username = "root";//数据库用户名
	private final static String password = "18720161187Ssw";//数据库密码
    private static Connection connect;//数据库连接对象
    static boolean ent=false;//用户是否已登陆
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(2525);//监听2525端口，创建server socket

            System.out.println("服务器启动");
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                System.out.println("成功加载数据库驱动程序！");
                connect = DriverManager.getConnection(url, username, password);
                //Statement stmt = connect.createStatement();
                //连接成功
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("未能成功加载数据库驱动程序或者与数据库建立连接！");
            }
            //循环等待客户端连接，为每个连接创建输入输出流
            while (true) {
                Socket socket = serverSocket.accept();//接收客户端连接
                System.out.println("客户端连接：" + socket.getRemoteSocketAddress());

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));//输入流
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));//输出流


                while (true) {
                    String message = in.readLine();//读取客户端消息

                    if (message == null) {
                        break;
                    }//客户端断开连接
                    //处理BYE
                    if (message.equals("BYE")) {
                        //清理会话并相应客户端
                        System.out.println(" 用户退出登录！");
                        String response = "BYE";
                        id="";
                        Pass_w="";
                        out.write(response);
                        out.newLine();
                        out.flush();
                        ent=false;
                        break;
                    }
                    
                    if (message.startsWith("HELO "))
                    {
                        id=message.substring(5);//从第五个字符开始取
                        System.out.println("收到信息：" + id);
                        //String response = " 账号: " + id + "   请输入PIN:";
                        String response ="500 AUTH REQUIRE";
                            
                        out.write(response);
                        out.newLine();//写入一个行分割符
                        out.flush();//强制发送给客户端
                    }
                    //重置状态
                    String response = "";
                    if (message.startsWith("PASS ")) 
                    {
                        
                        if(ent==false)//不在登陆状态
                        {
                            Pass_w=message.substring(5);
                        }
                        System.out.println("收到信息：" + Pass_w);
                        if(entry())//调用验证方法
                        {
                            ent=true;
                            System.out.println(entry());
                                //response = "PIN: " + Pass_w + "  登陆成功！   请选择您需要办理的业务：查询余额请输入‘1’;取钱请输入‘2’";
                            response ="525 OK!";
                        }
                        else
                        {
                            response="401 ERROR!" ;
                        }
                    } 
                    if (ent==true) 
                    {
                        if(message.equals("BALA"))
                        {
                            
                            response="AMNT:"+balance();

                        }//返回存款余额
                        else if(message.startsWith("WDRA "))
                        {
                            Balance=balance();
                            String draw=message.substring(5);
                            int wdra=Integer.parseInt(draw);//将draw转换为整数
                            if(wdra<=Balance)
                            {
                                response="525 OK";
                                draw(wdra);
                            }
                            else
                            {
                                response="401 ERROR!";
                            }

                        }
                    }
                    out.write(response);
                    out.newLine();
                    out.flush();                
                }
                //无响应，关闭通道
                in.close();
                out.close();
                socket.close();
                System.out.println("服务器失去连接");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    static boolean entry() {//查询是否登录
        boolean a = false;//验证是否成功
        String sql = "select * from user_inf where UserID='" + id + "' and Pass_w='" + Pass_w + "'"; // 注意此处使用的是单引号而不是全角引号
    
        try {
            Statement stmt = connect.createStatement();//用于执行sql
            ResultSet rs = stmt.executeQuery(sql);//执行sql语句
            if (rs.next()) {//如果有结果
                a = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return a;
    }

    static int balance()
    {
        int a=0;
        String sql = "select * from user_inf where UserID='" + id + "' and Pass_w='" + Pass_w + "'"; // 注意此处使用的是单引号而不是全角引号
    
        try {
            Statement stmt = connect.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                a=rs.getInt("Balance");//查询balance列
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return a;//Integer.toString(a);
    }

    static void draw(int a)//取钱
    {
        try {
            Statement stmt = connect.createStatement();
            stmt.executeUpdate("update user_inf set Balance="+(Balance-a)+" where UserID='" + id + "' and Pass_w='" + Pass_w + "'");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
//命令行输入java -cp "bin;.;lib\mysql-connector-j-8.0.32.jar" S1,记得打开mysql！