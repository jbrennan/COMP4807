The Team
--------

We were team "Get the job done", featuring Darryl Hill on Station 1, Me on Station 2, and Frank Perks on Station 3. We each played the following roles:

1. Darryl was in charge of finding blocks nestled amongst the upside down flower pots. This involved goal navigation guided by the Tracker, wall following and collision avoidance around the flower pots, and block sensing by the robot to find blocks.

2. My job in the middle station was to move blocks delivered by Darryl to be dropped off to the bottom where they could be picked up by Frank, and to also move blocks delivered by Frank and drop them off where Darryl could pick them up. This involved mostly goal following from the Tracker, along with some block sensing, and lots of dynamic course correction.

3. Frank's task was similar to Darryl. He had to find blocks laid out in his section which also was bifurcated by a wall with a small gap. Frank had to use wall following and collision avoidance to avoid smacking into the wall.

The Competition
---------------

The competition environment was set up as in the image below. There are pots to navigate in Station 1, a board and corners to avoid in Section 2, and there's a wall with a small gap to navigate in Station 3.

The gist of the task we had to accomplish was to exchange blocks from Station 1 and Station 3. As I was in charge of the middle station, I was in charge of doing the exchange between the top and bottom stations. My robot was essentially the fulcrum of the operation. The completion of the task depended on my robot.

As both team members were supposed to drop off 4 blocks each, I would have to deliver 8 in total: 4 from the top to the bottom, and 4 from the bottom to the top. All the while avoiding collisions with the environment.

![round_2.png]("Details")

My Approach
-----------

The approach our team aimed for was *extreme simplicity*. At first, we were trying to decide how best to accomplish the task. Some obvious issues with coordinating such a task (as described above) are problems with having a block for my robot to deliver (that is, if my robot wants to pick up a block from Station 1 and bring it to the bottom for Station 3, he has to know there IS a block to be picked up!), and making sure the robots wouldn't collide if there was such a block available. How would we know when Frank's robot had come to drop off a block and how would I avoid colliding with his robot before he'd safely exited my station's area?

We decided to take the simplest possible approach, at the expense of completing the task in the allotted time. The approach was simple: Do everything in a lockstep way. There are three main stages to this appraoch:

1. Robots 1 and 3 hunt for their blocks and deliver them one by one to the designated areas in Station 2, informing my Tracker the exact drop off location of each block (and from which station the message came). While this is happening *my robot was to be completely idle*, safely out of the way of either of the other robots.

2. After each of Robots 1 and 3 had delivered 4 of their blocks, they were to return to their stations, and become idle. Then and only then, my robot in station 2 would begin its task. It would first **Goal Navigate** using the Tracker to the most recently dropped off block from Station 1 (we had the blocks dropped off farthest first, so that I'd pick them up in reverse order, so I wouldn't knock down any other blocks in the way). When my robot was close enough to the Goal (given a small distance threshhold so it didn't have to be exact), it would orient itself so it was angled properly at the Goal.

    It would then go into `Block Sensing Mode` and try to pick up a block. After it found the block, it would then Goal Navigate again, this time to a designated drop off location for Station 3 to pick up later. The robot would also message Station 3 with the exact location of this drop off. Then the robot would start **Goal Navigating** again, to get the most recently dropped off block from Station 3 (just like how it started by navigating for Station 1 blocks earlier). When it reached its goal location, it would again orient itself, enter block seeking mode, and capture a block. With its block captured, it was to **Goal Navigate** again to the Station 1 drop off area, drop off the block, and inform Station 1 of its location.
    
    After completing one such cycle, it was to check to see how many more blocks were remaining. If there were no more blocks remaining, it was to Goal Navigate out of the way again, and inform the other Stations. This was a completion of its task. If it *had not* completed its task, it was to make another cycle and keep checking, etc.

3. After Station 2 had finished its task and the other 2 robots had been notified, those robots would begin collecting the dropped off blocks and returning them to their stations. While this was happening, my robot was to remain idle. He was done his job.

Implementation Details
----------------------

For the implementation of my Station's task, there were three main parts: The `MiddlePlanner.java` code, which handled most of the robot's logic; the `Navigate.spin` code which handled following Tracker instructions on the robot itself; and finally a messaging protocol established with the rest of the team for knowing when certain robot events have occurred and where (e.g., "Robot 1 dropped off a block at x,y"). Each of these will now be explained in detail.

###MiddlePlanner

The tracker is where the majority of the work was done for my Robot's task. The gist of it is I used the planner to do **all** robot related logic, and then I would send these logic commands to the Robot itself, who would then perform the tasks and report back.

The Planner code was implemented as a finite state machine, with the following possible states:

	public enum RobotState {
		RobotStateInvalid, // Used as default value before a state is set
		RobotStateStart, // The entrance state..robot waits to begin
		RobotStateSeekTop, // Goal navigate to Top pickup location and orient to it
		RobotStatePickTop, // Robot should be looking for a block and snatch it if found
		RobotStateDropBottom, // Robot navigates to the Bottom drop off target
		RobotStateDoActualDropBottom, // Robot performs the block drop and backs up slowly
		RobotStateSeekBottom, // Robot Navigates to the bottom Pickup Zone and orients
		RobotStatePickBottom, // Robot locates a block and picks it up
		RobotStateDropTop, // Robot navigates to the Top dropoff goal
		RobotStateDoActualDropTop, // Robot drops off the block at the top and backs up
		RobotStateFinishedCycle, // Evaluate if we're done. If not, do another cycle
		RobotStatePark, // If done, move out of the way and stop
		RobotStateEnd // Finally parked, shutdown and tell other stations
	}

The robot stays in `RobotStateStart`, which is idle, until both Stations 1 and 3 have told me they have dropped off 4 blocks each and their locations.

The basic runloop of the Planner code is recording the Robot's latest pose in `receivedPoseFromTracker(Pose p)`. It adds the pose to a list of poses maintained by the class. The main logic then occurs in the `receivedDataFromRobot(int[] data)` method. Here's the way it works:

1. On the Robot, it enters a runloop forever, and it clears out some flags, then it sends some data to the planner, either reporting what it's just done OR asking the Planner for its next task.
2. The `receivedDataFromRobot(int[] data)` gets called on the Planner, which is essentially one large `switch` statement, with a `case` for every possible robot state (this is how the transitions between the robot's finite state machine are handled).
3. The Planner decides what the robot needs to do next. It looks at the most recent robot Pose and the current state, and determines what the robot should do, whether that's move, rotate, or enter a new mode (like block seeking or block dropping, for example). When the Planner has decided what to do, it responds by sending the robot a set of bytes back and it returns from this method.
4. The robot just sits and waits for a response from the Planner after it asks. Then it reads what the Planner has told it to do and it performs the task. More detail will be given below in the robot's code section.

This means the Robot does very little logic on its own. The Planner code is what determines what needs to be done. The robot is essentially a dumb terminal performing these tasks. In order to avoid things like wall collisions, the Planner code sends very small instructions to the robot very rapidly, so the robot never has to come near a wall. The move or rotate instructions are for small amounts of movement, with the idea the robot will request many of these as it's moving. This allows for really quick and precise course corrections while Goal navigating, for example.

When the robot was in a state where it needed to *Goal Navigate*, it called a single method to perform the navigation. Having all goal navigating states refer to a single Navigation method meant my code could be changed in one place and every place it was needed automatically got the new functionality. This made debugging much simpler. It also gave me consistent behavior.

The method also allowed me to transition to the next state when the navigation had completed and oriented properly. When doing this, it would send a message to the robot to just "ASK_AGAIN", meaning the robot would do nothing, loop, and just make another request to the Planner. When this new request came in, the Planner's finite state machine had already transitioned to the next state. I found this was an elegant way to transition states and keep the robot and planner in sync. The method looked like the following:

	void goalNavigateAlongCurrentPathForRobotPoint(Pose pose, RobotState nextState, boolean forceNextState) {
		// This is Goal-navigation.
		
		// First see if he's close enough to the goal, in which case
		// transition to the next State.
		Point robotPoint = new Point(pose.x, pose.y);
		Goal currentGoal = goals.get(0); // Only dealing with 1 goal. If paths had more goals, change this
		
		if (currentGoal.isPointCloseEnoughToGoal(robotPoint)) {

			// Make sure we're properly oriented to the goal so we can just move forward when seeking a block. Then transition in this method.
			if (forceNextState) {
				// No need to orient
				// Change state and reset some internal flags.
				setRobotState(nextState);
				
				// Just so we have some instructions to reply with
				sendInstructionsToRobot(MOVE_NONE, ROTATE_NONE, ASK_AGAIN);
			} else {
				// Not done navigating.. still need to orient to the goal.
				orientToGoalForPose(pose, currentGoal, nextState);
			}
			
		} else {
			// We need to tell the robot to (keep) moving towards the Drop Goal.
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
				rotation = ROTATE_SMALL;
				movement = MOVE_NONE;
			}
			
			// Now send this command to the robot
			sendInstructionsToRobot(movement, rotation, GO);
			
		}
	}

Orienting towards the goal worked in a similar way, as can be seen in the Planner file.

When transitioning to a state where the robot would need to goal follow, I also invoked the `computePathFromRobotPoseToEndGoal(Pose currentRobotPose, RobotState state)` method so that a new path could be computed. This path would guide the robot from its current location to its destination (e.g., picking up a block to where it needs to be dropped off). In this method, I used a really simple way for making a "path" which really only had a destination (like taking the next pickup location, for example). I could have made this more sophisticated for better dodging obstacles (like creating a safer path), but I went for utmost simplicity in the name of just getting things done.

###Navigate.spin

This was the code which ran on the Robot, which was basically responsible for listening for commands from the Planner code and executing them. It had a main runloop where it would perform its most recent task if any (e.g., Rotate, move, seek a block, drop a block, etc.), and then it would report back to the Planner, either saying which task it had just completed, or it would ask for a new task. It would immediately stop and wait for a response from the Planner before doing anything else.

After the planner did its logic as described above, the Robot would read the response and perform any task needed, and the loop would happen over again.

This continued until the robot received the Complete message, in which case it would beeb a shutdown sound and exit the runloop.

The movement commands are fairly straightforward, just entering a loop and either moving or spinning as required.

The Block seeking code is a little more involved. It first opens the robot's grippers and tilts the head down, and then it must use the camera to try and find a block. In a loop, it searches until it finds a block. If given more time, it would have been a good idea to integrate a timeout to this code, so if the robot didn't find a block after so long, it would just return a failure condition. Instead, my robot will look until the end of time (or its battery dies, whichever comes first!). This is less than ideal. On every iteration of the searching loop, the robot uses its block sensor to see if it's got a block. In this case, it breaks from the loop, closes its grippers, sets an internal flag for reporting back to the Planner, and returns.

If it doesn't detect a block, it uses the camera to see if it can find a block. Because the robot has already been oriented towards the block (hopefully) at this point, then it should quickly find the block. It uses the camera data to see if it needs to move to the right or the left, or just move forward to find the block. And then it loops again.

###Message format between stations

The message format we agreed on to talk between stations was pretty simple and can be seen in the `StationMessage.java` and `MessageType.java` classes included below.

Essentially, we've used specially formatted strings which can be read and created, sent among the stations, indicating what kind of message was sent (like "Station 2 dropped off a block to the bottom at (x, y)").

There was a significant problem with this, however. **The Tracker was unreliable at delivering messages between stations 1 and 2**. This means I could not always get the corret messages from Darryl's station. It's incredibly frustrating because there's no way around that. I realize in this course there are "errors" with the robot, and the Tracker finding the robot. That's understandable because there exists noise, and the solution is to either smooth data or work around by asking again, etc. But these errors, as far as I can tell, are just bugs in the Tracker which I can neither fix (we're not allowed to modify the Tracker) or work around (how am I to reliably get messages from another tracker when the only software interface to accomplish this task is broken? My robot has absolutely **no way** to work around this problem and this should not be penialized).

The code is all below in the "Software" section.

Problems Encountered
--------------------

The biggest problem we encountered was a lack of time and coordination. Not only were we limited for lab time, but of course, as students we each had our share of other assignments which ate away at our time in the lab, too.

We also lacked time when actually running the competition itself. Using our super simple lockstep method, of course we didn't have enough time to complete the whole task in the 15 minutes allotted. My robot didn't even get a chance to perform his task before the time ran out.

My plan was to get our task completed and then optimize it where we could so we'd get more done in the 15 minutes, while keeping things as simple as possible. It was hard enough just getting the SIMPLE version of our robots working, let alone trying to do anything complicated like interacting all three robots at the same time. I've learned from my other programming courses that Simple is the best, so we wanted something as elegant as possible. In the end, it didn't prove fast enough. Given more time for us to prepare, I'm convinced we could have tweaked our solution enough to finish within the 15 minutes we had to do our task.

As for my Robot, I had several problems, though thankfully most of them were minor. The biggest problem was again timing. I did my best to take my time and *think* out all my code as best I could before writing it. I drew out my finite state machine on paper so I could visualize exactly what the robot had to do. I took my time and care writing the Planner code to follow this.

Originally, I had all my logic in the `receivedPoseFromTracker()` method, but I found when sending responses to the robot, the RBC would crap out on me and I'd lose messages, so my robot would miss my commands and end up spinning or moving forever and that was very frustrating.

Instead, I re-wrote my code to only send a command to the robot exactly when he had explicitly asked for a new command, by doing this in the `receivedDataFromRobot()` method. This simplified things even more, and made sure the robot never missed a command. But this a long time to re-write properly.

As for other hardware problems, the biggest problem was getting the robot to properly detect blocks. In the end, I don't feel like I had an entirely satisfactory solution, but it worked well enough. The best I could do was to aim/orient my robot to be exactly in line with the Goal location for the block (this goal was to be given to me by another Tracker Station). This helped by giving the robot better odds at being directly aligned with the block, and thus much more likely to detect it. Before adding the orientation code, my robot would very much likely not find the block at all.

The Tracker was also a source of much agony throughout the project, having to constantly manually reload the Planner code (suggestion: update the Tracker so that it remembers the last file loaded, and have it poll to see if the modification date on that file changes -- reloading it automatically if so. That probably would have given me an extra hour or two onto my life which has otherwise been wasted constantly reloading the same class over and over again!). It also had difficulty trying to locate the robot when updating poses. This meant at times my robot would sit idle for many seconds, as the Tracker waited for a new pose to arrive to better command the robot with. If the robot was in a darker area of the map, this would severly slow it down.

If I Could Turn Back Time
-------------------------

I'm confident in my code and the solutions I've found to most of these problems. They're all on the right track. All I could do better would be to have many, many more hours given to me for lab time, and having no other course work to eat up the rest of my time. I believe I had good solutions, but they needed tweaking, and that needed more time.

Also, because I was trying to coordinate with two other members, and because we each had our share of troubles, we never were able to fully test the complete running solution to the code. By the time I left on the last night of lab time, one of my team members was still writing code... he would not have a solution until nearly the end of the night! That wasn't possible to test against. It was a problem of coordination, as is to be expected in a project like this.

We could have improved our team approach again by tweaking things. My robot could have moved faster and with better paths. We also could have introduced very simple *concurrency* among the robots to make more efficient use of our time, but again, we didn't want to do this until we had the simple base-case working first.

Robot Hardware Problems
-----------------------

Most of the hardware problems I encountered were already described above. The encoders were never once correct for me, but to overcome this I just had to tweak them every single time I came into the lab. Frustrating, but I dealt with it.

The RBC was also flakey at best for me through the whole term. It's extremely easy to swamp it with data and have it essentially crash on you.

In the end, I found it was easiest if I did the least amount of work possible on the Robot. Spin code is attrocious and nearly impossble to debug. I found things were simpler if I did my work on the Planner code and simply made the robot do its biddings.

Video of the Robot
------------------

The video of my robot doing its task can be found here: https://vimeo.com/40411292 I ended up hard-coding values into the planner in order to record this video. You can see this happening in the constructor of my Planner code. I spin off a thread which waits for a few seconds. When the thread fires, it sends the Planner messages, simulating messages coming from the other Trackers about the positions of Blocks being dropped off.

I do it this way to best simulate what would actually happen if interacting with the other robots. While the thread is paused, the robot is actually running, and he's in the StartState, so he waits until after he's got all the blocks announced and ready to be dealt with. Then he starts to move.

While recording this video, after 2 complete cycles the robot dies (another random hardware problem???) but I ended the video there. There was another team coming in after me and I didn't have a chance to make another recording. As you'll see in the code, the robot would have continued to do 2 more cycles before completing his task, and they'd look identical to what's shown in the video.

Software
--------