import java.util.*;
import java.lang.Math;

public class NavigateMapPlanner extends Planner {
	
	final byte ROTATE_NONE = 0;
	final byte ROTATE_SMALL = 1;
	//final byte ROTATE_BIG = 2;
	final byte ROTATE_BACK_SMALL = 2;
	//final byte ROTATE_BACK_BIG = 4;
	final byte ROTATE_FULL = 3;
	
	final byte MOVE_NONE = 0;
	final byte MOVE_SMALL = 1;
	final byte STOP = 88;
	final byte GO = 0;
	final byte RECORD = 89;
	
	final int ROTATION = 0;
	final int MOVE = 1;
	final int CONTROL = 2;

	boolean firstPose;

	ArrayList<Pose> poses;
	ArrayList<Goal> goals;
	Goal currentGoal;
	int currentGoalNumber;
	// Constructor for the planner
	public NavigateMapPlanner() {
		firstPose = true;
		poses = new ArrayList<Pose>();
		
		goals = new ArrayList<Goal>();
		
		Pose[] poses = getUserDefinedPath();
		for (Pose p : poses) {
			goals.add(new Goal(new Point(p.x, p.y)));
			System.out.println("Added a new goal at: " + p.x + ", " + p.y);
		}
		
//		goals.add(new Goal(new Point(200, 300)));
//		goals.add(new Goal(new Point(200, 300)));
//		goals.add(new Goal(new Point(200, 300)));
//		goals.add(new Goal(new Point(200, 300)));
//		goals.add(new Goal(new Point(200, 300)));
		
		currentGoal = goals.get(0);
		
		currentGoalNumber = 0;
		
		setTraceFileUserHeaderData("Dirrs+|0|-3|0|0.05|3,Sonar|0|4|0|0.10|19");
		
		
		
	}
	
	private boolean _isSpinning = false;
	private boolean _ackRotate;

	// Called when the planner receives a pose from the tracker
	public void receivedPoseFromTracker(Pose p) {
		if (currentGoalNumber >= goals.size()) {
			System.out.println("Already done the goals... shouldn't be running!");
			return;
		}
		
		
		
		
		if (_isSpinning) {
			
			if (!_ackRotate) {
				System.out.println("Sending the rotate message again");
				sendRotateMessage();
			}else {
				System.out.println("Robot is currently in a spin. Ignoring pose update.");
			}
			
			
			return;
		}
		
		
		
		///////////////////
		//     NEW       //
		///////////////////
		
		// When the robot reaches a goal, we need to instruct it to rotate 360 degrees and take sensor data
		// While in the spin, it should be sending this data back to us.
		// We need to make sure when we're in this method the robot is not currently harvesting data (else return)
		// And we'll listen for this data in another method.
		
		

		
		byte[] outData = new byte[6]; // the data buffer to send to the robot.
		Point robotPoint = new Point(p.x, p.y);
		Goal current = goals.get(currentGoalNumber);
		
		if (current.isPointCloseEnoughToGoal(robotPoint)) {
			System.out.println("It looks like the robot is close enough to goal number " + currentGoalNumber);
			currentGoalNumber++;
			if (currentGoalNumber == goals.size()) {
				System.out.println("It looks like we've completed all goals. One last rotation.");
				sendRotateMessage();
				_isSpinning = true;
				_ackRotate = false;
				return;
			}
			// not done, update the current goal ref
			current = goals.get(currentGoalNumber);
			
			
			// Now tell the robot to enter a spin
			System.out.println("Going to make the robot spin and take sensor data.");
			sendRotateMessage();
			_isSpinning = true;
			_ackRotate = false;
			System.out.println("JUST SET THE SPIN!!");
			
			return;
			
		}
		
		int robotCurrentAngle = p.angle; // in DEGREES.
		int angleToTheGoal = (int)getAngle(current.location, robotPoint);
		
		System.out.println("The angle to the goal is: " + angleToTheGoal + " and current angle is: " + robotCurrentAngle);
		
		int distanceToGoal = distance(current.location.x - robotPoint.x, current.location.y - robotPoint.y);
		System.out.println("The distance to the goal is: " + distanceToGoal);
		
		
		
		/*
		
		diff = goalAngle - robotAngle
		if (abs(diff) < 5)
			robot.move
		else if (diff > 0)
			robot.turnLeft
		else
			robot.turnRight
		
		*/
		
		
		// rotate until the angle between the robot and the point is 0, then move
		int RANGE = 5;
		
		int angleDifference = angleToTheGoal - robotCurrentAngle;
		if (Math.abs(angleDifference) < RANGE) {
			outData[ROTATION] = ROTATE_NONE;
			outData[MOVE] = MOVE_SMALL;
		} else if (angleDifference > 0) {
			outData[ROTATION] = angleDifference < 180 ? ROTATE_BACK_SMALL : ROTATE_SMALL;
			outData[MOVE] = MOVE_NONE;
		} else { // negative angle difference
			int absDiff = Math.abs(angleDifference);
			outData[ROTATION] = ROTATE_SMALL;
			outData[MOVE] = MOVE_NONE;
		}
		
		
		//outData[ROTATION] = ROTATE_SMALL;
		//outData[MOVE] = MOVE_SMALL;
		sendDataToRobot(outData);
		
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
		// if (gx > rx && gy > ry) {
		// 	dx = gx - rx;
		// 	dy = gy - ry;
		// } else if (gx < rx && gy > ry) {
		// 	dx = rx - gx;
		// 	dy = gy - ry;
		// } else if (gx < rx && gy < ry) {
		// 	dx = rx - gx;
		// 	dy = ry - gy;
		// } else {
		// 	dx = rx - gx;
		// 	dy = ry - gy;
		// }
		
		dx = gx - rx;
		dy = gy - ry;
		

	    double inRads = Math.atan2(dy,dx);

	    // We need to map to coord system when 0 degree is at 3 O'clock, 270 at 12 O'clock
//	    if (inRads < 0)
//	        inRads = Math.abs(inRads);
//	    else
//	        inRads = 2*Math.PI - inRads;
	    double deg = Math.toDegrees(inRads);
	    if (deg < 0) {
	    	deg = 360 + deg;
	    }
	    return deg;
	}

	
	/* Basic idea:
	The robot has a current goal and should be moving towards that.
	We get constant updates from the tracker of where the robot actually is.
	From there, determine the robot's best course
		Then send this data to the robot
		Telling it to move for so long and in what angle?
	The robot moves only so far before stopping and sending its new position
		and then waits to receive further instruction.
	*/
	
	public void sendRotateMessage() {
		System.out.println("Sending ROTATE_FULL message");
		byte[] outData = new byte[6];
		outData[ROTATION] = ROTATE_FULL;
		outData[MOVE] = MOVE_NONE;
		outData[CONTROL] = RECORD;
		sendDataToRobot(outData);
	}
	

	// Called when the planner receives data from the robot
	public void receivedDataFromRobot(int[] data) {
		
		if (_isSpinning) {
			_ackRotate = true;
			System.out.println("Heard our first peep from the robot about spinning");
		}
		

		// Get the sensor data from the robot, and check to see if it's finished spinning.
		int d = data[0]*256 + data[1];
		int s = data[2]*256 + data[3];
		
		System.out.println("Got readings from bot. Dirrs: " + d + " Sonar: " + s);
		final int INVALID_DATA = 111;
		if (d == INVALID_DATA || s == INVALID_DATA) {
			System.out.println("Invalid readings...returning.");
			//return;
		}else {
			sendDataToTraceFile("" + d + "," + s);
		}
		
		
		int collecting_data = data[4]*256 + data[5];
		if (collecting_data == 0) {
			System.out.println("The robot is done spinning!");
			_isSpinning = false;
			if (currentGoalNumber == goals.size()) {
				// tell the robot to stop!
				byte[] outData = new byte[6];
				outData[ROTATION] = ROTATE_FULL;
				outData[MOVE] = MOVE_NONE;
				outData[CONTROL] = RECORD;
				sendDataToRobot(outData);
			}
		}

		//int x = data[0]*256 + data[1];
		//int y = data[2]*256 + data[3];
		//int a = data[4]*256 + data[5];

	

		//poses.add(newPose);
		//sendEstimatedPositionToTracker(newPose.x, newPose.y, newPose.angle);
	}

	// Called when the planner receives data from another station
	public void receivedDataFromStation(int stationId, int[] data) {
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
		
		public String toString() {
			return x + ", " + y;
		}
	}


/*

		// PROPBOT kinematics
	public double distancePerPulse() { // if traveling straight!
		double wheelCircumference = 6.86; // cm
		double pulses = 128;
		return (wheelCircumference * Math.PI) / pulses; // should be 0.1684cm
	}

	public double circumferenceOfSpin() {
		double robotWidth = 8.9; // cm
		return Math.PI * robotWidth; // 27.96cm
	} // each pulse results in a turn of 0.1684cm/pulse / 27.96cm = 0.006023%/pulse
	  // each pulse is a turn of 0.006023 %/pulse * 360deg = 2.17deg per pulse


	double radICC(int pL, int pR) {
		return (8.9 * (pL / (pR - pL)) + 4.45);
	}

	double angleDeltaRad(int pL, int pR) {
		return Math.toRadians(0.01892 * (pR - pL));
	}


	public Pose propForwardKinematics(Pose currentPose, int pL, int pR) {

		if (pL == pR) {
			double newX = currentPose.x + 0.1684 * pR * Math.cos(currentPose.angle);
			double newY = currentPose.y + 0.1684 * pR * Math.sin(currentPose.angle);
			double newAngle = currentPose.angle;

			return new Pose((int)newX, (int)newY, (int)newAngle);
		}


		if (pL + pR == 0) {
			// in a spin (or just not moving I guess... but it's all the same really)
			return new Pose(currentPose.x, currentPose.y, currentPose.angle + (int)angleDeltaRad(pL, pR));
		}

		double rICC = radICC(pL, pR);
		double angleDelta = angleDeltaRad(pL, pR);
		double oldAngle = currentPose.angle; // IS THIS IN RAD???

		double newX = currentPose.x + (rICC * (cos(angleDelta) * sin(oldAngle) + cos(oldAngle) * sin(angleDelta) - sin(oldAngle)));

		double newY = currentPose.y + (rICC * (sin(angleDelta) * sin(oldAngle) - cos(oldAngle) * cos(angleDelta) + cos(oldAngle)));

		double newAngle = oldAngle + angleDelta;

		return new Pose((int)newX, (int)newY, (int)newAngle);

	}

	public double cos (double v) {
		return Math.cos(v);
	}

	public double sin (double v) {
		return Math.sin(v);
	}

*/
}