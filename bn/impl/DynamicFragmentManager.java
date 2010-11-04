package bn.impl;

import java.util.ArrayList;
import java.util.Vector;

import bn.BNException;
import bn.distributions.Distribution;
import bn.messages.Message;

public class DynamicFragmentManager<DistributionType extends Distribution, MessageType extends Message,ValueType>  extends DynamicContextManager<DistributionType, MessageType, ValueType>
{
	public DynamicFragmentManager(ArrayList<MessageType> local_pis, ArrayList<MessageType> local_lambda, DBNFragment.FragmentSpot spot) throws BNException
	{
		super(local_pis,local_lambda);
		this.spot = spot;
		/*
		if(this.spot!=DBNFragment.FragmentSpot.Back)
		{
			this.end_incoming_lambdas = new Vector<MessageType>();
			this.end_outgoing_pis = new Vector<MessageType>();
		}
		if(this.spot!=DBNFragment.FragmentSpot.Front)
		{
			this.beg_incoming_pis = new Vector<MessageType>();
			this.beg_outgoing_lambdas = new Vector<MessageType>();
		}*/
	}
	
	@Override
	public DistributionType getCPD(Integer context) {
		return (context==0 && this.spot==DBNFragment.FragmentSpot.Front && this.hasInitial()) ? this.getInitial() : this.getAdvance();
	}
	
	
	
	DBNFragment.FragmentSpot spot;
	
	/*Vector<MessageType> end_outgoing_pis;
	Vector<MessageType> end_incoming_lambdas;
	Vector<MessageType> beg_incoming_pis;
	Vector<MessageType> beg_outgoing_lambdas;*/
}
