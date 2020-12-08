set terminal postscript landscape
set nolabel
set xlabel "window"
set xrange [0:30]
set ylabel "usec"
set yrange [0:8000000]
set output "udp.ps"
plot "data.dat" title "1gbps slinding window" with linespoints, 7559061 title "1gbps stopNwait" with line
