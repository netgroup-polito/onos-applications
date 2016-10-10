package it.polito.onosapp.appscapabilities.org.onosproject.appscapabilities.functionalcapability;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by gabriele on 09/10/16.
 */

public class FunctionalCapability {

    private String name;
    private String type;
    private boolean ready;
    private String family;
    private URI template;
    private Resources resources;
    @JsonProperty("function-specifications")
    private FunctionSpecifications functionSpecifications = new FunctionSpecifications();

    public FunctionalCapability(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public FunctionalCapability(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public URI getTemplate() {
        return template;
    }

    public void setTemplate(URI template) {
        this.template = template;
    }

    public Resources getResources() {
        return resources;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    public FunctionSpecifications getFunctionSpecifications() {
        return functionSpecifications;
    }

    public void setFunctionSpecifications(FunctionSpecifications functionSpecifications) {
        this.functionSpecifications = functionSpecifications;
    }

    // this class is because with YANG we obtain distorted JSONs (list inside container)
    public class FunctionSpecifications {

        @JsonProperty("function-specification")
        private Set<FunctionSpecification> functionSpecifications = new HashSet<>();

        public Set<FunctionSpecification> getFunctionSpecifications() {
            return functionSpecifications;
        }

        public void setFunctionSpecifications(Set<FunctionSpecification> functionSpecifications) {
            this.functionSpecifications = functionSpecifications;
        }
    }
}
