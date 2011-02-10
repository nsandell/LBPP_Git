package bn.impl.dynbn;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

import bn.BNException;
import bn.messages.Message;
import bn.messages.MessageSet;

public class DynamicContextManager
{

	public static class DynamicMessageSet<MessageType extends Message> implements MessageSet<MessageType>
	{
		private class DMSIterator implements Iterator<MessageType>
		{
			public DMSIterator(Iterator<MessageType> it1, Iterator<MessageType> it2)
			{
				if(it1.hasNext())
					this.it1 = it1;
				this.it2 = it2;
			}

			@Override
			public boolean hasNext() {
				if(it1!=null)
					return it1.hasNext();
				else
					return it2.hasNext();
			}

			@Override
			public MessageType next() throws NoSuchElementException
			{
				if(it1!=null)
				{
					try {
						return it1.next();
					} catch(NoSuchElementException e) {
						it1 = null;
						return it2.next();
					}
				}
				else
					return it2.next();
			}

			public void remove(){throw new UnsupportedOperationException();}
	
			private Iterator<MessageType> it1 = null, it2 = null;
		}
		
		@Override
		public Iterator<MessageType> iterator()
		{
			return new DMSIterator(this.intraMessages.iterator(), this.interMessages.iterator());
		}
		
		@Override
		public int size() {
			return this.size;
		}
		
		@Override
		public MessageType get(int index) {
			if(index >= this.intraMessages.size())
				return this.interMessages.get(index-this.intraMessages.size());
			else
				return this.intraMessages.get(index);
		}
		
		@Override
		public void remove(int index) {
			if(index >= this.intraMessages.size())
				this.interMessages.remove(index-this.intraMessages.size());
			else
				this.intraMessages.remove(index);
			this.size = this.interMessages.size() + this.intraMessages.size();
		}
		
		@Override
		public void removeAll() {
			this.interMessages.clear();
			this.intraMessages.clear();
			this.size = 0;
		}
		
		public int numIntraMessages()
		{
			return this.intraMessages.size();
		}
		
		public int numInterMessages()
		{
			return this.interMessages.size();
		}
		
		public void addInterMessage(MessageType mt)
		{
			this.interMessages.add(mt);
			this.size++;
		}
		
		public void addIntraMessage(MessageType mt)
		{
			this.intraMessages.add(mt);
			this.size++;
		}
		
		public void removeInterMessage(int index)
		{
			this.interMessages.remove(index);
			this.size--;
		}
		public void removeIntraMessage(int index)
		{
			this.intraMessages.remove(index);
			this.size--;
		}

		private int size = 0;
		private Vector<MessageType> intraMessages = new Vector<MessageType>();
		private Vector<MessageType> interMessages = new Vector<MessageType>();
	}
	
	public static class DynamicChildManager<MessageType extends Message>
	{
		public DynamicChildManager(int T)
		{
			this.T = T;
			for(int i= 0 ; i < T; i++)
			{
				this.incoming_lambdas.add(new DynamicMessageSet<MessageType>());
				this.outgoing_pis.add(new DynamicMessageSet<MessageType>());
			}
		}

		public DynamicMessageSet<MessageType> getIncomingLambdas(Integer t)
		{
			return this.incoming_lambdas.get(t);
		}

		public DynamicMessageSet<MessageType> getOutgoingPis(Integer t)
		{
			return this.outgoing_pis.get(t);
		}

		public int newIntraChild(Vector<MessageType> out_pi, Vector<MessageType> inc_lambda) throws BNException
		{
			if(out_pi.size() != this.T || inc_lambda.size() != this.T)
				throw new BNException("Expected length " + this.T + " array for intra child interface.");
			for(int i = 0; i < this.T; i++)
			{
				this.outgoing_pis.get(i).addIntraMessage(out_pi.get(i));
				this.incoming_lambdas.get(i).addIntraMessage(inc_lambda.get(i));
			}
			return this.incoming_lambdas.get(0).numIntraMessages()-1;
		}
		
		public void removeIntraChild(int index)
		{
			for(int t = 0; t < this.T; t++)
			{
				this.outgoing_pis.get(t).removeIntraMessage(index);
				this.incoming_lambdas.get(t).removeIntraMessage(index);
			}
		}
		
		public int newInterChild(Vector<MessageType> out_pi, Vector<MessageType> inc_lambda) throws BNException
		{
			if(out_pi.size() != this.T-1 || inc_lambda.size() != this.T-1)
				throw new BNException("Expected length " + (this.T-1) + " array for inter child interface.");
			for(int i = 0; i < this.T-1; i++)
			{
				this.outgoing_pis.get(i).addInterMessage(out_pi.get(i));
				this.incoming_lambdas.get(i).addInterMessage(inc_lambda.get(i));
			}
			return this.incoming_lambdas.get(0).numInterMessages()-1;
		}

		public void removeInterChild(int index)
		{
			for(int t = 0; t < this.T-1; t++)
			{
				this.outgoing_pis.get(t).removeInterMessage(index);
				this.incoming_lambdas.get(t).removeInterMessage(index);
			}
		}
		
		public void resetMessages()
		{
			for(int t = 0; t < T; t++)
				for(MessageType msg : this.outgoing_pis.get(t))
					msg.setInitial();
		}

		private int T;
		private ArrayList<DynamicMessageSet<MessageType>> incoming_lambdas = new ArrayList<DynamicMessageSet<MessageType>>(T);
		private ArrayList<DynamicMessageSet<MessageType>> outgoing_pis = new ArrayList<DynamicMessageSet<MessageType>>(T);
	}

	public static class DynamicParentManager<MessageType extends Message> 
	{
		public DynamicParentManager(int T)
		{
			this.T = T;
			for(int i= 0 ; i < T; i++)
			{
				this.outgoing_lambdas.add(new DynamicMessageSet<MessageType>());
				this.incoming_pis.add(new DynamicMessageSet<MessageType>());
			}
		}

		public DynamicMessageSet<MessageType> getOutgoingLambdas(Integer t)
		{
			return this.outgoing_lambdas.get(t);
		}

		public DynamicMessageSet<MessageType> getIncomingPis(Integer t)
		{
			return this.incoming_pis.get(t);
		}

		public int newIntraParent(Vector<MessageType> inc_pi, Vector<MessageType> out_lambda) throws BNException
		{
			if(inc_pi.size() != this.T || out_lambda.size() != this.T)
				throw new BNException("Expected length " + this.T + " array for intra child interface.");
			for(int i = 0; i < this.T; i++)
			{
				this.incoming_pis.get(i).addIntraMessage(inc_pi.get(i));
				this.outgoing_lambdas.get(i).addIntraMessage(out_lambda.get(i));
			}
			return this.outgoing_lambdas.get(0).numIntraMessages()-1;
		}
		
		public void removeIntraParent(int index)
		{
			for(int t = 0; t < this.T; t++)
			{
				this.incoming_pis.get(t).removeIntraMessage(index);
				this.outgoing_lambdas.get(t).removeIntraMessage(index);
			}
		}
		
		public int newInterParent(Vector<MessageType> inc_pi, Vector<MessageType> out_lambda) throws BNException
		{
			if(inc_pi.size() != this.T-1 || out_lambda.size() != this.T-1)
				throw new BNException("Expected length " + this.T + " array for intra child interface.");
			for(int i = 1; i < this.T; i++)
			{
				this.incoming_pis.get(i).addInterMessage(inc_pi.get(i-1));
				this.outgoing_lambdas.get(i).addInterMessage(out_lambda.get(i-1));
			}
			return this.outgoing_lambdas.get(0).numInterMessages()-1;
		}

		public void removeInterParent(int index)
		{
			for(int t = 1; t < this.T; t++)
			{
				this.incoming_pis.get(t).removeInterMessage(index);
				this.outgoing_lambdas.get(t).removeInterMessage(index);
			}
		}
		
		public void resetMessages()
		{
			for(int t = 0; t < T; t++)
				for(MessageType msg : this.outgoing_lambdas.get(t))
					msg.setInitial();
		}

		private int T;
		private ArrayList<DynamicMessageSet<MessageType>> outgoing_lambdas = new ArrayList<DynamicMessageSet<MessageType>>(T);
		private ArrayList<DynamicMessageSet<MessageType>> incoming_pis = new ArrayList<DynamicMessageSet<MessageType>>(T);
	}
	
}

