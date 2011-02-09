package bn.impl.dynbn.fragment.vertical;

/*
import java.util.ArrayList;
import bn.BNException;
import bn.distributions.Distribution;
import bn.impl.dynbn.fragment.vertical.DBNFragment;
import bn.impl.dynbn.fragment.vertical.DBNFragment.FragmentSpot;
import bn.messages.Message;
*/

public class DynamicFragmentManager//<DistributionType extends Distribution, MessageType extends Message,ValueType>  extends DynamicContextManager<DistributionType, MessageType, ValueType>
{
	
	//TODO fix all the fragment stuff when everything else is back to normal
	
	/*
	public DynamicFragmentManager(ArrayList<MessageType> local_pis, ArrayList<MessageType> local_lambda, DBNFragment.FragmentSpot spot) throws BNException
	{
		super(local_pis,local_lambda);
		this.spot = spot;
	}
	
	@Override
	public DistributionType getCPD(Integer context) {
		return (context==0 && this.spot==DBNFragment.FragmentSpot.Front && this.hasInitial()) ? this.getInitial() : this.getAdvance();
	}
	DBNFragment.FragmentSpot spot;
	*/
}
