/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cb;

import static ca.mcmaster.spccav1_3.Constants.LOG_FILE_EXTENSION;
import static ca.mcmaster.spccav1_3.Constants.LOG_FOLDER;
import ca.mcmaster.spccav1_3.cca.CCANode;
import ca.mcmaster.spccav1_3.cplex.ActiveSubtree;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class CBInstructionTree {
    
    
    private static Logger logger=Logger.getLogger(CBInstructionTree.class);
        
    public CCANode ccaRoot;
    public CBInstructionTree leftSubTree=null, rightSubtree=null;
    
    //here is the list of leafs to prune, if you migrate this CB tree
    public List<String> pruneList = new ArrayList<String> ();
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+CBInstructionTree.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
          
    }
    
    public CBInstructionTree (CCANode root){
        ccaRoot=root;
    }
    
    
    public void print (){
        print (this);
                
        //finally print leafs that should be pruned if the CB is used
        logger.debug( "The prune list for the CB tree is:");
        for (String nodeid: pruneList ){
            logger.debug(nodeid+",");
        }
    }
    
    private void print ( CBInstructionTree tree){
        logger.debug(tree.ccaRoot);
        logger.debug( "\n");
        if (tree.leftSubTree!=null) print(tree.leftSubTree);
        if (tree.rightSubtree!=null)print(tree.rightSubtree);
    }
}
