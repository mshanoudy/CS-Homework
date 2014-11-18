#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/select.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <termios.h>
#include <sys/time.h>
#include <time.h>
#include <linux/fb.h>

typedef unsigned short color_t;

/* Functions */
void init_graphics();
void exit_graphics();
void clear_screen();
char getKey();
void sleep_ms(long ms);
void draw_pixel(int x, int y, color_t color);
void draw_rect(int x1, int y1, int width, int height, color_t c);

/* Variables */
int      fb_fd;                             // File descriptor for the framebuffer
long int fb_size;                           // Total size of the framebuffer
char    *fb_pointer;                        // Pointer to framebuffer in memory
int      tty_fd;                            // File descriptor for the terminal
struct   termios terminal;                  // Contains information about the terminal
int      pixel_size;                        // Size of the pixels

void init_graphics()
{
    struct   fb_var_screeninfo var_screeninfo;  // Contains the virtual resolution
    struct   fb_fix_screeninfo fix_screeninfo;  // Contains the bit depth

    // Open the framebuffer
    fb_fd = open("/dev/fb0", O_RDWR);

    // Get framebuffer size
    ioctl(fb_fd, FBIOGET_VSCREENINFO, &var_screeninfo);
    ioctl(fb_fd, FBIOGET_FSCREENINFO, &fix_screeninfo);

    fb_size    = var_screeninfo.yres_virtual * fix_screeninfo.line_length;
    pixel_size = var_screeninfo.bits_per_pixel / 8;

    // Map framebuffer to memory
    fb_pointer = (char *) mmap(0, fb_size, PROT_READ | PROT_WRITE, MAP_SHARED, fb_fd, 0);
    
    // Open the terminal
    tty_fd = open("/dev/tty", O_RDWR);
    
    // Disable keypress echo and buffering
    ioctl(tty_fd, TCGETS, &terminal);
    terminal.c_lflag = terminal.c_lflag ^ (ICANON | ECHO);
    ioctl(tty_fd, TCSETS, &terminal);
}

void exit_graphics()
{
    // Reenable key press echo and buffering
    ioctl(tty_fd, TCGETS, &terminal);
    terminal.c_lflag = terminal.c_lflag ^ (ICANON | ECHO);
    ioctl(tty_fd, TCSETS, &terminal);

    munmap(fb_pointer, fb_size);
    close(fb_fd);
    close(tty_fd);
}

void clear_screen()
{
    char escape_sequence[8] = "\033[2J";
    write(tty_fd, &escape_sequence, sizeof(escape_sequence));
}


char getKey()
{
    fd_set rfds;           // Create file descriptor set for select
    FD_ZERO(&rfds);        // Clear set
    FD_SET(tty_fd, &rfds); // Add terminal file discriptor to set

    int keypress = select(tty_fd+1, &rfds, NULL, NULL, NULL);   
    if (keypress > -1)
    {
        char key;
        read(tty_fd, &key, sizeof(key));
        return key;
    }
    else
        return NULL;
} 

void sleep_ms(long ms)
{
    // Set the timespec struct to pass to nanosleep
    struct timespec time;
    time.tv_sec  = 0;
    time.tv_nsec = ms * 1000000;   

    nanosleep(&time, NULL);
}

void draw_pixel(int x, int y, color_t color)
{
    long int offset = (((y % 480) * 640) + (x % 640)) * pixel_size;

    char *pixel_pointer = (int)fb_pointer + offset;
    *pixel_pointer = color;
}

void draw_rect(int x1, int y1, int width, int height, color_t c)
{
    int x2, y2;
    for (y2 = 0; y2 < height; y2++)
        for (x2 = 0; x2 < width; x2++)
            draw_pixel((x1 + x2), (y1 + y2), c); 
}
