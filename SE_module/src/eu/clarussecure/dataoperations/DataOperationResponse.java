package eu.clarussecure.dataoperations;

/**
 * Parent class for data objects returned by the get
 * operation. Classes which contain the reconstructed
 * results returned from a get (inbound) call should
 * extend this class. Contains the information needed
 * by the protocol module to build the response to the
 * user application.
 */
public abstract class DataOperationResponse extends DataOperationResult {

    /**
     * Reconstructed data.
     */
	
	private String[] attributeNames;
    private String[][] contents;

    public String[][] getContents() {
        return contents;
    }
    
    public String[] getattributeNames() {
        return attributeNames;
    }
}
