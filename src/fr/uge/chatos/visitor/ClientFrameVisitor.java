package fr.uge.chatos.visitor;

import java.io.IOException;

import fr.uge.chatos.Client;
import fr.uge.chatos.context.ClientContext;
import fr.uge.chatos.frametypes.Accept_co_private;
import fr.uge.chatos.frametypes.Acceptance;
import fr.uge.chatos.frametypes.Established_private;
import fr.uge.chatos.frametypes.Id_private;
import fr.uge.chatos.frametypes.Private_msg;
import fr.uge.chatos.frametypes.Public_msg;
import fr.uge.chatos.frametypes.Refusal;
import fr.uge.chatos.frametypes.Refusal_co_private;
import fr.uge.chatos.frametypes.Request_co_private;
import fr.uge.chatos.frametypes.Unknown_user;

public class ClientFrameVisitor implements FrameVisitor{

	private Client client;
	private ClientContext ctx;
	private boolean accepted = false;

	public ClientFrameVisitor(Client client ,ClientContext ctx) {
		this.client = client;
		this.ctx = ctx;
	}
	
	@Override
	public void visit(Acceptance pck) {
		accepted = true;
		System.out.println("Welcome to the server !");	
	}

	@Override
	public void visit(Public_msg pck) {
		if (!accepted) {
			ctx.silentlyClose();
			return;
		}
		ctx.displayMessage(pck);		
	}
	
	@Override
	public void visit(Private_msg pck) {
		if (!accepted) {
			ctx.silentlyClose();
			return;
		}
		System.out.println("(Private) " + pck.getSender() + ": " + pck.getMessage() );
	}

	@Override
	public void visit(Refusal pck) {
		System.out.println("Connection refused.");
		ctx.silentlyClose();
		System.exit(0);
	}

	public void visit(Id_private pck) {
		if (!accepted) {
			ctx.silentlyClose();
			return;
		}
		System.out.println("User " + pck.getSender() + " and User " + pck.getReceiver()
		+ " have a private connection now with id " + pck.getId());
		try {
			client.initializePrivateConnection(pck.getId());
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
	}; 
	
	@Override 
	public void visit(Request_co_private pck){
		System.out.println(pck.getSender() + " Sent you a private connexion request | Accept (\\yes login) or Decline (\\no login) ?");
		client.processCommands();
	}
	
	@Override
	public void visit(Refusal_co_private pck) {
		System.out.println("The private connexion request has been refused by "+pck.getSender());
	}
	
	@Override
	public void visit(Accept_co_private pck) {
		System.out.println("The private connexion request has been accepted by "+pck.getSender());
	}
	
	@Override
	public void visit(Unknown_user unknown_user) {
		if (!accepted) {
			ctx.silentlyClose();
			return;
		}
		System.out.println("This user is not present on this server !");		
	}
 
	
}