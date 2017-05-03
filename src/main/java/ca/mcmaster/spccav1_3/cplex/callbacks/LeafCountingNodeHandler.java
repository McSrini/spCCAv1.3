/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cplex.callbacks;

import static ca.mcmaster.spccav1_3.Constants.*;
import ca.mcmaster.spccav1_3.cplex.datatypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
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
public class LeafCountingNodeHandler extends IloCplex.NodeCallback {
        
    private static Logger logger=Logger.getLogger(LeafCountingNodeHandler.class);
        
    public long numLeafs = ZERO;
    public long numLeafsWithGoodLP = ZERO;
    public double lpThreshold = ZERO;
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+LeafCountingNodeHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");     
            exit(1);
        }
          
    }
    
    public LeafCountingNodeHandler (double threshold ) {
        this.lpThreshold=threshold;
    }
 
    protected void main() throws IloException {
        this. numLeafs = ZERO;
        numLeafsWithGoodLP=ZERO;
        if(getNremainingNodes64()> ZERO){
                        
            this. numLeafs = getNremainingNodes64();
            
            for (long index = ZERO; index < numLeafs; index++){
                if (IS_MAXIMIZATION && getObjValue(index)>=lpThreshold) numLeafsWithGoodLP++;
                if (!IS_MAXIMIZATION&& getObjValue(index)<=lpThreshold) numLeafsWithGoodLP++;
            }
            
            abort();
        }
    }
    
 
}
