package org.sa.rainbow.brass.plan.p2_cp1;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sa.rainbow.brass.PropertiesConnector;
import org.sa.rainbow.brass.adaptation.PolicyToIG;
import org.sa.rainbow.brass.adaptation.PrismPolicy;
import org.sa.rainbow.brass.confsynthesis.SimpleConfigurationStore;
import org.sa.rainbow.brass.model.map.EnvMap;
import org.sa.rainbow.brass.plan.p2.DecisionEngine;

public class DecisionEngineCP1 extends DecisionEngine{

    public static double m_selected_candidate_time=0.0;
    public static double m_selected_candidate_energy=0.0;
	public static double m_energyWeight=0.5;
	public static double m_timelinessWeight=0.5;

    
   /**
    * Returns the maximum estimated time for a candidate policy in the scoreboard
    * @return
    */
   public static Double getMaxTime(){
    	return getMaxItem(0);
    }

   /**
    * Returns the maximum estimated remaining energy for a candidate policy in the scoreboard
    * @return
    */
    public static Double getMaxEnergy(){
    	return getMaxItem(1);
    }
    
    /**
     * Selects the policy with the best score (CP1)
     * @return String filename of the selected policy
     */
    public static String selectPolicy(){
       	Double maxTime = getMaxTime();
    	Double maxEnergy = getMaxEnergy();
    	Double maxScore=0.0;
    	
        Map.Entry<List, ArrayList<Double>> maxEntry = m_scoreboard.entrySet().iterator().next();
        for (Map.Entry<List, ArrayList<Double>> entry : m_scoreboard.entrySet())
        {
            Double entryTime = entry.getValue().get(0);
            Double entryTimeliness = 0.0;
            if (maxTime>0.0){
            	entryTimeliness = 1.0-(entryTime / maxTime);
            }
            
            Double entryEnergy = entry.getValue().get(1)/maxEnergy;
            
            
            Double entryScore = m_energyWeight * entryEnergy + m_timelinessWeight * entryTimeliness;
            
        	if ( entryScore > maxScore)
            {
                maxEntry = entry;
                maxScore = entryScore;
            }
        }
        m_selected_candidate_time = maxEntry.getValue().get(0);
        m_selected_candidate_energy = maxEntry.getValue().get(1);
        m_selected_candidate_score = maxScore;
        
        System.out.println("Selected candidate policy: "+m_candidates.get(maxEntry.getKey()));
        System.out.println("Score: "+String.valueOf(m_selected_candidate_score)+" Energy: "+String.valueOf(m_selected_candidate_energy)+" Time: "+String.valueOf(m_selected_candidate_time));
        
        return m_candidates.get(maxEntry.getKey())+".adv";
    }

    public static double getSelectedPolicyTime(){
        return m_selected_candidate_time;
    }
    
    /**
     * Class test
     * @param args
     */
    public static void main (String[] args) throws Exception {
        init (null);

        List<Point2D> coordinates = new ArrayList<Point2D>();
        PrismPolicy pp=null;

        EnvMap dummyMap = new EnvMap (null, null);
        System.out.println("Loading Map: "+PropertiesConnector.DEFAULT.getProperty(PropertiesConnector.MAP_PROPKEY));
        dummyMap.loadFromFile(PropertiesConnector.DEFAULT.getProperty(PropertiesConnector.MAP_PROPKEY));
        System.out.println("Setting map...");
        setMap(dummyMap);
        
        SimpleConfigurationStore cs = new SimpleConfigurationStore();
        System.out.println("Populating configuration list..");
        cs.populate();
        System.out.println("Setting configuration provider...");
        setConfigurationProvider(cs);
        
        
        for (int i=32000; i< 32500; i+=500){
        	System.out.println("Generating candidates for l1-l4...");
            generateCandidates("l1", "l4");
        	System.out.println("Scoring candidates...");
            scoreCandidates(dummyMap, String.valueOf(i), "1");
            System.out.println(String.valueOf(m_scoreboard));	        
            pp = new PrismPolicy(selectPolicy());
            pp.readPolicy();  
            String plan = pp.getPlan().toString();
            System.out.println(plan);
            PolicyToIG translator = new PolicyToIG(pp, dummyMap);
            System.out.println (translator.translate (20394, false));
            coordinates.add(new Point2D.Double(i, getSelectedPolicyTime()));
        }

        for (int j=0; j< coordinates.size(); j++){
            System.out.println(" ("+String.valueOf(coordinates.get(j).getX())+", "+String.valueOf(coordinates.get(j).getY())+") ");
        }

    }
    
}
