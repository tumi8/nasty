#!/usr/bin/python
# -*- coding: utf-8 -*-
# Export von AggregateSet-Zeitreihen aus NASTY-Tabellen
# erstellt von Gerhard Muenz, August 2008

import MySQLdb, getopt, os, sys, string, calendar, time

def query_metrics(c, starttime, endtime, interval, addr, mask, port, proto):
        def ip2int(adresse):
        # konvertiert eine IP im dotted-quad Format in einen Integer
                adresse=adresse.split('.')
                return 2**24*long(adresse[0])+2**16*long(adresse[1])+2**8*long(adresse[2])+long(adresse
                        [3])

        def create_where_expression(addr, mask, port, proto):
                bitmask = ['0x00000000', '0x80000000', '0xC0000000', '0xE0000000', \
                        '0xF0000000', '0xF8000000', '0xFC000000', '0xFE000000', \
                        '0xFF000000', '0xFF800000', '0xFFC00000', '0xFFE00000', \
                        '0xFFF00000', '0xFFF80000', '0xFFFC0000', '0xFFFE0000', \
                        '0xFFFF0000', '0xFFFF8000', '0xFFFFC000', '0xFFFFE000', \
                        '0xFFFFF000', '0xFFFFF800', '0xFFFFFC00', '0xFFFFFE00', \
                        '0xFFFFFF00', '0xFFFFFF80', '0xFFFFFFC0', '0xFFFFFFE0', \
                        '0xFFFFFFF0', '0xFFFFFFF8', '0xFFFFFFFC', '0xFFFFFFFE']
                if ((addr=='' or mask==0) and port=='' and proto==''):
                        return ''
                needand = False
                result = ' WHERE ('
                if addr != '':
                        if ((mask > 0) and (mask < 32)):
                                result=result+'((srcIp & '+bitmask[mask]+')='+str(ip2int(addr))+' OR (dstIp & '+bitmask[mask]+')='+str(ip2int(addr))+')'
                                needand = True
                        elif mask == 32:
                                result=result+'(srcIp='+str(ip2int(addr))+' OR dstIp='+str(ip2int(addr))+')'
                                needand = True
                if port != '':
                        if needand:
                                result = result + ' AND '
                        result=result+'(srcPort='+port+' OR dstPort='+port+')'
                        needand = True
                if proto != '':
                        if needand:
                                result = result + ' AND '
                        result = result+'proto='+proto
                return result+')'
                
	def time2tables(starttime, endtime):
        # sucht relevante Halbstundentabellen
		tables = []
		c.execute("SHOW TABLES LIKE 'h\\_%'")
		for row in c.fetchall():
			tabletime = calendar.timegm([string.atoi(row[0][2:6]), string.atoi(row[0][6:8]), string.atoi(row[0][8:10]), \
				string.atoi(row[0][11:13]), string.atoi(row[0][14])*30, 0, 0, 0, 0])
			if ((endtime==0 or tabletime < endtime) and (starttime==0 or tabletime+30*60 > starttime)):
				tables.append(row[0])
                return tables

	tables = time2tables(starttime, endtime)
	
	if tables==[]:
		print('Keine Tabellen für '+str(starttime)+'<=firstSwitched<='+str(endtime)+' gefunden.')
		return

        filter = create_where_expression(addr, mask, port, proto)
	timecol = "(firstSwitched DIV "+str(interval)+")*"+str(interval)+" AS time"
	if starttime>0:
		if endtime>0:
			timecond = "HAVING time BETWEEN "+str(starttime)+" AND "+str(endtime)
		else:
			timecond = "HAVING time>="+str(starttime)
	elif endtime>0:
		timecond = "HAVING time<="+str(endtime)
	else:
		timecond = ''
			
	earliest = 0
	
        for table in tables:

		c.execute('SELECT '+timecol+', SUM(bytes), SUM(pkts), COUNT(*), (COUNT(DISTINCT dstIp) + COUNT(DISTINCT srcIp)), (COUNT(DISTINCT dstPort) + COUNT(DISTINCT srcPort)), AVG(lastSwitched-firstSwitched) FROM '+table+filter+' GROUP BY time '+timecond)

		for row in c.fetchall():
			if earliest == 0:
				# initialize interval counter
				i = 0
				earliest = row[0]
			else:
				# increase interval counter and fill in zero rows if necessary
				i = i + 1
				while i < (row[0]-earliest) / interval:
					print(str(earliest + i*interval) + '\t0\t0\t0\t0\t0\t0')
					i = i + 1
			# print current row
			print(str(row[0])+'\t'+str(row[1])+'\t'+str(row[2])+'\t'+str(row[3])+'\t'+str(row[4])+'\t'+str(row[5])+'\t'+str(row[6]))

        return


def usage():
    print ('''Benutzung: ''' + sys.argv[0] + ''' -d <database> [options]
        Datenbankoptionen:
        -d, --database=                         Name der Datenbank
        -h, --host=                             Hostname
        -u, --user=                             Benutzername
        -p, --password=                         Passwort
	Zeitintervaloptionen:
        -I, --interval=                         Intervalllaenge
	-S, --starttime=                        Startzeit (UNIX-Zeit)
	-E, --endtime=                          Endzeit (UNIX-Zeit)
	Filteroptionen:
        -A, --address=                          IP-Adresse
        -M, --mask=                             Adressmaske
        -P, --port=                             Port
        -R, --protocol=                         Protokoll''')
    print ('''Ausgabe:
	 Intervallstartzeit, #bytes, #pkts, #records, #ips, #ports, avg_duration''')

def main():
        user=os.environ['LOGNAME']
        host='localhost'
        password=''
        database=''
        bin=False
        addr=''
        mask=32
        port=''
        proto=''
        interval=60
	starttime=0
	endtime=0

        try:
                opts, args = getopt.gnu_getopt(sys.argv[1:], "u:h:p:d:I:S:E:A:M:P:R:", ["user=", "host=", "password=", "database=", "interval=", "starttime=", "endtime=", "mask=", "port=", "protocol="])
        except getopt.GetoptError:
                print "Ungueltige Option."
                usage()
                sys.exit(2)

        for o, a in opts:
                if o in ("-u", "--user"):
                        user=a
                if o in ("-h", "--host"):
                        host=a
                if o in ("-p", "--password"):
                        password=a
                if o in ("-d", "--database"):
                        database=a
                if o in ("-I", "--interval"):
                        interval=string.atoi(a)
                if o in ("-S", "--starttime"):
                        starttime=string.atoi(a)
                if o in ("-E", "--endtime"):
                        endtime=string.atoi(a)
                if o in ("-A", "--address"):
                        addr=a
                if o in ("-M", "--mask"):
                        mask=string.atoi(a)
                if o in ("-P", "--port"):
                        port=str(string.atoi(a))
                if o in ("-R", "--protocol"):
                        proto=str(string.atoi(a))
                if o in ("-T", "--topas"):
                        topas=True
                if o in ("-D", "--distinct"):
                        distinct=True
	
	if interval<1 or starttime<0 or endtime<0:
		print('Startzeit, Endzeit und Interval müssen positiv sein')
		return
	starttime = (starttime//interval)*interval

        if (database):
		try:
			connection = MySQLdb.connect(host,user,password,database)
		except MySQLdb.OperationalError, message:  
			print ('%d: Konnte nicht mit Datenbank verbinden: %s' % (message[0], message[1]))
			return
		c = connection.cursor()
                query_metrics(c, starttime, endtime, interval, addr, mask, port, proto)
        else:
                usage()

if __name__ == "__main__":
    main()
