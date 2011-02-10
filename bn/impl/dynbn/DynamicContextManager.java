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
	
	public static class DynamicMessageIndex implements Comparable<DynamicMessageIndex>
	{
		protected DynamicMessageIndex(int idx)
		{
			this.index = idx;
		}
		protected int index;
		
		@Override
		public int compareTo(DynamicMessageIndex o) {
			return this.index-o.index;
		}
	}
	
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
			//return new DMSIterator(this.intraMessages.iterator(), this.interMessages.iterator());
			return new DMSIterator(this.interMessages.iterator(), this.intraMessages.iterator());
		}
		
		@Override
		public int size() {
			return this.size;
		}
		
		@Override
		public MessageType get(int index) {
			if(index >= this.interMessages.size())
				return this.intraMessages.get(index-this.interMessages.size());
			else
				return this.interMessages.get(index);
		}
		
		@Override
		public void remove(int index) {
			if(index >= this.interMessages.size())
				this.intraMessages.remove(index-this.interMessages.size());
			else
				this.interMessages.remove(index);
			this.size = this.intraMessages.size() + this.interMessages.size();
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
	
	public static class DynamicInterfaceManager<MessageType extends Message>
	{
		public DynamicInterfaceManager(int T)
		{
			this.T = T;
			for(int i= 0 ; i < T; i++)
			{
				this.lambdas.add(new DynamicMessageSet<MessageType>());
				this.pis.add(new DynamicMessageSet<MessageType>());
			}
		}

		public final DynamicMessageSet<MessageType> getLambdas(Integer t)
		{
			return this.lambdas.get(t);
		}

		public final DynamicMessageSet<MessageType> getPis(Integer t)
		{
			return this.pis.get(t);
		}

		public final DynamicMessageIndex newInterInterface(Vector<MessageType> pi, Vector<MessageType> lambda, int t0, int tf) throws BNException
		{
			if(pi.size() != this.T-1 || lambda.size() != this.T-1)
				throw new BNException("Expected length " + (this.T-1) + " array for inter child interface.");
			for(int i = t0; i < tf; i++)
			{
				this.pis.get(i).addInterMessage(pi.get(i-t0));
				this.lambdas.get(i).addInterMessage(lambda.get(i-t0));
			}
			DynamicMessageIndex idx = new DynamicMessageIndex(this.interIndices.size());
			this.interIndices.add(idx);
			return idx;
		}
		
		public final void removeInterInterface(DynamicMessageIndex index, int t0, int tf)
		{
			for(int t = t0; t < tf; t++)
			{
				this.pis.get(t).removeInterMessage(index.index);
				this.lambdas.get(t).removeInterMessage(index.index);
			}
			this.interIndices.remove(index.index);
			for(int i = index.index; i < this.interIndices.size(); i++)
				this.interIndices.get(i).index = i;
		}
		
		public final DynamicMessageIndex newIntraInterface(Vector<MessageType> pi, Vector<MessageType> lambda) throws BNException
		{
			if(pi.size() != this.T || lambda.size() != this.T)
				throw new BNException("Expected length " + this.T + " array for intra child interface.");
			for(int i = 0; i < this.T; i++)
			{
				this.pis.get(i).addIntraMessage(pi.get(i));
				this.lambdas.get(i).addIntraMessage(lambda.get(i));
			}
			DynamicMessageIndex idx = new DynamicMessageIndex(this.intraIndices.size());
			this.intraIndices.add(idx);
			return idx;
		}

		public final void removeIntraInterface(DynamicMessageIndex index)
		{
			for(int t = 0; t < this.T-1; t++)
			{
				this.pis.get(t).removeIntraMessage(index.index);
				this.lambdas.get(t).removeIntraMessage(index.index);
			}
			this.intraIndices.remove(index.index);
			for(int i = index.index; i < this.intraIndices.size(); i++)
				this.intraIndices.get(i).index = i;
		}
		
		public final void resetPis()
		{
			for(int t = 0; t < T; t++)
				for(MessageType msg : this.pis.get(t))
					msg.setInitial();
		}
		
		public final void resetLambdas()
		{
			for(int t = 0; t < T; t++)
				for(MessageType msg : this.lambdas.get(t))
					msg.setInitial();
		}
		
		public final void clear()
		{
			this.interIndices.clear();
			this.intraIndices.clear();
			for(int t = 0; t < T; t++)
			{
				this.lambdas.get(t).removeAll();
				this.pis.get(t).removeAll();
			}
		}
		
		private int T;
		Vector<DynamicMessageIndex> interIndices = new Vector<DynamicContextManager.DynamicMessageIndex>();
		Vector<DynamicMessageIndex> intraIndices = new Vector<DynamicContextManager.DynamicMessageIndex>();
		private ArrayList<DynamicMessageSet<MessageType>> lambdas = new ArrayList<DynamicMessageSet<MessageType>>(T);
		private ArrayList<DynamicMessageSet<MessageType>> pis = new ArrayList<DynamicMessageSet<MessageType>>(T);
	}
	
	public static class DynamicChildManager<MessageType extends Message>
	{
		public DynamicChildManager(int T)
		{
			this.manager = new DynamicInterfaceManager<MessageType>(T);
		}

		public DynamicMessageSet<MessageType> getIncomingLambdas(Integer t)
		{
			return this.manager.getLambdas(t);
		}

		public DynamicMessageSet<MessageType> getOutgoingPis(Integer t)
		{
			return this.manager.getPis(t);
		}

		public DynamicMessageIndex newIntraChild(Vector<MessageType> out_pi, Vector<MessageType> inc_lambda) throws BNException
		{
			return this.manager.newIntraInterface(out_pi, inc_lambda);
		}
		
		public void removeIntraChild(DynamicMessageIndex index)
		{
			this.manager.removeIntraInterface(index);
		}
		
		public DynamicMessageIndex newInterChild(Vector<MessageType> out_pi, Vector<MessageType> inc_lambda) throws BNException
		{
			return this.manager.newInterInterface(out_pi, inc_lambda, 0, manager.T-1);
		}

		public void removeInterChild(DynamicMessageIndex index)
		{
			this.manager.removeInterInterface(index, 0, manager.T-1);
		}
		
		public void resetMessages()
		{
			this.manager.resetPis();
		}
		
		public void clear()
		{
			this.manager.clear();
		}

		private DynamicInterfaceManager<MessageType> manager;
	}

	public static class DynamicParentManager<MessageType extends Message> 
	{
		public DynamicParentManager(int T)
		{
			this.manager = new DynamicInterfaceManager<MessageType>(T);
		}

		public DynamicMessageSet<MessageType> getOutgoingLambdas(Integer t)
		{
			return this.manager.getLambdas(t);
		}

		public DynamicMessageSet<MessageType> getIncomingPis(Integer t)
		{
			return this.manager.getPis(t);
		}

		public DynamicMessageIndex newIntraParent(Vector<MessageType> inc_pi, Vector<MessageType> out_lambda) throws BNException
		{
			return this.manager.newIntraInterface(inc_pi, out_lambda);
		}
		
		public void removeIntraParent(DynamicMessageIndex index)
		{
			this.manager.removeIntraInterface(index);
		}
		
		public DynamicMessageIndex newInterParent(Vector<MessageType> inc_pi, Vector<MessageType> out_lambda) throws BNException
		{
			return this.manager.newInterInterface(inc_pi, out_lambda, 1, this.manager.T);
		}

		public void removeInterParent(DynamicMessageIndex index)
		{
			this.manager.removeInterInterface(index, 1, this.manager.T);
		}
		
		public void resetMessages()
		{
			this.manager.resetLambdas();
		}
		
		public void clear()
		{
			this.manager.clear();
		}

		private DynamicInterfaceManager<MessageType> manager;
	}	
}

