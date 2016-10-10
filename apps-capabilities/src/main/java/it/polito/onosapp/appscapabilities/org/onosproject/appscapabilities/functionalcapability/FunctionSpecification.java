package it.polito.onosapp.appscapabilities.org.onosproject.appscapabilities.functionalcapability;

/**
 * Created by gabriele on 09/10/16.
 */
public class FunctionSpecification {

    private String name;
    private String value;
    private String unit;
    private String mean;

    public FunctionSpecification(String name, String value, String unit, String mean) {
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.mean = mean;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getMean() {
        return mean;
    }

    public void setMean(String mean) {
        this.mean = mean;
    }
}
