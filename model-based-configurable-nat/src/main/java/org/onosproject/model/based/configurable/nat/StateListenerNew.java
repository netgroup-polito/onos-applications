/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.onosproject.model.based.configurable.nat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.Thread.sleep;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
//import jyang.parser.YANG_Body;
//import jyang.parser.YANG_Config;
//import jyang.parser.YANG_Specification;
//import jyang.parser.YangTreeNode;
//import jyang.parser.yang;
//import jyang.tools.Yang2Yin;
import java.util.TimerTask;
import java.util.Timer;
import org.onlab.packet.Ip4Address;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.slf4j.LoggerFactory;
import java.util.Properties;
import java.io.FileInputStream;
import org.onlab.packet.IpAddress;


/**
 *
 * @author lara
 */
public class StateListenerNew extends Thread{
    private static final String YINFILE = "configuration/yinFile.json";
    private static final String YANGFILE = "configuration/yangFile.yang";
    private static final String MAPPINGFILE = "configuration/mappingFile.txt";  
    private String AppId;
    protected HashMap<String, Object> state;
    protected HashMap<String, Object> stateThreshold;
    protected HashMap<String, String> lists;
    private Object root;
    private boolean stopCondition = false;
    private List<String> toListenPush;
    private HashMap<String, Threshold> toListenThreshold;
    private List<PeriodicVariableTask> toListenTimer;
    private HashMap<String, String> YangToJava;
    /**
        staticListIndexes contains well known keys of a YANG list, they are used to localize information that
        are modelled as part of a list in the YANG model but are standalone objects in the Java code
        KEY: string that represents the YANG element referring to the well known key
        VALUE: value of the well known key in the YANG compliant JSON

        Example:
            The mapping file may contain the following row
            nat/interfaces[nat/public-interface]/address : root.wanInterface.ipv4.address

            nat/public-interface will be saved as a key of staticListIndexes, it should be modeled in a YANG as a keyref
            It's value may be either an ID or the interface name, according to what the YANG model states

     **/
    private HashMap<String, String> staticListIndexes;
    /**
     * allowedStaticIndexesInList contains, per each list, what are the static keys that can be associated to it
     */
    private HashMap<String, List<String>> allowedStaticIndexesInList;
    /**
     * keyOfYangLists contains the leaf name of a list element that is actually used as key of the list
     * Example:
     *      YANG excerpt:
     *          ....
     *          list natTable{
                    key port;
                    leaf port{
                        type uint16;
                        confnat:advertise onchange;
                    }
                    leaf inputAddress{
                        type string;
                        confnat:advertise onchange;
                    }
                ....

            keyOfYangLists.put("path/Yang/to/natTable", "port")
     */
    private HashMap<String, String> keyOfYangLists;
    private HashMap<String, String> YangType;
    private HashMap<String, Boolean> YangMandatory;
    private ConnectionModuleClient cM;
    private final ObjectNode rootJson;
    private final ObjectMapper mapper;
    private HashMap<String, Object> stateNew;
    private HashMap<String, Boolean> config;
    private Timer timer;
    protected final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());
    //protected List<String> state;
    //protected HashMap<String, ListValues> stateList;
    //private List<String> nullValuesToListen;
    //private List<NotifyMsg> whatHappened;
    //private ReadLock readLock;
    //private WriteLock writeLock;
 
    
    /*****PERSONALIZABLE FUNCTIONS*******/
    
    private Object personalizedDeserialization(Class<?> type, String json){
//        log.info("In personalized Deserialization the json is "+json);
        try{
            JsonNode jsonValue = mapper.readTree(json);
//            log.info("jsonValue is "+jsonValue);
            if(type == Ip4Address.class){
                Ip4Address value = Ip4Address.valueOf(jsonValue.asText());
//                log.info("value is.."+value);
                return value;
            }
            if(type == IpAddress.class){
                IpAddress value = IpAddress.valueOf(jsonValue.asText());
                return value;
            }
            if(type == Short.class){
                Short value = Short.parseShort(jsonValue.asText());
                return value;
            }
            if(type == PortNumber.class){
//                log.info("E' un port number, the value passed is "+json+" and the type is "+type);
                PortNumber value = PortNumber.portNumber(jsonValue.asLong());
                return value;
            }
            if(type == DeviceId.class){
                DeviceId value = DeviceId.deviceId(jsonValue.asText());
                return value;
            }
        }catch(Exception e){
            log.info("Can't convert the json correctly");
            log.error(e.getMessage());
            return null;
        }
        return null;
    }
    
    private Object personalizedSerialization(String field, Object value){
//        log.info("Il campo è "+field+" il valore "+value);
        if(value==null){
//            log.info("il valore è null");
            return null;
        }
//        log.info("Il tipo originale è "+value.getClass());
        String type = YangType.get(field);
//        log.info("Il tipo è "+type);
        if(type==null)
            return null;
        if(type.equals("boolean"))
            return Boolean.parseBoolean(value.toString());
        if(type.equals("uint8"))
            return Integer.parseInt(value.toString());
        if(type.equals("uint16"))
            return Integer.parseInt(value.toString());
        if(type.equals("int32"))
            return Integer.parseInt(value.toString());
        if(type.equals("inet:port-number"))
            return Long.parseLong(value.toString());
        return value.toString();
    }
    
    
    /***********END OF PERSONALIZED PART
     ******************************/
    
    private String personalizedKeyJson(String var, String javaVar, Object obj){
        try {
//            log.info("var.. "+var);
//            log.info("javaVar "+javaVar);
            Field[] objFields = obj.getClass().getFields();
            ObjectNode objJson = mapper.createObjectNode();
            for(int i=0; i<objFields.length; i++){
                String javaField = javaVar+"/"+objFields[i].getName();
                if(YangToJava.containsKey(javaField)){
                    String fieldName = YangToJava.get(javaField).substring(YangToJava.get(javaField).lastIndexOf("/")+1);
                    if(objFields[i].getClass().getPackage()==root.getClass().getPackage()){
                        String value = personalizedKeyJson(YangToJava.get(javaField), javaField, objFields[i].get(obj));   
                        objJson.put(fieldName, value);
                    }else{
                        Object parsed = personalizedSerialization(YangToJava.get(javaField), objFields[i].get(obj));
                        if(parsed != null){
                            if(Boolean.class.isAssignableFrom(parsed.getClass())){  
//                                    log.info("Trattato come boolean");
                                ((ObjectNode)objJson).put(fieldName, (Boolean)parsed);}
                            else if(parsed.getClass() == Long.class){
//                                    log.info("Trattato come long");
                                ((ObjectNode)objJson).put(fieldName, (Long)parsed);
                            }
                            else if(Integer.class.isAssignableFrom(parsed.getClass())){
//                                    log.info("Trattato come int");
                                ((ObjectNode)objJson).put(fieldName, (Integer)parsed);}
                            else if(Double.class.isAssignableFrom(parsed.getClass())){
//                                    log.info("trattato come double");
                                ((ObjectNode)objJson).put(fieldName, (Double)parsed);}
                            else {//log.info("Trattato come string");
                            ((ObjectNode)objJson).put(fieldName, parsed.toString());
                            }
                        }
                    }
//                    log.info("the jsonObj now "+objJson);
                }
                    
            }
            return mapper.writeValueAsString(objJson);
        } catch (Exception ex) {
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public StateListenerNew(String nf_id, Object root){
        this.root = root;
        AppId = nf_id;
        state = new HashMap<>();
        stateThreshold = new HashMap<>();
        toListenPush = new ArrayList<>();
        toListenThreshold = new HashMap<>();
        toListenTimer = new ArrayList<>();
        YangToJava = new HashMap<>();
        YangType = new HashMap<>();
        YangMandatory = new HashMap<>();
        lists = new HashMap<>();
        config = new HashMap<>();
        mapper = new ObjectMapper();
        timer = new Timer();
        allowedStaticIndexesInList = new HashMap<String, List<String>>();
        staticListIndexes = new HashMap<String, String>();
        keyOfYangLists = new HashMap<String, String>();
        //stateList = new HashMap<>();
        //nullValuesToListen = new ArrayList<>();
        //whatHappened = new ArrayList<>();
        //ReentrantReadWriteLock wHLock = new ReentrantReadWriteLock();
        //readLock = wHLock.readLock();
        //writeLock = wHLock.writeLock();
        
        
        
        ClassLoader loader = AppComponent.class.getClassLoader();
        try{
            
            //GET THE PROPERTIES
            Properties prop = new Properties();
            InputStream propFile = loader.getResourceAsStream("configuration/appProperties.properties");
            if (propFile!=null)prop.load(propFile);
            String baseUri = prop.getProperty("baseUri", "http://130.192.225.154:8080/frogsssa-1.0-SNAPSHOT/webresources/ConnectionModule");
            String eventsUri = prop.getProperty("eventsUri", "http://130.192.225.154:8080/frogsssa-1.0-SNAPSHOT/webresources/events");
            log.info("appId "+AppId);
            log.info("baseUri "+baseUri);
            log.info("eventsUri "+eventsUri);
            
            //CONNECTION TO THE CONNECTION MODULE
            cM = new ConnectionModuleClient(this, AppId, baseUri, eventsUri);
            
            //SETTING DATA MODEL - YANG
            InputStream yangFile = loader.getResourceAsStream(YANGFILE);
            String yangString = new String();
            try(Scanner s = new Scanner(yangFile)){
                while(s.hasNextLine())
                    yangString+=s.nextLine();
            }
            cM.SetDataModel(yangString);
            
            InputStream yinFile = loader.getResourceAsStream(YINFILE);
            JsonNode rootYin = mapper.readTree(yinFile);
            
            //PARSING YIN - GETTING THE LEAFS
            findYinLeafs(rootYin, rootYin.get("@name").textValue());

            /***
             * Debug INFO
             */
            log.info("***KEY LEAF OF YANG LISTS***");
            for (String name: keyOfYangLists.keySet()){
                String value = keyOfYangLists.get(name);
                log.info(name + " -> " + value);
            }
            log.info("*** ***");

        } catch (Exception ex) {
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //System.out.println("---CONFIG-----");
        //System.out.println(config);
//        log.info("++CONFIG "+config);
        
        //System.out.println("---toListenPush-----");
        //System.out.println(toListenPush);
//        log.info("--toListenPush "+toListenPush);
        
        //System.out.println("---toListenThreshold-----");
        //System.out.println(toListenThreshold);
//        log.info("--toListenThreshold "+toListenThreshold);
        
        //System.out.println("---toListenTimer-----");
        //System.out.println(toListenTimer);        
//        log.info("--toListenTimer "+toListenTimer);
        
        //PARSE MAPPING FILE
        try{
            readMappingFile(loader);
            /***
             * Debug INFO
             */
            log.info("***STATIC LIST INDEXES FOUND IN THE MAPPING FILE***");
            for (String name: staticListIndexes.keySet()){
                String value = staticListIndexes.get(name);
                log.info(name + " -> " + value);
            }
            log.info("*** ***");

            log.info("***ALLOWED STATIC INDEXES PER EACH YANG LIST***");
            for (String name: allowedStaticIndexesInList.keySet()){
                List<String> value = allowedStaticIndexesInList.get(name);
                for(String index : value)
                    log.info(name + " -> " + index);
            }
            log.info("*** ***");
	    /**
             * Debug INFO
             */
            log.debug("***YANG PATH MAPPING TO JAVA PATH***");
            for (String javaPath: YangToJava.keySet()){
                String yangPath = YangToJava.get(javaPath);
                log.debug(yangPath + " -> " + javaPath);
            }
            log.debug("*** ***");

        }catch(Exception e){
            log.error("Error during the parsing of the mapping file\nError report: " + e.getMessage());
        }
            
            //ADD VARIABLES TO LISTEN
            Collection<String> all = YangToJava.keySet();
            List<String> sorted = new ArrayList<String>(all);
            Collections.sort(sorted);
            List<String> leafs = new ArrayList<>();
            for(int i=0; i<sorted.size()-1; i++){
                String id0 = sorted.get(i);
                String id1 = sorted.get(i+1);
                if(!id1.contains(id0))
                    leafs.add(id0);
            }
            leafs.add(sorted.get(sorted.size()-1));
            for(String l:YangToJava.keySet()){
                if(l.endsWith("]")){
                    //IS A LIST - TO PUT ALSO IN THE LISTS' MAP
                    String index = l.substring(l.lastIndexOf("[")+1, l.lastIndexOf("]"));
                    String idList = l.substring(0, l.length()-index.length()-2);
                    lists.put(idList.substring(5)+"[]", index);
                }
            }
            rootJson = mapper.createObjectNode();
            
            //CREATE THE JSON TREE CORRESPONDENT TO THE YANG MODEL
            for(String l:leafs)
                createTree(rootJson, YangToJava.get(l));
            
            //START MONITORING
            this.start();
    }

    /**
     * readMappingFile parse the file that maps the information founded in a YANG compliant JSON into the Java variable
     * of the ONOS NAT application code.
     * The formalism of each line of the mapping file is the following:
     * yang/path/to/the/information : java/path/to/the/information ;
     * @throws Exception
     */
    private void readMappingFile(ClassLoader loader) throws Exception{
        InputStream mapFile = loader.getResourceAsStream(MAPPINGFILE);
        Scanner s = new Scanner(mapFile);

        while(s.hasNextLine()) {
            String line = s.nextLine();

            //Check if the line ends with a ';' character
            if(! line.endsWith(";")) {
                String message = "Bad formed line in mapping file. ';' is expected at the end of the line (" + line +
                ")";
                throw new Exception(message);
            }

            //If a line contains multiple mappings separated by ';', consider mappings separately
            String[] mappings = line.split(Pattern.quote(";"));
            for(String mapping : mappings) {
                mapping = mapping.substring(0, line.length() - 1);

                //Split the mapping information and check if the line is well formed
                String[] informationMapping = mapping.split(Pattern.quote(":"));
                if (informationMapping.length != 2) {
                    String message = "Bad formed line in mapping file. Two mapping information separated by a ':' are expected (" +
                            mapping + ")";
                    throw new Exception(message);
                }

		String pathYang = informationMapping[0].trim();
		String pathJava = informationMapping[1].trim();
		//pathJava is used as key of a map. Hence I need to discriminate the 'void' information
		//that can be found in the mapping file. 'void' identifies all the information that are
		//not mapped in any Java variable. In order to make such 'void' information unique,
		//a substring is appended -> void.<path_yang_to_the_information>
		if(pathJava.equals("void"))
			pathJava += "." + pathYang;

                //If any static list keys are found inside the YANG path, they are stored into staticListIndexes map
                //Then, allowedStaticIndexesInList stores, for each YANG list analyzed, the list of static keys found until now
                for(String key : findListKeys(pathYang)){
                    if(! staticListIndexes.keySet().contains(key))
                        staticListIndexes.put(key, null);
                        String listPath = pathYang.substring(0, pathYang.lastIndexOf("["));
                        if(allowedStaticIndexesInList.containsKey(listPath)){
                            List<String> currentListAllowedIndexes = allowedStaticIndexesInList.get(listPath);
                            if(! currentListAllowedIndexes.contains(key)) {
                                currentListAllowedIndexes.add(key);
                                allowedStaticIndexesInList.replace(listPath, currentListAllowedIndexes);
                            }
                        }
                        else{
                            List<String> currentListAllowedIndexes = new ArrayList<String>();
                            currentListAllowedIndexes.add(key);
                            allowedStaticIndexesInList.put(listPath, currentListAllowedIndexes);
                        }
                }

                YangToJava.put(pathJava, pathYang);
            }
        }
    }

    /**
     * findListKeys analyze the path of an information stored in a YANG model
     * The path is described through the state listener mapping file formalism
     * The methods returns all the static keys of a list
     * Examples:
     *      yangPath = "nat/interfaces[nat/public]/address"
     *      returned value = ["nat/public"]
     *
     *      yangPath = "nat/interfaces[nat/public]/ip[nat/ipv4]/address"
     *      returned value = ["nat/public", "nat/ipv4"]
     *
     *      yangPath = "nat/interfaces/address"
     *      returned value = []
     * @param yangPath
     * @return
     */
    private ArrayList<String> findListKeys(String yangPath){
	//The following regular expression is able to split values inside square brackets []
	//e.g., nat-config/interfaces[nat/public]/address becomes {"nat-config/interfaces", "[nat/public]", "/address"}
	//Using split(Pattern.quote("/") is not enough because the key may contain slashes '/'
	//TODO test if it works with multiple lists in the same path, e.g., a/b/c[x]/d/e[y]
        String[] values = yangPath.split("(?<=\\])|(?=\\[)");
        ArrayList<String> keyList = new ArrayList();
        for (String value : values){
            int startBracketIndex = value.indexOf("[");
            int endBracketIndex = value.indexOf("]");
            if(startBracketIndex != -1 && endBracketIndex != -1 && startBracketIndex < endBracketIndex){
                String key = value.substring(startBracketIndex + 1, endBracketIndex);
		if(! key.equals(""))
	                keyList.add(key);
            }
        }

        return keyList;
    }

    public void run(){
        while(!stopCondition){
            try {
                saveNewValues();
                sleep(5000);
            } catch (InterruptedException ex) {
                stopCondition = true;
                cM.deleteResources();
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        log.info("The program has been stopped");
        cM.deleteResources();
    }
    
    private void stopTimerTasks(){
        //STOP ALL THE PERIODIC THREADS
        log.info("Stopping periodicTasks....");
        toListenTimer.forEach((t) -> {
            t.cancel();
        });
        log.info("...Stopped periodic tasks");
    }
    
    public void saveNewValues(){
        //SAVE THE VALUE OF THE ONCHANGE VARIABLES
        stateNew = new HashMap<>();
        for(String s:toListenPush){
            try {
                String sj = fromYangToJava(s);
                saveValues(root, sj.substring(5), sj.substring(5), stateNew);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        //CHECK IF THEY'RE CHANGED -> SEND TO CONNECTION MODULE
        checkChangesSaved();

        //SAVE THE VALUES OF THE ONTHRESHOLD VARIABLES
        Map<String, Object> thr = new HashMap<>();
        for(String s:toListenThreshold.keySet()){
            try {
                if(YangToJava.containsValue(s)){
                    String sj = null;
                    for(String k:YangToJava.keySet())
                        if(YangToJava.get(k).equals(s)){
                            sj = k;
                            break;
                        }
                    saveValues(root, sj.substring(5), sj.substring(5), thr);
                }
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        //CHECK IF THEY HAVE TO BE NOTIFIED
        checkThreshold(thr);
    }
    
    public void saveValues(Object actual, String subToListen, String complete, Map<String, Object> toSave) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        if(subToListen.contains("/")){
            //IT'S NOT A TERMINAL ELEMENT - LEAF
            String inter = subToListen.substring(0, subToListen.indexOf("/"));
            if(inter.contains("[")){
                //IT'S A LIST OR A MAP
                String lName = inter.substring(0, inter.indexOf("["));
                String index = inter.substring(inter.indexOf("[")+1, inter.length()-1);
                actual = actual.getClass().getField(lName).get(actual);
                if(actual!=null){
                    if(List.class.isAssignableFrom(actual.getClass())){
                        for(Object item:(List)actual){
                            String indexValue = searchLeafInList(item, index);
                            String complToPass = complete.substring(0, complete.length()-subToListen.length())+lName+"["+indexValue+"]"+subToListen.substring(inter.length());
                            saveValues(item, subToListen.substring(inter.length()+1), complToPass, toSave);
                        }
                    }else if(Map.class.isAssignableFrom(actual.getClass())){
//                        log.info("Is a Map--!!");
//                        log.info("subToListen is "+subToListen);
//                        log.info("and actual is: "+actual);
                        for(Object key:((Map)actual).keySet()){
                            Object indexValue = key;
//                            log.info("indexValue "+indexValue);
//                            log.info("E il json: "+(new Gson()).toJson(indexValue));
                            String complToPass = complete.substring(0, complete.length()-subToListen.length())+lName+"["+((new Gson()).toJson(indexValue))+"]"+subToListen.substring(inter.length());
                            if(subToListen.substring(inter.length()+1).equals("{key}")){
                                //save the key
//                                log.info("Saving the key - simple");
                                toSave.put(complToPass, key);
                            }else if(subToListen.substring(inter.length()+1).startsWith("{key}")){
//                                log.info("The key is complex!!");
//                                log.info("The field in the key is "+subToListen.substring(inter.length()+7));
                                complToPass = complete.substring(0, complete.length()-subToListen.length())+lName+"["+((new Gson()).toJson(indexValue))+"]"+subToListen.substring(inter.length());
                                saveValues(key, subToListen.substring(inter.length()+7), complToPass, toSave);
                            }
                            else
                                saveValues(((Map)actual).get(key), subToListen.substring(inter.length()+1), complToPass, toSave);
                        }
                    }else 
                        return;
                }
            }else{
                if(inter.equals("{value}"))
                    saveValues(actual, subToListen.substring(inter.length()+1), complete, toSave);
                else{
                    //IT'S AN OBJECT (NOT LIST OR MAP)
                    actual = actual.getClass().getField(inter).get(actual);
                    if(actual!=null)
                        saveValues(actual, subToListen.substring(inter.length()+1), complete, toSave);
                }
            }
        }else{
            //IT'S A TERMINAL ELEMENT - LEAF
//            log.info("leaf - complete "+complete);
            if(subToListen.contains("[")){
                //IT'S THE ELEMENT OF A MAP
                String mapName = subToListen.substring(0, subToListen.indexOf("["));
                Map mappa = (Map) actual.getClass().getField(mapName).get(actual);
                if(mappa!=null){
                    for(Object k:mappa.keySet()){
                        String complToPass = complete.substring(0, complete.lastIndexOf("[")+1)+k.toString()+"]";
                        toSave.put(complToPass, mappa.get(k));
                    }
                }
            }else{
                if(!subToListen.equals("{value}"))
                    actual = actual.getClass().getField(subToListen).get(actual);
                toSave.put(complete, actual);
//                log.info("-*-saved "+actual);
            }
        }
    }
    
    private void checkChangesSaved(){
        List<NotifyMsg> happenings = new ArrayList<>();
        HashMap<String, Object> copyState = new HashMap<>();
        HashMap<String, Object> copyNewState = new HashMap<>();
        List<String> ancoraPresenti = new ArrayList<>();
        if(state!=null && stateNew!=null){
            copyState.putAll(state);
            copyNewState.putAll(stateNew);
            for(String k:state.keySet()){
                if(stateNew.containsKey(k)){
                    if(state.get(k)==null){
                        if(stateNew.get(k)!=null){
                           //ELEMENTS PRESENT IN THE NEW STATE AND NOT IN THE OLD ONE : ADDED
                           NotifyMsg e = new NotifyMsg();
                           e.act=action.ADDED;
                           e.var=trasformInPrint(k);
                           e.obj=stateNew.get(k).toString();
                           happenings.add(e);
                           log.info((new Gson()).toJson(e));
                        }else{
                            stateNew.remove(k);
                            copyNewState.remove(k);
                            continue;
                        }
                    }
                    if(stateNew.get(k)==null){
                        stateNew.remove(k);
                        copyNewState.remove(k);
                        continue;
                    }

                    //NOT ELIMINATED:
                    if(!state.get(k).equals(stateNew.get(k))){
                       //CHANGED VALUE
                       NotifyMsg e = new NotifyMsg();
                       e.act=action.UPDATED;
                       e.var=trasformInPrint(k);
                       e.obj=stateNew.get(k).toString();
                       happenings.add(e);
                       log.info((new Gson()).toJson(e));
                    }
                    copyState.remove(k);
                    copyNewState.remove(k);
                    ancoraPresenti.add(k);
                }
            }

            //UPDATE THE ACTUAL STATE
            state = stateNew;
            
            //copyState CONTAINS THE ELIMINATED
            ObjectNode rootJ = mapper.createObjectNode();
            for(String k:copyState.keySet()){
                NotifyMsg e = new NotifyMsg();
                e.act=action.REMOVED;
                e.obj=copyState.get(k).toString();
                e.var=trasformInPrint(k);
                happenings.add(e);
                insertInNode(rootJ, k, generalIndexes(k), e.obj);
                //System.out.println((new Gson()).toJson(e));
                log.info((new Gson()).toJson(e));
            }

            //copyNewState CONTAINS THE ADDED
            rootJ = mapper.createObjectNode();
            for(String k:copyNewState.keySet()){
                NotifyMsg e = new NotifyMsg();
                e.act=action.ADDED;
                e.obj=copyNewState.get(k).toString();
                e.var=trasformInPrint(k);
                happenings.add(e);
                insertInNode(rootJ, k, generalIndexes(k), e.obj);
                log.info((new Gson()).toJson(e));
            }
            
            rootJ = mapper.createObjectNode();
            for(String s:ancoraPresenti)
                insertInNode(rootJ, s, generalIndexes(s), "presente");

        }
        
        for(NotifyMsg e:happenings){
            //System.out.println(e.act + " "+e.var + " "+e.obj);
//            log.info(e.act+" "+e.var+" "+e.obj);
            //NOTIFICATION OF THE EVENTS TO THE CONNECTION MODULE
            cM.somethingChanged((new Gson()).toJson(e));
        }
        
    }
    
    
    //INSERTS THE VALUE IN THE CORRESPONDENT POSITION IN THE TREE
    private void insertInNode(ObjectNode node, String s, String complete, Object v){
        if(s.contains("/")){
            String f = s.substring(0, s.indexOf("/"));
            String field = (f.contains("["))?f.substring(0, f.indexOf("[")):f;
            String index = (f.contains("["))?f.substring(f.indexOf("[")+1, f.indexOf("]")):null;
            if(node.findValue(field)!=null){
                JsonNode next = node.get(field);
                if(next.isArray()){
                    Iterator<JsonNode> nodes = ((ArrayNode)next).elements();
                    String list = getListName(complete, s);
                    if(lists.containsKey(list)){
                        String ind = lists.get(list);
                        boolean found = false;
                        while(nodes.hasNext()){
                            ObjectNode objN = (ObjectNode)nodes.next();
                            if(objN.findValue(ind)!=null && objN.get(ind).asText().equals(index)){
                                insertInNode(objN, s.substring(s.indexOf("/")+1), complete, v);
                                found = true;
                                break;
                            }
                        }
                        if(found==false){
                            ObjectNode obj = mapper.createObjectNode();
                            obj.put(ind, index);
                            insertInNode(obj, s.substring(s.indexOf("/")+1), complete, v);
                            ((ArrayNode)next).add(obj);
                        }
                    }
                }else{
                    insertInNode((ObjectNode)next, s.substring(s.indexOf("/")+1), complete, v);
                }
            }else{
                if(index==null){
                    ObjectNode next = mapper.createObjectNode();
                    insertInNode(next, s.substring(s.indexOf("/")+1), complete, v);
                    node.put(field, next);
                }else{
                    ArrayNode array = mapper.createArrayNode();
                    String list = getListName(complete, s);
                    if(lists.containsKey(list)){
                        String ind = lists.get(list);
                        ObjectNode next = mapper.createObjectNode();
                        next.put(ind, index);
                        insertInNode(next, s.substring(s.indexOf("/")+1), complete, v);
                        array.add(next);
                    }
                    node.put(field, array);
                }
            }
        }else{
            if((node.findValue(s))==null && v!=null)
                node.put(s, v.toString());
        }
    }
    
    //GIVEN THE VAR ID, RETURNS THE NAME OF ONE OF THE LIST THAT CONTAINS THAT VARIABLE
    //ACCORDING TO THE ELEMENTS THAT DOESN'T HAVE TO BE CONSIDERED: LAST
    private String getListName(String complete, String last){
        String[] c = complete.split(Pattern.quote("/"));
        String[] l = last.split(Pattern.quote("/"));
        String res =new String();
        for(int i=0;i<c.length-l.length+1;i++)
            res+=c[i]+"/";
        res = res.substring(0,res.lastIndexOf("[")+1)+"]";
        return res;
    }

    //TRANSFORMS THE JAVA VAR IN THE CORRESPONDENT YANG ELEMENT ID
    private String trasformInPrint(String var) {
        String[] partsWithoutIndex = var.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
        String j=partsWithoutIndex[0];
        String onlyLastOne = partsWithoutIndex[0];
    //    log.info("lunghezza array1 "+partsWithoutIndex.length);
        String y=null;
        if(partsWithoutIndex.length>1)
            for(int i=1;i<partsWithoutIndex.length;i++){
    //            log.info("parts "+i+" value "+partsWithoutIndex[i]);
                if(i%2==0){
                    //nome lista
                    j+=partsWithoutIndex[i];
                    onlyLastOne+=partsWithoutIndex[i];
                }else{
                    if(lists.containsKey(j+"[]"))
                        j+="["+lists.get(j+"[]")+"]";
                    if(i==partsWithoutIndex.length-1){
                        if(lists.containsKey(j+"[]"))
                            onlyLastOne+=("["+lists.get(j+"[]")+"]");
                    }
                    else
                        onlyLastOne+=("["+partsWithoutIndex[i]+"]");
                            
                }
//                log.info("j is growing "+j);
            }
    //    log.info("---**the value transformed is root/"+j);
        String toVerify = "root/"+j;
        for(String s:YangToJava.keySet())
            if(s.equals(toVerify))
                    y=YangToJava.get("root/"+j);
//        log.info("y is "+y);
        if(y!=null){
            String[] yparse = y.split(Pattern.quote("[]"));
            String toPub=new String();
            for(int i=0; i<partsWithoutIndex.length;i++){
                if(i%2==0)
                    toPub+= yparse[i/2];
                else
                    toPub+="["+partsWithoutIndex[i]+"]";
            }
    //        log.info("And then toPub "+toPub);
            return toPub;
        }
        return y;
    }
     
    //TRANSFORMS THE JSON IN YANG TO THE JSON WITH THE NAMES OF THE ELEMENT IN JAVA
    private JsonNode getCorrectItem(String newVal, String complete){
        //complete in Yang
        //newVal in Yang
        try{
            JsonNode node = mapper.readTree(newVal);
//            log.info("The value is "+newVal);
            JsonNode newNode;
            if(node.isObject()){
//                log.info("It's obviously an object");
                newNode = mapper.createObjectNode();
                Iterator<String> fields = node.fieldNames();
                while(fields.hasNext()){
                    String fieldJava = null;
                    String fieldName = (String)fields.next();
//                    log.info("Processing field "+fieldName);
                    if(YangToJava.containsValue(complete+"/"+fieldName))
                        for(String k:YangToJava.keySet())
                            if(YangToJava.get(k).equals(complete+"/"+fieldName))
                                fieldJava=k;
//                    log.info("fieldJava is "+fieldJava);
                    if(fieldJava!=null){
                        String subfield = null;
                        if(complete.endsWith("]") && fieldJava.substring(fieldJava.lastIndexOf("/")-5, fieldJava.lastIndexOf("/")).equals("{key}")){
                            subfield = fieldJava.substring(fieldJava.lastIndexOf("/")+1);
                            fieldJava = "{key}";
                        }else
                            fieldJava=fieldJava.substring(fieldJava.lastIndexOf("/")+1);
                        if(node.get(fieldName).isValueNode())
                            if(!fieldJava.equals("{key}"))
                                ((ObjectNode)newNode).put(fieldJava, node.get(fieldName));
                            else
                                if(((ObjectNode)newNode).has("{key}"))
                                    ((ObjectNode)((ObjectNode)newNode).get("{key}")).put(subfield, node.get(fieldName));
                                else{
                                    ObjectNode kf = mapper.createObjectNode();
                                    kf.put(subfield, node.get(fieldName));
                                    ((ObjectNode)newNode).put("{key}", kf);
                                }
                        else{
                            String newCampo = (node.get(fieldName).isObject())?complete+"/"+fieldName:complete+"/"+fieldName+"[]";
                            JsonNode subItem = getCorrectItem(mapper.writeValueAsString(node.get(fieldName)),complete+"/"+fieldName);
                            if(!fieldName.equals("{key}"))
                                ((ObjectNode)newNode).put(fieldJava, subItem);
                            else{
                                if((((ObjectNode)newNode).has("{key}")))
                                    ((ObjectNode)((ObjectNode)newNode).get("{key}")).put(subfield, subItem);
                                else{
                                    ObjectNode kf = mapper.createObjectNode();
                                    kf.put(subfield, subItem);
                                    ((ObjectNode)newNode).put("{key}", kf);
                                }
                            }
                        }
//                        log.info("And voilà le newNode "+newNode);
                    }
                }
            }else{
                newNode = mapper.createArrayNode();
                Iterator<JsonNode> iter = ((ArrayNode)node).elements();
                while(iter.hasNext()){
                    JsonNode item = iter.next();
                    JsonNode subItem = getCorrectItem(mapper.writeValueAsString(item),complete+"[]");
                    ((ArrayNode)newNode).add(subItem);
                }
            }
            return newNode;
        }catch(IOException ex){
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    
    //CREATES THE SUB-TREE GIVEN THE VARIABLE L AND THE NODE NODE
    private void createTree(JsonNode node, String l) {
        if(l==null || l.equals(""))
            return;
        String[] splitted = l.split(Pattern.quote("/"));
        if(node.isObject()){
            JsonNode next;
            if(splitted[0].contains("[")){
                String inter = splitted[0].substring(0, splitted[0].indexOf("["));
                next = ((ObjectNode)node).get(inter);
                if(next==null)
                    ((ObjectNode)node).put(inter, mapper.createArrayNode());
                next = ((ObjectNode)node).get(inter);
            }else{
                next = ((ObjectNode)node).get(splitted[0]);
                if(next==null){
                    if(splitted.length>1 || (splitted.length>1&&splitted[1].contains("[")))
                        ((ObjectNode)node).put(splitted[0], mapper.createObjectNode());
                    else if (splitted.length==1 && splitted[0].contains("[")){
                        ArrayNode mappa = mapper.createArrayNode();
                        ObjectNode elemMappa = mapper.createObjectNode();
                        elemMappa.put("key", "");
                        elemMappa.put("value", "");
                        ((ObjectNode)node).put(splitted[0], mappa);
                    }else
                       ((ObjectNode)node).put(splitted[0], new String()); 
                }
                next = ((ObjectNode)node).get(splitted[0]);
            }
            if(splitted.length>1)
                createTree(next, l.substring(splitted[0].length()+1));
            if(splitted.length==1&&next.isArray()){
//                ObjectNode elemMappa = mapper.createObjectNode();
//                elemMappa.put("key", "");
//                elemMappa.put("value", "");
//                ((ArrayNode)next).add(elemMappa);
            }
        }else{
            JsonNode next;
            if(splitted[0].contains("[")){
                String inter = splitted[0].substring(0, splitted[0].indexOf("["));
                if(node.isArray()){
                    //è una lista
                    if(((ArrayNode)node).elements().hasNext()==false)
                        ((ArrayNode)node).addObject();
                    next = ((ArrayNode)node).get(0);
                    if(((ObjectNode)next).get(inter)==null)
                        ((ObjectNode)next).put(inter, mapper.createArrayNode());
                    next = ((ObjectNode)next).get(inter);
                }else{
                    //è l'elemento di una mappa
                    ArrayNode newNode = mapper.createArrayNode();
                    ObjectNode nn = mapper.createObjectNode();
                    nn.put("id", "");
                    nn.put("value", nn);
                    newNode.add(nn);
                    return;
                }
            }else{
                if(((ArrayNode)node).elements().hasNext()==false)
                    ((ArrayNode)node).addObject();
                next = ((ArrayNode)node).get(0);
                if(((ObjectNode)next).get(splitted[0])==null){
                    if(splitted.length>2)
                        ((ObjectNode)next).put(splitted[0], mapper.createObjectNode());
                    else
                       ((ObjectNode)next).put(splitted[0], new String()); 
                }
                next = ((ObjectNode)next).get(splitted[0]);
            }
            if(splitted.length>1)
                createTree(next, l.substring(splitted[0].length()+1));
        }
    }

    
    //RETURNS THE JSON OF THE OBJECT VAR - VAR IS IN YANG
    public Object getComplexObj(String var) throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        String[] spl = var.split(Pattern.quote("/"));
        JsonNode ref = rootJson;
        //CHECK THE TREE THAT HAS TO BE FILLED
        for(int i=0;i<spl.length;i++){
            String field =(spl[i].contains("["))?spl[i].substring(0, spl[i].indexOf("[")):spl[i];
            String index =(spl[i].contains("["))?spl[i].substring(spl[i].indexOf("[")+1, spl[i].indexOf("]")):null;
            if(ref.isObject()){
                if(((ObjectNode)ref).get(field)!=null){
                    ref = ((ObjectNode)ref).get(field);
                    if(index!=null && !index.equals("")){
                        ref=((ArrayNode)ref).get(0);
                    }if(index!=null && index.equals("")&& i!=spl.length-1)
                        return null;
                    continue;
                }else{
                    //System.out.println(var + " not found");
                    log.info("var not found "+field);
                    return null;
                }
            }else{
                if(((ArrayNode)ref).elements().hasNext()==false){
                    //System.out.println(var + " not found-array version");
                    return null;
                }
                ref = ((ArrayNode)ref).get(0);
                if(((ObjectNode)ref).get(field)==null){
                    //System.out.println(var +" not found!");
                    return null;
                }
                continue;
            }
        }
        log.info("Created tree: "+ref);
 
        if(ref.isValueNode()){
            //IT'S A LEAF
//            log.info("the variable in yang "+var);
            String varJava = fromYangToJava(var);
//            log.info("the  variable in java "+varJava);
//            log.info("Var java "+varJava);
            Object value = getLeafValue(varJava.substring(5));
//            log.info("..and the value "+value);
            //SERIALIZE CORRECTLY THE JSON VALUE
            var = noIndexes(var);
            Object serialized = personalizedSerialization(var, value);
//            log.info("maybe not well serialized? "+serialized);
            return serialized;
        }
               
        JsonNode res;// = (ref.isObject())?mapper.createObjectNode():mapper.createArrayNode();
        var=(ref.isArray()&&var.endsWith("[]"))?var.substring(0, var.length()-2):var;
        
//        log.info("To fill the result -> "+var);
        
        res = fillResult(ref, var);
        
        if(var.endsWith("]") && res.size()==0)
            res = null;
        //System.out.println(res);
//        log.info("The result is "+res);
        JsonNode r = mapper.createObjectNode();
        ((ObjectNode)r).put(var.substring(var.lastIndexOf("/")+1), res);
//        log.info("The result is ready");
        return res;
    }

    //PUT THE SUB-TREE VAR IN THE JSON SUB-TREE REF
    private JsonNode fillResult(JsonNode ref, String var) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
        JsonNode toRet;
        
        if(ref.isObject()){
            //fill fields
            toRet = mapper.createObjectNode();
            Iterator<String> field = ((ObjectNode)ref).fieldNames();
            if(!field.hasNext()){
                //code for the transformation from the Yang to the Java
                String varWithoutIndexes = new String();
                String[] varSp = var.split("["+Pattern.quote("[]")+"]");
                for(int i=0; i<varSp.length;i++)
                    if(i%2==0)
                        varWithoutIndexes+=varSp[i]+"[]";
                varWithoutIndexes = varWithoutIndexes.substring(0, varWithoutIndexes.length()-2);
                if(YangToJava.containsValue(varWithoutIndexes)){
                    String key = null;
                    for(String k:YangToJava.keySet())
                        if(YangToJava.get(k).equals(varWithoutIndexes))
                            key = k;
                    String[] yspez = var.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
                    String[] jspez = key.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
                    String jWithIndex = new String();
                    for(int i=0;i<yspez.length;i++){
                        if(i%2==0)
                            jWithIndex+=jspez[i];
                        else
                            jWithIndex+="["+yspez[i]+"]";
                    }
//                    log.info("Getting thw value of "+jWithIndex);
                    //jWithIndex is the name of the variable in the Java cose (preceeded by "root."
                    ((ObjectNode)toRet).put(var, getLeafValue(jWithIndex.substring(5)).toString());
                    field.next();
                }
                return toRet;
            }
            while(field.hasNext()){
                //INSERT THE VALUES OF THE FIELDS OF THE OBJECT
                String fieldName = field.next();
//                log.info("Getting the value of the field "+var+"/"+fieldName+" in the object "+ref);
                if(((ObjectNode)ref).get(fieldName).isValueNode()){
                    //IT'S A LEAF
                    //code for the transformation from the Yang to the Java
                    String varWithoutIndexes = new String();
                    String[] varSp = (var+"/"+fieldName).split("["+Pattern.quote("[]")+"]");
                    for(int i=0; i<varSp.length;i++)
                        if(i%2==0)
                            varWithoutIndexes+=varSp[i]+"[]";
                    varWithoutIndexes = varWithoutIndexes.substring(0, varWithoutIndexes.length()-2);
//                    log.info("varWithoutIndexes is correct? "+varWithoutIndexes);
                    if(YangToJava.containsValue(varWithoutIndexes)){
                        String key = null;
                        for(String k:YangToJava.keySet())
                            if(YangToJava.get(k).equals(varWithoutIndexes))
                                key = k;
                        String[] yspez = (var+"/"+fieldName).split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
                        String[] jspez = key.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
                        String jWithIndex = new String();
                        for(int i=0;i<yspez.length;i++){
                            if(i%2==0)
                                jWithIndex+=jspez[i];
                            else
                                jWithIndex+="["+yspez[i]+"]";
                        }
                        //jWithIndex is the name of hte variable in Java (preceeded by "root.")
                        Object value = getLeafValue(jWithIndex.substring(5));
//                        log.info("the variable to search is "+jWithIndex+" and its value: "+value);
                        if(value!=null){
                            //PERSONALIZED SERIALIZATION
                            Object parsed = personalizedSerialization(varWithoutIndexes, value);
//                            log.info("..parsed in "+parsed);
                            if(parsed != null){
                                if(Boolean.class.isAssignableFrom(parsed.getClass())){  
//                                    log.info("Trattato come boolean");
                                    ((ObjectNode)toRet).put(fieldName, (Boolean)parsed);}
                                else if(parsed.getClass() == Long.class){
//                                    log.info("Trattato come long");
                                    ((ObjectNode)toRet).put(fieldName, (Long)parsed);
                                }
                                else if(Integer.class.isAssignableFrom(parsed.getClass())){
//                                    log.info("Trattato come int");
                                    ((ObjectNode)toRet).put(fieldName, (Integer)parsed);}
                                else if(Double.class.isAssignableFrom(parsed.getClass())){
//                                    log.info("trattato come double");
                                    ((ObjectNode)toRet).put(fieldName, (Double)parsed);}
                                else {//log.info("Trattato come string");
                                ((ObjectNode)toRet).put(fieldName, parsed.toString());
                                }
                            }
                        }
                    }
                    else
                        log.info("It's not correct..");
                }else{
                    //IT'S NOT A LEAF - GO DEEPER
                    JsonNode f = fillResult(((ObjectNode)ref).get(fieldName), var+"/"+fieldName);
//                    if(f.size()!=0)
                        ((ObjectNode)toRet).put(fieldName, f);
                }
            }
            return toRet;
        }else{
            //IT'S AN ARRAYNODE
            //add elements
            String listWithoutIndex = noIndexes(var);
            toRet = mapper.createArrayNode();
            String listInJava = null;
            //code to transform the Yang name in the Java list's name
            for(String l:YangToJava.keySet()){
                if(YangToJava.get(l).contains(listWithoutIndex+"[") && YangToJava.get(l).substring(0, listWithoutIndex.length()+1).equals(listWithoutIndex+"[")){
                    String rem = YangToJava.get(l).substring(listWithoutIndex.length());
                    if(!rem.contains("/"))
                            listInJava = l;
                }
            }
//            log.info("The list in java is "+listInJava);
            String[] yspez = var.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
            String[] jspez = listInJava.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
            String jWithIndex = new String();
            for(int i=0;i<yspez.length;i++){
                if(i%2==0)
                    jWithIndex+=jspez[i];
                else
                    jWithIndex+="["+yspez[i]+"]";
            }
            //ListValues e = stateList.get(jWithIndex.substring(5)+"[]");
//            log.info("jWithIndex "+jWithIndex);
            String lN = generalIndexes(jWithIndex.substring(5))+"[]";
//            log.info("lN "+lN);
            String e = (lists.containsKey(lN))?lists.get(lN):null;
//            log.info("..ed e "+e);
            if(e!=null){
                //the list is contained in the list collection and the index is e
                String indice=e;
                Object list = getLists(root, jWithIndex.substring(5)+"[]", jWithIndex.substring(5)+"[]");
//                log.info("list.."+list);
                if(list!=null && List.class.isAssignableFrom(list.getClass())){
                    List<Object> elems = new ArrayList<>();
                    elems.addAll((List)list);
                    for(Object obj:elems){
                        //for all the elements - insert in the json
                        String idItem = searchLeafInList(obj, indice);
                        JsonNode child = fillResult(((ArrayNode)ref).get(0), var+"["+idItem+"]");
                        if(child.size()!=0)
                            ((ArrayNode)toRet).add(child);
                    }
                }else if(list!=null && Map.class.isAssignableFrom(list.getClass())){
//                    log.info("is a map and it is not null");
                    Map<Object, Object> elems = new HashMap<>();
                    elems.putAll((Map)list);
                    for(Object k:elems.keySet()){
                        //for all the elements - inster in the json
//                        log.info("the k in the keyset is "+k);
                        JsonNode child = fillResult(((ArrayNode)ref).get(0), var+"["+personalizedKeyJson(var,jWithIndex+"[{key}]/{key}", k)+"]");
//                        log.info("and the child.."+child);
                        if(child.size()!=0)
                            ((ArrayNode)toRet).add(child);
                    }
                }
//                if(e.List!=null)elems.addAll(e.List);
//                for(Object obj:elems){
//                    String idItem = searchLeafInList(obj, indice);
//                    ((ArrayNode)toRet).add(fillResult(((ArrayNode)ref).get(0), var+"["+idItem+"]"));
//                }
//                    
            }
                
            return toRet;
        }
    }

    
    //CONFIG THE VALUE OF A SUB-TREE
    private int setComplexObject(String var, String newVal) {
        try {
            JsonNode toSet = mapper.readTree(newVal);

            //check if all the values are configurable
            if(!configVariables(noIndexes(var), toSet)){
                log.info("not to config..");
                return 1;
            }		
            return fillVariables(toSet, var);
        } catch (IOException ex) {
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            return 1;
        }catch(NoSuchFieldException ex){
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            return 1;
        }catch(IllegalAccessException ex){
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            return 1;
        }
    }

    
    //CHECKS IF ALL THE VARIABLES IN THE SUB-TREE TOSET ARE CONFIGURABLE ->TRUE
    //ELSE -> FALSE
    private boolean configVariables(String var, JsonNode toSet){
        if(toSet.isValueNode()){
//            log.info("Is a value Node: "+var);
		try{
		    	String value =  mapper.writerWithDefaultPrettyPrinter().writeValueAsString(toSet);
			staticListIndexes.replace(var, value);
		}catch(Exception e){
			log.error("Fail during the Json conversion in 'pretty' String");
		}
            return config.get(var);
        }
        if(toSet.isObject()){	    
            Iterator<Entry<String, JsonNode>> iter = ((ObjectNode)toSet).fields();
            boolean ok = true;
            while(iter.hasNext()){
                Entry<String, JsonNode> field = iter.next();
                if(field.getValue().isValueNode()){
                    //leaf - check config
                    if(config.containsKey(var+"/"+field.getKey()))
                        ok = ok && config.get(var+"/"+field.getKey());
                    else{
//			log.info("Config does not contain "+ var + "/ " + field.getKey());
                        ok = true;
                    }

                    //Check if the current value is a static list key
                    String currentKey = var + "/" + field.getKey();
                    if(staticListIndexes.containsKey(currentKey)){			
                        staticListIndexes.replace(currentKey, field.getValue().toString());
//			log.info("New static index value " + currentKey + " : " + field.getValue().toString());
		    }
                }else
                    ok = ok && configVariables(var+"/"+field.getKey(), field.getValue());
            }
            return ok;
            
        }else{
            Iterator<JsonNode> children = ((ArrayNode)toSet).elements();
            boolean ok = true;
            while(children.hasNext()){
                var = (var.endsWith("]"))?var : var+"[]";
                ok = ok && configVariables(var, children.next());
            }
            return ok;
        }
    }
    
    //GIVEN THE JSON TOSET AND THE START VARIABLE VAR -> CONFIGURATION OF THE VARIABLES IN THE CODE
    //RETURNS 0 IF GOES OK
    //1 IF SETTING WAS IMPOSSIBLE
    //2 IF VARIABLE NOT FOUND
    private int fillVariables(JsonNode toSet, String var) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, IOException {
//        log.info("var "+var+" to Set "+toSet);
        if(toSet.isValueNode()){
            //LEAF
//            log.info("In fillVariables - reached leaf");
            String j = fromYangToJava(var);
            if(j!=null){
                if(setVariable(j.substring(5), j.substring(5), toSet.asText(), root))
                    return 0;
                else
                    return 1;
            }else
                return 2;
            //}
        }else{
            if(toSet.isObject()){
                if(var.endsWith("[]")){
                    //code to transform the Yang name to the Java list's name
                    String varWithoutIndexes = new String();
                    String[] varSp = var.split("["+Pattern.quote("[]")+"]");
                    for(int i=0; i<varSp.length;i++)
                        if(i%2==0)
                            varWithoutIndexes+=varSp[i]+"[]";
                    varWithoutIndexes = varWithoutIndexes.substring(0, varWithoutIndexes.length()-2);
                    if(YangToJava.containsValue(varWithoutIndexes)){
                        String key = null;
                        for(String k:YangToJava.keySet())
                            if(YangToJava.get(k).equals(varWithoutIndexes))
                                key = k;
                        String[] yspez = var.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
                        String[] jspez = key.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
                        String jWithIndex = new String();
                        for(int i=0;i<yspez.length;i++){
                            if(i%2==0)
                                jWithIndex+=jspez[i];
                            else
                                jWithIndex+="["+yspez[i]+"]";
                        }
                        jWithIndex = jWithIndex.substring(5);
                    
//                        log.info("**Prima che inizi tutto -> "+toSet);
                        //transform the list's elements from the Yang denomination to the Java
                        JsonNode newValJava = getCorrectItem(mapper.writeValueAsString(toSet), varWithoutIndexes+"[]");
                        if(newValJava!=null){
                            if(setVariable(jWithIndex+"[]", jWithIndex+"[]",mapper.writeValueAsString(newValJava), root))
                                return 0;
                            else
                                return 1;
                        }
                        return 1;
                    }else
                        return 2;
                }else{
                    //IT'S AN OBJECT
                    Iterator<String> fields = toSet.fieldNames();
                    int res = 0;
                    while(fields.hasNext()){
                        String fieldName = (String)fields.next();
//                        log.info("Setting "+fieldName);
                        int resc = fillVariables(toSet.get(fieldName), var+"/"+fieldName);
//                        log.info("resc "+resc);
                        res = (resc==0)?res:resc;
                    }
                    return res;
                }
            }else{
//                log.info("Sono nell'else - no object");
                //code to transform the name of the list from Yang to Java
                String varWithoutIndexes = new String();
                String[] varSp = var.split("["+Pattern.quote("[]")+"]");
                for(int i=0; i<varSp.length;i++)
                    if(i%2==0)
                        varWithoutIndexes+=varSp[i]+"[]";
                varWithoutIndexes = varWithoutIndexes.substring(0, varWithoutIndexes.length()-2);
//                log.info("Var without indexes "+varWithoutIndexes);
                if(YangToJava.containsValue(varWithoutIndexes)){
//                    log.info("Yang to Java contains the value");
                    String key = null;
                    for(String k:YangToJava.keySet())
                        if(YangToJava.get(k).equals(varWithoutIndexes))
                            key = k;
//                    log.info("And the key is "+key);
                    String[] yspez = var.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
                    String[] jspez = key.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
                    String jWithIndex = new String();
                    for(int i=0;i<yspez.length;i++){
                        if(i%2==0)
                            jWithIndex+=jspez[i];
                        else
                            jWithIndex+="["+yspez[i]+"]";
                    }
                    //jWithIndex is the name of the list in java
                    if(jWithIndex.length()<=5){
//                        log.info("Is root.!! Can't be a list");
                        return 2;
                    }
                    jWithIndex = jWithIndex.substring(5);
                    //crearne una nuova
                    Class<?> type=null;
                    String indice = null;
                    String jgen = generalIndexes(jWithIndex);
                    if(lists.containsKey(jgen+"[]")){
                        //THE LIST COLLECTION CONTAINS THIS ONE
//                        log.info("The list exists");
                        indice = lists.get(jgen+"[]");
                        Object actual = root;
                        
                        //GE THE TYPE OF THE ELEMENTS OF THE LIST
                        String[] fields = jWithIndex.split(Pattern.quote("/"));
                        Field f = actual.getClass().getDeclaredField(fields[0]);
                        for(int i=1;i<fields.length;i++){
                            if(fields[i].contains("[")){
                                if(java.util.List.class.isAssignableFrom(f.getType())){
                                    ParameterizedType pt = (ParameterizedType)f.getGenericType();
                                    Class<?> itemType = (Class<?>)pt.getActualTypeArguments()[0];
                                    f = itemType.getField(fields[i].substring(0, fields[i].indexOf("[")));
                                }else if(Map.class.isAssignableFrom(f.getType())){
                                    ParameterizedType pt = (ParameterizedType)f.getGenericType();
                                    Class<?> itemType = (Class<?>)pt.getActualTypeArguments()[0];
                                    f = itemType.getField(fields[i].substring(0, fields[i].indexOf("[")));
                                }else
                                    f = f.getType().getDeclaredField(fields[i].substring(0, fields[i].indexOf("[")));
                            }else{
                                if(java.util.List.class.isAssignableFrom(f.getType())){
                                    ParameterizedType pt = (ParameterizedType)f.getGenericType();
                                    Class<?> itemType = (Class<?>)pt.getActualTypeArguments()[0];
                                    f = itemType.getField(fields[i]);
                                }else if(Map.class.isAssignableFrom(f.getType())){
                                    ParameterizedType pt = (ParameterizedType)f.getGenericType();
                                    Class<?> itemType = (Class<?>)pt.getActualTypeArguments()[0];
                                    f = itemType.getField(fields[i].substring(0, fields[i].indexOf("[")));
                                }else
                                    f = f.getType().getDeclaredField(fields[i]);
                            }
                        }
                        ParameterizedType pt = (ParameterizedType)f.getGenericType();
                        type = (Class<?>)pt.getActualTypeArguments()[0];
                        
                    }else{
//                        log.info("The list doesn't exist");
                        return 2;
                    }
                    Iterator<JsonNode> iter = ((ArrayNode)toSet).elements();
                    int res = 0;
                    while(iter.hasNext()){                     
                        //INSERT THE VALUES OF THE LIST'S ELEMENTS
                        JsonNode newValJava = getCorrectItem(mapper.writeValueAsString(iter.next()), varWithoutIndexes+"[]");
                        if(newValJava!=null){
                            if(!setVariable(jWithIndex+"[]", jWithIndex+"[]",mapper.writeValueAsString(newValJava), root))
                                res = 1;
                        }
                    }   
                    return res;
                }
                return 2;
            }
        }
    }

    //REMOVES THE INDEX VALUES FROM THE LIST'S NAME
    private String noIndexes(String s){
        String[] split = s.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
        String ret = new String();
        for(int i=0;i<split.length;i++){
            if(i%2==0)
                ret+=split[i]+"[]";
        }
        if(!s.endsWith("]"))
            ret = ret.substring(0, ret.length()-2);
        return ret;
    }
        
    //THE NAME OF THE LIST CONTAINS BETWEEN THE SQUARED THE NAME OF THE INDEX
    private String generalIndexes(String s){
        String[] split = s.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
        String l = new String();
        for(int i=0;i<split.length;i++){
            if(i%2==0){
                l+=split[i];
            }else{
                if(lists.containsKey(l+"[]")){
                    String ind = lists.get(l+"[]");
                    l+="["+ind+"]";
                }
            }
        }
        return l;
    }
    
    //RETURNS THE LIST WITHOUT ANYTHING BETWEEN THE SQUARED
    private String deleteIndexes(String var){
        String[] parts = var.split("["+Pattern.quote("[")+"," +Pattern.quote("]")+"]");
        String res = new String();
        for(int i=0;i<parts.length;i++)
            if(i%2==0)
                res+=parts[i]+"[]";
        if(!var.endsWith("]"))
            res=res.substring(0, res.length()-2);
        return res;
    }
    
    //-------------------FOR THE NAT
    private boolean natTableModified(String var, String json){
        if(var.contains("natPortMap"))
            return true;
        if(json.contains("natTable"))
            return true;
        return false;
    }
    //------------------------------
    
    
    //COMMAND CALLED FROM THE CONNECTIONMODULECLIENT - MESSAGE FROM THE SERVICE LAYER
    public void parseCommand(String msgJson) throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException, IOException{
        CommandMsg msg = ((new Gson()).fromJson(msgJson, CommandMsg.class));
	log.info("prima di fromyangToJava");
        String var = fromYangToJava(msg.var);
	log.info("dopo fromyangotjava");
//        log.info("Command "+msg.act);
//        log.info("Variable "+msg.var);
//        log.info("Variable in the code "+var);
        switch(msg.act){
            case GET:
                //get the object
		log.info("prima di getComplexObj");
                Object result = getComplexObj(msg.var);
		log.info("dopo getComplexObj");
                log.info("result "+result);
//                log.info("result.."+result);
                msg.objret = mapper.writeValueAsString(result);
                //pass the result to the connection module
//                log.info("Result of the get "+msg.objret);
                cM.setResourceValue((new Gson().toJson(msg)));
                break;
                
            case CONFIG:		
                String noInd = deleteIndexes(msg.var);
                if(config.containsKey(noInd) && !config.get(noInd)){
                    //no configurable
                    log.info("Not configurable");
                    msg.objret = "2";
                    cM.setResourceValue((new Gson()).toJson(msg));
                    return;
                }
                try {		
                    Integer ret = 0;
                    //-------ADDED FOR THE NAT!
                    ((AppComponent)root).withdrawIntercepts();
                    //-------
		    
		    try{
			    //If var.substring(0, 4) == 'void', the message requests for the set
			    // of an information that is not mapped into any Java variables
			    if(! var.substring(0, 4).equals("void")){	
                		    //case 1: is a leaf - it is configurable (no configurable leafs are handled in the previous if)
                	       	    if(var!=null && !var.equals("root")&&state.containsKey(var.substring(5))){			
                           		 boolean setted = setVariable(var.substring(5), var.substring(5), (String)msg.obj, root);			
	                            	ret = (setted)?0:1;
        	                    }else{
                	            	//IT ISN'T THE VALUE OF A LEAF CONTAINED IN THE STATE									
                        	    	ret = setComplexObject(msg.var, (String)msg.obj);			
	       		    		log.info("***STATIC LIST INDEXES AFTER CONFIG***");
	                            	for (String name: staticListIndexes.keySet()){
	                                	String value = staticListIndexes.get(name);
		                                log.info(name + " -> " + value);
                	                }
    	                	    	log.info("*** ***");
                            	   }
			    }else{
				log.info("prima di replace");
                        	staticListIndexes.replace(msg.var, (String)msg.obj);
				log.info("dopo replace");
			    }
		    }catch(Exception e){
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			log.info("Eccezione: " + errors.toString());
		    }

                    msg.objret = ret.toString();
//                    log.info("Result: "+ret);
                    //return the result to the ConnectionModule			
                    cM.setResourceValue((new Gson()).toJson(msg));			               

                    //-------ADDED FOR THE NAT!
                    ((AppComponent)root).flowRuleService.removeFlowRulesById(((AppComponent)root).appId);
                    ((AppComponent)root).requestIntercepts();
                    
                    if(natTableModified(var, (String)msg.obj)){
                        //ACTIONS
                        log.info("Modified nat table");
                        ArrayNode table = (ArrayNode)getComplexObj("nat/natTable");
                        log.info("the nat table is "+table);
                        
                        Iterator<JsonNode> tableEntries = table.elements();
                        while(tableEntries.hasNext()){
                            ObjectNode entry = (ObjectNode)tableEntries.next();
                            log.info("entry "+entry);
                            Ip4Address inIp=null, outIp=null, natIp=null;
                            Short inPort=null, outPort=null, natPort=null;
                            int proto=0;
                            log.info("prima di prendere i valori");
                            if(entry.has("inputAddress")){
                                inIp = Ip4Address.valueOf(entry.get("inputAddress").textValue());
                            }
                            if(entry.has("outputAddress"))
                                outIp = Ip4Address.valueOf(entry.get("outputAddress").textValue());
                            if(entry.has("newAddress"))
                                natIp = Ip4Address.valueOf(entry.get("newAddress").textValue()); 
                            if(entry.has("inputPort"))
                                inPort = entry.get("inputPort").shortValue();
                            if(entry.has("outputPort"))
                                outPort = entry.get("outputPort").shortValue();
                            if(entry.has("newPort"))
                                natPort = entry.get("newPort").shortValue();
                            log.info("p3 "+natPort);
                                proto = entry.get("proto").asInt();
                            log.info("input address "+inIp);
                            log.info("input port "+inPort);
                            log.info("output address "+outIp);
                            log.info("output port "+outPort);
                            log.info("nat address "+natIp);
                            log.info("nat port "+natPort);
                            log.info("proto "+proto);
//                            ((AppComponent)root).installOutcomingNatRule(inIp, outIp, proto, inPort, natPort, MacAddress.NONE, PortNumber.portNumber(outPort));
                        }
                    }
		    log.info("***STATIC LIST INDEXES AFTER CONFIG***");
                        for (String name: staticListIndexes.keySet()){
                                String value = staticListIndexes.get(name);
                                log.info(name + " -> " + value);
                        }
                    log.info("*** ***");

                    //-------
                    return;
                } catch (NoSuchFieldException ex) {
                    Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                }
                //reached only if there is an exception!
                msg.objret = (new Integer(2)).toString();
//                log.info("Result: "+msg.objret);
                cM.setResourceValue((new Gson()).toJson(msg));

                //-------ADDED FOR THE NAT!
//                ((AppComponent)root).flowRuleService.removeFlowRulesById(((AppComponent)root).appId);
//                ((AppComponent)root).requestIntercepts();
                //-------
                break;
            case DELETE:
                //delete
//                log.info("Arrived from ConnectionModule the command DELETE for "+msg.var);
                Integer ret;
                try{
                    if(var==null || var.equals("root")){
                        log.info("Can't delete the variable");
                        ret = 1;
                    }else{
                        String YangGeneralVar = noIndexes(msg.var);
                        if(YangMandatory.containsKey(YangGeneralVar) && YangMandatory.get(YangGeneralVar)){
                            log.info("The variable is mandatory");
                            ret = 1;
                        }else
                            ret = deleteVariable(root, var.substring(5), var.substring(5));
                    }
                } catch (NoSuchFieldException ex) {
                    Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                    ret = 1;
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                    ret = 1;
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                    ret = 1;
                }
                msg.objret = ret.toString();
//                log.info("From delete returning "+ret);
                cM.setResourceValue((new Gson()).toJson(msg));
                break;
            }
    }
    
    
    //DELETE THE VARIABLE
    public int deleteVariable(Object actual, String var, String complete) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        String[] fs = var.split(Pattern.quote("/"));
        if(fs.length==1){
            //delete
            if(var.contains("[")){
                //delete an element of the list
                String index = var.substring(var.lastIndexOf("[")+1, var.lastIndexOf("]"));
                if(index!=null && index.matches("")){
                    //shouldn't do this!
                    Field f = actual.getClass().getField(var.substring(0,var.length()-2));
                    f.set(actual, null);
                    return 0;
                }
                String listName = complete.substring(0, complete.length()-index.length()-1);
                listName+="]";
                listName = generalIndexes(listName)+"[]";
                String indice = null;
//                log.info("Deleting from "+listName);
                if(lists.containsKey(listName))
                    indice = lists.get(listName);
                if(indice!=null){
//                    log.info("Index!=null");
                    actual = actual.getClass().getField(var.substring(0, var.lastIndexOf("["))).get(actual);
                    Object delete = null;
                    if(List.class.isAssignableFrom(actual.getClass())){
                    for(Object item:(List)actual){
                        if(item.getClass().getField(indice).get(item).toString().equals(index)){
                            delete = item;
                            break;
                        }
                    }
                    
                    //found the element to delete
                    if(delete!=null){
                        ((List)actual).remove(delete);
                        return 0;
                    }
                    }else if(Map.class.isAssignableFrom(actual.getClass())){
                        //delete from a map
//                        log.info("Is a Map!");
                        if(((Map)actual).containsKey(index)){
                           ((Map)actual).remove(index);
                           return 0;
                        }
                        else{
//                            log.info("Nb in delete : complete is "+complete);
                            for(Object k:((Map)actual).keySet()){
                                String jsonKey = (new Gson()).toJson(k);
                                if(jsonKey.equals(index) || ((index.startsWith("{")||index.startsWith("["))&&allDefault(index, k, complete))){
                                delete = k; break;}
                                }
                            if(delete!=null){
                                ((Map)actual).remove(delete);
                                return 0;
                            }
                            return 2;
                        }
                    }
                }
                return 2;
            }else{
                //default values to set = deleting variables
                //boolean -> false
                //number -> 0
                //otherwise -> null
                Field f = actual.getClass().getField(var);
                Class<?> type = f.getType();
                if(Boolean.class.isAssignableFrom(type))
                    f.set(actual, false);
                else if(f.get(actual) instanceof Number)
                    f.set(actual, 0);
                else
                    f.set(actual, null);
                return 0;
            }
        }else{
            //IT'S NOT A LEAF - GO DEEPER
            
            if(fs[0].contains("[")){
                //IT'S A LIST OR A MAP
                String fName = fs[0].substring(0, fs[0].indexOf("["));
                String index = fs[0].substring(fs[0].indexOf("[")+1, fs[0].length()-1);
                actual = actual.getClass().getField(fName).get(actual);
                String listName = complete.substring(0, complete.length()-var.length()+fName.length());
                String indice = null;
                listName = generalIndexes(listName);
                if(lists.containsKey(listName+"[]"))
                    indice = lists.get(listName+"[]");
                if(actual!=null){
                    if(List.class.isAssignableFrom(actual.getClass())){
                        for(Object item:(List)actual)
                            if(item.getClass().getField(indice).get(item).toString().equals(index))
                                return deleteVariable(item, var.substring(fs[0].length()+1), complete);
                    }else if(Map.class.isAssignableFrom(actual.getClass())){
                        for(Object k:((Map)actual).keySet())
                            if(k.toString().equals(index)){
                                if((var.substring(fs[0].length()+1)).startsWith("{key}"))
                                    return deleteVariable(k, var.substring(fs[0].length()+1), complete);
                                else
                                    return deleteVariable(((Map)actual).get(k), var.substring(fs[0].length()+1), complete);
                            }
                    }
                }
                return 2;
            }else{
                //IT'S AN OBJECT
                actual = actual.getClass().getField(fs[0]).get(actual);
                if(actual!=null)
                    return deleteVariable(actual, var.substring(fs[0].length()+1), complete);
                return 2;
            }
        }
    }
    
    
    //SETTING A VALUE TO A VARIABLE
    public boolean setVariable(String var, String complete, String newVal, Object actual) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
//        log.info("var -> "+var);

	//Check if the java variable path begins with 'void'
	//if it does, it is related to an information that is written in the application YANG model
	//but that is not present inside the Java code.
	//Since there is no variable to set, return true (nothing to do, job done c:)
	if(var.substring(0, 4).equals("void"))
		return true;
        String[] fs = var.split(Pattern.quote("/"));
        if(fs.length==1){
//            log.info("**we are in a leaf** - complete "+complete);
            //LEAF!
            if(var.contains("[")){
                //ELEMENT OF A LIST OR A MAP
                String index = var.substring(var.indexOf("[")+1, var.indexOf("]"));
                //type of the element of the list/map
                Field f = actual.getClass().getField(var.substring(0, var.lastIndexOf("[")));
                ParameterizedType pt = (ParameterizedType)f.getGenericType();
                Class<?> itemType = (Class<?>)pt.getActualTypeArguments()[0];
                if(List.class.isAssignableFrom(f.getType())){
                    if(f.get(actual)==null){
                        //INSERT A NEW LIST - NOW IS NULL
                        try {
//                            log.info("Setting the values of a list");
                            List<Object> l = (f.getType().isInterface())?new ArrayList<>():(List)f.getType().newInstance();
                            try{
                                l.add((new Gson()).fromJson(newVal, itemType));
                            }catch(Exception e){
                                //classic deserialization failed : PERSONALIZED DESERIALIZATION
                                Object toInsert = personalizedDeserialization(itemType, newVal);
                                if(toInsert!=null)
                                    l.add(toInsert);
                            }
                            f.set(actual, l);
                            return true;
                        } catch (InstantiationException ex) {
                            //SETTING FAILED
                            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                            log.info(ex.getMessage());
                        }
                    }else if(index.matches("")){
                        //INSERT A NEW ELEMENT IN A PRESENT LIST
                        ((List)f.get(actual)).add((new Gson()).fromJson(newVal, itemType));
                    }else{
                        //CHANGE THE VALUE OF AN ELEMENT OF THE LIST
                        String listName = complete.substring(0, complete.length()-index.length()-1);
                        listName = generalIndexes(listName)+"[]";
                        listName+="]";
                        if(lists.containsKey(listName)){
                            Object toChange = null;
                            String indice = lists.get(listName);
                            List<Object> l = (List)f.get(actual);
                            for(Object item:l){
                                if(item.getClass().getField(indice).get(item).toString().equals(index)){
                                    toChange = item;
                                    break;
                                }
                            }
                            if(toChange!=null){
                                try{
                                    l.add((new Gson()).fromJson(newVal, itemType));
                                    l.remove(toChange);
                                    return true;
                                }catch(Exception e){
                                    Object toInsert = personalizedDeserialization(itemType, newVal);
                                    if(toInsert!=null){
                                        l.add(toInsert);
                                        l.remove(toChange);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }else if(Map.class.isAssignableFrom(f.getType())){
//                    log.info("**I'm in the map option");
                    if(f.get(actual)==null){
                        //THERE IS NOT THE MAP - INITIALIZE
                        try {
//                            log.info("The map is null");
                            Map<Object, Object> m = (f.getType().isInterface())?new HashMap<>():(Map)f.getType().newInstance();
                            Class<?> valueType = (Class<?>)pt.getActualTypeArguments()[1];
                            ObjectNode node = (ObjectNode)mapper.readTree(newVal);
                            //define the key - removed from the Json structure
                            JsonNode kNode = node.get("{key}");
                            node.remove("{key}");
                            //The key is a value node: fields length == 1
                            Object k = null;
                            if(kNode.isValueNode())
                                k = (Number.class.isAssignableFrom(itemType))?kNode.asLong():kNode.asText();
                            else{
                                k = itemType.newInstance();
//                                log.info("Setting complex key "+kNode);
                                Iterator<String> kfields = kNode.fieldNames();
                                while(kfields.hasNext()){
                                    String fName = kfields.next();
                                    setVariable(fName, complete+"/"+fName, mapper.writeValueAsString(kNode.get(fName)), k);
                                }
                            }
                            Object value = valueType.newInstance();
                            Iterator<String> fields = node.fieldNames();
                            while(fields.hasNext()){
                                String fieldName = fields.next();
                                JsonNode v = node.get(fieldName);
                                Field fV = value.getClass().getField(fieldName);
                                
                                try{
                                    if(Number.class.isAssignableFrom(fV.getType()))
                                        fV.set(value, v.asDouble());
                                    else
                                        fV.set(value, (new Gson()).fromJson(mapper.writeValueAsString(v), fV.getType()));
                                }catch(Exception e){
                                    try{
                                        Object des = personalizedDeserialization(fV.getType(), mapper.writeValueAsString(v));
                                        fV.set(value, des);
                                    }catch(Exception ex){
                                        log.info("Can't deserialize correctly!");
                                    }
                                }
                            }
                            //value = ((new Gson()).fromJson(mapper.writeValueAsString(node), valueType));
//                            log.info("And k is.."+k);
                            if(k!=null)
                                m.put(k, value);
                            f.set(actual, m);
                            return true;
                            //!!
                            //m.put((new Gson()).fromJson(newVal, Map.Entry<Object,itemType>));
                            //System.out.println("SETTED M = "+f.get(actual));
                        } catch (InstantiationException ex) {
                            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }else if(index.matches("")){
//                        log.info("The index is null but the map not!");
                        //INSERT A NEW ELEMENT IN THE MAP
                        try{
                            Class<?> valueType = (Class<?>)pt.getActualTypeArguments()[1];
                            ObjectNode node = (ObjectNode)mapper.readTree(newVal);
//                            log.info("The new value to set is: "+newVal);
//                            log.info("And the name "+complete);
                            JsonNode kNode = node.get("{key}");
                            node.remove("{key}");
                            Object k = null;
                            if(kNode!=null && kNode.isValueNode())
                                k = (Number.class.isAssignableFrom(itemType))?kNode.asLong():kNode.asText();
                            else{
                                k = itemType.newInstance();
//                                log.info("Setting complex key "+kNode);
                                Iterator<String> kfields = kNode.fieldNames();
                                while(kfields.hasNext()){
                                    String fName = kfields.next();
                                    setVariable(fName, complete+"/"+fName, mapper.writeValueAsString(kNode.get(fName)), k);
                                }
                            }
                            Object value = valueType.newInstance();
                            Iterator<String> fields = node.fieldNames();
                            while(fields.hasNext()){
                                String fieldName = fields.next();
                                JsonNode v = node.get(fieldName);
//                                log.info("##the field is "+fieldName+" and the value is "+v );
                                if(fieldName.equals("{value}")){
                                    if(Number.class.isAssignableFrom(valueType))
                                        value = v.asDouble();
                                    else
                                        value = v.asText();
                                }else{
                                    Field fV = value.getClass().getField(fieldName);
                                    try{
                                        if(Number.class.isAssignableFrom(fV.getType())){
//                                            log.info("number value..."+v.numberValue());
                                            if(Double.class.isAssignableFrom(fV.getType()))
                                                fV.set(value, v.asDouble());
                                            else
                                                fV.set(value, v.numberValue());
                                        }
                                        else
                                            fV.set(value, (new Gson()).fromJson(mapper.writeValueAsString(v), fV.getType()));
                                    }catch(Exception e){
                                        try{
                                            Object des = personalizedDeserialization(fV.getType(), mapper.writeValueAsString(v));
                                               fV.set(value, des);
                                        }catch(Exception ex){
                                            log.info("Can't deserialize correctly!");
                                        }
                                    }
                                }
                            }
                            //value = ((new Gson()).fromJson(mapper.writeValueAsString(node), valueType));
                            
//                            log.info("And k is.."+k);
                            if(k!=null){
//                                log.info("k is != null");
//                                log.info("actual is "+actual);
//                                log.info("f is "+f);
                                Map m = (Map)f.get(actual);
                                m.put(k, value);
//                                log.info("new map is "+m);
                                f.set(actual, m);
//                                log.info("Is setted!!");
                            }
                            return true;
                        } catch (IOException ex) {
                            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                            log.info(ex.getMessage());
                        } catch (InstantiationException ex) {
                            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                            log.info(ex.getMessage());
                        }
                        //((Map)f.get(actual)).put((new Gson()).fromJson(newVal, itemType));
                    }else{
                        //CHANGE THE VALUE OF AN ELEMENT OF THE MAP
                        String listName = complete.substring(0, complete.length()-index.length()-1);
                        listName = generalIndexes(listName)+"[]";
                        listName+="]";
                        if(lists.containsKey(listName)){
                            Object toChange = null;
                            String indice = lists.get(listName);
                            if(List.class.isAssignableFrom(f.getType())){
                                List<Object> l = (List)f.get(actual);
                                for(Object item:l){
                                    if(item.getClass().getField(indice).get(item).toString().equals(index)){
                                        toChange = item;
                                        break;
                                    }
                                }
                                if(toChange!=null){
                                    try{
                                        l.add((new Gson()).fromJson(newVal, itemType));
                                        l.remove(toChange);
                                    }catch(Exception e){
                                        Object toInsert = personalizedDeserialization(itemType, newVal);
                                        if(toInsert!=null){
                                            l.add(toInsert);
                                            l.remove(toChange);
                                        }
                                    }
                                }
                            }else if(Map.class.isAssignableFrom(f.getType())){
                                Map<Object, Object> l = (Map)f.get(actual);
                                for(Object item:l.keySet()){
                                    if(item.toString().equals(index)){
                                        toChange = item;
                                        break;
                                    }
                                }
                                if(toChange!=null){    
                                    
                                    //l.put((new Gson()).fromJson(newVal, new TypeToken<l>(){}.getType()));
                                    l.remove(toChange);
                                }                                
                            }
                        }
                    }                    
                }
                   
            }else{
                //LEAF TO SET
                Field f = actual.getClass().getField(var);
//                log.info("--Arrivata al field da configurare "+f.getName()+" "+f.getGenericType());
//                log.info("Valore: "+newVal);
                Object toInsert;
                try{
                    toInsert = (new Gson()).fromJson(newVal, f.getGenericType());
//                    log.info("Translated");
                    f.set(actual, toInsert);
//                    log.info("Settato!");
                    return true;
                }catch(Exception e){
                    //classic deserialization failed : PERSONALIZED DESERIALIZATION
                    toInsert = personalizedDeserialization(f.getType(), newVal);
                    if(toInsert!=null){
                        f.set(actual, toInsert);
                        return true;
                    }
                    return false;
                }
//                log.info("okk settato");
            }
                
        }else{
            //THE LEAF IS AN ELEMENT OF A LIST OR A MAP
            if(fs[0].contains("[")){
                //select element in the list
                //get the type of the elements
                String listName = complete.substring(0, complete.length()-var.length()+fs[0].length());
                String idItem = listName.substring(listName.lastIndexOf("[")+1, listName.lastIndexOf("]"));
                listName = listName.substring(0, listName.length()-idItem.length()-2)+"[]";
                listName = generalIndexes(listName)+"[]";
                String indice = null;
                if(lists.containsKey(listName))
                    indice = lists.get(listName);
                Field listMap = actual.getClass().getField(fs[0].substring(0, fs[0].length()-idItem.length()-2));
                actual = listMap.get(actual);
                if(List.class.isAssignableFrom(actual.getClass())){
                for(Object litem:(List)actual){
                    boolean correct = litem.getClass().getField(indice).get(litem).toString().equals(idItem);
                    if(correct)
                        return setVariable(var.substring(fs[0].length()+1), complete, newVal, litem);
                }
                }else if(Map.class.isAssignableFrom(actual.getClass())){
                    //get the type of the key and of the value
                    ParameterizedType pt = (ParameterizedType)listMap.getGenericType();
                    Class<?> keyType = (Class<?>)pt.getActualTypeArguments()[0];
                    Class<?> valueType = (Class<?>)pt.getActualTypeArguments()[1];
                    if(actual!=null){
                        boolean found = false;
                        for(Object litem:((Map)actual).keySet()){
                            boolean correct = litem.toString().equals(idItem);
                            if(correct){
                                found = true;
                                return setVariable(var.substring(fs[0].length()+1), complete, newVal, ((Map)actual).get(litem));
                            }

/////////-------------------!!!!!!!!!!!FINISH THE SETTING!

                            if(!found){
                                try {
                                    //construct the element!
                                    JsonNode kNode = mapper.readTree(idItem);
                                    if(kNode.isValueNode()){
                                        
                                    }else{
                                        try {
                                            Object key = keyType.newInstance();
                                            
                                        } catch (InstantiationException ex) {
                                            log.info("Can't instantiate the object key");
                                            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                } catch (IOException ex) {
                                    Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }
                }
            }else{
                Field f = actual.getClass().getField(fs[0]);
//                log.info("Passing throug "+f.getGenericType());
                actual = f.get(actual);
                return setVariable(var.substring(fs[0].length()+1), complete, newVal, actual);
            }
        }
        return false;
    }
    
    public void stopSL(){
        stopTimerTasks();
        stopCondition = true;
        
    } 
    
    
    //returns the id value of the given item of the list
    private String searchLeafInList(Object actual, String idLista) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException{
        String id = null;
        Field[] fs = actual.getClass().getFields();
        for(int i=0; i<fs.length;i++)
            if(fs[i].getName().equals(idLista)){
                Object f = actual.getClass().getField(idLista).get(actual);
                id = (f==null)?null:f.toString();
                return id;
            }        
        return idLista;
    }
    
    private boolean allDefault(String index, Object obj, String tillHereJava){
        try {
//            log.info("tillHereJava is "+tillHereJava);
            tillHereJava = generalIndexes(tillHereJava.substring(1));
//            log.info("trasformed in "+tillHereJava);
            JsonNode indexObj = mapper.readTree(index);
            Field[] fields = obj.getClass().getFields();
            for(int i=0; i<fields.length;i++){
                String fieldName = fields[i].getName();
//                log.info("field : "+fieldName);
                String fYang = null;
//                log.info("roba da cercare "+"root/"+tillHereJava+"/{key}/"+fieldName);
                if(YangToJava.containsKey("root/"+tillHereJava+"/{key}/"+fieldName))
                    fYang = YangToJava.get("root/"+tillHereJava+"/{key}/"+fieldName);
//                log.info("fYang "+fYang);
                if(fYang!=null){
                    fYang = fYang.substring(fYang.lastIndexOf("/")+1);
//                    log.info("Ovvero "+fYang);
//                    log.info("oggetto nella chiave "+fields[i].get(obj));
//                    log.info("oggetto nell'index "+indexObj.get(fYang));
//                    log.info("indexObje ha il campo? "+indexObj.has(fYang));
//                    log.info("The value in indexObj "+indexObj.get(fYang).asText());
//                    log.info("And in obj "+fields[i].get(obj));
                    if(indexObj.has(fYang) && !indexObj.get(fYang).asText().equals(fields[i].get(obj).toString())){
//                        log.info("****torno comunque FALSE****");
                        return false;
                    }
                }
            }
            return true;
        } catch (IOException ex) {
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            log.info("the index an dthe object value are not comparable");
            return false;
        } catch (IllegalArgumentException ex) {
            log.info("Error in the comparation of fields");
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            log.info("Error in the comparation of fields");
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    //get the value of a specific leaf
    public Object getLeafValue(String id){
        if(state.containsKey(id))
            return state.get(id);
        try{
            Object actual = root;
            String[] fields = id.split(Pattern.quote("/"));
            String recompose = new String();
            for(int i = 0; i<fields.length; i++){
                //log.info("Actual is "+actual);
                if(actual==null)
                    return null;
                recompose +="/"+fields[i];
                if(fields[i].contains("[")){
                    String field = fields[i].substring(0, fields[i].lastIndexOf("["));
                    String index = fields[i].substring(fields[i].lastIndexOf("[")+1, fields[i].lastIndexOf("]"));
                    actual = actual.getClass().getField(field).get(actual);
                    if(Map.class.isAssignableFrom(actual.getClass())){
                        if(i<fields.length-1 && fields[i+1].equals("{key}")){
                            boolean found = false;
                            for(Object k:((Map)actual).keySet()){
                                String jsonKey = (new Gson()).toJson(k);
//                                log.info("The k is "+(new Gson()).toJson(k));
//                                log.info("recompose is "+noIndexes(recompose.substring(1)));
//                                JsonNode res = getCorrectItem(index, noIndexes(recompose.substring(1)));
//                                log.info("perché res è null? -> "+res);
                                if(jsonKey.equals(index) || ((index.startsWith("{")||index.startsWith("["))&&allDefault(index, k, recompose))){
                                    actual= k;
//                                    log.info("found k "+k);
                                    found = true;
                                    break;
                                }
                            }
                            if(!found)
                                actual = null;
                        }
                        else{
                            boolean found = false;
                            for(Object k:((Map)actual).keySet()){
                                String jsonKey = (new Gson()).toJson(k);
                                if(jsonKey.equals(index) || ((index.startsWith("{")||index.startsWith("["))&&allDefault(index, k, recompose))){
                                    actual= ((Map)actual).get(k);
                                    found = true;
                                    break;
                                }
                            }
                            if(!found)
                                actual = null;
                        }
                    }else{
                        String general = generalIndexes(recompose.substring(1));
                        actual = getListItemWithIndex((List)actual,index, general.substring(0, general.lastIndexOf("["))+"[]");
                    }
                }else{
                    if(fields[i].equals("{key}"))
                        continue;
                    if(fields[i].equals("{value}"))
                        continue;
//                    log.info("Getting in "+actual+" the field "+fields[i]+" -> "+actual.getClass());
                    Field[] listing = actual.getClass().getFields();
//                    for(int j=0; j<listing.length;j++)
//                        log.info("Say hello to the field "+listing[j].getName()+" : "+listing[j].getType()+" in actual its value is "+listing[j].get(actual));
                    actual = actual.getClass().getField(fields[i]).get(actual);
                }
            }
            return actual;
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

/*
	 * transformListIndexes receives the full path of a YANG information (e.g., config-nat/interfaces[wan]/address)
	 * and drop the index. If the index is a well-known index, this method put the name of the index
	 * in place of its value (e.g., config-nat/interfaces[wan]/address -> config-nat/interfaces[config-nat/nat/interface-public]/address).
	 */
	private String transformListIndexes(String path){
		//The following regular expression separates the [index] from the string
		//e.g. config-nat/interfaces[wan]/ip[v4]/address -> {"config-nat/interfaces", "[wan]", "/ip", "[v4], "/address"}
		String[] separated = path.split("(?<=\\])|(?=\\[)");
		List<String> buildingNewPath = new ArrayList<String>();
		log.info("dentro TRANSFORMLISTINDEXES");	
		//Starting from the string splitted by the above regular expression, join the indexes with the name list
		//e.g., {"config-nat/interfaces", "[wan]", "/ip", "[v4], "/address"} -> {"config-nat/interfaces[wan]", "/ip[v4], "/address"}
		for(int i = 0; i < separated.length; i ++ ){
			if(separated[i].contains("[") && separated[i].contains("]")){
				String tmp = buildingNewPath.get(buildingNewPath.size() - 1);
				tmp += separated[i];
				buildingNewPath.set(buildingNewPath.size() - 1, tmp);
			}
			else
				buildingNewPath.add(separated[i]);
		}
		
		//Analyze the index of each list, if it is a well known static index, which is valid for the analyzed list
		//substitue the value with the name of the index. Otherwise strip away the index
		//e.g. {"config-nat/interfaces[wan]", "/ip[v4], "/address"} -> {"config-nat/interfaces[config-nat/nat/interface-public]", "/ip[], "/address"}
		try{
		for(int i = 0; i < buildingNewPath.size(); i ++){
			String currentPath = buildingNewPath.get(i); 
			if(currentPath.contains("[") && currentPath.contains("]")){
				String index = currentPath.substring(currentPath.lastIndexOf('[') + 1, currentPath.lastIndexOf(']'));
				String listPath = currentPath.substring(0, currentPath.lastIndexOf('['));
				
				boolean found = false;				
				if(allowedStaticIndexesInList.containsKey(listPath)){
					log.info("dimensione allowedStaticIndexesinList: " + allowedStaticIndexesInList.size());
					for(String indexAllowed : allowedStaticIndexesInList.get(listPath)){
						//log.info("ris: " + staticListIndexes.get(indexAllowed).equals(index));
						log.info("indexAllowed: " + indexAllowed);
						log.info("res = " + staticListIndexes.get(indexAllowed));
						if(staticListIndexes.containsKey(indexAllowed) && staticListIndexes.get(indexAllowed) != null && staticListIndexes.get(indexAllowed).equals(index)){
							found = true;
							buildingNewPath.set(i, listPath + '[' + indexAllowed + ']');
						}
					}
					if(!found)
						buildingNewPath.set(i, listPath + "[]");
				}
			}
		}
		
		//join the path from the buildingNewPath list
        	String finalPath = new String();
        	for(String tmp : buildingNewPath)
        		finalPath += tmp;
		return finalPath;
        	}catch(Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			log.info("error: " + sw.toString());
		}
        	return null;
	}
    
    //TRANSLATION FROM THE YANG NAME TO THE JAVA NAME
    public String fromYangToJava(String path){
	log.info("path: " + path);
        String yang = transformListIndexes(path);
        log.info("yang "+yang);
        String j =null;
        if(YangToJava.containsValue(yang))
            for(String s:YangToJava.keySet())
                if(YangToJava.get(s).equals(yang))
                    j=s;
        log.info("j "+j);
        if(j==null)
            return j;
        String[] java = j.split("["+Pattern.quote("[")+"," +Pattern.quote("]")+"]");
        j=new String();
	String[] separated = path.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
        for(int i=0; i<java.length; i++){
            if(i%2==0){
                j+=java[i];
            }else{
//                if(!separated[i].equals("")&&(separated[i].startsWith("{")|| separated[i].startsWith("["))){
//                    try {
//                        yang+="[]";
////                        log.info("Passo yang = "+yang+" e separated[i] = "+separated[i]);
//                        JsonNode newIndex = getCorrectItem(separated[i], yang);
//                        log.info("Et voilà -> newIndex "+newIndex);
//                        j+="["+mapper.writeValueAsString(newIndex)+"]";
//                    } catch (JsonProcessingException ex) {
//                        log.info("Errrrrrror");
//                        Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }else    
                    j+="["+separated[i]+"]";
            }
        }
        log.info("j di nuovo "+j);
        if(path.endsWith("[]"))
            j+="[]";
        return j;
    }
    
        public Object getLists(Object actual, String remaining, String complete) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        String[] fs = remaining.split(Pattern.quote("/"));
        String fint = fs[0];
        if(fint.contains("[]")){
            //Siamo arrivati!
            fint = fint.substring(0, fint.length()-2);
            actual = actual.getClass().getField(fint).get(actual);
            return actual;
        }else{
            if(fint.contains("[")){
                //dobbiamo andare a prendere il valore giusto all'interno della lista
                String indice = fint.substring(fint.indexOf("[")+1, fint.length()-1);
                String listName = complete.substring(0, complete.length()-remaining.substring(fint.length()+1).length() -indice.length()-3) + "[]";                
                actual = actual.getClass().getField(fint.substring(0, fint.length()-indice.length()-2)).get(actual);
                Object item = null;
                if(List.class.isAssignableFrom(actual.getClass()))
                    item = getListItemWithIndex((List)actual, indice, listName);
                else if(Map.class.isAssignableFrom(actual.getClass()))
                    item = (((Map)actual).get(indice));
                return getLists(item, remaining.substring(fint.length()+1), complete);
            }else{
                //dobbiamo andare dentro l'oggetto
                actual = actual.getClass().getField(fint).get(actual);
                return getLists(actual, remaining.substring(fint.length()+1), complete);
            }
        }
    }

            //given a list, gets the element with the id specified
    private Object getListItemWithIndex(List list, String indice, String listName) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        String indexValue=null;
        String general = generalIndexes(listName)+"[]";
        if(lists.containsKey(listName)){
            indexValue = lists.get(listName);
        }
        if(indexValue!=null){
            for(Object obj:list){
                String i = obj.getClass().getField(indexValue).get(obj).toString();
                if(indice.equals(i))
                    return obj;
            }
        }
        return null;
    }
    

    //Versione "YIN" Json
    //SAVE THE VALUES OF THE CONFIG MANDATORY AND ADVERTISE ELEMENTS OF THE LEAFS
    //DIVIDE THE LEAFS IN THE DIFFERENT STRUCTURES BASED ON THE ADVERTISE VALUE
    //IF PERIODIC -> START TE NEW THREAD
    private void findYinLeafs(JsonNode y, String prev) {
        Iterator<Entry<String, JsonNode>> iter = y.fields();
        while(iter.hasNext()){
            Entry<String, JsonNode> value = iter.next();
            String fieldName = value.getKey();
            JsonNode valueNode = value.getValue();
            if(fieldName.equals("leaf")){
                //can be an array
                if(valueNode.isArray()){
                    Iterator<JsonNode> leafs = ((ArrayNode)valueNode).elements();
                    while(leafs.hasNext()){
                        ObjectNode child = (ObjectNode)leafs.next();
                        boolean conf;
                        if(child.get("config")!=null){
                            conf = child.get("config").get("@value").asBoolean();
                        }else{
                            conf = true;
                        }
                        //System.out.println("-+-config "+conf);
                        config.put(prev+"/"+child.get("@name").textValue(), conf);
                        
                        if(child.get("type")!=null){
                            String type = child.get("type").get("@name").asText();
                            YangType.put(prev+"/"+child.get("@name").textValue(),type);
                        }
                        if(child.get("mandatory")!=null){
                            Boolean mand = child.get("mandatory").get("@value").asBoolean();
                            YangMandatory.put(prev+"/"+child.get("@name").textValue(), mand);
                        }else
                            YangMandatory.put(prev+"/"+child.get("@name").textValue(), true);
                        //check advertise attribute - prefix:advertise
                        Iterator<String> searchAdv = child.fieldNames();
                        String pref=null;
                        while(searchAdv.hasNext()){
                            String f = searchAdv.next();
                            if(f.endsWith(":advertise")){
                                pref = f.substring(0, f.length()-10);
                                break;
                            }
                        }
                        if(pref!=null){
                            //the advertise field is specified
                            String adv = child.get(pref+":advertise").get("@advertise").asText();
                            if(adv.equals("onchange")){
                                toListenPush.add(prev+"/"+child.get("@name").textValue());
                            }else if(adv.equals("periodic")){
                                if(child.has(pref+":period")){
                                    long p = child.get(pref+":period").get("@period").asLong();
                                    PeriodicVariableTask task = new PeriodicVariableTask(this, prev+"/"+child.get("@name").textValue());
                                    toListenTimer.add(task);
                                    timer.schedule(task, p, p);
                                }
                                //has to have!!
                            }else if(adv.equals("onthreshold")){
                                Object min = null;
                                Object max = null;
                                if(child.has(pref+":minthreshold")){
                                    min = child.get(pref+":minthreshold").get("@minthreshold").asDouble();
                                }
                                if(child.has(pref+":maxthreshold")){
                                    max = child.get(pref+":maxthreshold").get("@maxthreshold").asDouble();
                                }
                                if(min!=null || max!=null)
                                    toListenThreshold.put(prev+"/"+child.get("@name").textValue(), new Threshold(min, max));
                            }
                            //if never - nothing
                        }
                        //default:never
                    }
                }else{
                    //one single leaf 
                    boolean conf;
                    if(valueNode.get("config")!=null){
                        conf = valueNode.get("config").get("@value").asBoolean();
                    }else{
                        conf = true;
                    }
                    //System.out.println("-+-config "+conf);
                    config.put(prev+"/"+valueNode.get("@name").asText(), conf);
                    Iterator<String> searchAdv = valueNode.fieldNames();
                    String pref=null;
                    while(searchAdv.hasNext()){
                        String f = searchAdv.next();
                        if(f.endsWith(":advertise")){
                            pref = f.substring(0, f.length()-10);
                            break;
                        }
                    }
                    if(pref!=null){
                        //the advertise field is specified
                        String adv = valueNode.get(pref+":advertise").get("@advertise").asText();
                        if(adv.equals("onchange")){
                            toListenPush.add(prev+"/"+valueNode.get("@name").asText());
                        }else if(adv.equals("periodic")){
                            if(valueNode.has(pref+":period")){
                                long p = valueNode.get(pref+":period").get("@period").asLong();
                                PeriodicVariableTask task = new PeriodicVariableTask(this, prev+"/"+valueNode.get("@name").asText());
                                toListenTimer.add(task);
                                timer.schedule(task, p, p);
                            }
                            //has to have!!
                        }else if(adv.equals("onthreshold")){
                            Object min = null;
                            Object max = null;
                            if(valueNode.has(pref+":minthreshold")){
                                min = valueNode.get(pref+":minthreshold").get("@minthreshold").asDouble();
                            }
                            if(valueNode.has(pref+":maxthreshold")){
                                max = valueNode.get(pref+":maxthreshold").get("@maxthreshold").asDouble();
                            }
                            if(min!=null || max!=null)
                                toListenThreshold.put(prev+"/"+valueNode.get("@name").textValue(), new Threshold(min, max));
                        }
                        //if never - nothing
                    }
                    //default:never
                }
            }else if(fieldName.equals("key")){
		    if(valueNode.has("@value")){
			    String key = new String();
			    key = valueNode.get("@value").textValue();
			    keyOfYangLists.put(prev, key);
		    }
	    }else{
                //traverse
                    if(valueNode.isArray()){
                        Iterator<JsonNode> objs = ((ArrayNode)valueNode).elements();
                        while(objs.hasNext()){
				JsonNode next = objs.next();
				if(next.has("@name")&&fieldName.equals("list"))
					findYinLeafs(next, prev+"/"+next.get("@name").textValue()+"[]");
				else if(next.has("@name"))
					findYinLeafs(next, prev+"/"+next.get("@name").textValue());                          
			}
                    }else{
                        if(valueNode.has("@name")&&fieldName.equals("list"))
                            findYinLeafs(valueNode, prev+"/"+valueNode.get("@name").textValue()+"[]");
                        else if(valueNode.has("@name"))
                            findYinLeafs(valueNode, prev+"/"+valueNode.get("@name").textValue());
                    }
            }
        }
    }


    //CHECK IF THE VALUES ARE TO NOTIFY AND EVENTUALLY SEND THE MESSAGE TO THE CONNECTIONMODULE
    private void checkThreshold(Map<String, Object> thr) {
        //values in stateNew
        for(String s: thr.keySet()){
            //if threshold -> publish
            boolean pub = false;
            String generalS = generalIndexes(s);
            String y = null;
            if(YangToJava.containsKey("root/"+generalS)){
                y = YangToJava.get("root/"+generalS);
                if(toListenThreshold.containsKey(y)){
                    if(toListenThreshold.get(y).MIN!=null){
                        if(toListenThreshold.get(y).MAX!=null){
                            if(((Number)thr.get(s)).doubleValue() > (Double)toListenThreshold.get(y).MIN && ((Number)thr.get(s)).doubleValue() < (Double)toListenThreshold.get(y).MAX)
                                pub = true;
                        }else if (((Number)thr.get(s)).doubleValue() > (Double)toListenThreshold.get(y).MIN){
                            pub = true;
                        }
                    }else{
                        if(((Number)thr.get(s)).doubleValue() < (Double)toListenThreshold.get(y).MAX)
                            pub = true;
                    }
                }
            }
            if(pub){
                if(!stateThreshold.containsKey(s) || !stateThreshold.get(s).equals(thr.get(s))){
                    NotifyMsg e = new NotifyMsg();
                    e.act = action.UPDATED;
                    e.var = trasformInPrint(s);
                    e.obj = thr.get(s).toString();
                    stateThreshold.put(s, thr.get(s));
                    //System.out.println("---*ONTHRESHOLD");
                    //System.out.println((new Gson()).toJson(e));
                    log.info("*OnThreshold* "+(new Gson()).toJson(e));
                    cM.somethingChanged((new Gson()).toJson(e));
                }
            }else{
                if(stateThreshold.containsKey(s))
                    stateThreshold.remove(s);
            }
        }
    }
 
    public enum action{ADDED, UPDATED, REMOVED, NOCHANGES, PERIODIC};
    public class NotifyMsg{
        public action act;
        public Object obj;
        public String var;
        public Date timestamp = new Date(System.currentTimeMillis());
        public action getAct() {
            return act;
        }

        public void setAct(action act) {
            this.act = act;
        }

        public Object getObj() {
            return obj;
        }

        public void setObj(Object obj) {
            this.obj = obj;
        }

        public String getVar() {
            return var;
        }

        public void setVar(String var) {
            this.var = var;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }
    }
    
    private class Threshold{
        public Object MAX;
        public Object MIN;
        
        public Threshold(Object MIN , Object MAX){
            this.MAX = MAX;
            this.MIN = MIN;
        }
    }
      
    
    //Task for periodic variables
    private class PeriodicVariableTask extends TimerTask{
        String var;
        StateListenerNew sl;
        
        public PeriodicVariableTask(StateListenerNew sl, String var){
            this.sl = sl;
            this.var = var;
            //System.out.println("COSTRUITO THREAD TIMER PER "+var);
        }
        
        public void run(){
    //        sl.log.info("**Periodic Task of " + var+ " running**");
            Map<String, Object> listToSave = new HashMap<>();
            try{
                if(YangToJava.containsValue(var)){
                    String j = null;
                    for(String k:YangToJava.keySet())
                        if(YangToJava.get(k).equals(var)){
                            j = k;
                            break;
                        }
    //                log.info("found "+j.substring(5));
                    sl.saveValues(sl.root, j.substring(5), j.substring(5), listToSave);
                }
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            }
            for(String s: listToSave.keySet()){
                NotifyMsg e = new NotifyMsg();
                e.act = action.PERIODIC;
                e.obj = listToSave.get(s).toString();
                e.var = sl.trasformInPrint(s);
    //            log.info("Trasformed in print "+s+" -> "+e.var);
                sl.cM.somethingChanged((new Gson()).toJson(e));
            }
        }
        
    }
    
    /*    //Versione YIN (xml) non usata
    private void findYinLeafs(Element e, String prev){
        if(e.getTagName().equals("leaf")){
            //System.out.println(prev+"/"+e.getAttribute("name"));
            NodeList att = e.getChildNodes();
            for(int i=0;i<att.getLength();i++){
                if(att.item(i).getNodeName().equals("config")){
                    log.info("Ho trovato il config");
                    boolean c = (att.item(i).getAttributes().item(0).getNodeValue().equals("true"))?true:false;
                    //System.out.println("-+-config "+att.item(i).getAttributes().item(0).getNodeValue());
                    config.put(prev.substring(1)+"/"+e.getAttribute("name"), c);
                }
                if(att.item(i).getNodeName().equals("type")){
                    log.info("Ho trovato il type");
                    String t = att.item(i).getAttributes().item(0).getNodeValue();
                    log.info(prev.substring(1)+"/"+e.getAttribute("name")+" is a "+t);
                    YangType.put(prev.substring(1)+"/"+e.getAttribute("name"), t);
                }
                //default
            }
            if(!config.containsKey(prev+"/"+e.getAttribute("name")))
                config.put(prev.substring(1)+"/"+e.getAttribute("name"), true);
            //System.out.println("Lista config -- "+config);
            return;
        }
        Node n = (Node)e;
        NodeList children = n.getChildNodes();
        boolean list = e.getNodeName().equals("list");
        for(int i=0;i<children.getLength();i++)
            if(children.item(i).getNodeType()==Node.ELEMENT_NODE){
                String pref = prev+"/"+e.getAttribute("name");
                pref=(list)?pref+"[]":pref;
                findYinLeafs((Element)children.item(i), pref);
            }
    }
    
    
    */
    
/*    private void findYangLeafs(YangTreeNode tree) {
        YANG_Body node = tree.getNode();
        //System.out.println(node);
        Vector<YangTreeNode> children = tree.getChilds();
        if(children.size()==0){
            //System.out.println("Is a leaf");
            YANG_Config config = node.getConfig();
            //System.out.println(config);
        }
        for(int i=0;i<children.size();i++){
            findYangLeafs(children.get(i));
        }
    }*/
    
    
    /*    private boolean configVariables(String var){
        var = deleteIndexes(var);
        String[] fields = var.split(Pattern.quote("/"));
        JsonNode n = rootJson;
        for(int i=0;i<fields.length;i++){
            if(fields[i].contains("[]"))
                fields[i] = fields[i].substring(0, fields[i].length()-2);
            if(n.isArray()){
                n = n.get(0);
                n = ((ObjectNode)n).get(fields[i]);
            }else{
                n = ((ObjectNode)n).get(fields[i]);
            }
        }
//        var = var.replace("/", "/");
        boolean c = checkConfig(n, var);
        return c;
    }
    
    
    private boolean checkConfig(JsonNode n, String v){
        if(n.isValueNode()){
            if(config.containsKey(v))
                return config.get(v);
            return false;
        }
        if(n.isArray()){
            n = n.get(0);
            v = (v.endsWith("]"))?v:v+"[]";
            return checkConfig(n, v);
        }else{
            Iterator<String> it = ((ObjectNode)n).fieldNames();
            boolean cc = true;
            while(it.hasNext()){
                String fName = (String)it.next();
                cc = cc && checkConfig(n.get(fName), v+"/"+fName);
            }
            return cc;            
        }
    }*/
    
//        private String allGeneralIndexes(String listName) {
//        String[] splitted = listName.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
//        String j=splitted[0];
//        String onlyLastOne = splitted[0];
//        String y=null;
//        if(splitted.length>1)
//            for(int i=1;i<splitted.length;i++){
//                if(i%2==0){
//                    //nome lista
//                    j+=splitted[i];
//                    onlyLastOne+=splitted[i];
//                }else{
//                    //chiave
//                    if(stateList.containsKey(onlyLastOne+"[]"))
//                        j+="["+stateList.get(onlyLastOne+"[]").idList+"]";
//                    onlyLastOne+=(i==splitted.length-1)?("["+stateList.get(onlyLastOne+"[]").idList+"]"):("["+splitted[i]+"]");
//                            
//                }
//            }
//        return j;
//    }
//        
//    //nullValuesToListen now exist? --> in state, removed form that list
//    //additions or removes in the lists --> added/deleted by state
//    //check if the variables in state have the same value or not
//    public void checkValue(){
//        try{
//            Gson gson = new Gson();
//            List<String> r = new ArrayList<>();
//            List<String> nulls = new ArrayList();
//            nulls.addAll(nullValuesToListen);
//            for(String s:nulls){
//                if(searchLeaf(root, s, s))
//                    r.add(s);
//            }
//            for(String s:r){
//                nullValuesToListen.remove(s);
//            }
//            List<String> copy = new ArrayList<>();
//            copy.addAll(stateList.keySet());
//            for(String lv:copy){
//                List<Object> act = (List)getLists(root, lv, lv);
//                List<NotifyMsg> wH = checkListChanges(lv, stateList.get(lv).List, act);  
//                
//                if(wH!=null){
//                    writeLock.lock();
//                    whatHappened.addAll(wH);
//                    writeLock.unlock();
//                }
//            }
//            List<String> copyState = new ArrayList();
//            if(state!=null)
//                copyState.addAll(state.keySet());
//            for(String s:copyState){
//                boolean c= getLeafValueChange(root, s, s);
//                if(!c){
//                    NotifyMsg e = new NotifyMsg();
//                    e.act=action.UPDATED;
//                    e.obj=state.get(s);
//                    e.var=s;
//                    writeLock.lock();
//                    whatHappened.add(e);
//                    writeLock.unlock();
//                }
//            }
//            List<NotifyMsg> wH = new ArrayList<>();
//            writeLock.lock();
//            wH.addAll(whatHappened);
//            whatHappened = new ArrayList<>();
//            writeLock.unlock();
//            if(wH!=null){
//                for(NotifyMsg e:wH){
//                    String toPrint = trasformInPrint(e.var);
//                    //System.out.println(e.act + " " + toPrint+" " + gson.toJson(e.obj));
//                }
//            }
//            
//            //System.out.println("state aggiornato: " + state);
//            //System.out.println("root "+(new Gson()).toJson(root));
//        } catch (NoSuchFieldException ex) {
//            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalArgumentException ex) {
//            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }  
//    //returns true if the actual value and the old value are the same, false othercase
//    //recursive
//    private boolean getLeafValueChange(Object actual, String remaining, String complete) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
//        String[] fields = remaining.split(Pattern.quote("/"));
//        String finteresting = fields[0];
//        String fremaining = (fields.length>1)?remaining.substring(finteresting.length()+1):null;
//        boolean lista = false;
//        if(finteresting.contains("[")){
//            //devo andare a cercare il giusto oggetto dentro la lista
//            lista = true;
//            String indice = finteresting.substring(finteresting.indexOf("[")+1, finteresting.indexOf("]"));
//            String listName = complete.substring(0, complete.length()-fremaining.length()-indice.length()-3) + "[]";
//            actual = actual.getClass().getField(finteresting.substring(0, finteresting.length()-indice.length()-2)).get(actual);
//            if(actual==null){
//                if(state.get(complete)==null)
//                    return false;
//                else{
//                    //System.out.println("Rimossa lista");
//                    return true;
//                }
//            }
//            Object item = getListItemWithIndex((List)actual, indice, listName);
//            if(item==null){
//                //System.out.println("No items in list");
//                return false;
//            }
//            return getLeafValueChange(item, fremaining, complete);
//        }else{
//            if(fields.length>1){
//                actual = actual.getClass().getField(finteresting).get(actual);
//                if(actual==null){
//                    if(state.get(complete) == null)
//                        return false;
//                    else{
//                        //System.out.println("Removed obj " + finteresting);
//                        return true;
//                    }
//                }
//                return getLeafValueChange(actual, fremaining, complete);
//            }
//            else{
//                actual = actual.getClass().getField(finteresting).get(actual);
//                if(!state.containsKey(complete)){
//                    state.put(complete, actual);
//                    return false;
//                }
//                boolean rValue = state.get(complete).equals(actual);
//                if(!rValue)
//                    state.replace(complete, actual);
//                return rValue;
//            } 
//        }
//    }
//    
        //callable by the app
//    public void addNewListener(String name){
//        try {
//            searchLeaf(root, name, name);
//            toListen.add(name);
//        } catch (NoSuchFieldException ex) {
//            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalArgumentException ex) {
//            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
////founds additions or removes of items in a list
//    public List<NotifyMsg> checkListChanges(String listName, List oldList, List newList) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
//        List<NotifyMsg> res = new ArrayList<>();
//        if(!stateList.containsKey(listName)){
//            //la lista è tutta nuova
//            String id = null; //prendere dal toListen
//            for(Object n:newList){
//                NotifyMsg e = new NotifyMsg();
//                e.act = action.ADDED;
//                e.obj = n;
//                e.var = listName;
//                res.add(e);
//                //ADD THE ELEMENT IN STATE
//                String savedInToListen = listName.substring(0, listName.length()-1) + id+"]";
//                List<String> save = new ArrayList<>();
//                for(String t:toListen){
//                    if(t.contains(savedInToListen) && t.substring(0, savedInToListen.length()).equals(savedInToListen))
//                        save.add(t);
//                }
//                for(String t:save){
//                    String fremaining = t.substring(savedInToListen.length());
//                    String idItem = savedInToListen.substring(0, savedInToListen.length()-id.length()-2)+n.getClass().getField(id).get(n).toString()+"]."+fremaining;
//                    searchLeaf(n, fremaining, idItem);
//                }
//            }
//            saveListstate(listName, id, newList);
//            return res;
//        }
//        String id = stateList.get(listName).idList;
//
//        if(oldList==null && newList==null){
//            return null;
//        }
//        if(oldList==null){
//            
//        }
//        if(newList==null){
//            for(Object old:oldList){
//                NotifyMsg e = new NotifyMsg();
//                e.act = action.REMOVED;
//                e.obj = old;
//                e.var = listName;
//                res.add(e);
//                //REMOVE THE ELEMENT IN STATE
//                List<String> remove = new ArrayList<>();
//                String identifier = listName.substring(0, listName.length()-1)+old.getClass().getField(id).get(old).toString()+"]";
//                for(String s:state.keySet()){
//                    if(s.contains(identifier) && s.substring(0, identifier.length()).equals(identifier))
//                        remove.add(s);
//                }
//                for(String s:remove)
//                    state.remove(s);
//            }
//            saveListstate(listName, id, newList);
//            return res;
//        }
//        List shadowCopy = new LinkedList();
//        shadowCopy.addAll(newList);
//        for(Object old:oldList){
//            String idValue = old.getClass().getField(id).get(old).toString();
//            boolean found = false;
//            for(Object n:shadowCopy){
//                String idValue2 = n.getClass().getField(id).get(n).toString();
//                if(idValue.equals(idValue2)){
//                    found = true;
//                    break;
//                }
//            }
//            if(found==false){
//                NotifyMsg e = new NotifyMsg();
//                e.act = action.REMOVED;
//                e.obj = old;
//                e.var = listName;
//                res.add(e);
//                //REMOVE THE ELEMENT IN STATE
//                List<String> remove = new ArrayList<>();
//                String identifier = listName.substring(0, listName.length()-1)+idValue+"]";
//                for(String s:state.keySet()){
//                    if(s.contains(identifier) && s.substring(0, identifier.length()).equals(identifier))
//                        remove.add(s);
//                }
//                for(String s:remove)
//                    state.remove(s);
//            }
//        }
//        for(Object n:shadowCopy){
//            String idValue = n.getClass().getField(id).get(n).toString();
//            boolean found = false;
//            for(Object old:oldList){
//                String idValue2 = old.getClass().getField(id).get(old).toString();
//                if(idValue.equals(idValue2)){
//                    found = true;
//                    break;
//                }
//            }
//            if(found==false){
//                NotifyMsg e = new NotifyMsg();
//                e.act = action.ADDED;
//                e.obj = n;
//                e.var = listName;
//                res.add(e);
//                //ADD THE ELEMENT IN STATE
//                String savedInToListen = allGeneralIndexes(listName);
//                savedInToListen +="["+id+"]";
//                List<String> save = new ArrayList<>();
//                for(String t:toListen){
//                    if(t.contains(savedInToListen) && t.substring(0, savedInToListen.length()).equals(savedInToListen))
//                        save.add(t);
//                }
//                for(String t:save){
//                    String fremaining = t.substring(savedInToListen.length()+1);
//                    //String idItem = savedInToListen.substring(0, savedInToListen.length()-id.length()-2)+idValue+"]."+fremaining;
//                    String idItem = listName.substring(0, listName.length()-1)+idValue+"]."+fremaining;
//                    searchLeaf(n, fremaining, idItem);
//                }
//            }
//            
//        }
//        if(!res.isEmpty())
//            saveListstate(listName, id, newList);    
//        return (res.isEmpty())?null:res;
//    }
//    //ricorsive method, if the object exists, puts the object to observe in state, and eventuals lists that founds in the stateList
//    //if still the object doesn't exists(or one of the "containers" puts the path to the object in the nullValuesToListen
//    private boolean searchLeaf(Object actual, String fields, String complete) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
//        String[] fs = fields.split(Pattern.quote("/"));
//        String finteresting = fs[0];
//        String fremaining = (fs.length>1)?fields.substring(finteresting.length()+1):null;
//        boolean lista = false;
//        String idLista=null;
//        Field f;
//        if(finteresting.contains("[")){
//            lista=true;
//            idLista = finteresting.substring(finteresting.indexOf("[")+1, finteresting.indexOf("]"));
//            finteresting = finteresting.substring(0, finteresting.indexOf("["));
//        }
//        f = actual.getClass().getField(finteresting);
//        actual = f.get(actual);
//        if(actual==null){
//            //salva temporaneamente: l'oggetto di interesse ancora non esiste
//            if(!nullValuesToListen.contains(complete))
//                nullValuesToListen.add(complete);
//            return false;
//        }
//        if(fs.length>1){
//            //calcola nuovo obj e nuovo fields
//            if(lista){
//                //searchLeaf in tutti gli elementi + controllo stato lista              
//                String idL = complete.substring(0,complete.length()-fremaining.length()-idLista.length()-2)+"]";
//                List ll = new ArrayList<>();
//                ll.addAll((List) actual);
//                boolean addedNw = saveListstate(idL, idLista, ll);
//                if(ll.size()!=0){
//                    boolean rValue = false;
//                    for(Object litem:ll){
//                        String idItem = searchLeafInList(litem, idLista);
//                        String idToPass = complete.substring(0,complete.length()-fremaining.length()-idLista.length()-2)+idItem+"]."+fremaining;
//                        boolean fitem = searchLeaf(litem, fremaining, idToPass);
//                        if(addedNw && fitem){
//                            NotifyMsg e = new NotifyMsg();
//                            e.act=action.ADDED;
//                            e.obj=litem;
//                            e.var = idToPass.substring(0, idToPass.length()-fremaining.length()-1);
//                            writeLock.lock();
//                            whatHappened.add(e);
//                            writeLock.unlock();
//                        }
//                        rValue=rValue||fitem;
//                    }
//                    return rValue;
//                }else
//                    return false;
//            }else
//                return searchLeaf(actual, fremaining, complete);
//        }else{
//            if(!state.containsKey(complete)){
//                NotifyMsg e = new NotifyMsg();
//                e.act=action.ADDED;
//                e.var=trasformInPrint(complete);
//                e.obj=actual;
//            }
//            state.put(complete, actual);
//            return true;
//        }
//    }
//    //copies the actual value of a list in the stateList
//    private boolean saveListstate(String key, String idLista, List ll) {
//        ////System.out.println("in saveListstate lists: " + stateList);
//        ListValues toRem = null;
//        if(ll==null){
//            stateList.remove(key);
//            //add in nullValuesToListen
//            String gen = allGeneralIndexes(key);
//            for(String s:toListen){
//                if(s.contains(gen) && s.substring(0, gen.length()).equals(gen)){
//                    try {
//                        boolean present = searchLeaf(root, s, s);
//                        if(!present)
//                            nullValuesToListen.add(s);
//                    } catch (NoSuchFieldException ex) {
//                        Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
//                    } catch (IllegalArgumentException ex) {
//                        Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
//                    } catch (IllegalAccessException ex) {
//                        Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//                    
//            }
//            ////System.out.println("Dopo lista nulla in saveListstate "+stateList);
//            return true;
//        }
//        List nl = new LinkedList<>();
//        for(int i=0;i<ll.size();i++){
//            nl.add(ll.get(i));
//        }
//        if(stateList.containsKey(key)){
//            stateList.get(key).List = nl;
//            return false;}
//        stateList.put(key, new ListValues(idLista, nl));
//        ////System.out.println("Alla fine ho aggiunto qualcosa "+stateList);
//        return true;
//    }

}
