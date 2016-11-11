package org.onosproject.ovsdbrest.rest;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.ovsdbrest.OvsdbRestException;
import org.onosproject.ovsdbrest.OvsdbRestService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.InputStream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * REST APIs for create/delete a bridge and create a port.
 */

@Path("/ovsdb")
public class BridgePortWebResource extends AbstractWebResource {
    private final Logger log = getLogger(getClass());

    @GET
    @Path("/test")
    public Response getTest() {
        ObjectNode responseBody = new ObjectNode(JsonNodeFactory.instance);
        responseBody.put("message", "it works!");
        return Response.status(200).entity(responseBody).build();
    }

    @POST
    @Path("/{ovsdb-ip}/bridge/{bridge-name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response addBridge(InputStream stream,
                              @PathParam("ovsdb-ip") String ovsdbIp,
                              @PathParam("bridge-name") String bridgeName) {
        try {
            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            OvsdbRestService ovsdbRestService = get(OvsdbRestService.class);
            ovsdbRestService.createBridge(ovsdbAddress, bridgeName);
            return Response.status(200).build();
        } catch (OvsdbRestException.BridgeAlreadyExistsException ex) {
            return Response.status(Response.Status.CONFLICT).entity("A bridge with this name already exists").build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{ovsdb-ip}/bridge/{bridge-name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteBridge(InputStream stream,
                                 @PathParam("ovsdb-ip") String ovsdbIp,
                                 @PathParam("bridge-name") String bridgeName) {
        try {

            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            OvsdbRestService ovsdbRestService = get(OvsdbRestService.class);
            ovsdbRestService.deleteBridge(ovsdbAddress, bridgeName);
            return Response.status(200).build();
        } catch (OvsdbRestException.BridgeNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity("No bridge found with the specified name").build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @POST
    @Path("/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addPort(InputStream stream,
                            @PathParam("ovsdb-ip") String ovsdbIp,
                            @PathParam("bridge-name") String bridgeName,
                            @PathParam("port-name") String portName) {
        try {
            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            OvsdbRestService ovsdbRestService = get(OvsdbRestService.class);
            ovsdbRestService.addPort(ovsdbAddress, bridgeName, portName);
            return Response.status(200).build();
        } catch (OvsdbRestException.BridgeNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity("No bridge found with the specified name").build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response deletePort(InputStream stream,
                               @PathParam("ovsdb-ip") String ovsdbIp,
                               @PathParam("bridge-name") String bridgeName,
                               @PathParam("port-name") String portName) {
        try {
            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            OvsdbRestService ovsdbRestService = get(OvsdbRestService.class);
            ovsdbRestService.removePort(ovsdbAddress, bridgeName, portName);
            return Response.status(200).build();
        } catch (OvsdbRestException.BridgeNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity("No bridge found with the specified name").build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @POST
    @Path("/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}/patch_peer/{patch-peer}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response setPatchPeer(InputStream stream,
                                 @PathParam("ovsdb-ip") String ovsdbIp,
                                 @PathParam("bridge-name") String bridgeName,
                                 @PathParam("port-name") String portName,
                                 @PathParam("patch-peer") String patchPeer) {
        try {
            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            OvsdbRestService ovsdbRestService = get(OvsdbRestService.class);
            ovsdbRestService.setPatchPeer(ovsdbAddress, bridgeName, portName, patchPeer);
            return Response.status(200).build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @POST
    @Path("/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}/gre/{remote-ip}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addGreTunnel(InputStream stream,
                                 @PathParam("ovsdb-ip") String ovsdbIp,
                                 @PathParam("bridge-name") String bridgeName,
                                 @PathParam("port-name") String portName,
                                 @PathParam("remote-ip") String remoteIp) {
        try {
            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            IpAddress tunnelRemoteIp = IpAddress.valueOf(remoteIp);
            OvsdbRestService ovsdbRestService = get(OvsdbRestService.class);
            ovsdbRestService.createGreTunnel(ovsdbAddress, bridgeName, portName, tunnelRemoteIp);
            return Response.status(200).build();
        } catch (OvsdbRestException.BridgeNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity("No bridge found with the specified name").build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{ovsdb-ip}/bridge/{bridge-name}/port/{port-name}/gre")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteGreTunnel(InputStream stream,
                                    @PathParam("ovsdb-ip") String ovsdbIp,
                                    @PathParam("bridge-name") String bridgeName,
                                    @PathParam("port-name") String portName) {
        try {
            IpAddress ovsdbAddress = IpAddress.valueOf(ovsdbIp);
            OvsdbRestService ovsdbRestService = get(OvsdbRestService.class);
            ovsdbRestService.deleteGreTunnel(ovsdbAddress, bridgeName, portName);
            return Response.status(200).build();
        } catch (OvsdbRestException.OvsdbDeviceException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }
}
