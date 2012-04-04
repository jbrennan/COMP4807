import java.util.*;
import java.lang.Math;

public class MiddlePlanner extends Planner {
	final byte ROTATE_NONE = 0;
	final byte ROTATE_SMALL = 1;
	final byte ROTATE_BIG = 2;
	final byte ROTATE_BACK_SMALL = 3;
	final byte ROTATE_BACK_BIG = 4;
	final byte ROTATE_FULL = 5;

	final byte MOVE_NONE = 0;
	final byte BE_STILL = 0;
	final byte MOVE_SMALL = 1;
	
	final byte STOP = 88;
	final byte GO = 0;
	final byte RECORD = 89;
	final byte SEEK_NONE = 90;
	final byte SEEK_BLOCK = 91;
	final byte DROP_BLOCK = 92;

	final int ROTATION = 0;
	final int MOVE = 1;
	final int CONTROL = 2;
	
	final int TOTAL_BLOCKS = 8;
	
	
	
	public enum RobotState {
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
		RobotStateEnd
	}
	

	boolean firstPose;

	ArrayList<Pose> poses;
	ArrayList<Goal> goals;
	
	
	// Locks to ensure I don't collide with another station's robot.
	boolean topZoneOpen = true;
	boolean bottomZoneOpen = true;
	
	
	// The goal areas for where I pick up the cylanders from other bots
	Goal topPickupZone = new Goal(new Point(0, 0));
	Goal bottomPickupZone = new Goal(new Point(480, 240));
	
	Goal topDropZone = new Goal(new Point(0, 240));
	Goal bottomDropZone = new Goal(new Point(480, 0));
	
	
	RobotState currentRobotState;
	
	int numberOfAvailableTopBlocks;
	int numberOfAvailableBottomBlocks;
	
	
	Goal currentGoal;
	int currentGoalNumber;
	
	
	// Flags
	boolean _allDone;
	boolean _haveSentRobotCommand;
	boolean _latestCommandAcknowledged;
	boolean _robotFoundBlock;
	
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
		
		
		currentGoal = goals.get(0);
		currentGoalNumber = 0;
		
		setRobotState(RobotStateStart);
	}
	
	
	
	void setRobotState(RobotState newState) {
		if (newState == currentRobotState)
			return;
		
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
	
	
	void goalNavigateAlongCurrentPathForRobotPoint(Pose pose, RobotState nextState) {
		// This is basically Goal-navigation now.
		
		// First see if he's close enough to the goal, in which case
		// transition to the next State.
		Point robotPoint = new Point(p.x, p.y);
		Goal currentGoal = goals.get(currentGoalNumber); // TODO: verify this
		
		if (currentGoal.isPointCloseEnoughToGoal(robotPoint)) {
			// Really, we have to make sure we do this for the WHOLE PATH OF GOALS
			// Not just 1 goal.
			// We're close enough to change states
			System.out.println(this.currentRobotState.toString() + ": Close enough");
			
			// Change state and reset some internal flags.
			setRobotState(nextState);
			
			// Just so we have some instructions to reply with
			sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
			
		} else {
			// We need to tell the robot to (keep) moving towards the Drop Zone.
			int robotCurrentAngle = p.angle; // degrees
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
		poses.add(p);
		
		
	}
	
	
	void announceCompletionToOtherStations() {
		System.out.println("IMPLEMENT ME -- announceCompletionToOtherStations()");
	}
	
	
	// Called when the planner receives data from the robot
	public void receivedDataFromRobot(int[] data) {
		
		Pose latestPose = poses.get(poses.size() - 1);
		int[] commandData = processedRobotData(data); // basically just intifies the data from the robot.
		
		
		switch (this.currentRobotState) {
			case RobotStateStart: {
				if ((numberOfTopBlocksReady + numberOfBottomBlocksReady) == TOTAL_BLOCKS && topZoneUnlocked && bottonZoneUnlocked) {
					// transition to the next state
					
					setRobotState(RobotStateSeekTop);
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
				Point robotPoint = new Point(latestPose.x, latestPose.y);
				Goal currentGoal = goals.get(currentGoalNumber); // TODO: verify this
				
				if (currentGoal.isPointCloseEnoughToGoal(robotPoint)) {
					// We're close enough to change states
					System.out.println("SeekTop: close enough to PickupZone");
					
					// Change state and reset some internal flags.
					setRobotState(RobotStatePickTop);
					
					// Just so we have some instructions to reply with
					sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
					
					
					
				} else {
					// We need to tell the robot to (keep) moving towards the Pickup Zone.
					int robotCurrentAngle = latestPose.angle; // degrees
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
						rotation = ROTATE_SMALL; // might need to do like above...
						movement = MOVE_NONE;
					}
					
					
					// Now send this command to the robot
					sendInstructionsToRobot(movement, rotation, GO);
					
				}
				
				
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
					setRobotState(RobotStateDropBottom);
					computePathFromRobotPoseToEndGoal(latestPose, RobotStateDropBottom);
				} else {
					// he must not have found it in time... sad face?
					System.out.println("PickTop: Robot did not find a block... transitioning anyway.");
					setRobotState(RobotStateDropBottom);
					computePathFromRobotPoseToEndGoal(latestPose, RobotStateDropBottom);
				}
				
				
				// He's either found or hasn't found a block, but he's already looked.
				// Now we tell him to just wait a sec and then ask again for the next state
				sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, STAY_STILL);
				break;

			}
			
			
			case RobotStateDropBottom: {
				// Moving towards the bottom drop zone
				goalNavigateAlongCurrentPathForRobotPoint(latestPose, RobotStateDoActualDropBottom);
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
					System.out.println("DoActualDropBottom: Robot has dropped the block.")
					setRobotState(RobotStateSeekBottom);
					computePathFromRobotPoseToEndGoal(latestPose, RobotStateSeekBottom);
					sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
				}
				
				break;
			}
			
			
			case RobotStateSeekBottom: {
				
				// Seek to the bottom area
				goalNavigateAlongCurrentPathForRobotPoint(latestPose, RobotStatePickBottom);
				break;
				
				
				// // The Robot has now actually dropped the block and is awaiting its next instruction
				// 
				// if (_haveSentDropBlockCommand) {
				// 	// We've sent this command, now check to see if we've heard the ACK
				// 	if (_lastCommandAcknowledged) {
				// 		
				// 		// We can assume he's dropped off the block now and REVERSED a bit.
				// 		// Now he should be goal following to the BOTTOM ZONE
				// 		goalNavigateAlongCurrentPathForRobotPoint(p, RobotStatePickBottom);
				// 		
				// 	} else {
				// 		// The robot has NOT ack'd yet... keep waiting
				// 		System.out.println("Waiting for SeekBottom ACK.");
				// 	}
				// } else {
				// 	// We have NOT yet sent the drop command, we need to send it now
				// 	System.out.println("Sending the DROP BLOCK message.");
				// 	sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, DROP_BLOCK);
				// 	_haveSentDropBlockCommand = true;
				// }
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
					setRobotState(RobotStateDropTop);
					computePathFromRobotPoseToEndGoal(latestPose, RobotStateDropTop);
				} else {
					// he must not have found it in time... sad face?
					System.out.println("PickBottom: Robot did not find a block... transitioning anyway.");
					setRobotState(RobotStateDropTop);
					computePathFromRobotPoseToEndGoal(latestPose, RobotStateDropTop);
				}
				
				
				// He's either found or hasn't found a block, but he's already looked.
				// Now we tell him to just wait a sec and then ask again for the next state
				sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, STAY_STILL);
				break;
				
				// // We need to tell the robot to go into BLOCK_SEEK mode
				// // We should also try to get an ACK from the robot he's in this mode?
				// if (_haveSentRobotPickBottomCommand) {
				// 	// We've already sent this command.. now check if we've been ack'd
				// 	if (_latestCommandAcknowledged) {
				// 		// We've heard the ACK
				// 		// Has the robot said he's found the block yet??
				// 		if (_robotFoundBlock) {
				// 			// At this point, the robot has told us he's found the block
				// 			// And he's sitting and waiting for his next command.
				// 			// So transition to RobotStateDropTop
				// 			setRobotState(RobotStateDropTop);
				// 			// Compute a path to this area????
				// 			// TODO: ^^^^^^^^^^^^^^^^^^^^^
				// 			// Basically take the robot's current pose and the goal location
				// 			// And create a path (array of Goals) to the drop area
				// 			// taking into account any obstacles
				// 			// (figure out where the end goal actually is....
				// 			computePathFromRobotPoseToEndGoal(p, RobotStateDropTop);
				// 		} else {
				// 			// Nope, keep seeking
				// 			// The robot should just keep looping until he finds one
				// 			// Then, he reports back to the TRACKER
				// 			// and breaks out of his loop, and listens for the next command.
				// 		}
				// 	} else {
				// 		// We have NOT heard the ACK... keep trying? or not..
				// 		System.out.println("Waiting for PickBottom ACK");
				// 	}
				// } else {
				// 	// We have NOT sent the command yet... send it!
				// 	System.out.println("Sending PickBottom SEEK_BLOCK message.");
				// 	sendInstructionsToRobot(MOVE_SMALL, ROTATE_NONE, SEEK_BLOCK);
				// 	_haveSentRobotPickTopCommand = true;
				// }
				// 
				// break;
			}
			
			
			case RobotStateDropTop: {
				
				// Move towards the dropoff zone, then transition to the FinishedCycle state, where the block is dropped and we decide what's next
				goalNavigateAlongCurrentPathForRobotPoint(p, RobotStateDoActualDropTop);
				
				break;
			}
			
			
			case RobotStateDoActualDropTop: {
				
				if (commandData[STATUS] == STATUS_COMMAND_REQUEST) {
					// ASking us what to do.. say drop the block!
					System.out.println("DoActualDropTop: telling robot to drop");
					sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, DROP_BLOCK);
				} else {
					// He's done it
					System.out.println("DoActualDropTop: Robot has dropped the block.")
					setRobotState(RobotStateFinishedCycle);
					computePathFromRobotPoseToEndGoal(latestPose, RobotStateFinishedCycle);
					sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
				}
				
				
				break;
			}
			
			
			case RobotStateFinishedCycle: {
				
				
				if (_numberOfDeliveredBlocks == TOTAL_BLOCKS) {
					System.out.println("All the blocks have been delivered. Move out of the way and STOP");
					setRobotState(RobotStateEnd);
					sendInstructionsToRobot(REVERSE_BIG, ROTATE_NONE, ALL_DONE);
				} else {
					System.out.println("Finished a cycle. More blocks remaining.");
					setRobotState(RobotStateSeekTop);
					computePathFromRobotPoseToEndGoal(latestPose, RobotStateSeekTop);
					sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
				}
				
				break;
				
				
				// We need to tell the robot to DROP a block and reverse a bit
				// We should also try to get an ACK from the robot once he's done this
				// if (_haveSentRobotCommand) {
				// 	// We've already sent this command.. now check if we've been ack'd
				// 	if (_latestCommandAcknowledged) {
				// 		// We've heard the ACK
				// 		
				// 		
				// 		// See if we're all done
				// 		if (_numberOfDeliveredBlocks == TOTAL_BLOCKS) {
				// 			System.out.println("All blocks have been delivered. Going to move out of the way and STOP.");
				// 			// in this state, move out of the way and tell the others I'm done.
				// 			setRobotState(RobotStateEnd);
				// 		} else {
				// 			// Not done yet... keep seeking.
				// 			System.out.println("Finished a cycle. More cycles left.");
				// 			setRobotState(RoboteStateSeekTop);
				// 			computePathFromRobotPoseToEndGoal(p, RobotStateSeekTop);
				// 		}
				// 		
				// 		
				// 		
				// 	} else {
				// 		// We have NOT heard the ACK... keep trying? or not..
				// 		System.out.println("Waiting for FinishedCycle ACK");
				// 	}
				// } else {
				// 	// We have NOT sent the command yet... send it!
				// 	System.out.println("Sending FinishedCycle DROP_BLOCK message.");
				// 	sendInstructionsToRobot(MOVE_SMALL, ROTATE_NONE, DROP_BLOCK);
				// 	_haveSentRobotCommand = true;
				// }
				
				
			}
			
			
			case RobotStateEnd: {
				
				System.out.println("The robot is all done... why is it asking again?");
				sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ALL_DONE);
				break;
				
				// if (_haveSentRobotCommand) {
				// 	// We've already sent this command.. now check if we've been ack'd
				// 	if (_latestCommandAcknowledged) {
				// 		// We've heard the ACK
				// 		
				// 		if (_allDone) {
				// 			return;
				// 		}
				// 		
				// 		// Reverse and STOP
				// 		sendInstructionsToRobot(REVERSE_BIG, ROTATE_NONE, STOP);
				// 		
				// 		// announce we're done
				// 		announceCompletionToOtherStations();
				// 		_allDone = true;
				// 		
				// 	} else {
				// 		// We have NOT heard the ACK... keep trying? or not..
				// 		System.out.println("Waiting for FinishedCycle ACK");
				// 	}
				// } else {
				// 	// We have NOT sent the command yet... send it!
				// 	System.out.println("Sending FinishedCycle DROP_BLOCK message.");
				// 	sendInstructionsToRobot(MOVE_SMALL, ROTATE_NONE, DROP_BLOCK);
				// 	_haveSentRobotCommand = true;
				// }
			}
		}		
	}
	
	
	public void receivedDataFromStation(int stationId, int[] data) {
		
	}
	
	
	public int distance(int a, int b) {
		int c2 = (int) ((Math.pow(a, 2)) + (Math.pow(b, 2)));
		return (int)(Math.sqrt(c2));
	}
	
	
	public double getAngle(Point goalPoint, Point robotPoint) {
		int gx = goalPoint.x;
		int gy = goalPoint.y;
		int rx = robotPoint.x;
		int ry = robotPoint.y;
		
		
		double dx = 0;
		double dy = 0;
		
		dx = gx - rx;
		dy = gy - ry;
		
	
		double inRads = Math.atan2(dy,dx);
	
		return Math.toDegrees(inRads);
	}
	
	
	class Goal {
		public Point location;
		public boolean isReached;
		
		public Goal(Point loc) {
			location = loc;
			isReached = false;
		}
		
		public boolean isPointCloseEnoughToGoal(Point p) {
			final int RANGE = 20;
			return (inAbsoluteRange(p.x, this.location.x, RANGE) && inAbsoluteRange(p.y, this.location.y, RANGE));
		}
		
		private boolean inAbsoluteRange(int a, int b, int range) {
			return (Math.abs((a - b)) < range);
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
	}
	
	
	
	
	
	// The current mode of the robot.
	// He may have to idle if, for example, one of the drop zones is currently occupied by another bot.
	// But he remains in his current mode until he's completed the goal.
	// public enum RobotMode {
	// 	RobotModeSeekTopBlock, // travelling towards the top to get a block in need of a move
	// 	RobotModeSeekBottomBlock, // travelling towards the bottom to get a block to bring back up
	// 	RobotModeReturnTopBlock, // moving a block towards the TOP drop zone
	// 	RobotModeReturnBottomBlock, // moving a block towards the BOTTOM drop zone
	// 	RobotModeWaitForNextBlock // Currently no blocks available, so be idle
	// }
	
	
	

	
	
	
	
}