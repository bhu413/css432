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
	public boolean inGame = true;
	public int score = 0;
	String[] answers = { "rock", "r", "paper", "p", "scissors", "s" };

	public ClientHandler(Socket clientSocket, ArrayList<ClientHandler> clients) throws IOException {
		this.client = clientSocket;
		this.clients = clients;
		in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		out = new PrintWriter(client.getOutputStream(), true);
	}

	@Override
	public void run() {
		boolean goIntoLoop = true;
		try {
			out.println("Please Register. What would you like your username to be? If you'd like a random name please type 'give me a name'");
			name = in.readLine();
			if (name.equalsIgnoreCase("give me a name")) {
				name = Server.getRandomName();
				out.println("Your name is: " + name);
			} else if(name.equals("/unregister")) {
				removeClient(this);
				goIntoLoop = false;
			} 
			else {
				out.println("That's a great username " + name + "!");
			}
			while (goIntoLoop) {
				String request = "";
				if (in.ready()) {
					request = in.readLine();
				}
				
				if (request.startsWith("/all")) {
					int firstSpace = request.indexOf(' ');
					if (firstSpace != -1) {
						outToAll(request.substring(firstSpace + 1));
					}
				} else if (request.equals("/unregister")) {
					removeClient(this);
					break;
				}
				else if (request.startsWith("/message")) {
					int firstSpace = request.indexOf(' ');
					int secondSpace = request.indexOf(' ', firstSpace + 1);
					if (firstSpace != -1 && secondSpace != -1) {
						outToOther(request.substring(firstSpace + 1, secondSpace), request.substring(secondSpace + 1));
					} else {
						out.println("bad message request");
					}  
				}
				else if (request.startsWith("/users")) {
	                listUsers();	                
				}
				
				else if (request.startsWith("/challenge")) {
					int firstSpace = request.indexOf(' ');
					String otherPlayerId = request.substring(firstSpace + 1);
					if (firstSpace != -1 && exists(otherPlayerId) && !getClient(otherPlayerId).inGame) { //run game logic to whoever was challenged
						this.inGame = false;
						ClientHandler otherPlayer = getClient(otherPlayerId);
						outToBoth(this, otherPlayer, "you guys ready");
						boolean challengeAccepted = this.challenge(otherPlayer);
						out.println(challengeAccepted); //TEST
						if(challengeAccepted) {
							this.inGame = true;
							player1Shoot(otherPlayer);
						}else {
							out.println(otherPlayerId + " does not want to play right now.");
						}
					} else {
						out.println(otherPlayerId + " does not exist or is in a game.");	
					}
				}else if (request.startsWith("/creategame")) {
					this.inGame = false;
				}else if (request.startsWith("/unavailable")) {
					this.inGame = true;
				}else if (request.startsWith("/score")) {
					out.println(this.score);
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
			aClient.out.println(name + " (to all): " + msg);
		}
	}

	private void removeClient(ClientHandler aClient) {
		clients.remove(aClient);
	}
	
	
	
	private void listUsers() {
        out.println("Current Users:");
        for (ClientHandler aClient : clients) {
            if (aClient.getName() != this.name) {
				int inGameInt = inGame ? 1 : 0;
				out.println(aClient.getName() + " " + inGameInt);
            }
        }
    }
	
	private void outToBoth(ClientHandler p1, ClientHandler p2, String message) {
		p1.out.println(message);
		p2.out.println(message);
	}
	
	private void outToOther(String user, String message) {
		ClientHandler other = getClient(user);
		if (other != null) {
			other.out.println(this.name + ": " + message);
		}
	}
	
//------------------------------Game Logic-----------------------------------	
	private boolean challenge(ClientHandler otherPlayer) throws IOException {
		return otherPlayer.challengeNotification(this.name);
	}
	
	private boolean challengeNotification(String user) throws IOException {
		String response = "";
		this.out.println(user + " has sent a challenge.");
		this.out.println("Do you accept? y/n");
		response = in.readLine();
		
		if(response.equalsIgnoreCase("y")) {
			this.inGame = true;
			this.out.println("Waiting for " + user + " choice...");
		}
		
		return response.equalsIgnoreCase("y");
	}
	
	private void player1Shoot(ClientHandler otherPlayer) throws IOException {
		out.println("Game Started!");
		out.println("What is your choice? (rock, paper, or scissors)");
		String p1Choice = in.readLine();
		String p2Choice = otherPlayer.player2Shoot(this);

		results(this, p1Choice, otherPlayer, p2Choice);
		
		this.inGame = false;
		otherPlayer.inGame = false;
	}
	
	private String player2Shoot(ClientHandler challenger) throws IOException {
		out.println("Game Started!");
		out.println("What is your choice? (rock, paper, or scissors)");
		String p2Choice = in.readLine();
		
		return p2Choice;
	}
	
	private void results(ClientHandler p1, String p1Choice, ClientHandler p2, String p2Choice) {
		if (!isValid(p1Choice)) {
			outToBoth(p1, p2, p1.name + ": Invalid move! Player 2 wins by default");
		}
		if (!isValid(p2Choice)) {
			outToBoth(p1, p2, p2.name + ": Invalid move! Player 1 wins by default");
		}

		if (gamePlay(p1Choice, p2Choice) == 0) {
			outToBoth(p1, p2, p1.name + " and " + p2.name + " both played " + p1Choice);
			outToBoth(p1, p2, "Tie!\nGame Over");
		} else if (gamePlay(p1Choice, p2Choice) == 1) {
			outToBoth(p1, p2, p1.name + " played " + p1Choice + "\n" + p2.name + " played " + p2Choice);
			outToBoth(p1, p2, p1.name + " wins!\nGame Over");
			p1.score++;
		} else if(gamePlay(p1Choice, p2Choice) == 2) {
			outToBoth(p1, p2, p1.name + " played " + p1Choice + "\n" + p2.name + " played " + p2Choice);
			outToBoth(p1, p2, p2.name + " wins!\nGame Over");
			p2.score++;
		}else if(gamePlay(p1Choice, p2Choice) == 3){
			p1.out.println(p2.name + " exited the game\n" + p1.name + " wins!\nGame Over");
			p1.score++;
		} else if(gamePlay(p1Choice, p2Choice) == 4){
			p2.out.println(p1.name + " exited the game\n" + p2.name + " wins!\nGame Over");
			p2.score++;
		}else {
			outToBoth(p1, p2, "Error occured!\nGame Dropped");
		}
	}
	
	public boolean isValid(String choice) {
		boolean valid = false;
		int count = 0;
		while (valid == false && count < answers.length) {
			if (choice.equalsIgnoreCase(answers[count]))
				valid = true;
			count++;
		}
		return valid;
	}

	public int gamePlay(String p1Choice, String p2Choice) {
		int winner = 0;
		if (p1Choice.equalsIgnoreCase(answers[0]) || p1Choice.equalsIgnoreCase(answers[1])) {
			if (p2Choice.equalsIgnoreCase(answers[2]) || p2Choice.equalsIgnoreCase(answers[3])) {
				winner = 2;
				return winner;
			} else if (p2Choice.equalsIgnoreCase(answers[4]) || p2Choice.equalsIgnoreCase(answers[5])) {
				winner = 1;
				return winner;
			}
		} else if (p1Choice.equalsIgnoreCase(answers[2]) || p1Choice.equalsIgnoreCase(answers[3])) {
			if (p2Choice.equalsIgnoreCase(answers[0]) || p2Choice.equalsIgnoreCase(answers[1])) {
				winner = 1;
				return winner;
			} else if (p2Choice.equalsIgnoreCase(answers[4]) || p2Choice.equalsIgnoreCase(answers[5])) {
				winner = 2;
				return winner;
			}
		} else if (p1Choice.equalsIgnoreCase(answers[4]) || p1Choice.equalsIgnoreCase(answers[5])) {
			if (p2Choice.equalsIgnoreCase(answers[2]) || p2Choice.equalsIgnoreCase(answers[3])) {
				winner = 1;
				return winner;
			} else if (p2Choice.equalsIgnoreCase(answers[0]) || p2Choice.equalsIgnoreCase(answers[1])) {
				winner = 2;
				return winner;
			}
		} else if(p2Choice.equalsIgnoreCase("/q")){
			winner = 3;
			return winner;
		}else if(p1Choice.equalsIgnoreCase("/q")){
			winner = 4;
			return winner;
		}else {
			winner = 0;
			return winner;
		}
		return winner;
	}
//------------------------------End Game Logic-------------------------------

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
