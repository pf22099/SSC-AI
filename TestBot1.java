import java.util.Stack;

import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class TestBot1 extends DefaultBWListener {

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;
    
    /**
     * @author Xiangmin
     * @since 02/16/2015
     * 
     * Added some variables for the exploration.
     */
    private Unit explorer;
    private Unit myBase;
    
    private Stack<BaseLocation> baseStack;
    private BaseLocation currentExploreBase;
    
    private Position enermyBasePosition;
    /**
     * Add ends.
     */

    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onUnitCreate(Unit unit) {
        System.out.println("New unit " + unit.getType());
    }

    @Override
    public void onStart() {
        game = mirror.getGame();
        self = game.self();
        game.enableFlag(1);
        /**
         * @author Xiangmin Liang
         * @since 02/16/2015
         * initialize the stack.
         */
        baseStack = new Stack<BaseLocation>();

        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        System.out.println("Map data ready");
        
        
        for (BaseLocation b : BWTA.getStartLocations()) {
        	baseStack.push(b);
        }
        currentExploreBase = baseStack.pop();
    }

    @Override
    public void onFrame() {
        game.setTextSize(10);
        game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());

        StringBuilder units = new StringBuilder("My units:\n");
        
        explore();
        
        //iterate through my units
        for (Unit myUnit : self.getUnits()) {
            units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");
            
//            if (myUnit.getType().isWorker() && myUnit.isIdle() && this.explorer == null) {
//            	explorer = myUnit;
//            	this.explorer.attack(currentExploreBase.getPosition());
//            	continue;
//            }
            
          //if there's enough minerals, train an SCV
            if (myUnit.getType() == UnitType.Terran_Command_Center && self.minerals() >= 50) {
            	//myBase = myUnit;
                myUnit.train(UnitType.Terran_SCV);
            }

            //if it's a drone and it's idle, send it to the closest mineral patch
            if (myUnit.getType().isWorker() && myUnit.isIdle()) {
                Unit closestMineral = null;

                //find the closest mineral
                for (Unit neutralUnit : game.neutral().getUnits()) {
                    if (neutralUnit.getType().isMineralField()) {
                        if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) {
                            closestMineral = neutralUnit;
                        }
                    }
                }

                //if a mineral patch was found, send the drone to gather it
                if (closestMineral != null) {
                    myUnit.gather(closestMineral, false);
                }
            }
        }
        
        

        //draw my units on screen
        game.drawTextScreen(10, 25, units.toString());
    }
    
    /**
     * @author Xiangmin Liang
     * @since 02/16/2015
     * 
     * In this explore function, a SCV named explorer will explore all positions that might be available for building
     * bases. When the explorer's destination is my own base, it will automatically move to the next position.
     * 
     * There are still a few problems in this function. For example, at some special position, the explorer will get stuck.
     * I am trying to solving this and keep this function updated.
     */
    public void explore() {
    	
    	//set a SCV to be the explorer for the first time getting into this method.
    	if(null == explorer) {
    		for (Unit myUnit : self.getUnits()) {
    			if (myUnit.getType().isWorker() && myUnit.isIdle() && this.explorer == null) {
                	explorer = myUnit;
                	this.explorer.attack(currentExploreBase.getPosition());
                	continue;
                }
    			if (myUnit.getType() == UnitType.Terran_Command_Center) {
    				myBase = myUnit;
    			}
    		}
    	}
    	
    	else {
    		int dis = explorer.getDistance(currentExploreBase.getPosition());
            //System.out.println(currentExploreBase.getPosition() + ": " +dis);
            
            //Save current position, if the explorer get attacked.
    		if(explorer.isUnderAttack()) {
    			enermyBasePosition = explorer.getPosition();
    			//System.out.println("!!!!!!!"+enermyBasePosition.toString());
    		}
    		
    		for(Unit allUnity: game.getAllUnits()) {
    			if(allUnity.getType() == UnitType.Zerg_Hatchery || allUnity.getType() == UnitType.Protoss_Nexus || 
    					(allUnity.getType() == UnitType.Terran_Command_Center) && allUnity.getDistance(myBase.getPosition()) > 60) {
    				int x = new Integer(explorer.getPosition().getX());
    				int y = new Integer(explorer.getPosition().getY());
    				if(null == enermyBasePosition) {
    					enermyBasePosition = new Position(x,y);
    				}
    				//System.out.println("!!!!!!!"+enermyBasePosition.toString());
    			}
    		}
    		
    		//If the destination is my own base, then ignore it and move to the next.
    		 if(myBase.getDistance(currentExploreBase.getPosition()) < 10) {
				 currentExploreBase = baseStack.pop();
	             explorer.move(currentExploreBase.getPosition());
	             return;
			 }
    		
    		 /**
    		  * If the explorer is really near to a possible-base position and did not find anything,
    			he will move to the next position.
    		  */
        	if(!baseStack.isEmpty() && explorer.getDistance(currentExploreBase.getPosition()) <= 60) {
            	currentExploreBase = baseStack.pop();
            	this.explorer.attack(currentExploreBase.getPosition());
            }
            
        	//As long as the explorer is farer than 5 to the destination, then keep moving forward.
            if(!baseStack.isEmpty() && explorer.getDistance(currentExploreBase.getPosition()) > 60) {
            	this.explorer.move(currentExploreBase.getPosition());
            }
        }
    	
    	if(enermyBasePosition != null) {
    		System.out.println(enermyBasePosition);
    		game.drawCircleMap(enermyBasePosition.getX(), enermyBasePosition.getY(), 100, new Color(255,0,0));
    	}
    }

    public static void main(String[] args) {
        new TestBot1().run();
    }
}