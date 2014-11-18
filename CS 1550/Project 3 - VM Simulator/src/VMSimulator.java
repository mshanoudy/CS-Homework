import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;

public class VMSimulator
{
    private ArrayList<Integer>   pageNumbers; // The reference string
    private ArrayList<Character> modeList;    // List of read/write modes corresponding to pageNumbers
    private boolean[] modifiedBits;           // Modified bits table
    private boolean[] referencedBits;         // Reference bits table
    private int[] frameTable;                 // Frame table
    private int[] pageDistanceTable;          // Keeps track of a page's distance from its next memory access

    private int totalEmptyFrames;// Number of empty frames available
    private int emptyFrameIndex; // Current index of the empty frame
    private int totalFrames;     // Total number of frames
    private int frameIndex;      // Index of current frame
    private int refStringSize;   // Length of the reference string
    private int refresh;         // Refresh interrupt threshold for NRU
    private int clockIndex;      // Clock algorithm "hand"

    private int pageHits;         // Number of page hits
    private int pageFaultsNoSwap; // Number of pages loaded into empty frames
    private int pageFaultsClean;  // Number of pages swapped clean
    private int pageFaultsDirty;  // Number of pages swapped dirty
    private int totalReads;       // Total number of memory reads
    private int totalWrites;      // Total number of memory writes
    private int totalAccesses;    // Total number of memory accesses

    private Random random;

    /**
     * Constructor
     * @param numberOfFrames The number of frames in memory
     * @param refreshRate The refresh rate for the NRU simulation
     * @param filename The trace file name
     * @throws Exception
     */
    public VMSimulator(int numberOfFrames, int refreshRate, String filename) throws Exception
    {
        pageNumbers       = new ArrayList<Integer>();
        modeList          = new ArrayList<Character>();
        modifiedBits      = new boolean[numberOfFrames];
        referencedBits    = new boolean[numberOfFrames];
        frameTable        = new int[numberOfFrames];
        pageDistanceTable = new int[numberOfFrames];
        random            = new Random();
        totalEmptyFrames  = numberOfFrames;
        totalFrames       = numberOfFrames;
        refresh           = refreshRate;
        emptyFrameIndex   = 0;
        frameIndex        = 0;
        clockIndex        = 0;
        pageHits          = 0;
        pageFaultsNoSwap  = 0;
        pageFaultsClean   = 0;
        pageFaultsDirty   = 0;
        totalReads        = 0;
        totalWrites       = 0;
        totalAccesses     = 0;

        buildTables(filename);

        refStringSize = pageNumbers.size();
    }

    /**
     * Runs the selected algorithm
     * @param algorithm The algorithm
     */
    public void runAlgorithm(String algorithm)
    {
        if (algorithm.equals("opt"))
            runOptimal();
        else if (algorithm.equals("clock"))
            runClock();
        else if (algorithm.equals("nru"))
            runNRU();
        else if (algorithm.equals("rand"))
            runRandom();

        printResults();
    }

    /**
     * Prints the results of the simulation
     */
    public void printResults()
    {
        printDividerLine(31);
        System.out.printf(" %s%n", "SUMMARY STATISTICS");
        printDividerLine(31);
        System.out.printf("| %18s %-8d |%n", "Number of Frames:", totalFrames);
        System.out.printf("| %18s %-8d |%n", "Total Page Hits:", pageHits);

        printDividerLine(31);
        System.out.printf(" %s%n", "PAGE FAULTS");
        printDividerLine(31);
        System.out.printf("| %18s %-8d |%n", "No Eviction:", pageFaultsNoSwap);
        System.out.printf("| %18s %-8d |%n", "Evict Clean:", pageFaultsClean);
        System.out.printf("| %18s %-8d |%n", "Evict Dirty:", pageFaultsDirty);
        int totalPageFaults = pageFaultsNoSwap + pageFaultsClean + pageFaultsDirty;
        System.out.printf("| %18s %-8d |%n", "Total:", totalPageFaults);

        printDividerLine(31);
        System.out.printf(" %s%n", "MEMORY ACCESSES");
        printDividerLine(31);
        System.out.printf("| %18s %-8d |%n", "Reads:", totalReads);
        System.out.printf("| %18s %-8d |%n", "Writes:", totalWrites);
        System.out.printf("| %18s %-8d |%n", "Total:", totalAccesses);
        printDividerLine(31);
    }

    private void printDividerLine(int width)
    {
        for (int x = 0; x < width; x++)
            System.out.print("-");
        System.out.print("\n");
    }

    /**
     * Runs the Optimal Page-Replacement Algorithm
     */
    private void runOptimal()
    {
        for (int pageIndex = 0; pageIndex < refStringSize; pageIndex++)
        {
            frameIndex = getPageIndex(pageNumbers.get(pageIndex));
            if (frameIndex >= 0)
                recordMemoryAccess(false, pageIndex, frameIndex);
            else
                optimalRoutine(pageIndex);

            updateDistanceTable(pageIndex);
        }
    }

    /**
     * Runs the Clock Page-Replacement Algorithm
     */
    private void runClock()
    {
        for (int pageIndex = 0; pageIndex < refStringSize; pageIndex++)
        {
            frameIndex = getPageIndex(pageNumbers.get(pageIndex));
            if (frameIndex >= 0)
                recordMemoryAccess(false, pageIndex, frameIndex);
            else if (isEmptyFrame())
                loadIntoEmptyFrame(pageIndex);
            else
                clockRoutine(pageIndex);
        }
    }

    /**
     * Runs the Not Recently Used Page-Replacement Algorithm
     */
    private void runNRU()
    {
        for (int pageIndex = 0; pageIndex < refStringSize; pageIndex++)
        {
            if (pageIndex % refresh == 0) // Refresh reference bits according to refresh rate
                refreshReferenceBits();

            frameIndex = getPageIndex(pageNumbers.get(pageIndex));
            if (frameIndex >= 0)
                recordMemoryAccess(false, pageIndex, frameIndex);
            else if (isEmptyFrame())
                loadIntoEmptyFrame(pageIndex);
            else
                notRecentlyUsedRoutine(pageIndex);
        }
    }

    /**
     * Runs the Random Page-Replacement Algorithm
     */
    private void runRandom()
    {
        for (int pageIndex = 0; pageIndex < refStringSize; pageIndex++)
        {
            frameIndex = getPageIndex(pageNumbers.get(pageIndex));
            if (frameIndex >= 0)
                recordMemoryAccess(false, pageIndex, frameIndex);
            else if (isEmptyFrame())
                loadIntoEmptyFrame(pageIndex);
            else
                randomRoutine(pageIndex);
        }
    }

    /**
     * Handles page faults for runOptimal()
     * @param pageIndex Index of the page number
     */
    private void optimalRoutine(int pageIndex)
    {
        if (isEmptyFrame())
        {// No page swap necessary
            setDistanceTableEntry(emptyFrameIndex, pageIndex);
            loadIntoEmptyFrame(pageIndex);
        }
        else
        {// Page swap necessary
            frameIndex = getIndexOfMaxDistance();
            loadIntoFrame(pageIndex);
            setDistanceTableEntry(frameIndex, pageIndex);
        }
    }

    /**
     * Sets the value for an individual entry in the page distance table
     * @param index The distance table index
     * @param pageIndex Index of the page number
     */
    private void setDistanceTableEntry(int index, int pageIndex)
    {
        pageDistanceTable[index] = findPageDistance(pageIndex);
    }

    /**
     * Updates the distance table
     * @param pageIndex Index of the page number
     */
    private void updateDistanceTable(int pageIndex)
    {
        for (int x = 0; x < totalFrames; x++)
        {
            if (pageDistanceTable[x] == 0)
                setDistanceTableEntry(x, pageIndex);
            else if (pageDistanceTable[x] > 0)
                pageDistanceTable[x]--;
            else
                pageDistanceTable[x] = -1;
        }
    }

    private int getIndexOfMaxDistance()
    {
        int maxIndex = 0;
        for (int x = 1; x < totalFrames; x++)
            if (pageDistanceTable[x] > pageDistanceTable[maxIndex])
                maxIndex = x;
        return maxIndex;
    }

    private int findPageDistance(int pageIndex)
    {
        int pageDistance = 1; // Set to one for loop
        int pageNumber = pageNumbers.get(pageIndex); // Current page number

        for (int x = pageIndex + 1; x < refStringSize; x++) // Start loop at the current page index + 1
        {
            if (pageNumber == pageNumbers.get(x))
                return pageDistance;
            else
                pageDistance++;
        }
        return pageDistance;
    }

    private void clockRoutine(int pageIndex)
    {
        while (true)
        {
            if (!referencedBits[clockIndex])
            {// Reference bit not set
                frameIndex = clockIndex;
                break;
            }
            referencedBits[clockIndex] = false;
            clockIndex = (clockIndex + 1) % totalFrames;
        }
        loadIntoFrame(pageIndex);
    }

    private void notRecentlyUsedRoutine(int pageIndex)
    {
        ArrayList<ArrayList<Integer>> pools = getClassPools();

        for (ArrayList<Integer> pool : pools)
            if (!pool.isEmpty())
            {
                frameIndex = pool.get(random.nextInt(pool.size()));
                break;
            }
        loadIntoFrame(pageIndex);
    }

    private ArrayList<ArrayList<Integer>> getClassPools()
    {
        ArrayList<Integer> pool_0 = new ArrayList<Integer>();
        ArrayList<Integer> pool_1 = new ArrayList<Integer>();
        ArrayList<Integer> pool_2 = new ArrayList<Integer>();
        ArrayList<Integer> pool_3 = new ArrayList<Integer>();

        for (int x = 0; x < totalFrames; x++)
        {// Separate loaded pages into four classes
            if (referencedBits[x] && modifiedBits[x])       // (1, 1)
                pool_3.add(x);
            else if (referencedBits[x] && !modifiedBits[x]) // (1, 0)
                pool_2.add(x);
            else if (!referencedBits[x] && modifiedBits[x]) // (0, 1)
                pool_1.add(x);
            else if (!referencedBits[x] && !modifiedBits[x])// (0, 0)
                pool_0.add(x);
        }

        ArrayList<ArrayList<Integer>> pools = new ArrayList<ArrayList<Integer>>();
        pools.add(pool_0);
        pools.add(pool_1);
        pools.add(pool_2);
        pools.add(pool_3);

        return pools;
    }

    private void randomRoutine(int pageIndex)
    {
        frameIndex = random.nextInt(totalFrames);
        loadIntoFrame(pageIndex);
    }

    private void loadIntoFrame(int pageIndex)
    {
        frameTable[frameIndex] = pageNumbers.get(pageIndex);
        recordMemoryAccess(true, pageIndex, frameIndex);
    }

    private void loadIntoEmptyFrame(int pageIndex)
    {
        frameTable[emptyFrameIndex] = pageNumbers.get(pageIndex);
        recordMemoryAccess(true, pageIndex, emptyFrameIndex);
        emptyFrameIndex++;
        totalEmptyFrames--;
    }

    private void recordMemoryAccess(boolean pageFault, int pageIndex, int frameIndex)
    {
        totalAccesses++;
        referencedBits[frameIndex] = true;  // Set reference bit

        switch (modeList.get(pageIndex))
        {// Read/Writes
            case 'R':
                totalReads++;
                break;
            case 'W':
                totalWrites++;
                modifiedBits[frameIndex] = true; // Set dirty bit
                break;
            default:
                break;
        }
        if (pageFault)
        {// Type of page fault
            if (totalEmptyFrames > 0)
                pageFaultsNoSwap++;
            else if (modifiedBits[frameIndex])
                pageFaultsDirty++;
            else
                pageFaultsClean++;
        }// No fault, page hit
        else
            pageHits++;
    }

    private void refreshReferenceBits()
    {
        for (int x = 0; x < totalFrames; x++)
            referencedBits[x] = false;
    }

    private int getPageIndex(int pageNumber)
    {
        for (int x = 0; x < totalFrames; x++)
            if (frameTable[x] == pageNumber)
                return x;
        return -1;
    }

    private boolean isEmptyFrame()
    {
        return totalEmptyFrames > 0;
    }

    private void buildTables(String filename) throws Exception
    {
        // Build pageNumbers and modeList
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line, splitLine[];
        while ((line = br.readLine()) != null)
        {
            splitLine = line.split("\\s+");
            pageNumbers.add(calculatePageNumber(splitLine[0]));
            modeList.add(splitLine[1].charAt(0));
        }
        br.close();

        // populate frameTable, modifiedBits, and referencedBits
        for (int x = 0; x < totalFrames; x++)
        {
            frameTable[x]        = -1;
            pageDistanceTable[x] = -1;
            modifiedBits[x]      = false;
            referencedBits[x]    = false;
        }
    }

    private int calculatePageNumber(String logicalAddress)
    {
        return Integer.parseInt(logicalAddress.substring(0, 5), 16);
    }
}
