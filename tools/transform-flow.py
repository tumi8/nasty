#!/usr/bin/python
# -*- coding: utf-8 -*-
#Programm zur Transformation mit Flow-Tools oder nfdump erzeugter Netzverkehrsdaten in mySQL-Tabellen
#erstellt am: 10.7.2006
#letzte Aenderung am 26.06.2009
#erstellt von: Manuel Meiborg (manuel.meiborg@gmx.de)
#verbessert von: Dominik Brettnacher
#verbessert von: Gerhard Muenz
#Version 2.2
#
#Dieses Script arbeitet auf den ASCII-Format Ausgaben der netflow-Tools. Die einzelnen Verkehrsdaten werden angepasst und entsprechend
#ihrer Zeitstempel in 11 spaltige MySQL-Tabellen geschrieben, die mit den tools aggregator.py und insert.py weiterbearbeitet werden koennen

import MySQLdb, getopt, os, sys, time, glob

# convert IP address string into integer
def ipZuZahl(adresse):
    adresse=adresse.split('.')
    return 2**24*long(adresse[0])+2**16*long(adresse[1])+2**8*long(adresse[2])+long(adresse[3])

def transform(files, user, host, password, database, format, exporter, filter, tmpfile, bin):
    # get exporter table index for given exporter IP address
    # create new entry in exporter table if exporter IP address does not exist
    def exporterID(expAddr):
	if(not exporters.has_key(expAddr)):
	    c.execute('INSERT INTO exporter (srcIP) VALUES (%s)', (expAddr))
	    c.execute('SELECT LAST_INSERT_ID()')
	    exporters[expAddr] = int(c.fetchone()[0])
	return exporters[expAddr]

    # lock flow tables
    def locktables():
       c.execute('LOCK TABLES ' + ','.join(map (lambda x: x + ' WRITE', tables)))

    # create new flow table
    def createtable(t):
	if(t not in tables):
	    c.execute('''CREATE TABLE ''' + t + '''( \
		srcIP integer(10) unsigned, \
		dstIP integer(10) unsigned, \
		srcPort smallint (5) unsigned, \
		dstPort smallint (5) unsigned, \
		proto tinyint (3) unsigned, \
		dstTos tinyint (3) unsigned, \
		bytes bigint (20) unsigned, \
		pkts bigint (20) unsigned,  \
		firstSwitched integer (10) unsigned, \
		lastSwitched integer (10) unsigned, \
		firstSwitchedMilli smallint (5) unsigned, \
		lastSwitchedMilli smallint (5) unsigned, \
		exporterID smallint (5) unsigned)''')
	    tables.append(t)
	    #locktables()

    # connect to database
    try:
	connection = MySQLdb.connect(host,user,password,database)
    except MySQLdb.OperationalError, message:  
	print ('%d: Could not connect to database: %s' % (message[0], message[1]))
	return

    # create temporary file
    if os.access(tmpfile, os.F_OK):
	print("Cannot create temporary file "+tmpfile+". File exists!")
	exit()
    print("Using temporary file "+tmpfile)
    tmpfilehandle = open(tmpfile,'w')

    c = connection.cursor()
    c.execute('''CREATE TABLE IF NOT EXISTS `exporter` ( \
	 `id` smallint(6) NOT NULL auto_increment, \
	 `sourceID` int(10) unsigned default NULL, \
	 `srcIP` int(10) unsigned default NULL, \
	 PRIMARY KEY  (`id`))''')

    # read all existing exporters
    exporters = {}
    c.execute('''SELECT srcIP,id FROM exporter''')
    for row in c.fetchall():
	exporters[int(row[0])] = int(row[1])

    # read all existing table names from database
    tables = []
    c.execute('''SHOW TABLES''')
    for row in c.fetchall():
	tables.append(row[0])

    #locktables()

    old_tablename=''
	   
    numberofrecords = 0;
    # for all files
    for files2 in files:
	for file in glob.glob(files2):
	    print("Processing "+file)
	    if bin:
		# open input pipe
		if format == 1:
		    if filter != '':
		    	handle = os.popen('flow-cat '+file+' | '+filter+' | flow-export -f2')
		    else:
		    	handle = os.popen('flow-export -f2 < '+file)
		    # skip first line
		    kopf = handle.readline()
		elif format ==2:
		    handle = os.popen('nfdump -q -o pipe -r ' + file)
	    else:
		handle = open(file,'r')

	    # read in records from file or pipe
	    for line in handle:
		if format == 1:
		# flow-tools format
#  0 unix_secs (export time in UNIX seconds)
#  1 unix_nsecs (residual nanoseconds)
#  2 sysuptime (export time in milliseconds from device uptime)
#  3 exaddr (exporter IP address)
#  4 dpkts
#  5 doctets
#  6 first (flow start in milliseconds from device uptime)
#  7 last (flow end in milliseconds from device uptime)
#  8 engine_type
#  9 engine_id
# 10 srcaddr
# 11 dstaddr
# 12 nexthop
# 13 input
# 14 output
# 15 srcport
# 16 dstport
# 17 prot
# 18 tos
# 19 tcp_flags
# 20 src_mask
# 21 dst_mask
# 22 src_as
# 23 dst_as
# example:
#1157476947,457,1029495428,212.88.128.35,7,1028,1029406775,1029407306,0,0,217.227.213.44,212.88.157.230,0.0.0.0,0,255,2740,80,6,0,27,0,0,9063,3320
		    zeile = line.split(',')
		    # unix sysup time = unix export time - sysup duration
		    unixSysUpTimeMilliSeconds = int(zeile[0])*1000 + int(zeile[1])//1000000 - int(zeile[2])
		    # unix timestamp = unix sysup time + relative timestamp
		    unixFirstSwitchedMilliSeconds = unixSysUpTimeMilliSeconds + int(zeile[6])
		    unixLastSwitchedMilliSeconds = unixSysUpTimeMilliSeconds + int(zeile[7])
		    firstSwitched = unixFirstSwitchedMilliSeconds // 1000
		    firstSwitchedMilli = unixFirstSwitchedMilliSeconds % 1000
		    lastSwitched = unixLastSwitchedMilliSeconds // 1000
		    lastSwitchedMilli = unixLastSwitchedMilliSeconds % 1000
		    if exporter != -1:
			 expid = exporterID(exporter)
		    else:
			 expid = exporterID(ipZuZahl(zeile[3]))
		    values = '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' % \
			(ipZuZahl(zeile[10]),\
			 ipZuZahl(zeile[11]),\
			 int(zeile[15]),\
			 int(zeile[16]),\
			 int(zeile[17]),\
			 int(zeile[18]),\
			 int(zeile[5]),\
			 int(zeile[4]),\
			 firstSwitched,\
			 lastSwitched,\
			 firstSwitchedMilli,\
			 lastSwitchedMilli,\
			 expid)
		elif format == 2:
		# nfdump format
#  0 Address family  PF_INET or PF_INET6
#  1 Time first seen UNIX time seconds
#  2 msec first seen Mili seconds first seen
#  3 Time last seen  UNIX time seconds
#  4 msec last seen  Mili seconds first seen
#  5 Protocol	Protocol
#6-9 Src address     Src address as 4 consecutive 32bit numbers.
# 10 Src port	Src port
#11-14 Dst address     Dst address as 4 consecutive 32bit numbers.
# 15 Dst port	Dst port
# 16 Src AS	  Src AS number
# 17 Dst AS	  Dst AS number
# 18 Input IF	Input Interface
# 19 Output IF       Output Interface
# 20 TCP Flags       TCP Flags
#000001 FIN.
#000010 SYN
#000100 RESET
#001000 PUSH
#010000 ACK
#100000 URGENT
#e.g. 6 => SYN + RESET
# 21 Tos	     Type of Service
# 22 Packets	 Packets
# 23 Bytes	   Bytes
		    zeile = line.split('|')
		    if zeile[0] != "2":
			 print("Skipping non-IPv4 flow")
			 continue
		    firstSwitched = int(zeile[1])
		    values = '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' % \
			(int(zeile[9]),\
			 int(zeile[14]),\
			 int(zeile[10]),\
			 int(zeile[15]),\
			 int(zeile[5]),\
			 int(zeile[21]),\
			 int(zeile[23]),\
			 int(zeile[22]),\
			 firstSwitched,\
			 int(zeile[3]),\
			 int(zeile[2]),\
			 int(zeile[4]),\
			 exporterID(exporter))

		# determine table name
		startzeit = time.gmtime(firstSwitched)
		tablename = 'h_'
		tablename+=str(startzeit[0]) # year
		tablename+=str(startzeit[1]).rjust(2).replace(' ','0') # month
		tablename+=str(startzeit[2]).rjust(2).replace(' ','0') # day
		tablename+='_'+str(startzeit[3]).rjust(2).replace(' ','0') # hour
		if startzeit[4]<30: # half hour		    
		    tablename+='_0'
		else:
		    tablename+='_1'

		if (old_tablename != '' and tablename != old_tablename):
		    # import temporary file into database if table name has changed
		    tmpfilehandle.flush()
		    # load into mysql
		    createtable(old_tablename)
		    c.execute('LOAD DATA LOCAL INFILE "'+tmpfile+'" INTO TABLE ' + old_tablename + ' (srcIP, dstIP, srcPort, dstPort, proto, dstTos, bytes, pkts, firstSwitched, lastSwitched, firstSwitchedMilli, lastSwitchedMilli, exporterID)')
		    # clear temporary file
		    tmpfilehandle.seek(0)
		    tmpfilehandle.truncate()

		# write to temporary file
		tmpfilehandle.write(values)
		numberofrecords = numberofrecords + 1
			
		old_tablename = tablename
			
	    # close file or pipe
	    if(handle.close() != None):
		print("Error closing file or pipe!")
		tmpfilehandle.close()
		os.remove(tmpfile)
		exit()
	    print("Current number of records: "+str(numberofrecords))

    # import temporary file into database before deleting it
    tmpfilehandle.flush()
    createtable(old_tablename)
    c.execute('LOAD DATA LOCAL INFILE "'+tmpfile+'" INTO TABLE ' + old_tablename + ' (srcIP, dstIP, srcPort, dstPort, proto, dstTos, bytes, pkts, firstSwitched, lastSwitched, firstSwitchedMilli, lastSwitchedMilli, exporterID)')

    tmpfilehandle.close()
    os.remove(tmpfile)

    return numberofrecords


def usage():
    print ('''Usage: ''' + sys.argv[0] + ''' -d <database> [options] <file> ...
	Options:
	-d, --database=		database name
	-h, --host=		host name
	-u, --user=		user name
	-p, --password=		password
	-f, --format=		file format: ft=flow-tools, nf=nfdump (default: ft)
	-e, --exporter=		exporter IP address (default: flow-tools: value from file, nfdump: 0.0.0.0)
	-t, --tempfile=		temporary file name (default: /tmp/transform-flows.tmp)
	-b, --binary		binary input (requires flow-tools or nfdump)
        -F, --filter=           optional flow-tools in the pipe between flow-cat and flow-export,
                                only works for file format "ft" and binary input
                                (requries flow-tools, see flow-tools man pages)''')

def main():
    start=(time.time())

    user=os.environ['LOGNAME']
    host='localhost'
    password=''
    database=''
    tmpfile='/tmp/transform-flows.tmp'
    format=1 # flow-tools
    filter=''
    exporter=-1
    bin=False

    try:
	opts, args = getopt.gnu_getopt(sys.argv[1:], "u:h:p:d:f:e:t:bF:", ["user=", "host=", "password=", "database=","format=","exporter=","tempfile=","binary","filter="])
    except getopt.GetoptError:
	print "Invalid option."
	usage()
	sys.exit(2)

    for o, a in opts:
	if o in ("-b", "--binary"):
	    bin=True
	if o in ("-u", "--user"):
	    user=a
	if o in ("-h", "--host"):
	    host=a
	if o in ("-p", "--password"):
	    password=a
	if o in ("-f", "--format"):
	    if a=="nf":
		format=2 # nfdump
	if o in ("-e", "--exporter"):
	    exporter=ipZuZahl(a)
	if o in ("-t", "--tempfile"):
	    tmpfile=a
	if o in ("-d", "--database"):
	    database=a
	if o in ("-F", "--filter"):
	    filter=a

    if exporter == -1 and format == 2:
	# nfdump requires exporter value
	exporter = 0

    if filter != '' and (format != 1 or bin == False):
	print('error: filter requires flow-tools format and binary input')
	exit()

    if (database and len(args)):
	i = transform(args, user, host, password, database, format, exporter, filter, tmpfile, bin)
	print(str(i)+" records imported")
	print("Run time: "+str(int(time.time()-start))+" seconds")
    else:
	usage()

if __name__ == "__main__":
    main()

