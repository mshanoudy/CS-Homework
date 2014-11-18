public class Main
{
    public static void main(String args[]) throws Exception
    {
        int numberOfFrames = 0, refreshRate = 0;
        String algorithmChoice = null, filename = null;

        // Read in command line arguments
        if ((args.length < 5) || (args.length > 7))
            System.out.println("Error: Correct amount of arguments required");
        else if (args.length == 5)
        {// No refresh rate
            numberOfFrames  = Integer.parseInt(args[1]);
            algorithmChoice = args[3];
            filename        = args[4];
        }
        else if (args.length == 7)
        {// With refresh rate
            numberOfFrames  = Integer.parseInt(args[1]);
            algorithmChoice = args[3];
            refreshRate     = Integer.parseInt(args[5]);
            filename        = args[6];
        }
        else
            System.out.println("Something is wrong");

        // Start simulator
        VMSimulator vmSimulator = new VMSimulator(numberOfFrames, refreshRate, filename);
        vmSimulator.runAlgorithm(algorithmChoice);
    }
}
