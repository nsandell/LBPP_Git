package complex.featural.proposal_generators;

import java.util.Vector;

import complex.IParentProcess;
import complex.featural.FeaturalModelController;
import complex.featural.FeaturalModelController.LatentPair;
import complex.featural.IFeaturalChild;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;

public class RandomAbsorbGenerator<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> implements ProposalGenerator<ChildProcess,ParentProcess>
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
	
	public Proposal<ChildProcess,ParentProcess> generate(FeaturalModelController<ChildProcess,ParentProcess> cont)
	{
		int N = cont.getLatentNodes().size();
		if(N < 2)
			return null;
		
		LatentPair<ChildProcess,ParentProcess> pair = cont.randomLatentPair();
		
		if(pair==null)
			return null;
		
		cont.log("Proposing that node " + pair.l1.getName() + " adds all the children of node " + pair.l2.getName());
		
		Vector<ChildProcess> newNds = new Vector<ChildProcess>();
		for(ChildProcess child : cont.getChildren(pair.l2))
			if(!cont.getChildren(pair.l1).contains(child))
				newNds.add(child);
		
		if(newNds.size()==0)
			return null;

		cont.log("Proposing that node " + pair.l1.getName() + " adds all the children of node " + pair.l2.getName());
		
		double fp = this.pabs/((double)(N-1)*N);
		double bp = this.pexp/((double)(N-1)*N);
		
		return new Proposal<ChildProcess,ParentProcess>(fp,bp,new ProposalAction.ConnectAction<ChildProcess,ParentProcess>(pair.l1, newNds));
	}
	
	double pabs, pexp;
}
