/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.onosproject.model.based.configurable.nat;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;

/**
 *
 * @author lara
 */
public class ConnectionModuleClient {
    public static final String BASE_URI = "http://130.192.225.154:8080/frogsssa-1.0-SNAPSHOT/ConnectionModule";
    public static Client client;
    public static WebTarget target;
    public static EventSource eventSource;
    public static StateListenerNew l;
    public String id;

    public ConnectionModuleClient(StateListenerNew l, String id){
        client = ClientBuilder.newBuilder()
                .register(SseFeature.class)
                .build();
        target = client.target(BASE_URI);
        this.l = l;
        this.id = id;
        l.log.info("Prima della create request");
        CreateRequest();
    }
    
    public void setId(String id){
        this.id = id;
    }

    public void closeSSE(){
        eventSource.close();
    }
    
    private void startSSE(String id){
        WebTarget endpoint = client.target("http://130.192.225.154:8080/frogsssa-1.0-SNAPSHOT/webresources/events").path(id);
        //WebTarget endpoint;
        eventSource = EventSource.target(endpoint).build();
        l.log.info("Ho costruito l'eventSource");
        EventListener listener = new EventListener() {
            
            @Override
            public void onEvent(InboundEvent ie) {
                System.out.println("received SSE");
                //try {
                    System.out.println(ie.getName() + " data is " +ie.readData());
                    l.log.info("++Received SSE data "+ie.readData());
                try {
                    //l.setVariable("id4", ie.readData());
                    //split and set commands
                    l.parseCommand(ie.readData());
                    /*} catch (IOException ex) {
                    Logger.getLogger(ConnectionModuleClient.class.getName()).log(Level.SEVERE, null, ex);
                    System.err.println("Closing the channel in the client");
                    }*/
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(ConnectionModuleClient.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchFieldException ex) {
                    Logger.getLogger(ConnectionModuleClient.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(ConnectionModuleClient.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(ConnectionModuleClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }  
        };
        eventSource.register(listener);
        eventSource.open();
    }
    
    public static String GETResourcesRequest(){
        String res;
        Response cr = target.request().accept("application/xml").get(Response.class);
        if(cr.getStatus()!=200){
            System.out.println("Error in the get");
            return "Error";
        }
        res = cr.getEntity().toString();
        return res;
    }
    
    public void setResourceValue(String msg){
        System.out.println("Passo al web service il valore "+msg);
        Response r = target.path(id.toString()).path("response").request().post(Entity.entity(msg, MediaType.TEXT_PLAIN), Response.class);
        if(r.getStatus()!=200){
            System.out.print("!!Error in the passage of a requested value");
        }
    }
    
    
    public String CreateRequest(){
        String res;
        l.log.info("!!-------*******Prima di mandare la create******-------!!!!");
        Response c = client.target("http://130.192.225.154:8080/frogsssa-1.0-SNAPSHOT").request().get();
        l.log.info("--Where to find web service-- "+c.getStatus()+c.getStatusInfo());
        Response cr = target.path("create").request().post(Entity.entity(id, MediaType.TEXT_PLAIN), Response.class);
        
        l.log.info("ho mandalo la richiesta, prima dello startSSE "+cr.getStatus());
//        if(cr.getStatus()!=200 && cr.getStatus()!=204){
//            System.out.println("Error in the post");
//            System.out.println(cr.getStatus());
//            return "Error";
//        }
    
        //res = cr.getEntity().toString();
        startSSE(id);
        return id;
    }
    
    public void SetDataModel(String input){
        Response cr = target.path(id).path("dataModel").request().post(Entity.entity(input, MediaType.TEXT_PLAIN), Response.class);
        if(cr.getStatus()!=204){
            System.out.println("Error in the post");
            System.out.println(cr.getStatus());
        }
    }
    
//    public boolean SetVariableCorrispondence(String x, String c){
//        String input = c;
//        Response cr = target.path(id.toString()).path(x).path("DM").request().post(Entity.entity(input, MediaType.TEXT_PLAIN), Response.class);
//        if(cr.getStatus()!=200){
//            System.out.println("Error in the post" + cr.getStatus());
//            return false;
//        }
//        if(cr.getEntity().toString().equals("true"))
//            return true;
//        else
//            return false;
//    }
//    
        public void somethingChanged(String m){
        
        Response cr = target.path(id.toString()).path("change").request().post(Entity.entity(m, MediaType.APPLICATION_JSON), Response.class);
        System.out.println("change posted, response: "+cr.getStatus());
        //SetVariableValue(s, o);
        }
    
//    public boolean SetVariableValue(String x, Object v){
//        //trasform object in json or xml!
//        String input = (new Gson()).toJson(v);
//        Response cr = target.path(id.toString()).path(x).path("Value").request().post(Entity.entity(input, MediaType.TEXT_PLAIN), Response.class);
//        if(cr.getStatus()!=200){
//            System.out.println("Error in the post" + cr.getStatus());
//            return false;
//        }
//        if(cr.getEntity().toString().equals("true"))
//            return true;
//        else
//            return false;
//    }
    
    public static String getResource(String id){
        Response cr = target.path(id).request().accept(MediaType.APPLICATION_XML).get(Response.class);
        if(cr.getStatus()!=200){
            System.out.println("Error in the get " + cr.getStatus());
            return null;
        }
        //to trasform in Resource
        return cr.getEntity().toString();
    }
    
    public static int getCount(){
        Response cr = target.path("count").request().accept(MediaType.TEXT_PLAIN).get(Response.class);
        if(cr.getStatus()!=200){
            System.out.println("Error in the get " + cr.getStatus());
            return 0;
        }
        return Integer.parseInt(cr.getEntity().toString());
    }
    
    public void deleteResources(){
        Response cr = target.path(id).request().delete();
        if(cr.getStatus()!=200){
            System.out.println("Error in the delete " + cr.getStatusInfo());
        }
    }
}
