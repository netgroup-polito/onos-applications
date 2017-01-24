package it.polito.onosapp.appscapabilities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Reference;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.app.ApplicationEvent;
import org.onosproject.app.ApplicationListener;
import org.onosproject.app.ApplicationState;
import it.polito.onosapp.appscapabilities.org.onosproject.appscapabilities.functionalcapability.FunctionalCapability;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

/**
 * Bridge and port controller.
 */
@Component(immediate = true)
@Service
public class AppsCapabilitiesComponent implements AppsCapabilitiesRestService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ApplicationId appId;
    private static final int DPID_BEGIN = 4;
    private static final int OFPORT = 6653;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationAdminService applicationAdminService;

    private Set<FunctionalCapability> functionalCapabilities;

    ApplicationListener applicationListener = new MyApplicationListener();

    @Activate
    protected void activate(BundleContext context) {
        appId = coreService.getAppId("org.onosproject.appscapabilities");

        functionalCapabilities = fetchApplicationsCapabilities();

        applicationAdminService.addListener(applicationListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        applicationAdminService.removeListener(applicationListener);

        log.info("Stopped");
    }

    /**
     * This method returns an initial set of functional capabilities based on current installed applications.
     * @return
     */

    Set<FunctionalCapability> fetchApplicationsCapabilities() {

        Set<FunctionalCapability> capabilities = new HashSet<>();

        for (Application app : applicationAdminService.getApplications()) {
            FunctionalCapability fc = fetchFunctionalCapabilityByAppName(app.id());
            if (fc != null) {
                if (applicationAdminService.getState(app.id()).equals(ApplicationState.ACTIVE)) {
                    fc.setReady(false);
                }
                capabilities.add(fc);
                log.info("Capability '{}' added.", app.id().name());
            }
        }

        return capabilities;
    }

    /**
     *  Application state listener.
     */
    private class MyApplicationListener implements ApplicationListener {

        @Override
        public void event(ApplicationEvent applicationEvent) {

            ApplicationId appId = applicationEvent.subject().id();
            String appName = appId.name();
            log.info("App '{}' event '{}'.", appName, applicationEvent.type());

            switch (applicationEvent.type()) {
                case APP_INSTALLED:
                    FunctionalCapability fc = fetchFunctionalCapabilityByAppName(appId);
                    if (fc != null) {
                        if (applicationAdminService.getState(appId).equals(ApplicationState.ACTIVE)) {
                            fc.setReady(false);
                        }
                        functionalCapabilities.add(fc);
                        log.info("Capability '{}' added.", appName);
                    }
                    break;
                case APP_UNINSTALLED:
                    for (FunctionalCapability iter : functionalCapabilities) {
                        if (iter.getName().equals(appName)) {
                            functionalCapabilities.remove(iter);
                            log.info("Capability '{}' removed.", appName);
                            break;
                        }
                    }
                    break;
                case APP_ACTIVATED:
                    for (FunctionalCapability iter : functionalCapabilities) {
                        if (iter.getName().equals(appName)) {
                            iter.setReady(false);
                            log.info("Capability '{}' disabled.", appName);
                            break;
                        }
                    }
                    break;
                case APP_DEACTIVATED:
                    for (FunctionalCapability iter : functionalCapabilities) {
                        if (iter.getName().equals(appName)) {
                            iter.setReady(true);
                            log.info("Capability '{}' enabled.", appName);
                            break;
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public boolean isRelevant(ApplicationEvent event) {
            return true;
        }
    }

    /**
     *  Should discover in some way the application capability (the app could be deactivated).
     *  @return functional capability of the app if it has one, null elsewhere
     */
    FunctionalCapability fetchFunctionalCapabilityByAppName(ApplicationId appId) {

        Application application = applicationAdminService.getApplication(appId);

        try {
            // TODO implement something better then fetching functional info from readme
            String jsonFunctionalCapability = application.readme().replace("'", "\"");
            ObjectMapper mapper = new ObjectMapper();
            FunctionalCapability fc = mapper.readValue(jsonFunctionalCapability, FunctionalCapability.class);
            log.info("Application {} has a functional capability of type: {}.", application.id().name(), fc.getType());
            return fc;
        } catch (IOException e) {
            log.debug(e.getMessage());
            log.info("Application {} does not have any functional capability.", application.id().name());
            e.printStackTrace();
            return null;
        }
        /*
        switch (appId.name()) {
            case "it.polito.onosapp.nat":
                FunctionalCapability fc = new FunctionalCapability(appId.name(), "NAT");
                fc.setReady(true);
                try {
                    fc.setTemplate(new URI("http://netgroup.polito.it/templates/nat.json"));
                } catch (URISyntaxException e) { }
                fc.setResources(null);
                fc.getFunctionSpecifications().getFunctionSpecifications().add(new FunctionSpecification(
                        "port-range",
                        "10000",
                        "-",
                        "lower_bound"
                ));
                fc.getFunctionSpecifications().getFunctionSpecifications().add(new FunctionSpecification(
                        "port-range",
                        "30000",
                        "-",
                        "upper_bound"
                ));
                return fc;
            default:
                return null;
        }
        */
    }

    private JsonNode jacksonObjectToJson(Object o) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String json = mapper.writeValueAsString(o);
        return mapper.readTree(json);
    }

    /* REST SERVICE */

    @Override
    public Set<FunctionalCapability> getFunctionalCapabilities() {

        return functionalCapabilities;
    }

    @Override
    public FunctionalCapability getFunctionalCapability(String appName) {

        for (FunctionalCapability fc : functionalCapabilities) {
            if (fc.getName().equals(appName)) {
                return fc;
            }
        }
        return null;
    }
}
