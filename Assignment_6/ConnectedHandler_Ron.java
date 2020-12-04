import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

public class ConnectedHandler extends Handler {

	/**
	 * @return an integer identifier, supposed to be unique.
	 */
	public static int getUniqueID() {
		return (int) (Math.random() * Integer.MAX_VALUE);
	}

	// don't change the two following definitions

	private static final String HELLO = "--HELLO--";
	private static final String ACK = "--ACK--";

	// destinationId;senderId;packetNumber;
	// packet number is assumed to be positive
	private static final String PREFIX = "([0-9])+;(-)?([0-9])+;([0-9])+;";
	private static final String MSG = "(.)*";

	/**
	 * the two following parameters are suitable for manual experimentation and
	 * automatic validation
	 */

	/** delay before retransmitting a non acked message */
	private static final int DELAY = 300;

	/** number of times a non acked message is sent before timeout */
	private static final int MAX_REPEAT = 10;

	/** A single Timer for all usages. Don't cancel it. **/
	private static final Timer TIMER = new Timer("ConnectedHandler's Timer", true);

	private final int localId;
	private final String destination;
	private Handler aboveHandler;
	// to be completed
	private int remoteId;
	private int localPacketNumber;
	private int remotePacketNumber;

	private static final boolean DEBUG = false;

	/**
	 * Initializes a new connected handler with the specified parameters
	 * 
	 * @param _under       the {@link Handler} on which the new handler will be
	 *                     stacked
	 * @param _localId     the connection Id used to identify this connected handler
	 * @param _destination a {@code String} identifying the destination
	 */
	public ConnectedHandler(final Handler _under, int _localId, String _destination) {
		super(_under, _localId, true);
		this.localId = _localId;
		this.destination = _destination;

		// to be completed
		localPacketNumber = 0; // local and remote packet number
		remotePacketNumber = 0;
		remoteId = -1;
		// wait for remote connection Id : how can i get it ?
		// send repetitively HELLO, waiting for ACK
		send(HELLO);
	}

	// don't change this definition
	@Override
	public void bind(Handler above) {
		if (!this.upsideHandlers.isEmpty())
			throw new IllegalArgumentException("cannot bind a second handler onto this " + this.getClass().getName());
		this.aboveHandler = above;
		super.bind(above);
	}

	@Override
	public void handle(Message message) {

		if (DEBUG) {
			System.out.println("Message received:" + message.payload);
		}

		if (Pattern.compile(PREFIX + HELLO).matcher(message.payload).matches()) {

			String[] msgFields = message.payload.split(";");
			int parsedRemoteId = Integer.parseInt(msgFields[0]); // check index
			int parsedLocalId = Integer.parseInt(msgFields[1]);
			int parsedPacketNumber = Integer.parseInt(msgFields[2]);
			boolean valid = parsedLocalId == -1 && parsedPacketNumber == 0;

			if (valid) {
				if (DEBUG) {
					System.out.println("Hello received");
				}
				if (remoteId == -1) {
					if (DEBUG) {
						System.out.println("First hello");
						System.out.println("Remote Id updated: " + parsedRemoteId);
					}
					remoteId = parsedRemoteId;
					send(ACK);
					remotePacketNumber++;
				} else {
					if (DEBUG) {
						System.out.println("Not first hello");
					}
					int tmp = remotePacketNumber;
					remotePacketNumber = parsedPacketNumber;
					send(ACK);
					remotePacketNumber = tmp;
				}
			}

		}

		else if (Pattern.compile(PREFIX + ACK).matcher(message.payload).matches()) {
			String[] msgFields = message.payload.split(";");
			int parsedRemoteId = Integer.parseInt(msgFields[0]); // check index
			int parsedLocalId = Integer.parseInt(msgFields[1]);
			int parsedPacketNumber = Integer.parseInt(msgFields[2]);
			if (DEBUG) {
				System.out.println("It's an ACK");
			}

			if (parsedLocalId == localId && parsedRemoteId == remoteId) {
				if (parsedPacketNumber == localPacketNumber) {
					if (DEBUG) {
						System.out.println("Match!" + localPacketNumber);
					}
					synchronized (this) {
						notifyAll();
					}

				} else if (DEBUG) {
					System.out.println("Doesn't match the last sent msg");
				}
			}
		}

		else if (Pattern.compile(PREFIX + MSG).matcher(message.payload).matches()) {

			if (aboveHandler == null) {
				return;
			}
			
			String[] msgFields = message.payload.split(";");
			if (msgFields.length == 3 || msgFields.length == 4) {
				int parsedRemoteId = Integer.parseInt(msgFields[0]); // check index
				int parsedLocalId = Integer.parseInt(msgFields[1]);
				int parsedPacketNumber = Integer.parseInt(msgFields[2]);

				if (remotePacketNumber > 0 && localPacketNumber > 0 && parsedPacketNumber > 0
						&& parsedLocalId == localId && parsedRemoteId == remoteId) {
					if (parsedPacketNumber == remotePacketNumber) {
						if (DEBUG) {
							System.out.println("MSG received");
						}
						send(ACK);
						remotePacketNumber++;

						if (msgFields.length == 4) {
							aboveHandler.receive(new Message(msgFields[3], Integer.toString(localId)));
						} else if (msgFields.length == 3) {
							aboveHandler.receive(new Message("", Integer.toString(localId)));
						}

					} else if (parsedPacketNumber == remotePacketNumber - 1) {
						remotePacketNumber -= 1;
						send(ACK);
						remotePacketNumber += 1;
					}
				}
			}
		}

		// two different packet number ??
	}

	@Override
	public void send(final String payload) {
		int tmp = (payload.equals(ACK)) ? remotePacketNumber : localPacketNumber;
		String msg = localId + ";" + remoteId + ";" + tmp + ";" + payload;

		// new TimerTask for each packet to be sent
		TimerTask tt = new TimerTask() {

			@Override
			public void run() {
				downside.send(msg, destination);
				if (DEBUG) {
					System.out.println("Message sent: " + msg);
				}
			}
		};

		if (payload.equals(HELLO)) {
			TIMER.schedule(tt, 0, DELAY); // 0 is for repetition
			while (remotePacketNumber == 0) {
				synchronized (this) {
					try {
						wait();
						if (DEBUG) {
							System.out.println("End wait HELLO");
						}
					} catch (InterruptedException e) {
						e.printStackTrace();

					}
				}
			}
			localPacketNumber++;
		}

		else if (!payload.equals(ACK)) {
			TIMER.schedule(tt, 0, DELAY); // 0 is for repetition
			synchronized (this) {
				try {
					wait();

				} catch (InterruptedException e) {
					e.printStackTrace();

				}
			}
			localPacketNumber++;
		} else {
			if (DEBUG) {
				System.out.println("Sending ACK: " + msg);
			}
			downside.send(msg, destination);
		}
		tt.cancel();
	}

	@Override
	public void send(String payload, String destinationAddress) {
		no_send();
	}

	@Override
	public void close() {
		// to be completed
		super.close();
	}

}
