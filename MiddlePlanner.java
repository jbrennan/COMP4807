import java.util.*;
import java.lang.Math;

public class MiddlePlanner extends Planner {
	
	// byte indices
	final int ROTATION = 0;
	final int MOVE = 1;
	final int CONTROL = 2;
	
	// Movement instructions
	final byte MOVE_NONE = 0;
	final byte MOVE_SMALL = 1;
	
	// Rotation instructions
	final byte ROTATE_NONE = 0;
	final byte ROTATE_SMALL = 1;
	final byte ROTATE_BACK_SMALL = 2;
	
	
	// Command instructions
	final byte STAY_STILL = 0;
	final byte ASK_AGAIN = 1;
	final byte GO = 2;
	final byte SEEK_BLOCK = 3;
	final byte DROP_BLOCK = 4;
	final byte ALL_DONE = 100;
	
	
	// From the robot
	final int STATUS = 0;
	
	// Status codes
	final int STATUS_COMMAND_REQUEST = 0;
	final int STATUS_BLOCK_FOUND = 1;

	
	final int TOTAL_BLOCKS = 8;
	
	
	
	public enum RobotState {
		RobotStateInvalid,
		RobotStateStart,
		RobotStateSeekTop,
		RobotStatePickTop,
		RobotStateDropBottom,
		RobotStateDoActualDropBottom, 
		RobotStateSeekBottom,
		RobotStatePickBottom,
		RobotStateDropTop,
		RobotStateDoActualDropTop,
		RobotStateFinishedCycle,
		RobotStatePark,
		RobotStateEnd
	}
	

	boolean firstPose;

	ArrayList<Pose> poses;
	ArrayList<Goal> goals;
	
	
	// The goal areas for where I pick up the cylanders from other bots
	Goal topPickupZone = new Goal(new Point(0, 0));
	Goal bottomPickupZone = new Goal(new Point(480, 240));
	
	Goal topDropZone = new Goal(new Point(0, 240));
	Goal bottomDropZone = new Goal(new Point(480, 0));
	
	
	RobotState currentRobotState;
	
	int _numberOfBottomBlocksReady;
	int _numberOfTopBlocksReady;
	
	int _numberOfDeliveredBlocks;
	
	
	//Goal currentGoal;
	//int currentGoalNumber;
	
	ArrayList<Goal> topPickupGoals;
	ArrayList<Goal> topDropoffGoals;
	ArrayList<Goal> bottomPickupGoals;
	ArrayList<Goal> bottomDropoffGoals;
	
	
	// Flags
	boolean _allDone;
	boolean _haveSentRobotCommand;
	boolean _latestCommandAcknowledged;
	boolean _robotFoundBlock;
	
	boolean _bottomZoneUnlocked, _topZoneUnlocked;
	
	// Constructor for the planner
	public MiddlePlanner() {
		
		firstPose = true;
		poses = new ArrayList<Pose>();
		goals = new ArrayList<Goal>();

		Pose[] poses = getUserDefinedPath();
		for (Pose p : poses) {
			goals.add(new Goal(new Point(p.x, p.y)));
			System.out.println("Added a new goal at: " + p.x + ", " + p.y);
		}
		
		if (goals.size() > 0) {
			//currentGoal = goals.get(0);
			//currentGoalNumber = 0;
		} else {
			//currentGoal = topPickupZone;
			//currentGoalNumber = 0;
		}
		
		this.currentRobotState = RobotState.RobotStateInvalid;
		setRobotState(RobotState.RobotStateStart);
		
		topPickupGoals = new ArrayList<Goal>();
		topDropoffGoals = new ArrayList<Goal>();
		bottomPickupGoals = new ArrayList<Goal>();
		bottomDropoffGoals = new ArrayList<Goal>();
		
		
		// Fill out the drop goals.. these are the destinations for drop zones
		// Offset these goals for where the robot should actually stop?
		for (int i = 0; i < 4; i++) {
			int x = 550;
			int y = 665 + (i * 40);
			bottomDropoffGoals.add(new Goal(new Point(x, y)));
		}
		
		
		for (int i = 0; i < 4; i++) {
			int x = 60;
			int y = 850 - (i * 40);
			topDropoffGoals.add(new Goal(new Point(x, y)));
		}
		
		
		
		// Hack just to get the robot to switch modes on its own for testing
		final MiddlePlanner that = this;
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(3000);
					System.out.println("Thread done sleeping. Going to trick the robot into starting!");
					
					String d;
					
					d = StationMessage.FormatToMessage(MessageType.STATION_1_RED_BLOCK_DROPPED_OFF, 60, 556);
					that.receivedDataFromStation(1, d);
					
					d = StationMessage.FormatToMessage(MessageType.STATION_1_RED_BLOCK_DROPPED_OFF, 60, 577);
					that.receivedDataFromStation(1, d);
					
					d = StationMessage.FormatToMessage(MessageType.STATION_1_RED_BLOCK_DROPPED_OFF, 60, 600);
					that.receivedDataFromStation(1, d);
					
					d = StationMessage.FormatToMessage(MessageType.STATION_1_RED_BLOCK_DROPPED_OFF, 60, 620);
					that.receivedDataFromStation(1, d);
					
					
					d = StationMessage.FormatToMessage(MessageType.STATION_3_BLUE_BLOCK_DROPPED_OFF, 568, 929);
					that.receivedDataFromStation(2, d);
					
					d = StationMessage.FormatToMessage(MessageType.STATION_3_BLUE_BLOCK_DROPPED_OFF, 568, 912);
					that.receivedDataFromStation(2, d);
					
					d = StationMessage.FormatToMessage(MessageType.STATION_3_BLUE_BLOCK_DROPPED_OFF, 568, 894);
					that.receivedDataFromStation(2, d);
					
					d = StationMessage.FormatToMessage(MessageType.STATION_3_BLUE_BLOCK_DROPPED_OFF, 568, 875);
					that.receivedDataFromStation(2, d);
					
					
					
					//_numberOfBottomBlocksReady = TOTAL_BLOCKS;
					_topZoneUnlocked = true;
					_bottomZoneUnlocked = true;
					//setRobotState(RobotState.RobotStatePickTop);
					
					
					// add some blocks
					//topPickupGoals.add(new Goal(new Point(60, 542)));
					
					//bottomPickupGoals.add(new Goal(new Point(560, 916)));
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					System.out.println("There was an error in the thread!!");
					e.printStackTrace();
				}
			}
		}).start();
		System.out.println("Thread started!!");
	}
	
	
	
	void setRobotState(RobotState newState) {
		if (newState == currentRobotState)
			return;
		System.out.println("Transitioning from " + this.currentRobotState.toString() + " to " + newState.toString());
		this.currentRobotState = newState;
		
		// Reset some flags.
		_haveSentRobotCommand = false;
		_latestCommandAcknowledged = false;
		_robotFoundBlock = false;
		
	}
	
	
	void sendInstructionsToRobot(byte movement, byte rotation, byte command) {
		byte[] outData = new byte[6]; // the data buffer to send to the robot.
		
		outData[ROTATION] = rotation;
		outData[MOVE] = movement;
		outData[CONTROL] = command;
		
		sendDataToRobot(outData);
	}
	
	
	void orientToGoalForPose(Pose pose, Goal goal, RobotState nextState) {
		int robotCurrentAngle = pose.angle; // degrees
		Point robotPoint = new Point(pose.x, pose.y);
		int angleToTheGoal = (int)getAngle(goal.location, robotPoint);

		
		int RANGE = 10;
		byte rotation = ROTATE_NONE;
		byte movement = MOVE_NONE;
		 
		int angleDifference = angleToTheGoal - robotCurrentAngle;
		System.out.println("Angle diff: " + angleDifference);
		if (Math.abs(angleDifference) < RANGE) {
			
			
			// We're good!!
			// Change state and reset some internal flags.
			setRobotState(nextState);
			
			// Just so we have some instructions to reply with
			sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
			return;
			
		} else if (angleDifference > 0) {
			
			// Still need to rotate;
			
			rotation = angleDifference < 180? ROTATE_BACK_SMALL : ROTATE_SMALL;
			
		} else {
			rotation = angleDifference < -180? ROTATE_BACK_SMALL : ROTATE_SMALL; // might need to do like above...
		}
		
		sendInstructionsToRobot(movement, rotation, GO);

	}

	
	
	void goalNavigateAlongCurrentPathForRobotPoint(Pose pose, RobotState nextState, boolean forceNextState) {
		// This is basically Goal-navigation now.
		
		// First see if he's close enough to the goal, in which case
		// transition to the next State.
		Point robotPoint = new Point(pose.x, pose.y);
		Goal currentGoal = goals.get(0); // TODO: Change this if we're dealing with multiple goals!!!
		
		if (currentGoal.isPointCloseEnoughToGoal(robotPoint)) {
			// Really, we have to make sure we do this for the WHOLE PATH OF GOALS
			// Not just 1 goal.
			// We're close enough to change states
			System.out.println(this.currentRobotState.toString() + ": Close enough to goal, going to orient.");
			
			
			// Make sure we're properly oriented to the goal so we can just move forward when seeking a block. Then transition in this method.
			if (forceNextState) {
				// We're good!!
				// Change state and reset some internal flags.
				setRobotState(nextState);
				
				// Just so we have some instructions to reply with
				sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
			} else {
				orientToGoalForPose(pose, currentGoal, nextState);
			}
			
		} else {
			// We need to tell the robot to (keep) moving towards the Drop Zone.
			int robotCurrentAngle = pose.angle; // degrees
			int angleToTheGoal = (int)getAngle(currentGoal.location, robotPoint);
			int distanceToGoal = distance(currentGoal.location.x - robotPoint.x,
											currentGoal.location.y -robotPoint.y);
			System.out.println("Distance to goal is: " + distanceToGoal);
			
			int RANGE = 5;
			byte rotation = ROTATE_NONE;
			byte movement = MOVE_NONE;
			 
			int angleDifference = angleToTheGoal - robotCurrentAngle;
			
			if (Math.abs(angleDifference) < RANGE) {
				rotation = ROTATE_NONE;
				movement = MOVE_SMALL;
			} else if (angleDifference > 0) {
				rotation = angleDifference < 180? ROTATE_BACK_SMALL : ROTATE_SMALL;
				movement = MOVE_NONE;
			} else {
				System.out.println("Less than 0..." + angleDifference);
				rotation = ROTATE_SMALL; // might need to do like above...
				movement = MOVE_NONE;
			}
			
			
			// Now send this command to the robot
			sendInstructionsToRobot(movement, rotation, GO);
			
		}
	}
	
	
	
	boolean receivedCommandAck = true;
	public void receivedPoseFromTracker(Pose p) {
		
		
		// Just keep updating to the latest pose... I suppose we could just keep a reference to the latest one. Whatever.
		if (p.x < 0 || p.y < 0) return; // skip invalid poses
		poses.add(p);
		
		
	}
	
	
	void announceCompletionToOtherStations() {
		
		String data1 = StationMessage.FormatToMessage(MessageType.ZONE_2_1_UNLOCKED);
		sendDataToStation(1, data1);
		sendDataToStation(3, data1);
		
		data1 = StationMessage.FormatToMessage(MessageType.ZONE_2_2_UNLOCKED);
		sendDataToStation(1, data1);
		sendDataToStation(3, data1);
		
		
		String data = StationMessage.FormatToMessage(MessageType.STATION_2_DONE);
		sendDataToStation(1, data);
		sendDataToStation(3, data);
	}
	
	
	void announceDropoffAtPointToStation(Point point, int stationID) {
		
		MessageType messageType;
		if (stationID == 1) {
			messageType = MessageType.STATION_2_RED_BLOCK_DROPPED_OFF;
		} else {
			
			messageType = MessageType.STATION_2_BLUE_BLOCK_DROPPED_OFF;
			
		}
		
		sendDataToStation(stationID, StationMessage.FormatToMessage(messageType, point.x, point.y));
	}
	
	
	private int[] processedRobotData(int[] data) {
		int length = data.length;
		if (length % 2 != 0) length += 1; // make sure it's an even number
		
		int[] processed = new int[length/2];
		int currentProcessedIndex = 0;
		for (int currentDataIndex = 0; currentDataIndex < data.length; currentDataIndex += 2) {
			processed[currentProcessedIndex++] = data[currentDataIndex]*256 + data[currentDataIndex+1];
		}
		
		return processed;
	}
	
	
	private void computePathFromRobotPoseToEndGoal(Pose currentRobotPose, RobotState state) {
		
		// Based on the current state and the current location, compute a path the robot needs to take to reach
		// the goal (the goal depends on the current state!).
		
		goals = new ArrayList<Goal>();
		switch (state) {
			case RobotStateDropBottom: {
				
				
				
				// Move in almost a straight line? Or just a straight line...
				goals.add(bottomDropoffGoals.get(0));
				
				// Must remember to remove this goal from the bottomDropoffGoals list... do it now?
				bottomDropoffGoals.remove(0);
				
				break;
			}
			
			
			case RobotStateSeekBottom: {
				
				
				// Pickup the last item in this list
				goals.add(bottomPickupGoals.get(bottomPickupGoals.size() - 1));
				
				bottomPickupGoals.remove(bottomPickupGoals.size() - 1);
				
				break;
			}
			
			
			case RobotStateDropTop: {
				
				goals.add(topDropoffGoals.get(0));
				topDropoffGoals.remove(0);
				
				break;
			}
			
			
			case RobotStateSeekTop: {
				
				goals.add(topPickupGoals.get(topPickupGoals.size() - 1));
				System.out.println("top goal is: " + goals.get(0).toString());
				topPickupGoals.remove(topPickupGoals.size() - 1);
				
				break;
			}
			
			
			case RobotStatePark: {
				goals.add(new Goal(new Point(160, 750)));
				
				break;
			}
			
			
			default: {
				System.out.println("Unhandled plan computation!!!!! " + state.toString());
				System.exit(-1); // force a crash
				break;
			}
			

			
		}
		
		System.out.println("New goal path looks like:");
		for (Goal g : goals) {
			System.out.println(g.toString());
		}
	}
	
	
	// Called when the planner receives data from the robot
	public void receivedDataFromRobot(int[] data) {
		
		Pose latestPose = poses.get(poses.size() - 1);
		int[] commandData = processedRobotData(data); // basically just intifies the data from the robot.
		
		
		switch (this.currentRobotState) {
			case RobotStateStart: {
				if ((_numberOfTopBlocksReady + _numberOfBottomBlocksReady) == TOTAL_BLOCKS && _topZoneUnlocked && _bottomZoneUnlocked) {
					// transition to the next state
					
					
					setRobotState(RobotState.RobotStateSeekTop);
					String data1 = StationMessage.FormatToMessage(MessageType.ZONE_2_1_LOCKED);
					sendDataToStation(1, data1);
					sendDataToStation(3, data1);
					data1 = StationMessage.FormatToMessage(MessageType.ZONE_2_2_LOCKED);
					sendDataToStation(1, data1);
					sendDataToStation(3, data1);
					computePathFromRobotPoseToEndGoal(latestPose, RobotState.RobotStateSeekTop);
				} else {
					// Stay in the same state... don't tell the robot?
					//this.currentRobotState = RobotStateStart;
					
					
					
					/* The robot will then stay still for a certain timeout and then ask again
						At which point we will try again to determine his current instruction
					*/
					
				}
				
				// Always stay still. The robot will send a call when he's done
				// At which point, if we've transitioned, then we'll tell him his new instructions
				sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, STAY_STILL);
				break;
			}
			
			
			case RobotStateSeekTop: {
				// The robot needs to be moving towards the top pickup zone
				// This is basically Goal-navigation now.
				
				// First see if he's close enough to the goal, in which case
				// transition to the next State.
				
				goalNavigateAlongCurrentPathForRobotPoint(latestPose, RobotState.RobotStatePickTop, false);
				
				
				// Point robotPoint = new Point(latestPose.x, latestPose.y);
				// Goal currentGoal = goals.get(currentGoalNumber); // TODO: verify this
				// 
				// if (currentGoal.isPointCloseEnoughToGoal(robotPoint)) {
				// 	// We're close enough to change states
				// 	System.out.println("SeekTop: close enough to PickupZone");
				// 	
				// 	// Change state and reset some internal flags.
				// 	setRobotState(RobotState.RobotStatePickTop);
				// 	
				// 	// Just so we have some instructions to reply with
				// 	sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
				// 	
				// 	
				// 	
				// } else {
				// 	// We need to tell the robot to (keep) moving towards the Pickup Zone.
				// 	int robotCurrentAngle = latestPose.angle; // degrees
				// 	int angleToTheGoal = (int)getAngle(currentGoal.location, robotPoint);
				// 	int distanceToGoal = distance(currentGoal.location.x - robotPoint.x,
				// 									currentGoal.location.y -robotPoint.y);
				// 	System.out.println("Distance to goal (" + currentGoal.location.x + ", " + currentGoal.location.y + ") is: " + distanceToGoal);
				// 	
				// 	int RANGE = 5;
				// 	byte rotation = ROTATE_NONE;
				// 	byte movement = MOVE_NONE;
				// 	 
				// 	int angleDifference = angleToTheGoal - robotCurrentAngle;
				// 	System.out.println("Angle diff: " + angleDifference);
				// 	
				// 	if (Math.abs(angleDifference) < RANGE) {
				// 		rotation = ROTATE_NONE;
				// 		movement = MOVE_SMALL;
				// 		System.out.println("Should move");
				// 	} else if (angleDifference > 0) {
				// 		rotation = angleDifference < 180? ROTATE_BACK_SMALL : ROTATE_SMALL;
				// 		movement = MOVE_NONE;
				// 	} else {
				// 		rotation = ROTATE_SMALL; // might need to do like above...
				// 		movement = MOVE_NONE;
				// 	}
				// 	
				// 	
				// 	// Now send this command to the robot
				// 	sendInstructionsToRobot(movement, rotation, GO);
				// 	
				// }
				
				
				break;
				
				
			}
			
			
			case RobotStatePickTop: {
				
				// We need to tell the robot to go into BLOCK_SEEK mode
				
				// We'll send the BLOCK_SEEK command, which he'll then do.
				// After he's found the block (OR A TIMEOUT?) then he'll message again, reporting
				if (commandData[STATUS] == STATUS_COMMAND_REQUEST) {
					// He's asking for what to do, so tell him to go seek
					System.out.println("PickTop: going to tell robot to SEEK_BLOCK");
					sendInstructionsToRobot(MOVE_SMALL, ROTATE_NONE, SEEK_BLOCK);
					break;
				} else if (commandData[STATUS] == STATUS_BLOCK_FOUND) {
					// He's found a block
					System.out.println("PickTop: Robot found a block. Transitioning...");
					setRobotState(RobotState.RobotStateDropBottom);
					computePathFromRobotPoseToEndGoal(latestPose, RobotState.RobotStateDropBottom);
				} else {
					// he must not have found it in time... sad face?
					System.out.println("PickTop: Robot did not find a block... transitioning anyway.");
					setRobotState(RobotState.RobotStateDropBottom);
					computePathFromRobotPoseToEndGoal(latestPose, RobotState.RobotStateDropBottom);
				}
				
				
				// He's either found or hasn't found a block, but he's already looked.
				// Now we tell him to just wait a sec and then ask again for the next state
				sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
				break;

			}
			
			
			case RobotStateDropBottom: {
				// Moving towards the bottom drop zone
				goalNavigateAlongCurrentPathForRobotPoint(latestPose, RobotState.RobotStateDoActualDropBottom, true);
				// When do we tell the robot to ACTUALLY drop it?
				break;
			}
			
			
			case RobotStateDoActualDropBottom: {
				if (commandData[STATUS] == STATUS_COMMAND_REQUEST) {
					// ASking us what to do.. say drop the block!
					System.out.println("DoActualDropBottom: telling robot to drop");
					sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, DROP_BLOCK);
				} else {
					// He's done it
					System.out.println("DoActualDropBottom: Robot has dropped the block.");
					_numberOfDeliveredBlocks++;
					announceDropoffAtPointToStation(new Point(latestPose.x, latestPose.y), 3);
					
					setRobotState(RobotState.RobotStateSeekBottom);
					computePathFromRobotPoseToEndGoal(latestPose, RobotState.RobotStateSeekBottom);
					sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
				}
				
				break;
			}
			
			
			case RobotStateSeekBottom: {
				
				// Seek to the bottom area
				goalNavigateAlongCurrentPathForRobotPoint(latestPose, RobotState.RobotStatePickBottom, false);
				break;
				
			}
			
			
			case RobotStatePickBottom: {
				
				
				// We need to tell the robot to go into BLOCK_SEEK mode
				
				// We'll send the BLOCK_SEEK command, which he'll then do.
				// After he's found the block (OR A TIMEOUT?) then he'll message again, reporting
				if (commandData[STATUS] == STATUS_COMMAND_REQUEST) {
					// He's asking for what to do, so tell him to go seek
					System.out.println("PickBottom: going to tell robot to SEEK_BLOCK");
					sendInstructionsToRobot(MOVE_SMALL, ROTATE_NONE, SEEK_BLOCK);
					break;
				} else if (commandData[STATUS] == STATUS_BLOCK_FOUND) {
					// He's found a block
					System.out.println("PickBottom: Robot found a block. Transitioning...");
					setRobotState(RobotState.RobotStateDropTop);
					computePathFromRobotPoseToEndGoal(latestPose, RobotState.RobotStateDropTop);
				} else {
					// he must not have found it in time... sad face?
					System.out.println("PickBottom: Robot did not find a block... transitioning anyway.");
					setRobotState(RobotState.RobotStateDropTop);
					computePathFromRobotPoseToEndGoal(latestPose, RobotState.RobotStateDropTop);
				}
				
				
				// He's either found or hasn't found a block, but he's already looked.
				// Now we tell him to just wait a sec and then ask again for the next state
				sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, STAY_STILL);
				break;
			}
			
			
			case RobotStateDropTop: {
				
				// Move towards the dropoff zone, then transition to the FinishedCycle state, where the block is dropped and we decide what's next
				goalNavigateAlongCurrentPathForRobotPoint(latestPose, RobotState.RobotStateDoActualDropTop, true);
				
				break;
			}
			
			
			case RobotStateDoActualDropTop: {
				
				if (commandData[STATUS] == STATUS_COMMAND_REQUEST) {
					// ASking us what to do.. say drop the block!
					System.out.println("DoActualDropTop: telling robot to drop");
					sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, DROP_BLOCK);
				} else {
					// He's done it
					
					_numberOfDeliveredBlocks++;
					System.out.println("DoActualDropTop: Robot has dropped the block.(" + _numberOfDeliveredBlocks + "/" + TOTAL_BLOCKS);
					
					
					// Tell the top station
					announceDropoffAtPointToStation(new Point(latestPose.x, latestPose.y), 1);
					
					setRobotState(RobotState.RobotStateFinishedCycle);
					//computePathFromRobotPoseToEndGoal(latestPose, RobotState.RobotStateFinishedCycle);
					sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
				}
				
				
				break;
			}
			
			
			case RobotStateFinishedCycle: {
				
				if (_numberOfDeliveredBlocks == TOTAL_BLOCKS) {
					System.out.println("All the blocks have been delivered. Move out of the way and STOP");
					setRobotState(RobotState.RobotStatePark);
					computePathFromRobotPoseToEndGoal(latestPose, RobotState.RobotStatePark);
					sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
				} else {
					System.out.println("Finished a cycle. More blocks remaining.");
					setRobotState(RobotState.RobotStateSeekTop);
					computePathFromRobotPoseToEndGoal(latestPose, RobotState.RobotStateSeekTop);
					sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
				}
				
				break;
			}
			
			
			case RobotStatePark: {
				

				goalNavigateAlongCurrentPathForRobotPoint(latestPose, RobotState.RobotStateEnd, true);
				
				break;
			}
			
			
			case RobotStateEnd: {
				
				System.out.println("The robot is all done... why is it asking again?");
				announceCompletionToOtherStations();
				sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ALL_DONE);
				break;
				
			}
		}		
	}
	
	
	public void receivedDataFromStation(int stationId, String data) {
		//System.out.println("Got " + data + " from station: " + stationId);
		
		//System.out.printf("Station ID: %d, Data: [ %s ]\n", stationId, data);
		System.out.println("StationID: " + stationId + data);
		
		StationMessage message = StationMessage.Parse(stationId, data);
		int x = message.getX();
		int y = message.getY();
		
		switch (message.getMessageType()) {
			case STATION_1_RED_BLOCK_DROPPED_OFF: {
				_numberOfTopBlocksReady++;
				System.out.println("Got a new top block");
				
				// add it to the pickup list
				
				// Maybe have to offset these points to make it easier for bot to find them?
				topPickupGoals.add(new Goal(new Point(x, y)));
				
				break;
			}
			
			
			case STATION_3_BLUE_BLOCK_DROPPED_OFF: {
				_numberOfBottomBlocksReady++;
				System.out.println("Got a new bottom block");
				
				// add it to the pickup list
				
				// Maybe have to offset these points to make it easier for bot to find them?
				bottomPickupGoals.add(new Goal(new Point(x, y)));
				break;
			}
			
			
			case ZONE_1_LOCKED: {
				_topZoneUnlocked = false;
				System.out.println("Locking the top zone");
				break;
			}
			
			
			case ZONE_1_UNLOCKED: {
				_topZoneUnlocked = true;
				System.out.println("Unlocking the top zone");
				break;
			}
			
			
			case ZONE_3_LOCKED: {
				_bottomZoneUnlocked = false;
				System.out.println("Locking the bottom zone");
				break;
			}
			
			
			case ZONE_3_UNLOCKED: {
				_bottomZoneUnlocked = true;
				System.out.println("Unlocking the bottom zone");
				break;
			}
			
			
			default: {
				System.out.println("Got a message we don't care about: " + message.getMessageType().toString());
			}
			
		}
		
	}
	
	
	public int distance(int a, int b) {
		int c2 = (int) ((Math.pow(a, 2)) + (Math.pow(b, 2)));
		return (int)(Math.sqrt(c2));
	}
	
	
	public double getAngle(Point goalPoint, Point robotPoint) {
		System.out.println("Goal: " + goalPoint.toString() + " robot: " + robotPoint.toString());
		int gx = goalPoint.x;
		int gy = goalPoint.y;
		int rx = robotPoint.x;
		int ry = robotPoint.y;
		
		
		double dx = 0;
		double dy = 0;
		
		dx = gx - rx;
		dy = gy - ry;
		

	    double inRads = Math.atan2(dy,dx);
	    double deg = Math.toDegrees(inRads);
	    if (deg < 0) {
	    	deg = 360 + deg;
	    }
	    return deg;
	}
	
	
	class Goal {
		public Point location;
		public boolean isReached;
		
		public Goal(Point loc) {
			location = loc;
			isReached = false;
		}
		
		public boolean isPointCloseEnoughToGoal(Point p) {
			final int RANGE = 30;
			return (inAbsoluteRange(p.x, this.location.x, RANGE) && inAbsoluteRange(p.y, this.location.y, RANGE));
		}
		
		private boolean inAbsoluteRange(int a, int b, int range) {
			return (Math.abs((a - b)) < range);
		}
		
		public String toString() {
			return location.toString();
		}
	}
	
	
	class Point {
		public int x, y;
		
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		public boolean equals(Point p) {
			return p.x == this.x && p.y == this.y;
		}
		
		public String toString() {
			return x + ", " + y;
		}
	}
	
}