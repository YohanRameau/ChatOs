package fr.uge.chatos.core;

import java.io.IOException;

import fr.uge.chatos.Client;
import fr.uge.chatos.context.ClientContext;
import fr.uge.chatos.frame.Accept_co_private;
import fr.uge.chatos.frame.Acceptance;
import fr.uge.chatos.frame.Established_private;
import fr.uge.chatos.frame.Id_private;
import fr.uge.chatos.frame.Public_msg;
import fr.uge.chatos.frame.Refusal;
import fr.uge.chatos.frame.Refusal_co_private;
import fr.uge.chatos.frame.Unknown_user;

public class ClientFrameVisitor implements FrameVisitor{

	private Client client;
	private ClientContext ctx;
	private boolean accepted = false;

	@Override
	public void visit(Acceptance pck) {
		accepted = true;
		System.out.println("Welcome to the server !");	
	}

	@Override
	public void visit(Established_private pck) {
		if (!accepted) {
			ctx.silentlyClose();
			return;
		}
		System.out.println("Private connection established !");
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
	public void visit(Refusal pck) {
		if (!accepted) {
			ctx.silentlyClose();
			return;
		}
		System.out.println("Connection refused.");	
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
