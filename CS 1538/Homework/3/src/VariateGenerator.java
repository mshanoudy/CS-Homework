import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class VariateGenerator
{
    private int listSize;
    private long startTime;
    private long endTime;
    private String filename;
    private Random random;
    private ArrayList<Double> variateList;

    public VariateGenerator()
    {
        this(10000, "results.txt");
    }

    public VariateGenerator(int listSize, String filename)
    {
        random = new Random();
        variateList = new ArrayList<>();
        this.listSize = listSize;
        this.filename = filename;
    }

    /**
     * Performs the Inverse Transform Method on a set of random numbers
     * Standard Normal CDF: F(x) = 1 / 1 + e^-1.702x
     * Inverse Function: F^-1(x) = -(ln((1/x) - 1) / 1.702)
     */
    public void inverseTransformMethod()
    {
        variateList.clear();

        startTimer();
        for (int x = 0; x < listSize; x++)
            variateList.add( -1 * ((Math.log((1 / random.nextDouble()) - 1 )) / 1.702 ));  // Apply inverse function
        endTimer();

        printTimeElapsed();
        saveNumbers();
    }

    /**
     * Performs the Accept/Reject Method on a set of random numbers
     * Exponential Distribution CDF: F(x; λ) 1 - exp(-λx), where λ = 1
     */
    public void acceptRejectMethod()
    {
        variateList.clear();

        double X, U;
        int successes = 0, failures = 0;

        startTimer();
        while (successes < listSize)
        {
            X = -Math.log(1 - random.nextDouble());         // Inverse transform of g(x): log(1 - u) / (−λ)
            U = random.nextDouble();                        // U[0,1)

            if (U <= Math.exp(-Math.pow((X - 1), 2) / 2))   // U <= exp(-(X - 1)^2 / 2)
            {
                successes++;
                variateList.add(X);
            }
            else
                failures++;
        }
        endTimer();

        printTimeElapsed();
        System.out.println("Failures: " + failures);
        saveNumbers();
    }

    /**
     * Same as acceptRejectMethod() except main test is implemented differently
     */
    public void acceptRejectMethod2()
    {
        variateList.clear();

        double X1, X2;
        int successes = 0, failures = 0;

        startTimer();
        while (successes < listSize)
        {
            X1 = -Math.log(1 - random.nextDouble());
            X2 = -Math.log(1 - random.nextDouble());

            if (X2 >= Math.pow((X1 - 1), 2) / 2) // X2 >= (X1 - 1)^2 / 2
            {
                successes++;
                variateList.add(X1);
            }
            else
                failures++;
        }
        endTimer();

        printTimeElapsed();
        System.out.println("Failures: " + failures);
        saveNumbers();
    }

    /**
     * Performs the Polar-Coordinate Method
     * Specifically, the Box-Muller Transform
     */
    public void polarCoordinateMethod()
    {
        variateList.clear();

        double V1, V2, U, Z1, Z2;
        int successes = 0;

        startTimer();
        while (successes < listSize)
        {
            V1 = nextDouble(-1, 1);                             // U[-1,1)
            V2 = nextDouble(-1, 1);                             // U[-1,1)
            U  = Math.pow(V1, 2) + Math.pow(V2, 2);             // U = V1^2 + V2^2

            if (U < 1)
            {
                successes += 2;
                Z1 = V1 * Math.sqrt((-2 * Math.log(U)) / U);    // Z1 = V1(-2 ln(U) / U)^1/2
                Z2 = V2 * Math.sqrt((-2 * Math.log(U)) / U);    // Z2 = V2(-2 ln(U) / U)^1/2
                variateList.add(Z1);
                variateList.add(Z2);
            }
        }
        endTimer();

        printTimeElapsed();
        saveNumbers();
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    /**
     * This method is implemented to be equivalent to doubles(Long.MAX_VALUE, randomNumberOrigin, randomNumberBound)
     * Taken from https://docs.oracle.com/javase/8/docs/api/java/util/Random.html#doubles-double-double-
     * @param origin The origin (inclusive) of each random value
     * @param bound The bound (exclusive) of each random value
     * @return A pseudorandom double value
     */
    private double nextDouble(double origin, double bound)
    {
        double r = random.nextDouble();
        r = r * (bound - origin) + origin;
        if (r >= bound) // Correct for rounding
            r = Math.nextDown(bound);
        return r;
    }

    private void saveNumbers()
    {
        try
        {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"));
            for (Double number : variateList)
                writer.write(number + "\n");
            writer.close();

        } catch (IOException e)
        {
            System.out.println("Error with writing random number stream to file");
        }
    }

    private void startTimer()
    {
        startTime = System.nanoTime();
    }

    private void endTimer()
    {
        endTime = System.nanoTime();
    }

    private void printTimeElapsed()
    {
        long elapsedTime = (endTime - startTime);
        System.out.println("Elapsed Time: " + elapsedTime);
    }
}
