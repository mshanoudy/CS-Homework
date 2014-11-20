import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class VariateGenerator
{
    private int listSize;
    private String filename;
    private Random random;

    public VariateGenerator()
    {
        this(10000, "results.txt");
    }

    public VariateGenerator(int listSize, String filename)
    {
        random = new Random();
        this.listSize = listSize;
        this.filename = filename;
    }

    /**
     * Performs the Inverse Transform Method on a random number list
     * Standard Normal CDF: F(x) = 1 / 1 + e^-1.702x
     * Inverse Function: F^-1(x) = -(ln((1/x) - 1) / 1.702)
     */
    public void inverseTransformMethod()
    {
        ArrayList<Double> variateList = new ArrayList<>();

        for (int x = 0; x < listSize; x++)
            variateList.add( -1 * ((Math.log((1 / random.nextDouble()) - 1 )) / 1.702 ));  // Apply inverse function

        saveNumbers(variateList);
    }

    public void acceptRejectMethod()
    {
        ArrayList<Double> variateList = new ArrayList<>();
    }

    public void polarCoordinateMethod()
    {

    }

    private void saveNumbers(ArrayList<Double> variateList)
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
}
