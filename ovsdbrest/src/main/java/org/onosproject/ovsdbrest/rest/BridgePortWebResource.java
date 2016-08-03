package org.onosproject.ovsdbrest.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.onlab.packet.IpAddress;
import org.onosproject.ovsdbrest.OvsdbRestService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * REST APIs for create/delete a bridge and create a port.
 */
@Path("bridge")
public class BridgePortWebResource extends AbstractWebResource {
    private final Logger log = getLogger(getClass());

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response createBridge(InputStream stream) {
        try {
            JsonNode responseBody = mapper().readTree(stream);
            log.info(responseBody.toString());
            IpAddress ipAddress = IpAddress.valueOf(responseBody.path("ovsdbIp").textValue());
            String bridgeName = responseBody.path("bridgeName").asText();
            OvsdbRestService ovsdbRestService = get(OvsdbRestService.class);
            ovsdbRestService.createBridge(ipAddress, bridgeName);
            return Response.status(200).build();
        } catch (IOException ioe) {
            log.info("Json parse error: " + ioe.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }
}
