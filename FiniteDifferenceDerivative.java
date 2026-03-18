public class FiniteDifferenceDerivative {

    public static double f(double x) {
        return x * x;   
    }
    public static void main(String[] args) {

        double x = 2.0;   
        double h = 0.0001; 

        double derivative = (f(x + h) - f(x - h)) / (2 * h);

        System.out.println("Derivative at x = " + x + " is approximately: " + derivative);
    }
}
