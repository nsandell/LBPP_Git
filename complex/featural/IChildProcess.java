package complex.featural;

import complex.CMException;
import complex.metrics.Coherence.DisagreementMeasure;

public interface IChildProcess extends DisagreementMeasure{
	String getName();
	void backupParameters() throws CMException;
	void restoreParameters() throws CMException;
	public void addParent(IParentProcess parent);
	public void killParent(IParentProcess parent);
	public void optimize();
}
