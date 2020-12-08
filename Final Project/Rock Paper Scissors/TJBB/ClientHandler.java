package TJBB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {
	private Socket client;
	private BufferedReader in;
	private PrintWriter out;
	private ArrayList<ClientHandler> clients;
	private String name = "";
	public boolean inGame = false;

	public ClientHandler(Socket clientSocket, ArrayList<ClientHandler> clients) throws IOException {
		this.client = clientSocket;
		this.clients = clients;
		in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		out = new PrintWriter(client.getOutputStream(), true);
	}

	@Override
	public void run() {
		try {
			out.println(
					"Please Register. What would you like your username to be? If you'd like a random name please type 'give me a name'");
			name = in.readLine();
			if (name.equalsIgnoreCase("give me a name")) {
				name = Server.getRandomName();
				out.println("Your name is: " + name);
			} else {
				out.println("That's a great username " + name + "!");
			}
			while (true) {
				String request = in.readLine();
				if (request.startsWith("/all")) {
					int firstSpace = request.indexOf(' ');
					if (firstSpace != -1) {
						outToAll(request.substring(firstSpace + 1));
					}
				} else if (request.equals("/unregister")) {
					removeClient(this);
				} else if (request.startsWith("/users")) {
	                listUsers();	                
				}
				else if (request.startsWith("/challenge")) {
					//TBD
				}
			}
		} catch (IOException e) {
			System.err.println("IO exception in client handler");
			System.err.println(e.getStackTrace());
		} finally {
			out.close();
		} try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void outToAll(String msg) {
		for (ClientHandler aClient : clients) {
			aClient.out.println(name + ": " + msg);
		}
	}

	private void removeClient(ClientHandler aClient) {
		int index = 0;
		for (ClientHandler current : clients) {
			if (current.equals(aClient)) {
				clients.remove(index);
			} else {
				index++;
			}
		}
	}
	
	private void listUsers() {
        out.println("Current Users:");
        for (ClientHandler aClient : clients) {
            if (aClient.getName() != this.name) {
                out.println(aClient.getName());
            }
        }
    }
	public String getName() {
		return name;
	}
}
