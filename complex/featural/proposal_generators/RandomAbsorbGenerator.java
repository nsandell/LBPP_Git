package complex.featural.proposal_generators;

import java.util.Vector;

import complex.featural.ModelController;
import complex.featural.ModelController.LatentPair;
import complex.featural.IChildProcess;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;

public class RandomAbsorbGenerator implements ProposalGenerator
{
	public RandomAbsorbGenerator(double pabs, double pexp)
	{
		this.pexp = pexp;
		this.pabs = pabs;
	}
	
	public String name()
	{
		return "Random Absorbs";
	}
	
	public Proposal generate(ModelController cont)
	{
		int N = cont.getLatentNodes().size();
		
		LatentPair pair = cont.randomLatentPair();
		
		if(pair==null)
			return null;
		
		cont.log("Proposing that node " + pair.l1.getName() + " adds all the children of node " + pair.l2.getName());
		
		Vector<IChildProcess> newNds = new Vector<IChildProcess>();
		for(IChildProcess child : cont.getChildren(pair.l2))
			if(!cont.getChildren(pair.l1).contains(child))
				newNds.add(child);
		
		if(newNds.size()==0)
			return null;

		cont.log("Proposing that node " + pair.l1.getName() + " adds all the children of node " + pair.l2.getName());
		
		double fp = this.pabs/((double)(N-1)*N);
		double bp = this.pexp/((double)(N-1)*N);
		
		return new Proposal(fp,bp,new ProposalAction.ConnectAction(pair.l1, newNds));
	}
	
	double pabs, pexp;
}
