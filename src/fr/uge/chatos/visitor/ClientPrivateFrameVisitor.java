package fr.uge.chatos.visitor;


import fr.uge.chatos.Client;
import fr.uge.chatos.context.PrivateClientContext;
import fr.uge.chatos.frametypes.Established_private;
import fr.uge.chatos.frametypes.Public_msg;
import fr.uge.chatos.frametypes.Unknown_user;

public class ClientPrivateFrameVisitor implements FrameVisitor{

	private Client client;
	private PrivateClientContext ctx;
	private boolean accepted = true;

	public ClientPrivateFrameVisitor(Client client , PrivateClientContext ctx) {
		this.client = client;
		this.ctx = ctx;
	}

	@Override
	public void visit(Established_private pck) {
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
	public void visit(Unknown_user unknown_user) {
		if (!accepted) {
			ctx.silentlyClose();
			return;
		}
		System.out.println("This user is not present on this server !");		
	}
 
	
}
