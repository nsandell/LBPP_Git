package complex;

import bn.dynamic.IDBNNode;
import complex.metrics.Coherence.DisagreementMeasure;

public interface IChildProcess extends DisagreementMeasure{

	String getName();
	
	double parameterLL();

	void backupParameters() throws CMException;
	void restoreParameters() throws CMException;
	void optimize();
	
	IDBNNode hook();
}
