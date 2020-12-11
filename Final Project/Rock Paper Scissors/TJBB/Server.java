package TJBB;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Server {
	//random name generator is no longer used for unique names sake
	private static String[] names = {"Ransu", "Gautstafr", "Eusebius" , "Rahman", "Rajesh", "Jeremie", "Marianela", "Orli", "Wazo", "Teodoro"};
	private static String[] adjs = {"Decisive", "Unique", "Onerous", "Boiling", "Uppity", "Diligent", "Aback", "Abrasive", "Stormy", "Ruthless"};
    private static final int port = 5000;

    private static ArrayList<ClientHandler> clients = new ArrayList<>();
    private static ExecutorService pool = Executors.newFixedThreadPool(5);
   
    private static int clientcount=0;
    
    public static void main(String[] args) throws IOException {
        startServer();
    }
  
    public static void startServer() throws IOException {
        ServerSocket listener = new ServerSocket(port);
              
        while(true)
        {
        	System.out.println("[SERVER] Waiting for client connection...");
            Socket client = listener.accept();
            System.out.println("[SERVER] Connected to client!");
            ClientHandler clientThread = new ClientHandler(client, clients);
           
            clients.add(clientThread);        	
            System.out.println(clients.toString());
            clientcount = getClientcount() + 1;
            pool.execute(clientThread);
        }
        
    } 


	public static String getRandomName() {
		// TODO Auto-generated method stub
		String name = names[(int) (Math.random() * names.length)];
		String adj = adjs[(int)(Math.random() * adjs.length)];
		return adj + " " + name;
	}

	public static int getClientcount() {
		return clientcount;
	}

}