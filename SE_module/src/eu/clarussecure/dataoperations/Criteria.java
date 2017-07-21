package eu.clarussecure.dataoperations;

import java.io.Serializable;

/**
 * Created URV.
 */
public class Criteria implements Serializable {
    private String attributeName;
    private String operator;
    private String value;

    public Criteria (String attributeName, String operator, String value) {
        this.attributeName = attributeName;
        this.operator = operator;
        this.value = value;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
