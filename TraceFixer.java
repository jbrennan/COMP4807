import java.io.*;

public class TraceFixer {
	
	public static void main(String[] args) {
		TraceFixer fixer = new TraceFixer(args[0], args[1]);
		fixer.fix();
	}
	
	private String followFile;
	private String goalFollow;
	
	private String followValues;
	private String goalValues;
	
	private String mergedValues;
	
	public TraceFixer(String follow, String goal) {
		this.followFile = follow;
		this.goalFollow = goal;
		
		// Read in the values from the files
		mergedValues = "";
		readInFile(follow, followValues, true);
		readInFile(goal, goalValues, false);
		
		System.out.println(mergedValues);
		
	}
	
	
	public void fix() {
		String merge = followValues; // to start
		//String headerless = goalValues.substring("x,y,angle,Dirrs+|0|-3|0|0.05|3,Sonar|0|4|0|0.10|19".length());
		//System.out.println(headerless);
		
		// Save the string as a file.
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter("merged_trace.trc"));
			writer.write(mergedValues);
	
		} catch (IOException e) {
			
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				
			}
		}
	}
	
	
	private void readInFile(String fileName, String outputString, boolean firstFile) {
		try{
			// Open the file that is the first 
			// command line parameter
			FileInputStream fstream = new FileInputStream(fileName);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			
		  
			String strLine = br.readLine();
			boolean firstLine = true;
			
			while (strLine != null)   {
				if (firstLine && !firstFile) {
					System.out.println("Skipping this line: " + strLine);
					firstLine = false;
				} else {
					mergedValues += strLine + "\n";
				}
				
				//System.out.println(strLine);
				strLine = br.readLine();
				
			}
			//Close the input stream
			//System.out.println(outputString);
			in.close();
			} catch (Exception e){
				//Catch exception if any
				System.err.println("Error: " + e.getMessage());
		}
	}
	
	
}