package org.onosproject.ovsdbrest.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.ovsdbrest.OvsdbRestException;
import org.onosproject.ovsdbrest.OvsdbRestService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
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

    @GET
    @Path("/test")
    public Response getTest() {
        return Response.status(200).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addBridge(InputStream stream) {
        try {
            ObjectNode requestBody = (ObjectNode) mapper().readTree(stream);
            log.info(requestBody.toString());
            IpAddress ipAddress = IpAddress.valueOf(requestBody.path("ovsdbIp").textValue());
            String bridgeName = requestBody.path("bridgeName").asText();
            OvsdbRestService ovsdbRestService = get(OvsdbRestService.class);
            ovsdbRestService.createBridge(ipAddress, bridgeName);
            return Response.status(200).build();
        } catch (IOException ioe) {
            log.info("Json parse error: " + ioe.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        } catch (OvsdbRestException.BridgeAlreadyExistsException ex) {
            return Response.status(Response.Status.CONFLICT).entity("A bridge with this name already exists").build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteBridge(InputStream stream,
                                 @HeaderParam("ovsdb-ip") String ovsdbIp,
                                 @HeaderParam("bridge-name") String bridgeName) {
        try {
            IpAddress ipAddress = IpAddress.valueOf(ovsdbIp);
            OvsdbRestService ovsdbRestService = get(OvsdbRestService.class);
            ovsdbRestService.deleteBridge(ipAddress, bridgeName);
            return Response.status(200).build();
        } catch (OvsdbRestException.BridgeNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity("No bridge found with the specified name").build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }
}
