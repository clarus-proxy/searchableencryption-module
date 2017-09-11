/*******************************************************************************
 * Copyright (c) 2017, EURECOM
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *     - Neither the name of EURECOM nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact: Monir AZRAOUI, Melek ÖNEN, Refik MOLVA
 * name.surname(at)eurecom(dot)fr
 *
*******************************************************************************/
package eu.clarussecure.dataoperations.SEmodule;

import java.io.InputStream;
import java.util.Map;

import eu.clarussecure.dataoperations.Criteria;
import eu.clarussecure.dataoperations.DataOperationCommand;

public class SearchableEncryptionCommand extends DataOperationCommand {
    /**
     *
     */
    private static final long serialVersionUID = -779784486199839788L;

    public SearchableEncryptionCommand() {
        super();
    }

    /**
     * Names of the protected attributes
     */
    private String[] protectedAttributeNames;

    /**
     * Names of the binary attributes contained in
     * the InputStream extraBinaryContent.
     */
    private String[] extraProtectedAttributeNames;

    /**
     * Binary data to append to the call. The
     * internal structure and parsing of this
     * data is the responsibility of the module
     * developer.
     */
    private InputStream[] extraBinaryContent;

    /**
     * Mapping between the original attribute names
     * and the protected attribute names.
     */
    private Map<String, String> mapping;

    /**
     * Protected content. Formatted the same
     * way as the original contents.
     */
    private String[][] protectedContents;

    /**
     * Search criteria
     */
    protected Criteria[] criteria;

    @Override
    public String[] getProtectedAttributeNames() {
        return protectedAttributeNames;
    }

    @Override
    public String[] getExtraProtectedAttributeNames() {
        return extraProtectedAttributeNames;
    }

    @Override
    public InputStream[] getExtraBinaryContent() {
        return extraBinaryContent;
    }

    @Override
    public Map<String, String> getMapping() {
        return mapping;
    }

    @Override
    public Criteria[] getCriteria() {
        return criteria;
    }

    @Override
    public String[][] getProtectedContents() {
        return protectedContents;
    }

    @Override
    public void setProtectedAttributeNames(String[] protectedAttributeNames) {
        this.protectedAttributeNames = protectedAttributeNames;
    }

    @Override
    public void setExtraProtectedAttributeNames(String[] extraProtectedAttributeNames) {
        this.extraProtectedAttributeNames = extraProtectedAttributeNames;
    }

    @Override
    public void setProtectedContents(String[][] protectedContents) {
        this.protectedContents = protectedContents;
    }

    @Override
    public void setExtraBinaryContent(InputStream[] extraBinaryContent) {
        this.extraBinaryContent = extraBinaryContent;
    }

    @Override
    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }

}
