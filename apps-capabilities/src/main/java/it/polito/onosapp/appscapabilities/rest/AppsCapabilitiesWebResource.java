package it.polito.onosapp.appscapabilities.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.polito.onosapp.appscapabilities.AppsCapabilitiesRestService;
import it.polito.onosapp.appscapabilities.org.onosproject.appscapabilities.functionalcapability.FunctionalCapability;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * REST APIs for create/delete a bridge and create a port.
 */
@Path("capability")
public class AppsCapabilitiesWebResource extends AbstractWebResource {
    private final Logger log = getLogger(getClass());

    @GET
    @Path("/test")
    public Response getTest() {
        ObjectNode responseBody = new ObjectNode(JsonNodeFactory.instance);
        responseBody.put("message", "it works!");
        return Response.status(200).entity(responseBody).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllCapabilities() throws IOException {

        AppsCapabilitiesRestService appsCapabilitiesRestService = get(AppsCapabilitiesRestService.class);
        List<FunctionalCapability> fcList = new LinkedList<>();
        fcList.addAll(appsCapabilitiesRestService.getFunctionalCapabilities());

        if (fcList != null) {
            // non sono riuscito a far funzionare in automatico jackson
            JsonNode jsonNode = jacksonObjectToJson(fcList);
            ObjectNode responseBody = new ObjectNode(JsonNodeFactory.instance);
            // TODO non riesco a tornare una lista! ho dovuto incapsularla in un dict
            responseBody.putPOJO("functional-capabilities", jsonNode);
            return Response.ok(responseBody).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/{app-name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAppCapability(@PathParam("app-name") String appName) throws IOException {

        AppsCapabilitiesRestService appsCapabilitiesRestService = get(AppsCapabilitiesRestService.class);
        FunctionalCapability fc = appsCapabilitiesRestService.getFunctionalCapability(appName);

        if (fc != null) {
            // non sono riuscito a far funzionare in automatico jackson
            JsonNode jsonNode = jacksonObjectToJson(fc);
            return Response.ok(jsonNode).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private JsonNode jacksonObjectToJson(Object o) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String json = mapper.writeValueAsString(o);
        return mapper.readTree(json);
    }
}
