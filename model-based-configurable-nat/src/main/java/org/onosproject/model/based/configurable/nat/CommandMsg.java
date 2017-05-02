/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.onosproject.model.based.configurable.nat;

/**
 *
 * @author lara
 */
class CommandMsg {
    Long id;
    command act;
    String var;
    String objret;
    Object obj;
    public enum command{GET, CONFIG, DELETE};
}
