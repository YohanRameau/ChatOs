package fr.uge.chatos.core;

import fr.uge.chatos.Server;
import fr.uge.chatos.context.ServerContextPrivate;
import fr.uge.chatos.frame.Public_msg;

public class ServerPrivateFrameVisitor implements FrameVisitor{

	private Server server;
	private ServerContextPrivate ctx;
	
	
	public ServerPrivateFrameVisitor(Server server, ServerContextPrivate ctx) {
		this.server = server;
		this.ctx = ctx;
	}


	@Override
	public void visit(Public_msg pck) {
		server.broadcast(pck);		
	}

 
}
