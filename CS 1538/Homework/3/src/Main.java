
public class Main
{
    public static void main(String args[])
    {
        /* For Testing */
        VariateGenerator variateGenerator = new VariateGenerator();

        System.out.println("Inverse Transform");
        variateGenerator.setFilename("InverseTransform.txt");
        variateGenerator.inverseTransformMethod();

        System.out.println("Accept/Reject - Standard");
        variateGenerator.setFilename("AcceptReject1.txt");
        variateGenerator.acceptRejectMethod();

        System.out.println("Accept/Reject - Optimized");
        variateGenerator.setFilename("AcceptReject2.txt");
        variateGenerator.acceptRejectMethod2();

        System.out.println("Polar-Coordinate");
        variateGenerator.setFilename("PolarCoordinate.txt");
        variateGenerator.polarCoordinateMethod();
    }
}
