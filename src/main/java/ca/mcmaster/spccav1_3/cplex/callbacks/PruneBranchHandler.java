/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cplex.callbacks;

import ca.mcmaster.spccav1_3.utilities.BranchHandlerUtilities;
import static ca.mcmaster.spccav1_3.Constants.*;
import ca.mcmaster.spccav1_3.cplex.datatypes.BranchingInstruction;
import ca.mcmaster.spccav1_3.cplex.datatypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class PruneBranchHandler extends IloCplex.BranchCallback {
    
    private static Logger logger=Logger.getLogger(PruneBranchHandler.class);
         
    //list of nodes to be pruned
    public List<String> pruneList = new ArrayList<String>();
    
    public double bestReamining_LPValue = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+PruneBranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
          
    }
    
    public PruneBranchHandler (List<String> pruneList){
        this.  pruneList=  pruneList;
    }
 
    protected void main() throws IloException {
        if (pruneList!=null){
            if (  pruneList.contains( getNodeId().toString()) ) {
                pruneList.remove(  getNodeId().toString() );
                //logger.debug(getNodeId().toString() + " reamaining size "+pruneList.size()) ;
                prune();
            } 
        }
        
    }//end main
    
 
}
