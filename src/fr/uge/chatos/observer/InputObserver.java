package fr.uge.chatos.observer;


public interface InputObserver {
	
		public void observe();
		public void onPrivateMessage(String message);
		public void onPrivateRequest(String message);
		public void onAccept(String message);
		public void onRefuse(String message);
		public void onPublicMessage(String message);
}