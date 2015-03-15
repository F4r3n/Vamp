#!/usr/bin/python

import sys

from os import system, remove
def readFile(path):
	print path
	file = open(path,'r')
	content = file.read()
	values = [x.strip() for x in content.split(',')]
	out = open("out.dat",'w')
	index = 0
	for x in values:
		out.write(str(index)+" "+x+"\n")
		print(x)
		print(index)
		index = index + 1
	file.close()
	out.close()
	system("gnuplot -persist plot.gp")
	return

readFile(sys.argv[1])
