package complex.featural.proposal_generators;

import complex.featural.ModelController;
import complex.featural.ModelController.LatentPair;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;

public class RandomAbsorbGenerator implements ProposalGenerator
{
	public RandomAbsorbGenerator(double pabs, double pexp)
	{
		this.pexp = pexp;
		this.pabs = pabs;
	}
	
	public Proposal generate(ModelController cont)
	{
		int N = cont.getLatentNodes().size();
		
		LatentPair pair = cont.randomLatentPair();
		
		if(pair==null)
			return null;
		
		cont.log("Proposing that node " + pair.l1 + " adds all the children of node " + pair.l2);
		
		double fp = this.pabs/((double)(N-1)*N);
		double bp = this.pexp/((double)(N-1)*N);
		
		return new Proposal(fp,bp,new ProposalAction.ConnectAction(pair.l1, cont.getChildren(pair.l2)));
	}
	
	double pabs, pexp;
}
