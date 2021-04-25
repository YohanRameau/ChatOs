package fr.uge.chatos.observer;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

import fr.uge.chatos.Client;
import fr.uge.chatos.context.ClientContext;
import fr.uge.chatos.context.PrivateClientContext;
import fr.uge.chatos.frametypes.Accept_co_private;
import fr.uge.chatos.frametypes.PrivateConnectionMessage;
import fr.uge.chatos.frametypes.Private_msg;
import fr.uge.chatos.frametypes.Public_msg;
import fr.uge.chatos.frametypes.Refusal_co_private;
import fr.uge.chatos.frametypes.Request_co_private;

public class ConsoleObserver implements InputObserver {
		
		private final Client client;
		private final String login;
		private final HashSet<String> requesters;
		private final ClientContext context;
		private final Map<String, PrivateClientContext> privateConnectionMap;
		private final ArrayBlockingQueue<String> commandQueue;
		
		public ConsoleObserver(Client client, String login, HashSet<String> requesters, ClientContext context, Map<String, PrivateClientContext> privateConnectionMap, ArrayBlockingQueue<String> commandQueue) {
			this.client = client;
			this.login = login;
			this.requesters = requesters;
			this.context = context;
			this.privateConnectionMap = privateConnectionMap;
			this.commandQueue = commandQueue;
		}
		
		/**
		 * Parse an input to determinate the type of command
		 * 
		 * @param msg to parse
		 */
		@Override
		public void observe() {
			if (commandQueue.isEmpty()) {
				return;
			}
			String msg = commandQueue.remove();
			Objects.requireNonNull(msg);
			if (msg.length() > 0) {
				switch (msg.charAt(0)) {
				case '@':
					onPrivateMessage(msg.substring(1));
					return;
				case '/':
					onPrivateRequest(msg.substring(1));
					return;
				case '\\':
					String[] tokens = msg.split(" ", 2);
					if (tokens.length != 2) {
						System.out.println("Unknown Command.");;
					}
					switch (tokens[0].toLowerCase()) {
					case "\\yes":
						onAccept(msg.substring(1));
						return;
					case "\\no":
						onRefuse(msg.substring(1));
						return;
					default:
						System.out.println("Respect the following format -> \\yes login or \\no login");
					}
				default:
					break;
				}
			}
			onPublicMessage(msg);;
		}
		
		/**
		 * 
		 */
		@Override
		public void onPrivateMessage(String message) {
			String[] tokens = message.split(" ", 2);
			if (tokens.length != 2) {
				System.out.println(
						"Parsing error: the input have a bad format. @login message for private message");
				return;
			}
			if (tokens[0].equals(login)) {
				// TODO CANNOT SEND A PRIVATE MESSAGE FOR HIMSELF.
				System.out.println("You cannot send a private message for yourself");
				return;
			}
			context.queueMessage(new Private_msg(login, tokens[0], tokens[1]));
		}

		/**
		 * 
		 */
		@Override
		public void onPrivateRequest(String message) {
			String[] tokens = message.split(" ", 2);
			if (tokens.length != 1 && tokens.length != 2) {
				System.out.println(
						"Parsing error: the input have a bad format. /login message for private connection request");
				return;
			}
			if(tokens[0].equals(login)) {
				System.out.println("YOU CANNOT SEND A PRIVATE request FOR YOURSELF.");
				return;
			}
			
			var pctx = privateConnectionMap.get(tokens[0]);
			
			if (pctx == null) {
				context.queueMessage(new Request_co_private(login, tokens[0]));
			} 
			else if(pctx != null && tokens.length == 1) {	
				System.out.println("Private connection with " + tokens[0] + " is finish.");
				pctx.silentlyClose();
			}else {
				queuePrivateConnectionMessage(pctx, tokens[1]);
			}	
		}
		
		/**
		 * 
		 */
		@Override
		public void onAccept(String message) {
			String[] tokens = message.split(" ", 2);
			if(!requesters.contains(tokens[1])) {
				System.out.println(tokens[1] + " don't ask you for a private connection.");
				return;
			}
			context.queueMessage(new Accept_co_private(login, tokens[1]));
			
		}

		/**
		 * 
		 */
		@Override
		public void onRefuse(String message) {
			String[] tokens = message.split(" ", 2);
			if(!requesters.contains(tokens[1])) {
				System.out.println(tokens[1] + " don't ask you for a private connection.");
				
				return;
			}
			context.queueMessage(new Refusal_co_private(login, tokens[1]));
			client.removePrivateRequester(tokens[1]);
		}
		
		/**
		 * Encode a public message into a ByteBuffer and transfer it on the
		 * context queue.
		 * 
		 * @param message to publicly send
		 */
		@Override
		public void onPublicMessage(String message) {
			context.queueMessage(new Public_msg(login, message));
		}
		
		

		/**
		 * Add a private connection message to the client's queue
		 * 
		 * @param login of the client
		 * @param message to send
		 * @return boolean 
		 */
		private void queuePrivateConnectionMessage(PrivateClientContext pctx, String message) {
			pctx.queueMessage(new PrivateConnectionMessage(message, pctx.getId()));
		}
				

		/**
		 * Register client into a private connection map
		 * 
		 * @param login of the client to register
		 * @param ctx the context to add to the map
		 */
		public void registerLogin(String login, PrivateClientContext ctx){
			privateConnectionMap.put(login, ctx);
		}
		
	}