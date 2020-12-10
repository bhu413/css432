package TJBB;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.PrintWriter;

public class Client{
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;
    
    public static void main(String args[]) throws Exception
	{
		Socket socket = new Socket(SERVER_IP, SERVER_PORT);
		
		ServerConnection serverConn = new ServerConnection(socket);
		
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		PrintWriter sout = new PrintWriter(socket.getOutputStream(), true);
		//PrintStream sout=new PrintStream(sk.getOutputStream());
		String command;
		
		new Thread(serverConn).start();
		
		while (  true )
		{
			command = stdin.readLine();
			
			if (command.equals("-1")) break;
			sout.println(command);
			
		}
		 socket.close(); 
		 System.exit(0);
		}

		 
/*		 sin.close();
		 sout.close();
 		stdin.close();
*/ 		
	}