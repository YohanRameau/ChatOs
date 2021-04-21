package fr.uge.chatos.visitor;

import fr.uge.chatos.Server;
import fr.uge.chatos.context.ServerContext;
import fr.uge.chatos.frametypes.Accept_co_private;
import fr.uge.chatos.frametypes.Acceptance;
import fr.uge.chatos.frametypes.Established_private;
import fr.uge.chatos.frametypes.Id_private;
import fr.uge.chatos.frametypes.Login_private;
import fr.uge.chatos.frametypes.Private_msg;
import fr.uge.chatos.frametypes.Public_msg;
import fr.uge.chatos.frametypes.Refusal;
import fr.uge.chatos.frametypes.Refusal_co_private;
import fr.uge.chatos.frametypes.Request_co_private;
import fr.uge.chatos.frametypes.Request_co_server;
import fr.uge.chatos.frametypes.Unknown_user;

public class ServerFrameVisitor implements FrameVisitor{

	private Server server;
	private ServerContext ctx;
	private boolean accepted = false;
	
	
	public ServerFrameVisitor(Server server, ServerContext ctx, boolean accepted) {
		this.server = server;
		this.ctx = ctx;
		this.accepted = accepted;
	}

	@Override
	public void visit(Accept_co_private pck) {
		long id = server.generateId();
		var idPrivate1 = new Id_private(pck.getSender(), pck.getReceiver(), id);
		var idPrivate2 = new Id_private(pck.getReceiver(), pck.getSender(), id);
		ctx.unicastOrUnknow(idPrivate1);
		ctx.unicastOrUnknow(idPrivate2);
	}

	@Override
	public void visit(Established_private pck) {
		System.out.println("Private connection established !");
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
		System.out.println("Visit server visitor " + pck.getSender() + " " + pck.getMessage());
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
	public void visit(Request_co_server pck) {
		System.out.println("REQUEST CO SERVER: " + pck.getSender());
		ctx.identificationProcess(pck.getSender());
		accepted = true;
	}
 
}
