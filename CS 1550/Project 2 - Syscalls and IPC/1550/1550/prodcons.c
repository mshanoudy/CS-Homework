#include <unistd.h>
#include <sys/mman.h>
#include <stdio.h>
#include <sys/types.h>
#include <errno.h>
#include <linux/spinlock.h>

typedef cs1550_sem semaphore_t;

/* Constants */
const int  MIN_BUFFER_SIZE = 2;
const char ALPHABET[26]    = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
/* Functions */
void spawn_producers(int val);
void spawn_consumers(int val);
void producer_thread(char name);
void consumer_thread(char name);
void wait(semaphore_t *sem);
void signal(semaphore_t *sem);
/* Global Variables */
void        *memory_ptr;   // Pointer to start of mapped memory
int16_t     *int_buffer;   // Buffer to hold integers
semaphore_t  ints_sem;     // Semaphore that represents available integers
semaphore_t  space_sem;    // Semaphore that represents available buffer space
DEFINE_SPINLOCK(prod_lock);// Spinlock for producers
DEFINE_SPINLOCK(cons_lock);// Spinlock for consumers
int          in, out;      // Indexes for producers and consumers
int16_t      product;      // Integer produced by producer threads
int          n;            // The number of indexes available in the buffer

int main(int argc, char** argv)
{
    if (argc != 3)
    {// Check argument count
        printf("%s\n", "Incorrect number of arguments");
        return 0;
    }    
    if (argv[0] <= 0 | argv[1] <= 0 | argv[0] > 26 | argv[1] > 26)
    {// Check prod/cons arguments
        printf("%s\n", "Incorrect producer/consumer arguments");
        return 0;
    }

    int producer_count = argv[0]; // The number of producers
    int consumer_count = argv[1]; // The number of consumers
    int N              = argv[2]; // The size of the buffer in bytes

    if (N < 2)
        N = MIN_BUFFER_SIZE;
    n = N / 2;

    // Map the shared memory region
    *memory_ptr = mmap(NULL, N, PROT_READ|PROT_WRITE, MAP_SHARED|MAP_ANONYMOUS, 0, 0);

    // Initialize the integer buffer
    int16_t int_array[n];
    int_buffer = int_array;

    in = 0, out = 0;
    product = 0;

    // Initialize the semaphores
    ints_sem->value  = 0;
    ints_sem->head   = 0;
    ints_sem->tail   = 0;
    space_sem->value = n;
    space_sem->head  = 0;
    space_sem->tail  = 0;

    // Spawn producers and consumers
    spawn_producers(producer_count - 1);
    spawn_consumers(consumer_count - 1);
}

void spawn_producers(int val)
{
    pid_t pid;

    pid = fork();

    if (pid == -1)
    {// Error check the fork
        fprintf(stderr, "Can't fork, error %d\n", errno);
        exit(EXIT_FAILURE);
    }    
    else if (pid == 0)              // Child thread
        producer_thread(ALPHABET[val]);  
    else if (val >= 0)              // Parent thread
        spawn_producers(val - 1);
}

void producer_thread(char name)
{
    while (1)
    {
        wait(&space_sem);           // Decrement the amount of buffer space
        spin_lock(&prod_lock);      // Lock critical region
        int_buffer[in] = product;   // Write the product integer to the buffer
        printf("Producer %c Produced: %i\n", name, product);
        product++;                  // Increment the product integer
        in = (in + 1) % n;          // Increment the producer index
        spin_unlock(&prod_lock);    // Unlock critical region
        signal(&ints_sem);          // Increment the amount of available integers
    }
}

void spawn_consumers(int val)
{
    pid_t pid;

    pid = fork();

    if (pid == -1)
    {// Error check the fork
        fprintf(stderr, "Can't fork, error %d\n", errno);
        exit(EXIT_FAILURE);
    }    
    else if (pid == 0)              // Child thread
        consumer_thread(ALPHABET[val]);  
    else if (val >= 0)              // Parent thread
        spawn_consumers(val - 1);
}

void consumer_thread(char name)
{
    int16_t commodity; 

    while (1)
    {
        wait(&ints_sem);            // Decrement the amount of integers available for consumption
        spin_lock(&cons_lock);      // Lock critical region
        commodity = int_buffer[out];// Get the commodity integer from the buffer
        printf("Consumer %c Consumed: %i\n", name, commodity);
        out = (out + 1) % n;        // Increment the consumer index
        spin_unlock(&cons_lock);    // Unlock critical region
        signal(&space_sem);         // Increment the amount of buffer space
    }
}

void wait(semaphore_t *sem)
{
    syscall(__NR_cs1550_down, sem);
}

void signal(semaphore_t *sem)
{
    syscall(__NR_cs1550_up, sem);    
}
