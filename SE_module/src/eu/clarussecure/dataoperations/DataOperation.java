package eu.clarussecure.dataoperations;

import java.util.List;

/**
 * CLARUS Data Operation module interface.
 */
public interface DataOperation {
    /** Outbound GET operation.
     * @param attributeNames names of the attributes, as given in the security policy for the current dataset.
     * @param criteria conditions of the get call, in the same order as the attributeNames.
     * @return a List of DataOperationCommands.
     */
    public List<DataOperationCommand> get(String[] attributeNames, Criteria[] criteria);

    /** Inbound GET operation (RESPONSE), reconstructs data received by CSP.
     * @param promise references to the original call
     * @param contents data returned by the CSP.
     * @return a list of DataOperationResults. Note that DataOperationCommand
     *         and DataOperationResponse might be returned. The protocol module
     *         should differenciate the two kinds of results via a 'instanceof'
     *         instruction. DataOperationResponses contain the reconstructed data.
     *         DataOperationCommands should trigger a new call to the CSP.
     */
    public List<DataOperationResult> get(List<DataOperationCommand> promise, List<String[][]> contents);

    /** Outbound POST Operation, modifies data according to security policy.
     * @param attributeNames names of the attributes, as given in the security policy for the current dataset.
     * @param contents unprotected records
     * @return A list of DataOperationCommands
     */
    public List<DataOperationCommand> post(String[] attributeNames, String[][] contents);

    /** Outbound PUT Operation, modifies data specified by criteria, according to security policy.
     * @param attributeNames names of the attributes, as given in the security policy for the current dataset.
     * @param criteria conditions of the get call, in the same order as the attributeNames.
     * @param contents unprotected records
     * @return a List of DataOperationCommand object, which contains the necessary information
     *         to build a call to the CSP.
     */
    public List<DataOperationCommand> put(String[] attributeNames, Criteria[] criteria, String[][] contents);

    /** Outbound DELETE Operation, deletes data specified by criteria.
     * @param attributeNames names of the attributes, as given in the security policy for the current dataset.
     * @param criteria conditions of the get call, in the same order as the attributeNames.
     */
    public List<DataOperationCommand> delete(String[] attributeNames, Criteria[] criteria);

    /** Returns the mappings between unprotected attribute names and protected attribute
     * names.
     * @param attributeNames unprotected attribute names.
     * @return a List of Mapping objects.
     */
    public List<Mapping> head(String[] attributeNames);
}
