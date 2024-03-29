public class StationMessage
{
	
	private int m_X;
	private int m_Y;
	private MessageType m_MessageType;
	
	public static StationMessage Parse(int stationId, String messageData) {
		int x = -1;
		int y = -1;
		MessageType messageType;
	
		String[] tokens = messageData.split(":");
		messageType = MessageType.values()[Integer.parseInt(tokens[0])];
		
		if (messageType != MessageType.STATION_2_DONE) {
			x = Integer.parseInt(tokens[1]);
			y = Integer.parseInt(tokens[2]);	
		}
		
		return new StationMessage(x, y, messageType);
	}
	
	
	public static String FormatToMessage(MessageType messageType, int x, int y) {
		return String.format("%d:%d:%d", messageType.ordinal(), x, y);
	}
	
	
	public static String FormatToMessage(MessageType messageType) {
		return String.format("%d", messageType.ordinal());
	}
	
	
	public StationMessage(int x, int y, MessageType messageType) {
		m_X = x;
		m_Y = y;
		m_MessageType = messageType;
	}
	
	
	public int getX() {
		return m_X;
	}
	
	
	public int getY() {
		return m_Y;
	}
	
	
	public MessageType getMessageType() {
		return m_MessageType;
	}
	
	
	@Override
	public String toString() {
		return String.format("[MessageType=%s][X=%d, Y=%d]", m_MessageType, m_X, m_Y);
	}
}