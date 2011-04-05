package complex.featural.proposal_generators;

import java.util.Vector;

import util.MathUtil;

import complex.IParentProcess;
import complex.featural.FeaturalModelController;
import complex.featural.IFeaturalChild;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;

public class CoherenceUniqueParenter<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> implements ProposalGenerator<ChildProcess,ParentProcess> {
	
	public CoherenceUniqueParenter(double pcspl, double prspl, double prmerge, double pdmerge)
	{
		this.pcspl = pcspl; this.prspl = prspl; this.prmerge = prmerge; this.pdmerge = pdmerge;
	}
	double pcspl; double prspl; double prmerge; double pdmerge;

	@Override
	public Proposal<ChildProcess,ParentProcess> generate(FeaturalModelController<ChildProcess,ParentProcess> cont) {
		
		Vector<ChildProcess> children = cont.getObservedNodes();
		double[] coherence = new double[children.size()];
		double sum = 0;
		for(int i = 0; i < coherence.length; i++) {
			for(int t  = 0; t < cont.getT(); t++)
				coherence[i] = children.get(i).getDisagreement(t);
			coherence[i] += smoothness;
				
			//Coherence.coherence(children,cont.getT());
			sum += coherence[i];
		}
		for(int i = 0; i < coherence.length; i++)
			coherence[i] /= sum;
		
		int selection = MathUtil.discreteSample(coherence);
		System.out.println("Attempting to give " + children.get(selection).getName() + " a unique parent based on incoherence.");
		double fp = 1;//this.pcspl*coherence[selection] + this.prspl/coherence.length;
		double bp = 1;//this.prmerge/coherence.length/(coherence.length+1); //TODO determine if we want to exactly determine reverse move probability
	
		return new Proposal<ChildProcess,ParentProcess>(fp,bp,new ProposalAction.UniqueParentAddAction<ChildProcess, ParentProcess>(children.get(selection),true));
	}

	@Override
	public String name() {
		return "Disagreement-based unique parenter";
	}

	public double smoothness = 0;
}
