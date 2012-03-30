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

	final int ROTATION = 0;
	final int MOVE = 1;
	final int CONTROL = 2;
	
	final int TOTAL_BLOCKS = 8;
	
	
	
	public enum RobotState {
		RobotStateStart,
		RobotStateSeekTop,
		RobotStatePickTop,
		RobotStateDropBottom,
		RobotStateSeekBottom,
		RobotStatePickBottom,
		RobotStateDropTop,
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
		
		currentRobotState = RobotStateStart;
	}
	
	void sendInstructionsToRobot(byte movement, byte rotation, byte command) {
		byte[] outData = new byte[6]; // the data buffer to send to the robot.
		
		outData[ROTATION] = rotation;
		outData[MOVE] = movement;
		outData[CONTROL] = command;
		
		sendDataToRobot(outData);
	}
	
	
	void goalNavigateAlongCurrentPathForRobotPoint(Pose pose, RobotState nextState) {
		// This really needs to be refactored into a common method!!
		// TODO: THIS ^^^^^^^^^^
		// This is basically Goal-navigation now.
		
		// First see if he's close enough to the goal, in which case
		// transition to the next State.
		Point robotPoint = new Point(p.x, p.y);
		Goal currentGoal = goals.get(currentGoalNumber); // TODO: verify this
		
		if (currentGoal.isPointCloseEnoughToGoal(robotPoint)) {
			// Really, we have to make sure we do this for the WHOLE PATH OF GOALS
			// Not just 1 goal.
			// We're close enough to change states
			System.out.println("Close enough");
			
			// Change state and reset some internal flags.
			this.currentRobotState = nextState;
			_haveSentRobotPickTopCommand = false;
			_haveSentRobotPickBottomCommand = false;
			_latestCommandAcknowledged = false;
			_robotFoundBlock = false;
			
			
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
		
		
		switch (this.currentRobotState) {
			case RobotStateStart: {
				if ((numberOfTopBlocksReady + numberOfBottomBlocksReady) == TOTAL_BLOCKS && topZoneUnlocked && bottonZoneUnlocked) {
					// transition to the next state
					this.currentRobotState = RobotStateSeekTop;
				} else {
					// Stay in the same state... don't tell the robot?
					this.currentRobotState = RobotStateStart;
				}
				
				break;
			}
			
			
			case RobotStateSeekTop: {
				// The robot needs to be moving towards the top pickup zone
				// This is basically Goal-navigation now.
				
				// First see if he's close enough to the goal, in which case
				// transition to the next State.
				Point robotPoint = new Point(p.x, p.y);
				Goal currentGoal = goals.get(currentGoalNumber); // TODO: verify this
				
				if (currentGoal.isPointCloseEnoughToGoal(robotPoint)) {
					// We're close enough to change states
					System.out.println("SeekTop: close enough to PickupZone");
					
					// Change state and reset some internal flags.
					this.currentRobotState = RobotStatePickTop;
					_haveSentRobotPickTopCommand = false;
					_latestCommandAcknowledged = false;
					_robotFoundBlock = false;
					
					
				} else {
					// We need to tell the robot to (keep) moving towards the Pickup Zone.
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
				
				
				break;
				
				
			}
			
			
			case RobotStatePickTop: {
				
				// We need to tell the robot to go into BLOCK_SEEK mode
				// We should also try to get an ACK from the robot he's in this mode?
				if (_haveSentRobotPickTopCommand) {
					// We've already sent this command.. now check if we've been ack'd
					if (_latestCommandAcknowledged) {
						// We've heard the ACK
						// Has the robot said he's found the block yet??
						if (_robotFoundBlock) {
							// At this point, the robot has told us he's found the block
							// And he's sitting and waiting for his next command.
							// So transition to RobotStateDropBottom
							this.currentRobotState = RobotStateDropBottom;
							// Compute a path to this area????
							// TODO: ^^^^^^^^^^^^^^^^^^^^^
							// Basically take the robot's current pose and the goal location
							// And create a path (array of Goals) to the drop area
							// taking into account any obstacles
							// (figure out where the end goal actually is....
							computePathFromRobotPoseToEndGoal(p, RobotStateDropBottom);
						} else {
							// Nope, keep seeking
							// The robot should just keep looping until he finds one
							// Then, he reports back to the TRACKER
							// and breaks out of his loop, and listens for the next command.
						}
					} else {
						// We have NOT heard the ACK... keep trying? or not..
						System.out.println("Waiting for PickTop ACK");
					}
				} else {
					// We have NOT sent the command yet... send it!
					System.out.println("Sending PickTop SEEK_BLOCK message.");
					sendInstructionsToRobot(MOVE_SMALL, ROTATE_NONE, SEEK_BLOCK);
					_haveSentRobotPickTopCommand = true;
				}
				
				// Stay in this mode until the robot tells us he's got a block
				
				
				break;
			}
			
			
			case RobotStateDropBottom: {
				goalNavigateAlongCurrentPathForRobotPoint(p, RobotStateSeekBottom);
				// When do we tell the robot to ACTUALLY drop it?
				break;
			}
			
			
			case RobotStateSeekBottom: {
				
				
				
			}
			
		}
		
		
		
		switch(this.currentMode) {
			
			case RobotModeWaitForNextBlock: {
				
				if (numberOfAvailableBottomBlocks + numberOfAvailableTopBlocks == 7) {
					// we can start looking
					currentMode = RobotModeSeekTopBlock;
					
					
					// start orienting the robot towards the top pickup goal!!
					
				} else {
					// Not ready to start looking for blocks yet.
					
					
					// OR!! Maybe we're only here because we're locked out, so we need to wait. hmmm
					// Or would that happen? I guess not in this mode, it's only for waiting for a block,
					// not when we're locked out....
					
					
					// tell the robot to just be idle
					sendInstructionsToRobot(BE_STILL, ROTATE_NONE, SEEK_NONE);
				}
				
				break;
			}
			
			case RobotModeSeekTopBlock: {
				
				// tell the robot to TRY and move to the top pickup area.
				// BUT if the area is LOCKED, then he'll have to just wait.
				if (topZoneOpen == false) {
					// we're not allowed in the top zone just quite yet
				}
				
				// See where the robot currently is wrt to the TopDangerZone, regardless of angle
				// that is, treat the dangerZone like a line (across the Y axis)
				double distance = distanceToDangerZone(DANGER_ZONE_TOP, p);
				
				// this distance might be negative?
				
				if (distance  < 0) {
					// we're outside the top zone, so we're safe
					// keep moving towards the zone goal
				} else {
					// we're INSIDE the dangerZone, so be careful.
					// Tell the Station ONE if we haven't already done so
					// Then tell the robot to look for a block if we haven't already done so
					// Just because we're in the zone doesn't mean we're close enough to the pickup zone
				}
				
				
				break;
			}
			
			case RobotModeSeekBottomBlock: {
				
				// tell the robot to TRY and move to the bottom pickup area.
				// UNLESS the area is Locked, then he'll just have to wait.
				
				break;
			}
			
			case RobotModeReturnTopBlock: {
				
				// tell the robot to try and return a block to the top area, unless it is locked
				// Well, can move so close before pausing
				
				break;
			}
			
			case RobotModeReturnBottomBlock: {
				
				// tell the robot to try and return a block to the bottom area, unless it is locked
				// Move so close before pausing.
				
				break;
			}
		}
		
	}
	
	
	// Called when the planner receives data from the robot
	public void receivedDataFromRobot(int[] data) {
		
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