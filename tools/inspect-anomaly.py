#!/usr/bin/python
# -*- coding: utf-8 -*-
# Inspect anomalous time intervals in NASTY tables
# Gerhard Muenz, April 2008
# -*- coding: utf-8 -*-

import MySQLdb, getopt, os, sys, string, calendar, time

def inspect(c, starttime, interval, length, addr, mask, port, proto, hitter, filterdirection):
        def ip2int(adresse):
        # konvertiert eine IP im dotted-quad Format in einen Integer
                adresse=adresse.split('.')
                return 2**24*long(adresse[0])+2**16*long(adresse[1])+2**8*long(adresse[2])+long(adresse
                        [3])

        def int2ip(i):
        # konvertiert einen Integer in eine IP im dotted-quad Format
                return str(i//2**24)+"."+str((i//2**16)%256)+"."+str((i//2**8)%256)+"."+str(i%256)

        def filter2where(src_or_dst, addr, mask, port, proto):
        # generiert einen WHERE-Ausdruck für den Filter (src_or_dst muss "src" oder "dst" sein)
                bitmask = ['0x00000000', '0x80000000', '0xC0000000', '0xE0000000', \
                        '0xF0000000', '0xF8000000', '0xFC000000', '0xFE000000', \
                        '0xFF000000', '0xFF800000', '0xFFC00000', '0xFFE00000', \
                        '0xFFF00000', '0xFFF80000', '0xFFFC0000', '0xFFFE0000', \
                        '0xFFFF0000', '0xFFFF8000', '0xFFFFC000', '0xFFFFE000', \
                        '0xFFFFF000', '0xFFFFF800', '0xFFFFFC00', '0xFFFFFE00', \
                        '0xFFFFFF00', '0xFFFFFF80', '0xFFFFFFC0', '0xFFFFFFE0', \
                        '0xFFFFFFF0', '0xFFFFFFF8', '0xFFFFFFFC', '0xFFFFFFFE']
                if ((addr=='' or mask==0) and port=='' and proto==''):
                        return '1'
                needand = False
                result = '('
                if addr != '':
                        if ((mask > 0) and (mask < 32)):
                                result=result+'('+src_or_dst+'Ip & '+bitmask[mask]+')='+str(ip2int(addr))
                                needand = True
                        elif mask == 32:
                                result=result+src_or_dst+'Ip='+str(ip2int(addr))
                                needand = True
                if port != '':
                        if needand:
                                result = result + ' AND '
                        result=result+src_or_dst+'Port='+port
                        needand = True
                if proto != '':
                        if needand:
                                result = result + ' AND '
                        result = result+'proto='+proto
                return result+')'

	def time2tables(starttime, interval, length):
        # sucht relevante Halbstundentabellen
		tables = []
		c.execute("SHOW TABLES LIKE 'h\\_%'")
		for row in c.fetchall():
			tabletime = calendar.timegm([string.atoi(row[0][2:6]), string.atoi(row[0][6:8]), string.atoi(row[0][8:10]), \
				string.atoi(row[0][11:13]), string.atoi(row[0][14])*30, 0, 0, 0, 0])
			if (tabletime < (starttime+interval*length) and tabletime+30*60 > starttime):
				tables.append(row[0])
                return tables

	def getselectstr(table, colnames, wheres, other):
		result = "SELECT "
		isfirst = True
		for col in colnames:
			if isfirst:
				result += col
				isfirst = False
			else:
				result += ", "+col
		result += " FROM "+table
		isfirst = True
		for w in wheres:
			if isfirst:
				result += " WHERE ("+w+")"
				isfirst = False
			else:
				result += " AND ("+w+")"
		if other != "":
			result += " "+other
		return result
		
	def printresult(result, colnames, ipcols):
		rowstr = ""
		for col in colnames:
			rowstr += col + "\t"
		print(rowstr)
		for row in result:
			rowstr = ""
			i = 0
			for col in row:
				if i in ipcols:
					rowstr += int2ip(col) + "\t"
				else:
					rowstr += str(col) + "\t"
				i += 1
			print rowstr

		
	tables = time2tables(starttime, interval, length)
	
	if tables==[]:
		print('Keine Tabellen für '+str(starttime)+'<=firstSwitched<='+str(starttime+interval*length)+' gefunden.')
		return
	else:
		print('Relevante Tabellen für '+str(starttime)+'<=firstSwitched<='+str(starttime+interval*length)+':')
		tablesstr = ''
		for t in tables:
			tablesstr = tablesstr + t + " "
		print(tablesstr)
	print('')

	timecol = "(firstSwitched DIV "+str(interval)+")*"+str(interval)+" AS time"
	timecond = "HAVING time BETWEEN "+str(starttime)+" AND "+str(starttime+interval*length-1)
	filter = ''

	if addr != "" or port != "" or proto != "":
		print('Filter:')
		if addr != "":
			print(filterdirection+"IP: "+addr+"/"+str(mask))
		if port != "":
			print(filterdirection+"Port:       "+port)
		if proto != "":
			print("Protokoll:  "+proto)
		print('')
		if filterdirection == '':
			filter = filter2where("dst", addr, mask, port, proto)+" OR "+filter2where("src", addr, mask, port, proto)
		else:
			filter = filter2where(filterdirection, addr, mask, port, proto)

	if hitter == []:
		columns = [timecol, "SUM(bytes)", "SUM(pkts)", "COUNT(*)", "COUNT(DISTINCT srcIp, dstIp)", "COUNT(DISTINCT srcPort, dstPort)", "AVG(lastSwitched-firstSwitched)"]
		colnames = ["Time      ", "Bytes", "Pkts", "Flows", "IPs", "Ports", "Duration"]
		query = getselectstr(tables[0], columns, [filter], "GROUP BY time "+timecond)
		print(query)
		c.execute(query)
		printresult(c.fetchall(), colnames, [])
	else:
		skipdistinctports = False
		skipdistinctips = False
		if hitter[1] == filterdirection and port != '' and proto != '':
			skipdistinctports = True
		if hitter[1] == filterdirection and addr != '' and mask == 32:
			skipdistinctips = True
		if hitter[0][3:5] == "Ip":
			ipcols = [1]
		else:
			ipcols = []

		columns = [timecol, hitter[0], "SUM(bytes) AS sb", "SUM(pkts) AS sp", "COUNT(*) AS c", "COUNT(DISTINCT "+hitter[1]+"Ip) AS di", "COUNT(DISTINCT "+hitter[1]+"Port) AS dp", "AVG(lastSwitched-firstSwitched)"]
		colnames = ["Time      ", hitter[0], "Bytes", "Pkts", "Flows", "DstIPs", "DstPorts", "Duration"]

		query = getselectstr(tables[0], columns, [filter], "GROUP BY time, "+hitter[0]+" "+timecond+" ORDER BY sb DESC LIMIT 10")
		print("Heavy-Hitters: "+hitter[0]+" mit meisten Bytes")
		print(query)
		c.execute(query)
		printresult(c.fetchall(), colnames, ipcols)
	
		query = getselectstr(tables[0], columns, [filter], "GROUP BY time, "+hitter[0]+" "+timecond+" ORDER BY sp DESC LIMIT 10")
		print('')
		print("Heavy-Hitters: "+hitter[0]+" mit meisten Paketen")
		print(query)
		c.execute(query)
		printresult(c.fetchall(), colnames, ipcols)
	
		query = getselectstr(tables[0], columns, [filter], "GROUP BY time, "+hitter[0]+" "+timecond+" ORDER BY c DESC LIMIT 10")
		print('')
		print("Heavy-Hitters: "+hitter[0]+" mit meisten Flows")
		print(query)
		c.execute(query)
		printresult(c.fetchall(), colnames, ipcols)

		if skipdistinctips == False:
			query = getselectstr(tables[0], columns, [filter], "GROUP BY time, "+hitter[0]+" "+timecond+" ORDER BY di DESC LIMIT 10")
			print('')
			print("Heavy-Hitters: "+hitter[0]+" mit meisten "+hitter[1]+"Ips")
			print(query)
			c.execute(query)
			printresult(c.fetchall(), colnames, ipcols)
		else:
			print('')
			print(filterdirection+"IP-Filter gesetzt, ueberspringe "+hitter[0]+" mit meisten "+hitter[1]+"Ips")

		if skipdistinctports == False:
			query = getselectstr(tables[0], columns, [filter], "GROUP BY time, "+hitter[0]+" "+timecond+" ORDER BY dp DESC LIMIT 10")
			print('')
			print("Heavy-Hitters: "+hitter[0]+" mit meisten "+hitter[1]+"Ports")
			print(query)
			c.execute(query)
			printresult(c.fetchall(), colnames, ipcols)
		else:
			print('')
			print(filterdirection+"Port-Filter gesetzt, ueberspringe "+hitter[0]+" mit meisten "+hitter[1]+"Ips")
	return
    


        
    

def usage():
    print ('''Benutzung: ''' + sys.argv[0] + ''' -d <database> [options]
        Datenbankoptionen:
        -d, --database      Name der Datenbank
        -h, --host=         Hostname
        -u, --user=         Benutzername
        -p, --password=     Passwort
        Zeitintervaloptionen:
        -T, --time=         Startzeit (in UTC-Sekunden)
        -I, --interval=     Intervalllaenge
        -L, --length=       Anzahl zu untersuchender Intervalle
        Filteroptionen:
        -A, --address=      IP-Adresse
        -M, --mask=         Adressmaske
        -P, --port=         Port
        -R, --protocol=     Protokoll
	-S|-D, --src|--dst  Filter nur auf Quelle/Ziel anwenden
        Ausgabeoptionen	:
	-H, --hitters=      Heavy-Hitter-Statistik fuer "srcIp", "dstIp", "srcPort" oder "dstPort"''')


def main():
        user=os.environ['LOGNAME']
        host='localhost'
        password=''
        database=''
        starttime=0
        interval=60
	length=1
        addr=''
        mask=32
        port=''
        proto=''
	hitter=[]
	filterdirection=''

        try:
                opts, args = getopt.gnu_getopt(sys.argv[1:], "u:h:p:d:T:I:L:A:M:P:R:H:SD", ["user=", "host=", "password=", "database=", "time=", "interval=", "length=", "address=", "mask=", "port=", "protocol=", "hitters=", "src", "dst"])
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
                if o in ("-T", "--time"):
                        starttime = string.atoi(a)
                if o in ("-I", "--interval"):
                        interval = string.atoi(a)
                if o in ("-L", "--length"):
                        length = string.atoi(a)
                if o in ("-A", "--address"):
                        addr=a
                if o in ("-M", "--mask"):
                        mask=string.atoi(a)
                if o in ("-P", "--port"):
                        port=str(string.atoi(a))
                if o in ("-R", "--protocol"):
                        proto=str(string.atoi(a))
                if o in ("-H", "--hitters"):
			if string.lower(a) == "srcip":
				hitter = ["srcIp", "dst"]
			elif string.lower(a) == "dstip":
				hitter = ["dstIp", "src"]
			elif string.lower(a) == "dstport":
				hitter = ["dstPort", "src"]
			elif string.lower(a) == "srcport":
				hitter = ["srcPort", "dst"]
                if o in ("-S", "--src"):
                        filterdirection="src"
                if o in ("-D", "--dst"):
                        filterdirection="dst"

	if interval<1 or starttime<0 or length<1:
		print('Startzeit, Interval und Länge müssen positiv sein')
		return
	starttime = (starttime//interval)*interval

        if (database):
		try:
			connection = MySQLdb.connect(host,user,password,database)
		except MySQLdb.OperationalError, message:  
			print ('%d: Konnte nicht mit Datenbank verbinden: %s' % (message[0], message[1]))
			return
		c = connection.cursor()
                inspect(c, starttime, interval, length, addr, mask, port, proto, hitter, filterdirection)
        else:
                usage()

if __name__ == "__main__":
    main()
