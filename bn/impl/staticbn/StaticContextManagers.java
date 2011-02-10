package bn.impl.staticbn;

import java.util.Iterator;
import java.util.Vector;

import bn.BNException;
import bn.messages.Message;
import bn.messages.MessageSet;

public class StaticContextManagers
{
	
	private static class StaticMessageSet<MessageType extends Message> implements MessageSet<MessageType>
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
	
	public static class StaticMessageIndex
	{
		StaticMessageIndex(int index)
		{
			this.index = index;
		}
		int index;
	}
	
	private static class InterfaceManager<MessageType extends Message>
	{
		public MessageSet<MessageType> getPis()
		{
			return this.pis;
		}

		public MessageSet<MessageType> getLambdas()
		{
			return this.lambdas;
		}
		
		public StaticMessageIndex newIntf(MessageType pi, MessageType lambda) 
		{
			this.pis.add(pi);
			this.lambdas.add(lambda);
			StaticMessageIndex idx = new StaticMessageIndex(this.pis.size()-1);
			this.messageIndices.add(idx);
			return idx;
		}
		
		public void removeIntf(StaticMessageIndex index) throws BNException
		{
			if(index.index>=this.pis.size())
				throw new BNException("Attempted to remove nonexistant parent..");
			this.pis.remove(index.index);
			this.lambdas.remove(index.index);
			for(StaticMessageIndex idx : this.messageIndices)
				if(idx.index > index.index)
					idx.index--;
		}	
		
		public void clear()
		{
			this.messageIndices.clear();
			this.lambdas.removeAll();
			this.pis.removeAll();
		}
		
		public void resetLambdas()
		{
			for(MessageType msg : this.lambdas)
				msg.setInitial();
		}

		public void resetPis()
		{
			for(MessageType msg : this.pis)
				msg.setInitial();
		}
		
		private Vector<StaticMessageIndex> messageIndices = new Vector<StaticMessageIndex>();
		private StaticMessageSet<MessageType> pis = new StaticMessageSet<MessageType>();
		private StaticMessageSet<MessageType> lambdas = new StaticMessageSet<MessageType>();
	}

	public static class StaticParentManager<MessageType extends Message>
	{
		public MessageSet<MessageType> getIncomingPis()
		{
			return this.intfMgr.getPis();
		}

		public MessageSet<MessageType> getOutgoingLambdas()
		{
			return this.intfMgr.getLambdas();
		}
		
		public StaticMessageIndex newParent(MessageType inc_pi, MessageType outgoing_lambda) 
		{
			return this.intfMgr.newIntf(inc_pi, outgoing_lambda);
		}
		
		public void removeParent(StaticMessageIndex index) throws BNException
		{
			this.intfMgr.removeIntf(index);
		}
		
		public void resetMessages()
		{
			this.intfMgr.resetLambdas();
		}
		
		public void clear()
		{
			this.intfMgr.clear();
		}
	
		private InterfaceManager<MessageType> intfMgr = new InterfaceManager<MessageType>();
	}
	
	public static class StaticChildManager<MessageType extends Message>
	{
		public MessageSet<MessageType> getIncomingLambdas()
		{
			return this.intfMgr.getLambdas();
		}
		
		public MessageSet<MessageType> getOutgoingPis()
		{
			return this.intfMgr.getPis();
		}	
		
		public StaticMessageIndex newChild(MessageType out_pi, MessageType lambda)
		{
			return this.intfMgr.newIntf(out_pi, lambda);
		}

		public void removeChild(StaticMessageIndex index) throws BNException {
			this.intfMgr.removeIntf(index);
		}
		
		public void resetMessages()
		{
			this.intfMgr.resetPis();
		}
		
		public void clear()
		{
			this.intfMgr.clear();
		}

		private InterfaceManager<MessageType> intfMgr = new InterfaceManager<MessageType>();
	}
}
