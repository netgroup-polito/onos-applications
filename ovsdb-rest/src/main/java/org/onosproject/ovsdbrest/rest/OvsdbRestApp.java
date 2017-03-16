package org.onosproject.ovsdbrest.rest;

import org.onlab.rest.AbstractWebApplication;

import java.util.Set;

/**
 * Loaders of web resource classes.
 */
public class OvsdbRestApp extends AbstractWebApplication {

    @Override
    public Set<Class<?>> getClasses() {
        return getClasses(OvsdbBridgeWebResource.class);
    }
}
