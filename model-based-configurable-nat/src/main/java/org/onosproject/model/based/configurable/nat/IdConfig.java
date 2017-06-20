package org.onosproject.model.based.configurable.nat;

import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gabriele on 20/06/17.
 */
public class IdConfig extends Config<ApplicationId> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String USER_ID = "user-id";
    private static final String GRAPH_ID = "graph-id";
    private static final String NF_ID = "nf-id";

    public String getId() {

        log.debug(object.toString());
        String user_id = object.path(USER_ID).textValue();
        String graph_id = object.path(GRAPH_ID).textValue();
        String nf_id = object.path(NF_ID).textValue();

        return user_id + "/" + graph_id + "/" + nf_id;
    }
}
