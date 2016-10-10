package it.polito.onosapp.appscapabilities;

import it.polito.onosapp.appscapabilities.org.onosproject.appscapabilities.functionalcapability.FunctionalCapability;

import java.util.Set;

/**
 * APIs for ovsdb driver access.
 */
public interface AppsCapabilitiesRestService {

    /**
     * Return all the functional capabilities according with installed apps.
     */
    Set<FunctionalCapability> getFunctionalCapabilities();

    /**
     * Return the functional capability for the given app.
     * @param appName application name
     */
    FunctionalCapability getFunctionalCapability(String appName);
}
