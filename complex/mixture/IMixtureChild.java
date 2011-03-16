package complex.mixture;

import complex.IChildProcess;
import complex.IParentProcess;

public interface IMixtureChild extends IChildProcess {

	void setParent(IParentProcess rent);
}
