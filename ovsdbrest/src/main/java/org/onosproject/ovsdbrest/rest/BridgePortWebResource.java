package org.onosproject.ovsdbrest.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.onlab.packet.IpAddress;
import org.onosproject.ovsdbrest.OvsdbRestService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * REST APIs for create/delete a bridge and create a port.
 */
public class BridgePortWebResource extends AbstractWebResource {
    private final Logger log = getLogger(getClass());

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createBridge(InputStream stream) {
        try {
            JsonNode requestBody = mapper().readTree(stream);
            log.info(requestBody.toString());
            IpAddress ipAddress = IpAddress.valueOf(requestBody.path("ovsdbIp").textValue());
            String bridgeName = requestBody.path("bridgeName").asText();
            OvsdbRestService ovsdbRestService = get(OvsdbRestService.class);
            ovsdbRestService.createBridge(ipAddress, bridgeName);
            return Response.status(200).build();
        } catch (IOException ioe) {
            log.info("Json parse error: " + ioe.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }
}
