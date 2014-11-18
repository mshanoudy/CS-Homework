#include "library.h"

int main(int argc, char** argv)
{
    init_graphics();
    clear_screen();

    char key;

    color_t color = 31;

    int width  = 10;
    int height = 10;
    
    int  x = (640 - 20) / 2;
    int  y = (480 - 20) / 2;

    draw_rect(x, y, width, height, color);

    do
    {
        key = getKey();
        if      (key == '1') { height = 10; width = 10; }
        else if (key == '2') { height = 20; width = 20; }
        else if (key == '3') { height = 30; width = 30; }
        else if (key == '4') { height = 40; width = 40; }
        else if (key == '5') { height = 50; width = 50; }
        else if (key == '6') { height = 60; width = 60; }
        else if (key == '7') { height = 70; width = 70; }
        else if (key == '8') { height = 80; width = 80; }
        else if (key == '9') { height = 90; width = 90; }
        else if (key == 'w') y -= 10;
        else if (key == 's') y += 10;
        else if (key == 'a') x -= 10;
        else if (key == 'd') x += 10;

        clear_screen();
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > 638) x = 638;
        if (y > 478) y = 478;
        draw_rect(x, y, width, height, color);
        sleep_ms(20);
    } while (key != 'q');

    exit_graphics();

    return 0;
} 
