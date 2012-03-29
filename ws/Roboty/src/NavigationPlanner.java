import java.util.*;
import java.lang.Math;

public class NavigationPlanner extends Planner {

	boolean firstPose;

	ArrayList<Pose> poses;
	ArrayList<Goal> goals;
	Goal currentGoal;
	int currentGoalNumber;
	// Constructor for the planner
	public NavigationPlanner() {
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
		
		
		
	}

	// Called when the planner receives a pose from the tracker
	public void receivedPoseFromTracker(Pose p) {
		if (currentGoalNumber >= goals.size()) {
			System.out.println("Already done the goals... shouldn't be running!");
			return;
		}
//		if (firstPose) {
//			byte[] outData = new byte[6];
//			outData[0] = (byte)(p.x / 256);
//			outData[1] = (byte)(p.x % 256);
//			outData[2] = (byte)(p.y / 256);
//			outData[3] = (byte)(p.y % 256);
//			outData[4] = (byte)(p.angle / 256);
//			outData[5] = (byte)(p.angle % 256);
//			poses.add(p);
//			sendDataToRobot(outData);
//			firstPose = false;
//		}
		
		// Build up the point from the received data
		// Check to see if it's near enough to the CURRENT_GOAL
		//	if so, then mark it and move along to the next goal.
		//		if all goals are met, then tell the robot to STOP
		//	calculate how to get to the current goal based on the robot's current pose
		//	SEND this instruction to the robot
		//
		//	Include in packet:
		//		Rotation amount. Robot will rotate first before moving, if needed.
		//		Movement amount. Robot will move after rotating, if needed, the desired amount.
		//		STOP. If so, the robot beeps and stops.
		final byte ROTATE_NONE = 0;
		final byte ROTATE_SMALL = 1;
		final byte ROTATE_BIG = 2;
		final byte ROTATE_BACK_SMALL = 3;
		final byte ROTATE_BACK_BIG = 4;
		
		final byte MOVE_NONE = 0;
		final byte MOVE_SMALL = 1;
		final byte STOP = 88;
		final byte GO = 0;
		
		final int ROTATION = 0;
		final int MOVE = 1;
		final int CONTROL = 2;
		
		byte[] outData = new byte[6]; // the data buffer to send to the robot.
		Point robotPoint = new Point(p.x, p.y);
		Goal current = goals.get(currentGoalNumber);
		
		if (current.isPointCloseEnoughToGoal(robotPoint)) {
			System.out.println("It looks like the robot is close enough to goal number " + currentGoalNumber);
			currentGoalNumber++;
			if (currentGoalNumber == goals.size()) {
				System.out.println("It looks like we've completed all goals. STOP.");
				outData[ROTATION] = ROTATE_NONE;
				outData[MOVE] = MOVE_NONE;
				outData[CONTROL] = STOP;
				sendDataToRobot(outData);
				return;
			}
			// not done, update the current goal ref
			current = goals.get(currentGoalNumber);
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
		} else if (diff > 0) {
			outData[ROTATION] = ROTATE_BACK_SMALL;
			outData[MOVE] = MOVE_NONE;
		} else {
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

	    return Math.toDegrees(inRads);
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
	

	// Called when the planner receives data from the robot
	public void receivedDataFromRobot(int[] data) {

		// calculate the estimated pose from what we recieve from the robot

		//int x = data[0]*256 + data[1];
		//int y = data[2]*256 + data[3];
		//int a = data[4]*256 + data[5];

		int pulseLeft = data[0] * 256 + data[1];
		int pulseRight = data[2] * 256 + data[3];

		Pose currentPose = poses.get(poses.size() - 1);
		Pose newPose = propForwardKinematics(currentPose, pulseLeft, pulseRight);

		poses.add(newPose);

		sendEstimatedPositionToTracker(newPose.x, newPose.y, newPose.angle);
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
	}



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


}