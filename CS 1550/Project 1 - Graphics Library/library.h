
#include <termios.h>

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
