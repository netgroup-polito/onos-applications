package it.polito.onosapp.appscapabilities.rest;

import org.onlab.rest.AbstractWebApplication;

import java.util.Set;

/**
 * Loaders of web resource classes.
 */
public class AppsCapabilitiesRestApp extends AbstractWebApplication {

    @Override
    public Set<Class<?>> getClasses() {
        return getClasses(AppsCapabilitiesWebResource.class);
    }
}
