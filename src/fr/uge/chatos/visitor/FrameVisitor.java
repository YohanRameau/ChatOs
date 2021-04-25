package fr.uge.chatos.visitor;

import fr.uge.chatos.frametypes.Accept_co_private;
import fr.uge.chatos.frametypes.Acceptance;
import fr.uge.chatos.frametypes.Established_private;
import fr.uge.chatos.frametypes.Id_private;
import fr.uge.chatos.frametypes.Login_private;
import fr.uge.chatos.frametypes.PrivateConnectionMessage;
import fr.uge.chatos.frametypes.Private_msg;
import fr.uge.chatos.frametypes.Public_msg;
import fr.uge.chatos.frametypes.Refusal;
import fr.uge.chatos.frametypes.Refusal_co_private;
import fr.uge.chatos.frametypes.Request_co_private;
import fr.uge.chatos.frametypes.Request_co_server;
import fr.uge.chatos.frametypes.Unknown_user;

public interface FrameVisitor {
	public default void visit(Accept_co_private pck) {
		return;
	};
	public default void visit(Acceptance pck) {
		return;
	};
	public default void visit(Established_private pck) {
		return;
	};
	public default void visit(Id_private pck) {
		return;
	};
	public default void visit(Login_private pck) {
		return;
	};
	public default void visit(Private_msg pck) {
		return;
	};
	public default void visit(Public_msg pck) {
		return;
	};
	public default void visit(Refusal_co_private pck) {
		return;
	};
	public default void visit(Request_co_private pck) {
		return;
	};
	public default void visit(Refusal pck) {
		return;
	};
	public default void visit(Request_co_server pck) {
		return;
	};
	public default void visit(Unknown_user unknown_user) {
		return;
	};
	
	public default void visit(PrivateConnectionMessage unknown_user) {
		return;
	};
}
