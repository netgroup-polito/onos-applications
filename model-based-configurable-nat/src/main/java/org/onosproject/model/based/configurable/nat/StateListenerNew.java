/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.onosproject.model.based.configurable.nat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.Random;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.ArrayNode;

/**
 *
 * @author lara
 */
public class StateListenerNew extends Thread{
    //protected List<String> state;
    protected HashMap<String, Object> state;
    //protected HashMap<String, ListValues> stateList;
    protected HashMap<String, String> lists;
    private Object root;
    private boolean stopCondition = false;
    private List<String> toListen;
    //private List<String> nullValuesToListen;
    private HashMap<String, String> YangToJava;
    //private List<events> whatHappened;
    //private ReadLock readLock;
    //private WriteLock writeLock;
    private ConnectionModuleClient cM;
    private final ObjectNode rootJson;
    private final ObjectMapper mapper;
    private HashMap<String, Object> stateNew;
    
    public StateListenerNew(Object root){
        this.root = root;
        state = new HashMap<>();
        //stateList = new HashMap<>();
        toListen = new ArrayList<>();
        //nullValuesToListen = new ArrayList<>();
        YangToJava = new HashMap<>();
        //whatHappened = new ArrayList<>();
        //ReentrantReadWriteLock wHLock = new ReentrantReadWriteLock();
        //readLock = wHLock.readLock();
        //writeLock = wHLock.writeLock();
        lists = new HashMap<>();
        //cM = new ConnectionModuleClient(this, "StateListener");
        
        //PARSE MAPPING FILE
        ClassLoader loader = getClass().getClassLoader();
        File mapFile = new File(loader.getResource("files/mappingFile.txt").getFile());
        try(Scanner s = new Scanner(mapFile)){
            while(s.hasNextLine()){
                String line = s.nextLine();
                String[] couples = line.split(Pattern.quote(";"));
                for(int i=0; i<couples.length;i++){
                    String[] yj = couples[i].split(Pattern.quote(":"));
                    if(yj.length==2)
                        YangToJava.put(yj[1].trim(), yj[0].trim());
                }
                //System.out.println(YangToJava.toString());
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        }
        //ADD VARIABLES TO LISTEN
        Collection<String> all = YangToJava.keySet();
        List<String> sorted = new ArrayList<String>(all);
        Collections.sort(sorted);
        System.out.println(sorted);
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
                String index = l.substring(l.lastIndexOf("[")+1, l.lastIndexOf("]"));
                String idList = l.substring(0, l.length()-index.length()-2);
                lists.put(idList.substring(5)+"[]", index);
            }
        }
        
        mapper = new ObjectMapper();
        rootJson = mapper.createObjectNode();
        
        for(String l:leafs)
            createTree(rootJson, YangToJava.get(l));
        
        System.out.println(leafs);
        for(String s:leafs){
            String s1 = s.substring(5);
            toListen.add(s1);
            //this.addNewListener(s1);
        }
        
        this.start();
    }
    
    
    public void run(){
        while(!stopCondition){
            try {
                System.out.println("Parte il ciclo");
                //checkValue();
                saveNewValues();
                sleep(5000);
            } catch (InterruptedException ex) {
                stopCondition = true;
                cM.deleteResources();
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        cM.deleteResources();
    }
    
    public void saveNewValues(){
        stateNew = new HashMap<>();
        for(String s:toListen){
            try {
                saveValues(root, s, s);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //System.out.println(stateNew);
        checkChangesSaved();
        //System.out.println("new value of state -- ");
        //System.out.println(state);
    }
    
    public void saveValues(Object actual, String subToListen, String complete) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        if(subToListen.contains(".")){
            String inter = subToListen.substring(0, subToListen.indexOf("."));
            if(inter.contains("[")){
                String lName = inter.substring(0, inter.indexOf("["));
                String index = inter.substring(inter.indexOf("[")+1, inter.length()-1);
                actual = actual.getClass().getField(lName).get(actual);
                if(actual!=null){
                    if(List.class.isAssignableFrom(actual.getClass())){
                        for(Object item:(List)actual){
                            String indexValue = searchLeafInList(item, index);
                            String complToPass = complete.substring(0, complete.length()-subToListen.length())+lName+"["+indexValue+"]"+subToListen.substring(inter.length());
                            saveValues(item, subToListen.substring(inter.length()+1), complToPass);
                        }
                    }else if(Map.class.isAssignableFrom(actual.getClass())){
                        for(Object key:((Map)actual).keySet()){
                            String indexValue = key.toString();
                            String complToPass = complete.substring(0, complete.length()-subToListen.length())+lName+"["+indexValue+"]"+subToListen.substring(inter.length());
                            saveValues(((Map)actual).get(key), subToListen.substring(inter.length()+1), complToPass);
                        }
                    }else 
                        return;
                }
            }else{
                actual = actual.getClass().getField(inter).get(actual);
                if(actual!=null)
                    saveValues(actual, subToListen.substring(inter.length()+1), complete);
            }
        }else{
            //leaf
            if(subToListen.contains("[")){
                //è una mappa
                String mapName = subToListen.substring(0, subToListen.indexOf("["));
//                String ind = subToListen.substring(subToListen.indexOf("[")+1, subToListen.indexOf("]"));
                Map mappa = (Map) actual.getClass().getField(mapName).get(actual);
                for(Object k:mappa.keySet()){
                    String complToPass = complete.substring(0, complete.lastIndexOf("[")+1)+k.toString()+"]";
                    stateNew.put(complToPass, mappa.get(k));
                }
            }else{
                actual = actual.getClass().getField(subToListen).get(actual);
                stateNew.put(complete, actual);
            }
        }
    }
    
    private void checkChangesSaved(){
        List<events> happenings = new ArrayList<>();
        HashMap<String, Object> copyState = new HashMap<>();
        HashMap<String, Object> copyNewState = new HashMap<>();
        List<String> ancoraPresenti = new ArrayList<>();
        if(state!=null && stateNew!=null){
            copyState.putAll(state);
            copyNewState.putAll(stateNew);
            for(String k:state.keySet()){
                if(stateNew.containsKey(k)){
                    if(stateNew.get(k)==null){
                        stateNew.remove(k);
                        copyNewState.remove(k);
                        continue;
                    }
                    //non sono stati eliminati
                    if(!state.get(k).equals(stateNew.get(k))){
                       //CHANGED VALUE
                       events e = new events();
                       e.act=action.UPDATED;
                       e.var=trasformInPrint(k);
                       e.obj=stateNew.get(k);
                       happenings.add(e);
                    }
                    copyState.remove(k);
                    copyNewState.remove(k);
                    ancoraPresenti.add(k);
                }
            }
            //update the actual state
            state = stateNew;
            //copyState contains the eliminated
            ObjectNode rootJ = mapper.createObjectNode();
            for(String k:copyState.keySet()){
                events e = new events();
                e.act=action.REMOVED;
                e.obj=copyState.get(k);
                e.var=trasformInPrint(k);
                happenings.add(e);
                insertInNode(rootJ, k, generalIndexes(k), e.obj);
            }
            System.out.println("REM --");
            System.out.println(rootJ);

            //copyNewState contains the added
            rootJ = mapper.createObjectNode();
            for(String k:copyNewState.keySet()){
                events e = new events();
                e.act=action.ADDED;
                e.obj=copyNewState.get(k);
                e.var=trasformInPrint(k);
                happenings.add(e);
                insertInNode(rootJ, k, generalIndexes(k), e.obj);
            }
            System.out.println("ADD--");
            System.out.println(rootJ);
            
            rootJ = mapper.createObjectNode();
            for(String s:ancoraPresenti)
                insertInNode(rootJ, s, generalIndexes(s), "presente");
            System.out.println("--Presenti--");
            System.out.println(rootJ);
        }
        
        for(events e:happenings){
            System.out.println(e.act + " "+e.var + " "+e.obj);
            //cM.somethingChanged((new Gson()).toJson(e));
        }
        
    }
    
    private void insertInNode(ObjectNode node, String s, String complete, Object v){
        if(s.contains(".")){
            String f = s.substring(0, s.indexOf("."));
            String field = (f.contains("["))?f.substring(0, f.indexOf("[")):f;
            String index = (f.contains("["))?f.substring(f.indexOf("[")+1, f.indexOf("]")):null;
            if(node.findValue(field)!=null){
                JsonNode next = node.get(field);
                if(next.isArray()){
                    Iterator<JsonNode> nodes = ((ArrayNode)next).getElements();
                    String list = getListName(complete, s);
                    if(lists.containsKey(list)){
                        String ind = lists.get(list);
                        boolean found = false;
                        while(nodes.hasNext()){
                            ObjectNode objN = (ObjectNode)nodes.next();
                            if(objN.findValue(ind)!=null && objN.get(ind).asText().equals(index)){
                                insertInNode(objN, s.substring(s.indexOf(".")+1), complete, v);
                                found = true;
                                break;
                            }
                        }
                        if(found==false){
                            ObjectNode obj = mapper.createObjectNode();
                            obj.put(ind, index);
                            insertInNode(obj, s.substring(s.indexOf(".")+1), complete, v);
                            ((ArrayNode)next).add(obj);
                        }
                    }
                }else{
                    insertInNode((ObjectNode)next, s.substring(s.indexOf(".")+1), complete, v);
                }
            }else{
                if(index==null){
                    ObjectNode next = mapper.createObjectNode();
                    insertInNode(next, s.substring(s.indexOf(".")+1), complete, v);
                    node.put(field, next);
                }else{
                    ArrayNode array = mapper.createArrayNode();
                    String list = getListName(complete, s);
                    if(lists.containsKey(list)){
                        String ind = lists.get(list);
                        ObjectNode next = mapper.createObjectNode();
                        next.put(ind, index);
                        insertInNode(next, s.substring(s.indexOf(".")+1), complete, v);
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
    
    private String getListName(String complete, String last){
        String[] c = complete.split(Pattern.quote("."));
        String[] l = last.split(Pattern.quote("."));
        String res =new String();
        for(int i=0;i<c.length-l.length+1;i++)
            res+=c[i]+".";
        res = res.substring(0,res.lastIndexOf("[")+1)+"]";
        return res;
    }

    private String trasformInPrint(String var) {
        String[] partsWithoutIndex = var.split("["+Pattern.quote("[")+"," +Pattern.quote("]")+"]");
        String j=partsWithoutIndex[0];
        String onlyLastOne = partsWithoutIndex[0];
        String y=null;
        if(partsWithoutIndex.length>1)
            for(int i=1;i<partsWithoutIndex.length;i++){
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
            }
        String toVerify = "root."+j;
        for(String s:YangToJava.keySet())
            if(s.equals(toVerify))
                    y=YangToJava.get("root."+j);
        if(y!=null){
            String[] yparse = y.split(Pattern.quote("[]"));
            String toPub=new String();
            for(int i=0; i<partsWithoutIndex.length;i++){
                if(i%2==0)
                    toPub+= yparse[i/2];
                else
                    toPub+="["+partsWithoutIndex[i]+"]";
            }
            return toPub;
        }
        return y;
    }
     
    private JsonNode getCorrectItem(String newVal, String complete){
        //complete in Yang
        //newVal in Yang
        try{
            JsonNode node = mapper.readTree(newVal);
            JsonNode newNode;
            if(node.isObject()){
                newNode = mapper.createObjectNode();
                Iterator<String> fields = node.getFieldNames();
                while(fields.hasNext()){
                    String fieldJava = null;
                    String fieldName = (String)fields.next();
                    if(YangToJava.containsValue(complete+"."+fieldName))
                        for(String k:YangToJava.keySet())
                            if(YangToJava.get(k).equals(complete+"."+fieldName))
                                fieldJava=k;
                    if(fieldJava!=null){
                        fieldJava=fieldJava.substring(fieldJava.lastIndexOf(".")+1);
                        if(node.get(fieldName).isValueNode())
                            ((ObjectNode)newNode).put(fieldJava, node.get(fieldName));
                        else{
                            String newCampo = (node.get(fieldName).isObject())?complete+"."+fieldName:complete+"."+fieldName+"[]";
                            JsonNode subItem = getCorrectItem(mapper.writeValueAsString(node.get(fieldName)),complete+"."+fieldName);
                            ((ObjectNode)newNode).put(fieldJava, subItem);
                        }
                    }
                }
            }else{
                newNode = mapper.createArrayNode();
                Iterator<JsonNode> iter = ((ArrayNode)node).getElements();
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
    
    private void createTree(JsonNode node, String l) {
        if(l==null || l.equals(""))
            return;
        String[] splitted = l.split(Pattern.quote("."));
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
                    if(splitted.length>2 || (splitted.length>1&&splitted[1].contains("[")))
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
                ObjectNode elemMappa = mapper.createObjectNode();
                elemMappa.put("key", "");
                elemMappa.put("value", "");
                ((ArrayNode)next).add(elemMappa);
            }
        }else{
            JsonNode next;
            if(splitted[0].contains("[")){
                String inter = splitted[0].substring(0, splitted[0].indexOf("["));
                if(node.isArray()){
                    //è una lista
                    if(((ArrayNode)node).getElements().hasNext()==false)
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
                if(((ArrayNode)node).getElements().hasNext()==false)
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

    public JsonNode getComplexObj(String var) throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        String[] spl = var.split(Pattern.quote("."));
        JsonNode ref = rootJson;
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
                    System.out.println(var + " not found");
                    return null;
                }
            }else{
                if(((ArrayNode)ref).getElements().hasNext()==false){
                    System.out.println(var + " not found-array version");
                    return null;
                }
                ref = ((ArrayNode)ref).get(0);
                if(((ObjectNode)ref).get(field)==null){
                    System.out.println(var +" not found!");
                    return null;
                }
                continue;
            }
        }
        System.out.println(ref);
        if(ref.isValueNode()){
            //is a leaf, but it is not present in state: doesn't exist
            return null;
        }
        JsonNode res = (ref.isObject())?mapper.createObjectNode():mapper.createArrayNode();
        var=(res.isArray()&&var.endsWith("[]"))?var.substring(0, var.length()-2):var;
        res = fillResult(ref, var);
        System.out.println(res);
        return res;
    }

    private JsonNode fillResult(JsonNode ref, String var) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
        JsonNode toRet;
        if(ref.isObject()){
            //fill fields
            toRet = mapper.createObjectNode();
            Iterator<String> field = ((ObjectNode)ref).getFieldNames();
            if(!field.hasNext()){
                //searchCorrispondentField
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
                    ((ObjectNode)toRet).put(var, getLeafValue(jWithIndex.substring(5)).toString());
                }
                return toRet;
            }
            while(field.hasNext()){
                String fieldName = field.next();
                if(((ObjectNode)ref).get(fieldName).isValueNode()){
                    String varWithoutIndexes = new String();
                    String[] varSp = (var+"."+fieldName).split("["+Pattern.quote("[]")+"]");
                    for(int i=0; i<varSp.length;i++)
                        if(i%2==0)
                            varWithoutIndexes+=varSp[i]+"[]";
                    varWithoutIndexes = varWithoutIndexes.substring(0, varWithoutIndexes.length()-2);
                    if(YangToJava.containsValue(varWithoutIndexes)){
                        String key = null;
                        for(String k:YangToJava.keySet())
                            if(YangToJava.get(k).equals(varWithoutIndexes))
                                key = k;
                        String[] yspez = (var+"."+fieldName).split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
                        String[] jspez = key.split("["+Pattern.quote("[")+Pattern.quote("]")+"]");
                        String jWithIndex = new String();
                        for(int i=0;i<yspez.length;i++){
                            if(i%2==0)
                                jWithIndex+=jspez[i];
                            else
                                jWithIndex+="["+yspez[i]+"]";
                        }
                        Object value = getLeafValue(jWithIndex.substring(5));
                        if(value!=null)
                            ((ObjectNode)toRet).put(fieldName, value.toString());
                    }
                }else{
                    JsonNode f = fillResult(((ObjectNode)ref).get(fieldName), var+"."+fieldName);
                    if(f.size()!=0)
                        ((ObjectNode)toRet).put(fieldName, f);
                }
            }
            return toRet;
        }else{
            
            //add elements
            String listWithoutIndex = new String();
                String[] varSp = var.split("["+Pattern.quote("[]")+"]");
                for(int i=0; i<varSp.length;i++)
                    if(i%2==0)
                        listWithoutIndex+=varSp[i]+"[]";
                listWithoutIndex = listWithoutIndex.substring(0, listWithoutIndex.length()-2);
            toRet = mapper.createArrayNode();
            String listInJava = null;
            for(String l:YangToJava.keySet()){
                if(YangToJava.get(l).contains(listWithoutIndex+"[") && YangToJava.get(l).substring(0, listWithoutIndex.length()+1).equals(listWithoutIndex+"[")){
                    String rem = YangToJava.get(l).substring(listWithoutIndex.length());
                    if(!rem.contains("."))
                            listInJava = l;
                }
            }
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
            String lN = generalIndexes(jWithIndex.substring(5))+"[]";
            String e = (lists.containsKey(lN))?lists.get(lN):null;
            if(e!=null){
                String indice=e;
                Object list = getLists(root, jWithIndex.substring(5)+"[]", jWithIndex.substring(5)+"[]");
                if(list!=null && List.class.isAssignableFrom(list.getClass())){
                    List<Object> elems = new ArrayList<>();
                    elems.addAll((List)list);
                    for(Object obj:elems){
                        String idItem = searchLeafInList(obj, indice);
                        JsonNode child = fillResult(((ArrayNode)ref).get(0), var+"["+idItem+"]");
                        if(child.size()!=0)
                            ((ArrayNode)toRet).add(child);
                    }
                }else if(list!=null && Map.class.isAssignableFrom(list.getClass())){
                    Map<Object, Object> elems = new HashMap<>();
                    elems.putAll((Map)list);
                    for(Object k:elems.keySet()){
                        JsonNode child = fillResult(((ArrayNode)ref).get(0), var+"["+k+"]");
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

    private void setComplexObject(String var, String newVal) {
        try {
            JsonNode toSet = mapper.readTree(newVal);
            System.out.println(toSet);
            
            fillVariables(toSet, var);
        } catch (IOException ex) {
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        }catch(NoSuchFieldException ex){
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        }catch(IllegalAccessException ex){
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    private void fillVariables(JsonNode toSet, String var) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, IOException {
        
        if(toSet.isValueNode()){
            //set the corrispondent leaf
            String j = fromYangToJava(var);
            //if(state.containsKey(j.substring(5))){
            if(j!=null)
                setVariable(j.substring(5), j.substring(5), toSet.asText(), root);
            //}
        }else{
            if(toSet.isObject()){
                Iterator<String> fields = toSet.getFieldNames();
                while(fields.hasNext()){
                    String fieldName = (String)fields.next();
                    fillVariables(toSet.get(fieldName), var+"."+fieldName);
                }
            }else{
                //capire qual è la lista corrispondente
                //without indexes
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
                    //crearne una nuova
                    Class<?> type=null;
                    String indice = null;
                    String jgen = generalIndexes(jWithIndex);
                    if(lists.containsKey(jgen+"[]")){
                        indice = lists.get(jgen+"[]");
                        Object actual = root;
                        String[] fields = jWithIndex.split(Pattern.quote("."));
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
                        
//                        for(String s:YangToJava.keySet()){
//                            if(s.contains(key) && s.length()>key.length() && s.substring(0, key.length()+1).equals(key+"[")){
//                                indice = s.substring(key.length()+1);
//                                indice = indice.substring(0, indice.indexOf("]"));
//                                break;
//                            }
//                        }
                    }
                    setVariable(jWithIndex, jWithIndex, null, root);
                    List<Object> newList = new ArrayList<>();
                    
                        Iterator<JsonNode> iter = ((ArrayNode)toSet).getElements();
                        while(iter.hasNext()){                     
                            //insert the list element
                            JsonNode newValJava = getCorrectItem(mapper.writeValueAsString(iter.next()), varWithoutIndexes+"[]");
                            if(newValJava!=null)
                                setVariable(jWithIndex+"[]", jWithIndex+"[]",mapper.writeValueAsString(newValJava), root);
                           
                        }                  
                }
            }
        }
    }

        
    private String generalIndexes(String s){
        String[] split = s.split("["+Pattern.quote("[")+"," +Pattern.quote("]")+"]");
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
    
    
    public void parseCommand(String msgJson) throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException, IOException{
        CommandMsg msg = ((new Gson()).fromJson(msgJson, CommandMsg.class));
        String var = fromYangToJava(msg.var);
        switch(msg.act){
            case GET:
                System.out.println("devo passare "+var);
                if(var==null)
                    msg.obj=null;
                else if(!var.equals("root") && state.containsKey(var.substring(5))){
                    ObjectNode on= mapper.createObjectNode();
                    String field = (msg.var.contains("."))?msg.var.substring(msg.var.lastIndexOf(".")+1):msg.var;
                    on.put(field, getLeafValue(var.substring(5)).toString());
                   msg.objret = mapper.writeValueAsString(on);
                   System.out.println("E' una foglia "+msg.objret);
                }
                else{
            
                //creare oggetto da passare!
                msg.objret = mapper.writeValueAsString(getComplexObj(msg.var));
            
                }
                //cM.setResourceValue((new Gson().toJson(msg)));
                break;
            case CONFIG:
                try {
                    if(var!=null){
                        if(!var.equals("root")&&state.containsKey(var.substring(5)))
                            setVariable(var.substring(5), var.substring(5), (String)msg.obj, root);
                        else
                            setComplexObject(msg.var, (String)msg.obj);
                    }
                } catch (NoSuchFieldException ex) {
                    Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            case DELETE:
                //delete
                try{
                    if(var==null || var.equals("root")){
                        System.out.println("Can't delete the root obj!");
                    }else
                        deleteVariable(root, var.substring(5), var.substring(5));
                } catch (NoSuchFieldException ex) {
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
        }
                break;
        }
    }
    
    public void deleteVariable(Object actual, String var, String complete) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        String[] fs = var.split(Pattern.quote("."));
        if(fs.length==1){
            //delete
            if(var.contains("[")){
                //delete an element of the list
                String index = var.substring(var.lastIndexOf("[")+1, var.lastIndexOf("]"));
                if(index!=null && index.matches("")){
                    Field f = actual.getClass().getField(var.substring(0,var.length()-2));
                    f.set(actual, null);
                    return;
                }
                String listName = complete.substring(0, complete.length()-index.length()-1);
                listName+="]";
                listName = generalIndexes(listName)+"[]";
                String indice = null;
                if(lists.containsKey(listName))
                    indice = lists.get(listName);
                if(indice!=null){
                    actual = actual.getClass().getField(var.substring(0, var.lastIndexOf("["))).get(actual);
                    Object delete = null;
                    if(List.class.isAssignableFrom(actual.getClass())){
                    for(Object item:(List)actual){
                        if(item.getClass().getField(indice).get(item).toString().equals(index)){
                            delete = item;
                            break;
                        }
                    }
                    if(delete!=null) ((List)actual).remove(delete);
                    }else if(Map.class.isAssignableFrom(actual.getClass())){
                        if(((Map)actual).containsKey(index))
                           ((Map)actual).remove(index);
                        else{
                            for(Object k:((Map)actual).keySet())
                                if(k.toString().equals(index)){
                                    delete = k; break;}
                            if(delete!=null)
                                ((Map)actual).remove(delete);
                        }
                    }
                }
            }else{
                Field f = actual.getClass().getField(var);
                f.set(actual, null);
            }
        }else{
            //enter
            if(fs[0].contains("[")){
                String fName = fs[0].substring(0, fs[0].indexOf("["));
                String index = fs[0].substring(fs[0].indexOf("[")+1, fs[0].length()-1);
                actual = actual = actual.getClass().getField(fName).get(actual);
                String listName = complete.substring(0, complete.length()-var.length()+fName.length());
                String indice = null;
                listName = generalIndexes(listName);
                if(lists.containsKey(listName+"[]"))
                    indice = lists.get(listName+"[]");
                if(actual!=null){
                    if(List.class.isAssignableFrom(actual.getClass())){
                        for(Object item:(List)actual)
                            if(item.getClass().getField(indice).get(item).toString().equals(index))
                                deleteVariable(item, var.substring(fs[0].length()+1), complete);
                    }else if(Map.class.isAssignableFrom(actual.getClass())){
                        for(Object k:((Map)actual).keySet())
                            if(k.toString().equals(index))
                                deleteVariable(((Map)actual).get(k), var.substring(fs[0].length()+1), complete);
                
                    }
                }
            }else{
                actual = actual.getClass().getField(fs[0]).get(actual);
                if(actual!=null)
                    deleteVariable(actual, var.substring(fs[0].length()+1), complete);
            }
        }
    }
    
    public boolean setVariable(String var, String complete, String newVal, Object actual) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        String[] fs = var.split(Pattern.quote("."));
        if(fs.length==1){
            //to set
            if(var.contains("[")){
                String index = var.substring(var.indexOf("[")+1, var.indexOf("]"));
                Field f = actual.getClass().getField(var.substring(0, var.lastIndexOf("[")));
                ParameterizedType pt = (ParameterizedType)f.getGenericType();
                Class<?> itemType = (Class<?>)pt.getActualTypeArguments()[0];
                if(List.class.isAssignableFrom(f.getType())){
                    if(f.get(actual)==null){
                        try {
                            List<Object> l = (f.getType().isInterface())?new ArrayList<>():(List)f.getType().newInstance();
                            l.add((new Gson()).fromJson(newVal, itemType));
                            f.set(actual, l);
                        } catch (InstantiationException ex) {
                            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }else if(index.matches("")){
                        ((List)f.get(actual)).add((new Gson()).fromJson(newVal, itemType));
                    }else{
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
                                l.add((new Gson()).fromJson(newVal, itemType));
                                l.remove(toChange);
                            }
                        }
                    }
                }else if(Map.class.isAssignableFrom(f.getType())){
                    if(f.get(actual)==null){
                        try {
                            Map<Object, Object> m = (f.getType().isInterface())?new HashMap<>():(Map)f.getType().newInstance();
                            
                            //!!
                            //m.put((new Gson()).fromJson(newVal, Map.Entry<Object,itemType>));
                            f.set(actual, m);
                        } catch (InstantiationException ex) {
                            Logger.getLogger(StateListenerNew.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }else if(index.matches("")){
                        //((Map)f.get(actual)).put((new Gson()).fromJson(newVal, itemType));
                    }else{
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
                                    l.add((new Gson()).fromJson(newVal, itemType));
                                    l.remove(toChange);
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
                Field f = actual.getClass().getField(var);
                    f.set(actual, (new Gson()).fromJson(newVal, f.getGenericType()));
            }
        }else{
            if(fs[0].contains("[")){
                //select element in the list
                String listName = complete.substring(0, complete.length()-var.length()+fs[0].length());
                String idItem = listName.substring(listName.lastIndexOf("[")+1, listName.lastIndexOf("]"));
                listName = listName.substring(0, listName.length()-idItem.length()-2)+"[]";
                listName = generalIndexes(listName)+"[]";
                String indice = null;
                if(lists.containsKey(listName))
                    indice = lists.get(listName);
                actual = actual.getClass().getField(fs[0].substring(0, fs[0].length()-idItem.length()-2)).get(actual);
                if(List.class.isAssignableFrom(actual.getClass())){
                for(Object litem:(List)actual){
                    boolean correct = litem.getClass().getField(indice).get(litem).toString().equals(idItem);
                    if(correct)
                        setVariable(var.substring(fs[0].length()+1), complete, newVal, litem);
                }
                }else if(Map.class.isAssignableFrom(actual.getClass())){
                for(Object litem:((Map)actual).keySet()){
                    boolean correct = litem.toString().equals(idItem);
                    if(correct)
                        setVariable(var.substring(fs[0].length()+1), complete, newVal, ((Map)actual).get(litem));
                }
                }
            }else{
                Field f = actual.getClass().getField(fs[0]);
                actual = f.get(actual);
                setVariable(var.substring(fs[0].length()+1), complete, newVal, actual);
            }
        }
        return false;
    }
    
    public void stopSL(){
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
    
    //get the value of a specific leaf
    public Object getLeafValue(String id){
        if(state.containsKey(id))
            return state.get(id);
        return null;
    }
    
    public String fromYangToJava(String y){
        String[] separated = y.split("["+Pattern.quote("[")+"," +Pattern.quote("]")+"]");
        String yang = new String();
        for(int i=0; i<separated.length;i++)
            if(i%2==0 && i!=separated.length-1)
                yang+=separated[i]+"[]";
        if(separated.length%2==1)
            yang+=separated[separated.length-1];
        String j =null;
        if(YangToJava.containsValue(yang))
            for(String s:YangToJava.keySet())
                if(YangToJava.get(s).equals(yang))
                    j=s;
        if(j==null)
            return j;
        String[] java = j.split("["+Pattern.quote("[")+"," +Pattern.quote("]")+"]");
        j=new String();
        for(int i=0; i<java.length; i++){
            if(i%2==0)
                j+=java[i];
            else
                j+="["+separated[i]+"]";
        }
        if(y.endsWith("[]"))
            j+="[]";
        return j;
    }
    
        public Object getLists(Object actual, String remaining, String complete) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        String[] fs = remaining.split(Pattern.quote("."));
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

 
     public enum action{ADDED, UPDATED, REMOVED, NOCHANGES};
    public class events{
        public action act;
        public Object obj;
        public String var;
    }
    public class CommandMsg {
        Long id;
        command act;
        String var;
        String objret;
        Object obj;
    }
    public enum command{GET, CONFIG, DELETE};

      
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
//                List<events> wH = checkListChanges(lv, stateList.get(lv).List, act);  
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
//                    events e = new events();
//                    e.act=action.UPDATED;
//                    e.obj=state.get(s);
//                    e.var=s;
//                    writeLock.lock();
//                    whatHappened.add(e);
//                    writeLock.unlock();
//                }
//            }
//            List<events> wH = new ArrayList<>();
//            writeLock.lock();
//            wH.addAll(whatHappened);
//            whatHappened = new ArrayList<>();
//            writeLock.unlock();
//            if(wH!=null){
//                for(events e:wH){
//                    String toPrint = trasformInPrint(e.var);
//                    System.out.println(e.act + " " + toPrint+" " + gson.toJson(e.obj));
//                }
//            }
//            
//            System.out.println("state aggiornato: " + state);
//            System.out.println("root "+(new Gson()).toJson(root));
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
//        String[] fields = remaining.split(Pattern.quote("."));
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
//                    System.out.println("Rimossa lista");
//                    return true;
//                }
//            }
//            Object item = getListItemWithIndex((List)actual, indice, listName);
//            if(item==null){
//                System.out.println("No items in list");
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
//                        System.out.println("Removed obj " + finteresting);
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
//    public List<events> checkListChanges(String listName, List oldList, List newList) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
//        List<events> res = new ArrayList<>();
//        if(!stateList.containsKey(listName)){
//            //la lista è tutta nuova
//            String id = null; //prendere dal toListen
//            for(Object n:newList){
//                events e = new events();
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
//                events e = new events();
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
//                events e = new events();
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
//                events e = new events();
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
//        String[] fs = fields.split(Pattern.quote("."));
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
//                            events e = new events();
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
//                events e = new events();
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
//        //System.out.println("in saveListstate lists: " + stateList);
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
//            //System.out.println("Dopo lista nulla in saveListstate "+stateList);
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
//        //System.out.println("Alla fine ho aggiunto qualcosa "+stateList);
//        return true;
//    }

}
