package bn.impl.dynbn;

import java.io.PrintStream;

import java.util.ArrayList;

import util.MathUtil;

import bn.BNException;
import bn.distributions.CountdownDistribution;
import bn.distributions.CountdownDistribution.CountdownStatistic;
import bn.distributions.CountdownDistribution.SwitchingCountdownDistribution;
import bn.distributions.CountdownDistribution.SwitchingCountdownDistribution.SwitchingCDStat;
import bn.distributions.Distribution;
import bn.distributions.Distribution.SufficientStatistic;
import bn.dynamic.ICountdownNode;
import bn.impl.dynbn.DynamicContextManager.DynamicMessageIndex;
import bn.impl.dynbn.DynamicContextManager;
import bn.impl.dynbn.DynamicContextManager.DynamicMessageSet;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.FiniteDiscreteMessage.FDiscMessageInterfaceSet;
import bn.messages.Message.MessageInterfaceSet;
import bn.messages.MessageSet;

public class CountdownNode2 extends DBNNode implements ICountdownNode {
	
	//This node will take a single, binary parent that will switch its distribution.
	//TODO right now this can be used to sample or infer, but not both!
	
	public CountdownNode2(DynamicBayesianNetwork net, String name, int maxlen)
	{
		super(net,name);
		this.lambdas = new ArrayList<FiniteDiscreteMessage>();
		this.interlambdas = new ArrayList<FiniteDiscreteMessage>();
		this.pis = new ArrayList<FiniteDiscreteMessage>();
		this.interpis = new ArrayList<FiniteDiscreteMessage>();
		this.marginal = new ArrayList<FiniteDiscreteMessage>();
		this.truncation = maxlen;
		this.values = new Integer[net.getT()];
		
		this.childrenMessages = new DynamicContextManager.DynamicChildManager<FiniteDiscreteMessage>(net.getT());
		
		for(int i = 0; i <net.getT(); i++)
		{
			this.lambdas.add(FiniteDiscreteMessage.normalMessage(maxlen));
			this.interlambdas.add(FiniteDiscreteMessage.normalMessage(maxlen));
			this.interpis.add(FiniteDiscreteMessage.normalMessage(maxlen));
			this.pis.add(FiniteDiscreteMessage.normalMessage(maxlen));
			this.marginal.add(FiniteDiscreteMessage.normalMessage(maxlen));
		}
	}

	@Override
	public double betheFreeEnergy() throws BNException{
		double bfe = 0;
		for(int t = 0; t < this.getNetwork().T; t++)
		{
			bfe += this.betheFreeEnergy(t);
		}
		return bfe;
	}
	
	private double betheFreeEnergy(int t) throws BNException {
		
		double E = 0, H1 = 0, H2 = 0;
	
		return 0;
		// For the moment, treat like a two-state variable 
		
		/*if(t==0)
		{
			for(int i = 0; i < this.truncation; i++)
			{
				double p = dist.distributions[0].getDist()[i];
				E -= p*Math.log(p);
				H2 += this.marginal.get(0).getValue(i)*Math.log(this.marginal.get(0).getValue(i));
			}
			H2 *= (this.intraChildren.size()+1);
		}
		else
		{
			FiniteDiscreteMessage incppi = this.interpis.get(t-1);
			FiniteDiscreteMessage incspi = this.switchingpis.get(t-1);
			double[][] p = new double[this.truncation][2];
			double sum = 0;
			for(int i = 0; i < this.truncation; i++)
			{
				p[i][0] = incppi.getValue(0)*incspi.getValue(0)*dist.distributions[0].getDist()[i]*this.lambdas.get(t).getValue(i);
				p[i][1] = incppi.getValue(0)*incspi.getValue(1)*dist.distributions[1].getDist()[i]*this.lambdas.get(t).getValue(i);
				sum += p[i][0] + p[i][1];
			}
			for(int i = 0; i < this.truncation; i++)
			{
				E -= p[i][0]*Math.log(dist.distributions[0].getDist()[i]);
				E -= p[i][1]*Math.log(dist.distributions[1].getDist()[i]);
				H1 += p[i][0]*Math.log(p[i][0]);
				H1 += p[i][1]*Math.log(p[i][1]);
			}
			FiniteDiscreteMessage marg = this.marginal.get(t);
			for(int i = 0; i < this.truncation; i++)
				H2 += marg.getValue(i)*Math.log(marg.getValue(i));
			H2*= (1+this.intraChildren.size());
		}*/
		
		// For E, only uncertain terms matter, so only zt-1 = 
		
		//return E+H1-H2;
	}

	@Override
	public SufficientStatistic getSufficientStatistic() throws BNException {
		
		SwitchingCDStat stat = this.dist.getSufficientStatisticObj();
		
		FiniteDiscreteMessage inc_pi0 = new FiniteDiscreteMessage(this.truncation);
		inc_pi0.setValue(0, 1.0);
		stat.update(inc_pi0,this.lambdas.get(0), new double[]{1.0,0.0});
		
		for(int t = 1; t < this.getNetwork().getT(); t++)
		{
			DynamicMessageSet<FiniteDiscreteMessage> incLambs = this.childrenMessages.getIncomingLambdas(t-1);
			double p0 = 1;
			double p1 = 1;
			for(FiniteDiscreteMessage msg : incLambs)
			{
				p0 *= msg.getValue(0);
				p1 *= msg.getValue(1);
			}
			p0 = p0/(p0+p1); p1 = 1-p0;
			FiniteDiscreteMessage pi_prev = this.pis.get(t-1);
			FiniteDiscreteMessage inc_pi = new FiniteDiscreteMessage(this.truncation);
			inc_pi.setValue(0,p0*pi_prev.getValue(0));
			for(int i = 1; i < pi_prev.getCardinality(); i++)
				inc_pi.setValue(i, p1*pi_prev.getValue(i));
			inc_pi.normalize();
			stat.update(inc_pi, this.lambdas.get(t),new double[]{this.switchingpis.get(t-1).getValue(0),this.switchingpis.get(t-1).getValue(1)});
			//stat.stats[0].update(inc_pi, this.lambdas.get(t),this.interpis.get(t).getValue(0));
			//stat.stats[1].update(inc_pi, this.lambdas.get(t),this.interpis.get(t).getValue(1));
		}
		return stat;
	}

	@Override
	public String getNodeDefinition() {
		String def = this.getName() + ":CountDownNode("+this.truncation+")\n";
		def += this.getName() + "__DIST < " + this.dist.getDefinition() + "\n";
		def += this.getName() + "~" + this.getName() + "__DIST\n\n";
		return def;
	}

	@Override
	public void sample() {
//		if(!this.sample) //TODO This won't work until have parent values
//			return;
//		//Sample initial value
//		double[] dist = this.dist.getDist();
//		this.values[0] = MathUtil.discreteSample(dist);
//		int t = 0;
//		while(t < this.getNetwork().getT()-1)
//		{
//			if(values[t]==0)
//			{
//				t++;
//				values[t] = MathUtil.discreteSample(dist);
//			}
//			while(t < this.getNetwork().getT()-1 && values[t] > 0)
//			{
//				t++;
//				values[t] = values[t-1]-1;
//			}
//		}
	}

	@Override
	public void setSample(boolean sample) {
		this.sample = sample;
	}
	boolean sample = true;
	

	@Override
	public void clearEvidence()
	{
		for(int i = 0; i < this.getNetwork().getT(); i++)
			this.values[i] = null;
	}

	@Override
	public double conditionalLL(int t) {
		return this.values[t]==null ? 0 : this.marginal.get(t).getValue(this.values[t]);
	}

	@Override
	public void setInitialDistribution(Distribution dist) throws BNException {
		throw new BNException("Countdown nodes do not accept initial distributions");
	}

	@Override
	public void setAdvanceDistribution(Distribution dist) throws BNException {
		if(!(dist instanceof SwitchingCountdownDistribution))
			throw new BNException("Attempted to se countdown node distribution to something other than a countdown distribution.");
		this.dist = (SwitchingCountdownDistribution)dist;
		if(this.dist.distributions.length!=2)
		{
			this.dist=null;
			throw new BNException("Need switching countdown distribution with 2 distributions.");
		}
	}

	@Override
	public Distribution getAdvanceDistribution() {
		return this.dist;
	}

	@Override
	public Distribution getInitialDistribution() {
		return null;
	}

	@Override
	protected MessageInterfaceSet<?> newChildInterface(int T)
			throws BNException {
		FDiscMessageInterfaceSet ret = new FDiscMessageInterfaceSet(2);
		for(int i = 0; i < T; i++)
		{
			ret.lambda_v.add(FiniteDiscreteMessage.normalMessage(2));
			FiniteDiscreteMessage pi = new FiniteDiscreteMessage(2);
			pi.setValue(0, this.marginal.get(i).getValue(0));
			pi.setValue(1, 1-pi.getValue(0));
			ret.pi_v.add(pi);
		}
		return ret;
	}

	@Override
	protected DynamicMessageIndex addInterParentInterface(
			MessageInterfaceSet<?> mia) throws BNException {
		if(this.switchingpis!=null)
			throw new BNException("Switching countdown already has parent.");
		if(!(mia instanceof FDiscMessageInterfaceSet))
				throw new BNException("Switching parent must be discrete and binary.");
		FDiscMessageInterfaceSet set = (FDiscMessageInterfaceSet)mia;
		if(set.lambda_v.get(0).getCardinality()!=2)
				throw new BNException("Switching parent must be discrete and binary.");
		this.switchingpis = new ArrayList<FiniteDiscreteMessage>(set.pi_v); 
		this.switchingoutlambs = new ArrayList<FiniteDiscreteMessage>(set.lambda_v); 
		return new DynamicMessageIndex(0);
	}

	@Override
	protected DynamicMessageIndex addIntraParentInterface(
			MessageInterfaceSet<?> mia) throws BNException {
		throw new BNException("The only parent of a countdown node is itself.");

	}

	@Override
	protected DynamicMessageIndex addInterChildInterface(
			MessageInterfaceSet<?> mia) throws BNException {
		if(!(mia instanceof FDiscMessageInterfaceSet))
			throw new BNException("Attempted to add a child interface that is not a finite discrete message set.");
		FDiscMessageInterfaceSet set = (FDiscMessageInterfaceSet)mia;
		return this.childrenMessages.newInterChild(set.pi_v,set.lambda_v);
	}

	@Override
	protected DynamicMessageIndex addIntraChildInterface(
			MessageInterfaceSet<?> mia) throws BNException {
		if(!(mia instanceof FDiscMessageInterfaceSet))
			throw new BNException("Attempted to add a child interface that is not a finite discrete message set.");
		FDiscMessageInterfaceSet set = (FDiscMessageInterfaceSet)mia;
		return this.childrenMessages.newIntraChild(set.pi_v,set.lambda_v);
	}

	@Override
	protected void removeInterParentInterface(DynamicMessageIndex index)
			throws BNException {
	}

	@Override
	protected void removeIntraParentInterface(DynamicMessageIndex index)
			throws BNException {
	}

	@Override
	protected void removeInterChildInterface(DynamicMessageIndex index)
			throws BNException {
		this.childrenMessages.removeInterChild(index);
	}

	@Override
	protected void removeIntraChildInterface(DynamicMessageIndex index)
			throws BNException {
		this.childrenMessages.removeIntraChild(index);
	}

	@Override
	public void validate() throws BNException {
		//TODO Anything that can be wrong?
	}

	@Override
	protected double updateMessages(int t) throws BNException {
		
		// Update local lambda
		if(this.values[t]==null)
		{
			//TODO THIS HAS PARALELLIZATION ISSUES WILL JUST GO NONPARALLEL FOR DISSERTATION
			FiniteDiscreteMessage lambda_now = this.lambdas.get(t);

			DynamicMessageSet<FiniteDiscreteMessage> incLambs = this.childrenMessages.getIncomingLambdas(t);
			double p0 = 1;
			double p1 = 1;
			for(FiniteDiscreteMessage msg : incLambs)
			{
				p0 *= msg.getValue(0);
				p1 *= msg.getValue(1);
			}
			p0 = p0/(p0+p1); p1 = 1-p0;
			
			if(t==this.bayesNet.T-1)
			{
				lambda_now.setValue(0, p0);
				for(int i = 1; i < this.truncation; i++)
					lambda_now.setValue(i, p1);
			}
			else
			{
				FiniteDiscreteMessage inc_lambda = this.interlambdas.get(t);
	
				lambda_now.setValue(0, p0*inc_lambda.getValue(0));
				for(int i = 1; i < this.truncation; i++)
					lambda_now.setValue(i, p1*inc_lambda.getValue(i));
			}
			lambda_now.normalize();
		}
		else
		{
			FiniteDiscreteMessage lambda_now = this.lambdas.get(t);
			lambda_now.empty();
			lambda_now.setDelta(this.values[t], 1.0);
		}
		
		// Update local pi
		if(t==0)
		{
			FiniteDiscreteMessage pi_now = this.pis.get(t);
			double[] dist = this.dist.distributions[0].getDist();
			for(int i = 0; i < dist.length; i++)
				pi_now.setValue(i, dist[i]);
		}
		else 
		{
			FiniteDiscreteMessage pi_now = this.pis.get(t);
			FiniteDiscreteMessage incomingPi = this.interpis.get(t-1);
			FiniteDiscreteMessage incomingSwitch = this.switchingpis.get(t-1);
			
			double[] dist1 = this.dist.distributions[0].getDist();
			double[] dist2 = this.dist.distributions[1].getDist();
			for(int i = 0; i < this.truncation - 1; i++)
				pi_now.setValue(i,incomingPi.getValue(i+1));
			double p0 = incomingPi.getValue(0);
			pi_now.setValue(this.truncation-1, 0);
			for(int i = 0; i < this.truncation; i++)
				pi_now.setValue(i,pi_now.getValue(i) + p0*(incomingSwitch.getValue(0)*dist1[i]+incomingSwitch.getValue(1)*dist2[i]));
			pi_now.normalize(); //Probably only necessary to take care of accumulating rounding errors..
		}
		
		//Update outgoing lambdas
		if(t > 0)
		{
			FiniteDiscreteMessage outLambMe = this.interlambdas.get(t-1);
			FiniteDiscreteMessage localLambda = this.lambdas.get(t);
			FiniteDiscreteMessage incomingSwitch = this.switchingpis.get(t-1);
			FiniteDiscreteMessage incomingPiMe = this.interpis.get(t-1);
			FiniteDiscreteMessage outgoingSwitch = this.switchingoutlambs.get(t-1);
		
			for(int i = 1; i < this.truncation; i++)
				outLambMe.setValue(i, localLambda.getValue(i-1));
			double sum = 0;
			double[] dist1 = this.dist.distributions[0].getDist();
			double[] dist2 = this.dist.distributions[1].getDist();
			
			double ps0 = incomingSwitch.getValue(0);
			double ps1 = incomingSwitch.getValue(1);
			ps0 = ps0/(ps0+ps1);ps1 = 1-ps0;
			for(int i = 0; i < this.truncation; i++)
				sum += localLambda.getValue(i)*(ps0*dist1[i]+ps1*dist2[i]);
			outLambMe.setValue(0, sum);
			
			outgoingSwitch.empty();
			double sum1 = 0;
			double sum2 = 0;
			for(int j = 0; j < this.truncation; j++)
			{
				sum1 += localLambda.getValue(j)*dist1[j]*incomingPiMe.getValue(0);
				sum2 += localLambda.getValue(j)*dist2[j]*incomingPiMe.getValue(0);
				if(j < this.truncation-1)
				{
					double tmp = localLambda.getValue(j)*incomingPiMe.getValue(j+1);
					sum1 += tmp;
					sum2 += tmp;
				}
			}
			outgoingSwitch.setValue(0, sum1);
			outgoingSwitch.setValue(1, sum2);
			outgoingSwitch.normalize();
		}
		
		//Update outgoing pis
		FiniteDiscreteMessage localPi = this.pis.get(t);
		FiniteDiscreteMessage incLambMe = null;
		if(t!=this.bayesNet.T-1)
			incLambMe = this.interlambdas.get(t);
		
		DynamicMessageSet<FiniteDiscreteMessage> incLambs = this.childrenMessages.getIncomingLambdas(t);
		DynamicMessageSet<FiniteDiscreteMessage> outPis = this.childrenMessages.getOutgoingPis(t);
		//TODO Ignoring cases where we have a value since we have more pressing concerns.,also this is inefficient
		
		FiniteDiscreteMessage prod2 = new FiniteDiscreteMessage(2);
		if(incLambMe!=null)
		{
			FiniteDiscreteMessage tmp = localPi.multiply(incLambMe);tmp.normalize();
			prod2.setValue(0, tmp.getValue(0));
			prod2.setValue(1, 1-tmp.getValue(0));
		}
		else
		{
			prod2.setValue(0, localPi.getValue(0));
			prod2.setValue(1, localPi.getValue(1));
		}
		
		for(int i = 0; i < outPis.size(); i++)
		{
			FiniteDiscreteMessage outpi = outPis.get(i);
			outpi.adopt(prod2);
			for(int j = 0; j < outPis.size(); j++)
			{
				if(i==j) continue;
				outpi = outpi.multiply(incLambs.get(j));
			}
			outpi.normalize();
		}
		
		if(t<this.bayesNet.T-1)
		{
			double p0 = 1, p1 = 1;
			for(int i = 0;  i < incLambs.size(); i++)
			{
				p0 *= incLambs.get(i).getValue(0);
				p1 *= incLambs.get(i).getValue(1);
			}
			FiniteDiscreteMessage outPiMe = this.interpis.get(t);
			outPiMe.setValue(0, p0*localPi.getValue(0));
			for(int i = 1; i < this.truncation; i++)
				outPiMe.setValue(i, p1*localPi.getValue(i));
			outPiMe.normalize();
		}
		

		FiniteDiscreteMessage newmarg = this.pis.get(t).multiply(this.lambdas.get(t));
		newmarg.normalize();
		FiniteDiscreteMessage marg = this.marginal.get(t);
		double maxerr = 0;
		for(int i = 0; i < this.truncation; i++)
			maxerr = Math.max(maxerr, Math.abs(newmarg.getValue(i)-marg.getValue(i)));
		marg.adopt(newmarg);
		return maxerr;
	}
	
	@Override 
	public Integer getValue(int t)
	{
		return this.values[t];
	}
	
	public FiniteDiscreteMessage getMarginal(int t)
	{
		return this.marginal.get(t);
	}

	@Override
	public void resetMessages() {
		for(int i = 0; i < this.getNetwork().T; i++)
		{
			this.marginal.get(i).setInitial();
			this.lambdas.get(i).setInitial();
			this.pis.get(i).setInitial();
			this.childrenMessages.resetMessages();
		}
		for(FiniteDiscreteMessage msg : interpis)
			msg.setInitial();
		for(FiniteDiscreteMessage msg : interlambdas)
			msg.setInitial();
		for(FiniteDiscreteMessage msg : switchingpis)
			msg.setInitial();
		for(FiniteDiscreteMessage msg : switchingoutlambs)
			msg.setInitial();
	}

	@Override
	public void printDistributionInfo(PrintStream ps) {
		this.dist.printDistribution(ps);
	}

	@Override
	protected double optimizeParametersI() throws BNException {
		return this.dist.optimize(this.getSufficientStatistic());
	}

	@Override
	protected double optimizeParametersI(SufficientStatistic stat)
			throws BNException {
		return this.dist.optimize(stat);
	}

	private Integer[] values;
	private int truncation;
	private SwitchingCountdownDistribution dist;
	private ArrayList<FiniteDiscreteMessage> lambdas, pis, marginal;
	private ArrayList<FiniteDiscreteMessage> interlambdas, interpis;
	private ArrayList<FiniteDiscreteMessage> switchingpis, switchingoutlambs;
	
	private DynamicContextManager.DynamicChildManager<FiniteDiscreteMessage> childrenMessages;
}
