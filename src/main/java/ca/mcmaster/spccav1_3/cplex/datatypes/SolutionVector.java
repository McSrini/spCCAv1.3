/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cplex.datatypes;
 
import java.io.Serializable;
import java.util.*;

/**
 *
 * @author tamvadss
 */
public class SolutionVector implements Serializable {
    
    List<String> variableNames = new ArrayList<String>();
    List<Double> values = new ArrayList<Double>();
    
    public void add (String name, double value) {
        variableNames.add(name);
        values.add(value);
    }
    
}
