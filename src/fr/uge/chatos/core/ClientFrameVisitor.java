package fr.uge.chatos.core;

import fr.uge.chatos.Server;
import fr.uge.chatos.context.ServerContext;
import fr.uge.chatos.frame.Accept_co_private;
import fr.uge.chatos.frame.Acceptance;
import fr.uge.chatos.frame.Established_private;
import fr.uge.chatos.frame.Id_private;
import fr.uge.chatos.frame.Login_private;
import fr.uge.chatos.frame.Private_msg;
import fr.uge.chatos.frame.Public_msg;
import fr.uge.chatos.frame.Refusal;
import fr.uge.chatos.frame.Refusal_co_private;
import fr.uge.chatos.frame.Request_co_private;
import fr.uge.chatos.frame.Request_co_server;
import fr.uge.chatos.frame.Unknown_user;

public class ClientFrameVisitor implements FrameVisitor{

	private Server server;
	private ServerContext ctx;
	private boolean accepted = false;
	
	@Override
	public void visit(Accept_co_private pck) {
		long id = server.generateId();
		var idPrivate1 = new Id_private(pck.getSender(), pck.getReceiver(), id);
		var idPrivate2 = new Id_private(pck.getReceiver(), pck.getSender(), id);
		ctx.unicastOrUnknow(idPrivate1);
		ctx.unicastOrUnknow(idPrivate2);
	}

	@Override
	public void visit(Acceptance pck) {
		System.out.println("Welcome to the server !");	
	}

	@Override
	public void visit(Established_private pck) {
		System.out.println("Private connection established !");
	}

	@Override
	public void visit(Id_private pck) {
		var privateLogin = new Login_private(pck.getId());
		ctx.queueMessage(privateLogin);
	}

	@Override
	public void visit(Login_private pck) {
		var establishedPck = new Established_private();
		ctx.queueMessage(establishedPck);
	}

	@Override
	public void visit(Private_msg pck) {
		ctx.unicastOrUnknow(pck);
	}

	@Override
	public void visit(Public_msg pck) {
		server.broadcast(pck);		
	}

	@Override
	public void visit(Refusal_co_private pck) {
		if (!server.unicast(pck)) {
			var unknown_user = new Unknown_user();
			ctx.queueMessage(unknown_user);
			return;
		}
	}

	@Override
	public void visit(Request_co_private pck) {
		ctx.askPrivateConnection(pck);		
	}

	@Override
	public void visit(Refusal pck) {
		System.out.println("Connection refused.");	
	}

	@Override
	public void visit(Request_co_server pck) {
		ctx.identificationProcess(pck.getSender());
		accepted = true;
	}

	@Override
	public void visit(Unknown_user unknown_user) {
		System.out.println("This user is not present on this server !");		
	}
 
	
}
