import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class RNGenerator
{
    private final int SEED = 123456789;

    private ArrayList<Double> numberList;
    private int generationAmount;

    public RNGenerator()
    {
        this(10000);
    }

    public RNGenerator(int generationAmount)
    {
        numberList = new ArrayList<>();
        this.generationAmount = generationAmount;
    }

    /**
     * Standard Java RNG which stores the stream of normalized random numbers to numberList
     */
    public void standardGenerator()
    {
        Random random = new Random(SEED);

        for (int x = 0; x < generationAmount; x++)
            numberList.add(random.nextDouble());
    }

    /**
     * Linear congruential generator which stores the stream of normalized random numbers to numberList
     * LCG equation: Xi+1 = ((a * Xi) + c) mod m
     * @param a A chosen constant
     * @param c A chosen constant
     * @param m A chosen constant
     */
    public void LCG(double a, double c, double m)
    {
        double n;

        // X0
        numberList.add((SEED / m));
        n = SEED;

        for (int x = 1; x < generationAmount; x++)
        {
            n = ((a * n) + c) % m;
            numberList.add(n / m);
        }
    }

    /**
     * Saves the random number stream to a file
     * @param filename The file name
     */
    public void saveToFile(String filename)
    {
        if (numberList.isEmpty())
            System.out.println("Please run standardGenerator() or LCG() to populate the number list before saving");
        else
            try
            {
                Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"));
                for (Double number : numberList)
                    writer.write(number + "\n");
                writer.close();
            } catch (IOException e)
            {
                System.out.println("Error with writing random number stream to file");
            }
    }

    public ArrayList<Double> getNumberList()
    {
        return numberList;
    }
}
