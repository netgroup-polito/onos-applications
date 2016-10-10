package it.polito.onosapp.appscapabilities;

import it.polito.onosapp.appscapabilities.org.onosproject.appscapabilities.functionalcapability.FunctionSpecification;
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
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;

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
            FunctionalCapability fc = fetchFunctionalCapabilityByAppName(app.id().name());
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
                    FunctionalCapability fc = fetchFunctionalCapabilityByAppName(appName);
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
     *  TODO For now it returns static entries for known apps
     *  @return functional capability of the app if it has one, null elsewhere
     */
    FunctionalCapability fetchFunctionalCapabilityByAppName(String appName) {

        switch (appName) {
            case "it.polito.onosapp.nat":
                FunctionalCapability fc = new FunctionalCapability(appName, "NAT");
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
                        "lower_bound"
                ));
                return fc;
            default:
                return null;
        }
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
