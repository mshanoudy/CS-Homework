import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class StatisticalTester
{
    ArrayList<Double> numberList;

    public StatisticalTester(String filename)
    {
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null)
                numberList.add(Double.parseDouble(line));
            br.close();
        } catch (IOException e)
        {
            System.out.println("Error: Problem reading the file");
        }

    }

    public StatisticalTester(ArrayList<Double> numberList)
    {
        this.numberList = numberList;
    }

    public void chiSquareTest()
    {
        ArrayList<ArrayList<Double>> freqList = new ArrayList<ArrayList<Double>>();
        for (int x = 0; x < 10; x++)
            freqList.add(new ArrayList<Double>());

        double[] observed = new double[10];
        double   expected = numberList.size() / 10;

        // Separate numberList into numberArray representing the distributions
        for (Double number : numberList)
        {
            if (number >= 0 && number < 0.10)
                freqList.get(0).add(number);
            else if (number >= 0.10 && number < 0.20)
                freqList.get(1).add(number);
            else if (number >= 0.20 && number < 0.30)
                freqList.get(2).add(number);
            else if (number >= 0.30 && number < 0.40)
                freqList.get(3).add(number);
            else if (number >= 0.40 && number < 0.50)
                freqList.get(4).add(number);
            else if (number >= 0.50 && number < 0.60)
                freqList.get(5).add(number);
            else if (number >= 0.60 && number < 0.70)
                freqList.get(6).add(number);
            else if (number >= 0.70 && number < 0.80)
                freqList.get(7).add(number);
            else if (number >= 0.80 && number < 0.90)
                freqList.get(8).add(number);
            else
                freqList.get(9).add(number);
        }

        // Count the observed distributions
        for (int x = 0; x < 10; x++)
            observed[x] = freqList.get(x).size();

        // Perform Chi Square Test
        double C = 0;
        for (int x = 0; x < 10; x++)
            C += ((observed[x] - expected) * (observed[x] - expected)) / expected;
        if (C < 12.142) // 80%
            System.out.println("Accept the null hypothesis at 80% significance level: C = " + C + ", x^2 = 12.142");
        else
            System.out.println("Reject the null hypothesis at 80% significance level: C = " + C + ", x^2 = 12.142");
        if (C < 14.684) // 90%
            System.out.println("Accept the null hypothesis at 90% significance level: C = " + C + ", x^2 = 14.684");
        else
            System.out.println("Reject the null hypothesis at 90% significance level: C = " + C + ", x^2 = 14.684");
        if (C < 16.919) // 95%
            System.out.println("Accept the null hypothesis at 95% significance level: C = " + C + ", x^2 = 16.919");
        else
            System.out.println("Reject the null hypothesis at 95% significance level: C = " + C + ", x^2 = 16.919");
    }

    public void kolmogorovSmirnovTest()
    {
        double[] D_plus  = new double[100];
        double[] D_minus = new double[100];
        double N = 100;

        // Take first 100 numbers and sort
        double[] numberArray = new double[100];
        for (int x = 0; x < N; x++)
            numberArray[x] = numberList.get(x);
        Arrays.sort(numberArray);

        // Calculate D+ and D- and take max(D+, D-)
        double D = D_plus[0];
        for (int x = 0; x < N; x++)
        {
            D_plus[x]  = ((x + 1) / N) - numberArray[x];
            D_minus[x] = numberArray[x] - (x / N);

            if (D_plus[x] > D)
                D = D_plus[x];
            if (D_minus[x] > D)
                D = D_minus[x];
        }

        if (D < 0.107) // 80%
            System.out.println("Accept the null hypothesis at 80% significance level: D = " + D + ", D_alpha = 0.107");
        else
            System.out.println("Reject the null hypothesis at 80% significance level: D = " + D + ", D_alpha = 0.107");
        if (D < 0.122) // 90%
            System.out.println("Accept the null hypothesis at 90% significance level: D = " + D + ", D_alpha = 0.122");
        else
            System.out.println("Reject the null hypothesis at 90% significance level: D = " + D + ", D_alpha = 0.122");
        if (D < 0.136) // 95%
            System.out.println("Accept the null hypothesis at 95% significance level: D = " + D + ", D_alpha = 0.136");
        else
            System.out.println("Reject the null hypothesis at 95% significance level: D = " + D + ", D_alpha = 0.136");
    }

    public void runsTest()
    {
        // Find the observed mean
        double obvMean = 0;
        for (Double number : numberList)
            obvMean += number;
        obvMean = obvMean / numberList.size();

        // Mark the elements by + or - the mean
        boolean[] markers = new boolean[numberList.size()];
        for (int x = 0; x < markers.length; x++)
            markers[x] = numberList.get(x) > obvMean;

        // Count the number of runs
        double S_plus = 0, S_minus = 0, runs = 0;

        boolean lastMarker = markers[0];
        if (lastMarker)
            S_plus++;
        else
            S_minus++;
        runs++;
        for (int x = 1; x < markers.length; x++)
        {
            if (!(lastMarker == markers[x]))
            {// New run
                if (markers[x])
                    S_plus++;
                else
                    S_minus++;
                runs++;
                lastMarker = markers[x];
            }
            else
            {// Continue current run
                if (lastMarker)
                    S_plus++;
                else
                    S_minus++;
            }
        }

        // Calculate test statistic
        double S  = S_plus + S_minus;                       // S = S+ + S-
        double mu = ((2 * S_plus * S_minus) / S) + 1;       // mu = ((2 * S+ * S-) / S) + 1
        double variance = ((mu - 1) * (mu - 2)) / (S - 1);  // Var = (mu - 1)(mu - 2) / (S - 1)
        double Z  = (runs - mu) / Math.sqrt(variance);       // Z = (O - E) / Var^1/2

        System.out.println("Z score: " + Z);
    }

    public void autocorrelationsTest()
    {

    }
}
