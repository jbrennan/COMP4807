import java.util.*;
import java.lang.Math;

public class BorderMapPlanner extends Planner {

	boolean firstPose;

	ArrayList<Pose> poses;
	ArrayList<Goal> goals;
	Goal currentGoal;
	// Constructor for the planner
	public BorderMapPlanner() {
		//firstPose = true;
		poses = new ArrayList<Pose>();
		
		goals = new ArrayList<Goal>();
		
		
		//setTraceFileUserHeaderData("DIRRS,Sonar");
		setTraceFileUserHeaderData("Dirrs+|0|-3|0|0.05|3,Sonar|0|4|0|0.10|19");
		// goals.add(new Goal(new Point(200, 300)));
		// goals.add(new Goal(new Point(200, 300)));
		// goals.add(new Goal(new Point(200, 300)));
		// goals.add(new Goal(new Point(200, 300)));
		// goals.add(new Goal(new Point(200, 300)));
		// 
		// currentGoal = goals.get(0);
		
		
		
	}

	// Called when the planner receives a pose from the tracker
	public void receivedPoseFromTracker(Pose p) {
		if (firstPose) {
			byte[] outData = new byte[6];
			outData[0] = (byte)(p.x / 256);
			outData[1] = (byte)(p.x % 256);
			outData[2] = (byte)(p.y / 256);
			outData[3] = (byte)(p.y % 256);
			outData[4] = (byte)(p.angle / 256);
			outData[5] = (byte)(p.angle % 256);
			poses.add(p);
			sendDataToRobot(outData);
			firstPose = false;
		}
	}
	
	public void receivedDataFromRobot(int[] data) { 
		int s = data[0]*256 + data[1];
		int d = data[2]*256 + data[3];
		
		System.out.println("Dirrs: " + d + " Sonar: " + s);
		final int INVALID_DATA = 111;
		if (d == INVALID_DATA || s == INVALID_DATA) {
			System.out.println("Invalid readings...returning.");
			return;
		}
		 
		sendDataToTraceFile("" + d + "," + s);
	}
	

	// Called when the planner receives data from the robot
	// public void receivedDataFromRobot(int[] data) {
	// 
	// 	// calculate the estimated pose from what we recieve from the robot
	// 
	// 	//int x = data[0]*256 + data[1];
	// 	//int y = data[2]*256 + data[3];
	// 	//int a = data[4]*256 + data[5];
	// 
	// 	int pulseLeft = data[0] * 256 + data[1];
	// 	int pulseRight = data[2] * 256 + data[3];
	// 
	// 
	// 	sendEstimatedPositionToTracker(newPose.x, newPose.y, newPose.angle);
	// }

	// Called when the planner receives data from another station
	public void receivedDataFromStation(int stationId, int[] data) {
	}
	
	
	class Goal {
//		public Point location;
//		public boolean isReached;
//		
//		public Goal(Point loc) {
//			location = loc;
//			isReached = false;
//		}
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