import java.math.*;

class TestMath {
	public static void main(String[] args) {
		double atan = Math.atan2(Double.parseDouble(args[0]), Double.parseDouble(args[1]));
		System.out.println(Math.toDegrees(atan));
	}
}