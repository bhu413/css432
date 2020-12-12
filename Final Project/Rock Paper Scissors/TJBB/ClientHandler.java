package TJBB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;

public class ClientHandler implements Runnable, Comparable {
	private Socket client;
	private BufferedReader in;
	private PrintWriter out;
	private ArrayList<ClientHandler> clients;
	private String name = "";
	public boolean inGame = true;
	public int score = 0;
	String[] answers = { "rock", "r", "paper", "p", "scissors", "s" , "/unregister", "/q"};

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
			out.println("Please Register. What would you like your username to be?");
			
			String inputtedName = in.readLine();
			while(nameExists(inputtedName)) {
				out.println("Sorry, that name is taken. Please Try Again");
				inputtedName = in.readLine();
			}
			name = inputtedName;
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
				else if (request.startsWith("/games")) {
	                listUsers();	                
				}
				
				else if (request.startsWith("/join")) {
					int firstSpace = request.indexOf(' ');
					String otherPlayerId = request.substring(firstSpace + 1);
					if (firstSpace != -1 && exists(otherPlayerId) && !getClient(otherPlayerId).inGame) { //run game logic to whoever was challenged
						this.inGame = false;
						out.println("Attempting to join...");
						ClientHandler otherPlayer = getClient(otherPlayerId);
						boolean challengeAccepted = this.challenge(otherPlayer);
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
					out.println("Game has been created - players can now join you");
				}else if (request.startsWith("/unavailable")) {
					this.inGame = true;
				}else if (request.startsWith("/score")) {
					out.println(this.score);
				}else if (request.startsWith("/leaderboard")) {
					leaderboard();
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
        out.println("Available Games:");
        for (ClientHandler aClient : clients) {
            if (!aClient.getName().equals(this.name) && !aClient.inGame) {
				out.println(aClient.getName());
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
		//so they don't get challenged by 2 ppl at the same time
		this.inGame = true;
		String response = "";
		this.out.println(user + " has requested to join.");
		this.out.println("Do you accept? y/n");
		response = in.readLine();
		
		if(response.equalsIgnoreCase("y")) {
			this.out.println("Waiting for " + user + " choice...");
		} else {
			this.inGame = false;
		}
		
		return response.equalsIgnoreCase("y");
	}
	
	private void player1Shoot(ClientHandler otherPlayer) throws IOException {
		out.println("Game Started!");
		out.println("What is your choice? (rock, paper, or scissors)");
		String p1Choice = in.readLine();
		out.println("Waiting for other player...");
		String p2Choice = otherPlayer.player2Shoot(this);

		results(this, p1Choice, otherPlayer, p2Choice);
		
	}
	
	private String player2Shoot(ClientHandler challenger) throws IOException {
		out.println("Game Started!");
		out.println("What is your choice? (rock, paper, or scissors)");
		String p2Choice = in.readLine();
		
		return p2Choice;
	}
	
	private void results(ClientHandler p1, String p1Choice, ClientHandler p2, String p2Choice) {
		if (!isValid(p1Choice)) {
			outToBoth(p1, p2, p1.name + " made an invalid move! " + p2.name + " wins by default");
		} else if (!isValid(p2Choice)) {
			outToBoth(p1, p2, p2.name + " made an invalid move! " + p1.name + " wins by default");
		}else {
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
			}else if(gamePlay(p1Choice, p2Choice) == 5){
				p1.out.println(p2.name + " has disconnected\n" + p1.name + " wins!\nGame Over");
				p1.score++;
				removeClient(p2);
			} else if(gamePlay(p1Choice, p2Choice) == 6){
				p2.out.println(p1.name + " has disconnected\n" + p2.name + " wins!\nGame Over");
				p2.score++;
				removeClient(p1);
			}
			else {
				outToBoth(p1, p2, "Error occured!\nGame Dropped");
			}
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
			} else if(p2Choice.equalsIgnoreCase("/q")){
				winner = 3;
				return winner;
				}
		} else if (p1Choice.equalsIgnoreCase(answers[2]) || p1Choice.equalsIgnoreCase(answers[3])) {
			if (p2Choice.equalsIgnoreCase(answers[0]) || p2Choice.equalsIgnoreCase(answers[1])) {
				winner = 1;
				return winner;
			} else if (p2Choice.equalsIgnoreCase(answers[4]) || p2Choice.equalsIgnoreCase(answers[5])) {
				winner = 2;
				return winner;
			} else if(p2Choice.equalsIgnoreCase("/q")){
				winner = 3;
				return winner;
				}
		} else if (p1Choice.equalsIgnoreCase(answers[4]) || p1Choice.equalsIgnoreCase(answers[5])) {
			if (p2Choice.equalsIgnoreCase(answers[2]) || p2Choice.equalsIgnoreCase(answers[3])) {
				winner = 1;
				return winner;
			} else if (p2Choice.equalsIgnoreCase(answers[0]) || p2Choice.equalsIgnoreCase(answers[1])) {
				winner = 2;
				return winner;
			} else if(p2Choice.equalsIgnoreCase("/q")){
				winner = 3;
				return winner;
				}
		} else if(p1Choice.equalsIgnoreCase("/q")){
			if(p2Choice.equalsIgnoreCase("/q"))	{
				winner = 0;
				return winner;
			}
			else{ 
				winner = 4;
				return winner;
			}
		} else if (p2Choice.equals("/unregister")) {
			winner = 5;
			return winner;
		} else if (p1Choice.equals("/unregister")) {
			winner = 6;
			return winner;
		}
		else {
			winner = 0;
			return winner;
		}
		return winner;
	}
//------------------------------End Game Logic-------------------------------
	
//------------------------------leaderboard----------------------------------
	public void leaderboard() {
		ArrayList<ClientHandler> temp = new ArrayList<ClientHandler>();
		temp = (ArrayList) clients.clone();
//		ArrayList <Integer> scores = new ArrayList<Integer>();
//		int score;
//		for(int i = 0; i < Server.getClientcount(); i++) {
//		    score = clients.get(i).score;
//		    scores.add(i, score);
//		}
//		
//		Collections.sort(scores);
//		Collections.reverse(scores);
//		
//		
//		ArrayList<String> topScores = new ArrayList<String>(Server.getClientcount());
//		String[] listTop = new String[Server.getClientcount()];
//		for(int i = 0; i < Server.getClientcount(); i++) {
//			topScores.add(0,matchScore(scores.get(i)));
//			listTop[i] = topScores.get(0);
//			topScores.remove(0);
//		}
		
		Collections.sort(temp);
		Collections.reverse(temp);
		
		for(ClientHandler aClient : temp) {
			out.println(aClient.name + "\t" + aClient.score);
		}
		
	}
	
//	public String matchScore(int score) {
//		String highScore = "";
//		for(ClientHandler aClient : clients) {
//			if (aClient.score == score) highScore = aClient.getName();
//		}
//		
//		return highScore;
//	}
	
//	public String printHighScores(int count, ArrayList <Integer> scores, String[] topScores) {
//		String leaderBoard = "";
//		for(int i = 0; i < count; i++) {
//			leaderBoard += topScores[i] + "    " + scores.get(i) + "\n";
//		}
//		
//		return leaderBoard;
//	}

	private boolean exists(String user) {
		boolean doesExist = false;
		for (ClientHandler aClient : clients) {
            if (aClient.getName().equals(user)) 
            	doesExist = true;
        }
		if(!doesExist) out.println(user + " does not exist in the server.");
		return doesExist;
	}
	
	private boolean nameExists(String user) {
		boolean doesExist = false;
		for (ClientHandler aClient : clients) {
            if (aClient.getName().equals(user)) 
            	doesExist = true;
        }
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

	@Override
	public int compareTo(Object otherPlayer) {
			ClientHandler otherGuy = (ClientHandler) otherPlayer;
			return this.score - otherGuy.score;
	}
}
