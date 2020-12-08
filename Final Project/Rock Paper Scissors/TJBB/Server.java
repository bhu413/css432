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
	private static String[] names = {"Ransu", "Gautstafr", "Eusebius" , "Rahman", "Rajesh", "Jeremie", "Marianela", "Orli", "Wazo", "Teodoro"};
	private static String[] adjs = {"Decisive", "Unique", "Onerous", "Boiling", "Uppity", "Diligent", "Aback", "Abrasive", "Stormy", "Ruthless"};
    private static final int port = 5000;
    //ServerSocket server=null;
    //Socket client=null;

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
            //ServerThread runnable= new ServerThread(client,clientcount,this);
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
/*    Server(int port){
        this.port=port;
        pool = Executors.newFixedThreadPool(5);
    } */

 
/*     private static class ServerThread implements Runnable {
        
        Server server=null;
        Socket client=null;
        BufferedReader cin;
        PrintStream cout;
        Scanner sc=new Scanner(System.in);
        int id;
        String s;
        
        ServerThread(Socket client, int count ,Server server ) throws IOException {
            
            this.client=client;
            this.server=server;
            this.id=count;
            System.out.println("Connection "+id+"established with client "+client);
            
            cin=new BufferedReader(new InputStreamReader(client.getInputStream()));
            cout=new PrintStream(client.getOutputStream());
        
        }

       @Override
        public void run() {
            int x=1;
         try{
         while(true){
               s=cin.readLine();
  			 
			System. out.print("Client("+id+") :"+s+"\n");
			System.out.print("Server : ");
			//s=stdin.readLine();
                            s=sc.nextLine();
                        if (s.equalsIgnoreCase("bye"))
                        {
                            cout.println("BYE");
                            x=0;
                            System.out.println("Connection ended by server");
                            break;
                        }
			cout.println(s);
		}
		
            
                cin.close();
                client.close();
		cout.close();
                if(x==0) {
			System.out.println( "Server cleaning up." );
			System.exit(0);
                }
         } 
         catch(IOException ex){
             System.out.println("Error : "+ex);
         }
            
 		
        }
} */

}

