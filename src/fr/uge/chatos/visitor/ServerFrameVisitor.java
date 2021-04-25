package fr.uge.chatos.visitor;

import java.nio.channels.SelectionKey;

import fr.uge.chatos.Server;
import fr.uge.chatos.context.ServerContext;
import fr.uge.chatos.frametypes.Accept_co_private;
import fr.uge.chatos.frametypes.Established_private;
import fr.uge.chatos.frametypes.Id_private;
import fr.uge.chatos.frametypes.Login_private;
import fr.uge.chatos.frametypes.PrivateConnectionMessage;
import fr.uge.chatos.frametypes.Private_msg;
import fr.uge.chatos.frametypes.Public_msg;
import fr.uge.chatos.frametypes.Refusal_co_private;
import fr.uge.chatos.frametypes.Request_co_private;
import fr.uge.chatos.frametypes.Request_co_server;
import fr.uge.chatos.frametypes.SendToOne;

public class ServerFrameVisitor implements FrameVisitor{

	private Server server;
	private ServerContext ctx;
	private final SelectionKey key;
	private boolean acceptedPublicConnection = false;
	private boolean acceptedPrivateConnection = false;
	
	
	
	public ServerFrameVisitor(Server server, ServerContext ctx, SelectionKey key) {
		this.server = server;
		this.ctx = ctx;
		this.key = key;
	}
	
	public boolean privateConnection() {
		return acceptedPrivateConnection;
	}
	

	private void closeIfNotPublicConnection() {
		if(!acceptedPublicConnection  ) {
			ctx.silentlyClose();
		}
	}
	
	
	@Override
	public void visit(Accept_co_private pck) {
		closeIfNotPublicConnection();
		var sender = pck.getSender();
		var receiver = pck.getReceiver();
		if(ctx.isRequester(sender)) {
			return;
		}
		long id = server.initializedPrivateconnection();
		System.out.println("VISIT ID : " + id);
		var idPrivate1 = new Id_private(sender, receiver, id);
		var idPrivate2 = new Id_private(receiver, sender, id);
		ctx.unicastOrUnknow(idPrivate1);
		ctx.unicastOrUnknow(idPrivate2);
	}

	@Override
	public void visit(Login_private pck) {
		acceptedPrivateConnection = true;
		if(!server.registerPrivateConnection(pck.getId(), ctx)) {
			ctx.silentlyClose();
			return;
		}
		var establishedPck = new Established_private();
		ctx.queueMessage(establishedPck);
	}

	@Override
	public void visit(Private_msg pck) {
		closeIfNotPublicConnection();
		ctx.unicastOrUnknow(pck);
	}

	@Override
	public void visit(Public_msg pck) {
		closeIfNotPublicConnection();
		server.broadcast(pck);		
	}

	@Override
	public void visit(Refusal_co_private pck) {
		closeIfNotPublicConnection();
		ctx.unicastOrUnknow(pck);
		server.disabledConnectionTry(ctx, pck);
	}

	@Override
	public void visit(Request_co_private pck) {
		closeIfNotPublicConnection();
		ctx.askPrivateConnection(pck);		
	}
	

	@Override
	public void visit(Request_co_server pck) {
		ctx.identificationProcess(pck.getSender());
		acceptedPublicConnection = true;
	}
	
	@Override
	public void visit(PrivateConnectionMessage pck) {
		if(!privateConnection()) {
			ctx.silentlyClose();
		}
		var tmp = server.getPrivateConnectionInfo(pck.getId());
		if(tmp.isEmpty()) {
			ctx.silentlyClose();
			return;
		}
		var pci = tmp.get();
		pci.edgeSending(ctx, pck);
	}
 
}
