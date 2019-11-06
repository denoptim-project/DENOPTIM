package gui;

/**
 * Interface for parameter forms. 
 * This interface requires the existence of a methods that allow recovery of the parameters from the forms.
 * 
 * @author Marco Foscato
 */
public interface IParametersForm {
    public void putParametersToString(StringBuilder sb);
}
