package bn.messages;


public interface MessageSet<MessageType extends Message> extends Iterable<MessageType>
{
	public int size();
	public MessageType get(int index);
	public void remove(int index);
	public void removeAll();
}
