package gui;

/**
 * Interface for parameter forms. 
 * This interface requires the existence of a methods that allow recovery of the parameters from the forms.
 * 
 * @author Marco Foscato
 */
public interface IParametersForm {
	public void importParametersFromDenoptimParamsFile(String fileName) throws Exception;
	public void importSingleParameter(String key, String value) throws Exception;
    public void putParametersToString(StringBuilder sb) throws Exception;
}
