#include <stdio.h>

typedef struct tm {
    int tm_sec;
    int tm_min;
    int tm_hour;
    int tm_mday;
    int tm_mon;
    int tm_year;
    int tm_wday;
    int tm_yday;
    int tm_isdst;
    /* Those are for future use. */
    int __tm_gmtoff__;
    __const char *__tm_zone__;
} tm;


void print_time(tm t) {
    printf("    int tm_sec%d\n    int tm_min%d\n    int tm_hour%d\n    int tm_mday%d\n    int tm_mon%d\n    int tm_year%d\n    int tm_wday%d\n    int tm_yday%d\n    int tm_isdst%d\n\n    long int __tm_gmtoff__%d\n    __const char *__tm_zone__%s\n\n",
	   t.tm_sec,
	   t.tm_min,
	   t.tm_hour,
	   t.tm_mday,
	   t.tm_mon,
	   t.tm_year,
	   t.tm_wday,
	   t.tm_yday,
	   t.tm_isdst,
	   t.__tm_gmtoff__,
	   t.__tm_zone__
    );

}
