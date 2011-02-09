package bn.impl.staticbn;

import java.util.Iterator;
import java.util.Vector;

import bn.BNException;
import bn.interfaces.MessageSet;
import bn.messages.Message;

public class StaticContextManagers
{
	
	public static class StaticMessageSet<MessageType extends Message> implements MessageSet<MessageType>
	{

		@Override
		public Iterator<MessageType> iterator() {
			return this.messages.iterator();
		}

		@Override
		public int size() {
			return this.messages.size();
		}

		@Override
		public MessageType get(int index) {
			return this.messages.get(index);
		}

		@Override
		public void remove(int index) {
			this.messages.remove(index);
		}

		@Override
		public void removeAll() {
			this.messages.clear();
		}
		
		public void add(MessageType o)
		{
			this.messages.add(o);
		}
		
		Vector<MessageType> messages = new Vector<MessageType>();
	}

	public static class StaticParentManager<MessageType extends Message>
	{
		public MessageSet<MessageType> getIncomingPis()
		{
			return this.incoming_pis;
		}

		public MessageSet<MessageType> getOutgoingLambdas()
		{
			return this.outgoing_lambdas;
		}
		
		public int newParent(MessageType inc_pi, MessageType outgoing_lambda) 
		{
			this.incoming_pis.add(inc_pi);
			this.outgoing_lambdas.add(outgoing_lambda);
			return this.incoming_pis.size()-1;
		}
		
		public void removeParent(int index) throws BNException
		{
			if(index>=this.incoming_pis.size())
				throw new BNException("Attempted to remove nonexistant parent..");
			this.incoming_pis.remove(index);
			this.outgoing_lambdas.remove(index);
		}
		
		public void resetMessages()
		{
			for(MessageType msg : this.outgoing_lambdas)
				msg.setInitial();
		}
		
		private StaticMessageSet<MessageType> incoming_pis = new StaticMessageSet<MessageType>();
		private StaticMessageSet<MessageType> outgoing_lambdas = new StaticMessageSet<MessageType>();
	}
	
	public static class StaticChildManager<MessageType extends Message>
	{
		public MessageSet<MessageType> getIncomingLambdas() {
			return this.incoming_lambdas;
		}
		
		public MessageSet<MessageType> getOutgoingPis()
		{
			return this.outgoing_pis;
		}	
		
		public int newChild(MessageType out_pi, MessageType lambda)
		{
			this.outgoing_pis.add(out_pi);
			this.incoming_lambdas.add(lambda);
			return this.outgoing_pis.size()-1;
		}

		public void removeChild(int index) throws BNException {
			if(index >= this.outgoing_pis.size())
				throw new BNException("Attempted to remove non-existant child interface.");
			this.outgoing_pis.remove(index);
			this.incoming_lambdas.remove(index);
		}
		
		public void resetMessages()
		{
			for(MessageType msg : this.outgoing_pis)
				msg.setInitial();
		}

		private StaticMessageSet<MessageType> incoming_lambdas = new StaticMessageSet<MessageType>();
		private StaticMessageSet<MessageType> outgoing_pis = new StaticMessageSet<MessageType>();
	}
}
