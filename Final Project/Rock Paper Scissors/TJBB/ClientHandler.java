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
				if (request == null) {
					removeClient(this);
					break;
				}
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
				
				//find if client exists ---exists(Client) return boolean----
				//if they exist, check if they are in a game
				//if not in a game and exists, send challenge message ---challenge(Client) return void----
				//challenged player responds with answer ---challengeNotification(Client) return void----
				//display result
				//if challenge accepted
				//take in player choices
				//run game logic
				//display results to both players
				//reset inGame variable so other players can challenge
				else if (request.startsWith("/challenge")) {
					int firstSpace = request.indexOf(' ');
					String otherPlayer = request.substring(firstSpace + 1);
					if (firstSpace != -1 && exists(otherPlayer) && !getClient(otherPlayer).inGame) { //run game logic to whoever was challenged
						
					}
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
				clients.remove(aClient);
	}
	
	private void listUsers() {
        out.println("Current Users:");
        for (ClientHandler aClient : clients) {
            if (aClient.getName() != this.name) {
                out.println(aClient.getName());
            }
        }
    }
	
	private boolean challenge(ClientHandler otherPlayer) throws IOException {
		return otherPlayer.challengeNotification(this.name);
	}
	
	private boolean challengeNotification(String user) throws IOException {
		String response = "";
		this.out.println(user + " has sent a challenge.");
		this.out.println("Do you accept? y/n");
		response = in.readLine();
		
		return response.equalsIgnoreCase("y");
	}
	
	private boolean exists(String user) {
		boolean doesExist = false;
		for (ClientHandler aClient : clients) {
            if (aClient.getName().equals(user)) 
            	doesExist = true;
        }
		if(!doesExist) out.println(user + " does not exist in the server.");
		return doesExist;
	}
	
	public String getName() {
		return name;
	}
	
	public ClientHandler getClient(String user) {
		if(exists(user)) {
			for (ClientHandler aClient : clients) {
	            if (aClient.getName().equals(user)) 
	            	return aClient;
	        }
		}
		return null;
	}
}
