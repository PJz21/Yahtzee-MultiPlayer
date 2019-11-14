import java.util.ArrayList;

public class SharedYahtzeeState {

	private ArrayList<ArrayList<Integer>> globalScoreboard;
	private boolean accessing=false; // true a thread has a lock, false otherwise

	// Constructor
	SharedYahtzeeState(ArrayList<ArrayList<Integer>> SharedVariable) {
		globalScoreboard = SharedVariable;
	}

	synchronized void acquireLock() throws InterruptedException{
		Thread currentThread = Thread.currentThread(); // get a ref to the current thread
		System.out.println(currentThread.getName()+" is attempting to acquire a lock!");
		while (accessing) {
			System.out.println(currentThread.getName()+" waiting to get a lock as someone else is accessing...");
			wait();
		}

		accessing = true;
		System.out.println(currentThread.getName() + " got a lock!");
	}

	synchronized void releaseLock() {
		accessing = false;
		notifyAll();
		Thread me = Thread.currentThread(); // get a ref to the current thread
		System.out.println(me.getName()+" released a lock!");
	}

	synchronized void updateScoreBoard(int myThreadNumber, int score, int round) {

		int currentScore = globalScoreboard.get(myThreadNumber).get(1);

		globalScoreboard.get(myThreadNumber).set(0,round + 1);
		globalScoreboard.get(myThreadNumber).set(1,(currentScore + score));
	}

	synchronized void roundCompleted() throws InterruptedException {

		for (ArrayList<Integer> integers : globalScoreboard) {
			if (!integers.get(0).equals(globalScoreboard.get(0).get(0))) {
				wait();
			}
		}
		notifyAll();

	}

	synchronized int winner() {
		int winner = 0;

		for (int i = 0; i < globalScoreboard.size(); i++) {
			if (globalScoreboard.get(winner).get(1) < globalScoreboard.get(i).get(1)) {
				winner = i;
			}
		}

		return winner + 1;
	}

	synchronized ArrayList<ArrayList<Integer>> getScoreBoard() {
		return globalScoreboard;
	}
}

