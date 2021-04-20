package fr.uge.chatos.core;

import fr.uge.chatos.frame.Accept_co_private;
import fr.uge.chatos.frame.Acceptance;
import fr.uge.chatos.frame.Established_private;
import fr.uge.chatos.frame.Id_private;
import fr.uge.chatos.frame.Login_private;
import fr.uge.chatos.frame.Private_msg;
import fr.uge.chatos.frame.Public_msg;
import fr.uge.chatos.frame.Refusal_co_private;
import fr.uge.chatos.frame.Request_co_private;
import fr.uge.chatos.frame.Refusal;
import fr.uge.chatos.frame.Request_co_server;
import fr.uge.chatos.frame.Unknown_user;

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
}
