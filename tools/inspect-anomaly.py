#!/usr/bin/python
# -*- coding: utf-8 -*-
# Inspect anomalous time intervals in NASTY tables
# Gerhard Muenz, April 2008
# -*- coding: utf-8 -*-

import MySQLdb, getopt, os, sys, string, calendar, time

def inspect(c, starttime, interval, length, addr, mask, port, proto, hitter, srcendpoint, dstendpoint):
        def ip2int(adresse):
        # konvertiert eine IP im dotted-quad Format in einen Integer
                adresse=adresse.split('.')
                return 2**24*long(adresse[0])+2**16*long(adresse[1])+2**8*long(adresse[2])+long(adresse
                        [3])

        def int2ip(i):
        # konvertiert einen Integer in eine IP im dotted-quad Format
                return str(i//2**24)+"."+str((i//2**16)%256)+"."+str((i//2**8)%256)+"."+str(i%256)

        def filter2where(src_or_dst, addr, mask, port):
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

	def getselectstr(tables, colnames, wheres, other):
		result = ""
		isfirsttable = True
		for t in tables:
			if isfirsttable:
				result += "(SELECT "
				isfirsttable = False
			else:
				result += " UNION\n(SELECT "
			isfirst = True
			for col in colnames:
				if isfirst:
					result += col
					isfirst = False
				else:
					result += ", "+col
			result += " FROM "+t
			isfirst = True
			for w in wheres:
				if w != '':
					if isfirst:
						result += " WHERE ("+w+")"
						isfirst = False
					else:
						result += " AND ("+w+")"
			if other != "":
				result += " "+other
			result += ")"
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
		tablesstr = '  '
		for t in tables:
			tablesstr = tablesstr + t + " "
		print(tablesstr)
	print('')

	timecol = "(firstSwitched DIV "+str(interval)+")*"+str(interval)+" AS time"
	timecond = "HAVING time BETWEEN "+str(starttime)+" AND "+str(starttime+interval*length-1)
	ipfilter = ""
	portfilter = ""
	protofilter = ""
	srcfilter = ""
	dstfilter = ""

	print('Filter:')
	if addr != "":
		print("  IP-Adresse  "+addr+"/"+str(mask))
		ipfilter = filter2where("dst", addr, mask, "")+" OR "+filter2where("src", addr, mask, "")
	if port != "":
		print("  Port:       "+port)
		portfilter = filter2where("dst", "", 0, port)+" OR "+filter2where("src", "", 0, port)
	if proto != "":
		print("  Protokoll:  "+proto)
		protofilter = "proto="+proto
	if srcendpoint != []:
		print("  Quelle:     "+srcendpoint[0][0]+"/"+srcendpoint[0][1]+":"+srcendpoint[1])
		srcfilter = filter2where("src", srcendpoint[0][0], string.atoi(srcendpoint[0][1]), srcendpoint[1])
	if dstendpoint != []:
		print("  Ziel:       "+dstendpoint[0][0]+"/"+dstendpoint[0][1]+":"+dstendpoint[1])
		dstfilter = filter2where("dst", dstendpoint[0][0], string.atoi(dstendpoint[0][1]), dstendpoint[1])
	print('')

	if hitter == []:
		if proto == "1":
			columns = [timecol, "SUM(bytes)", "SUM(pkts)", "COUNT(*)", "COUNT(DISTINCT srcIp, dstIp)", "AVG(lastSwitched-firstSwitched)"]
			colnames = ["Time      ", "Bytes", "Pkts", "Flows", "IPs", "Duration"]
		else:
			columns = [timecol, "SUM(bytes)", "SUM(pkts)", "COUNT(*)", "COUNT(DISTINCT srcIp, dstIp)", "COUNT(DISTINCT srcPort, dstPort)", "AVG(lastSwitched-firstSwitched)"]
			colnames = ["Time      ", "Bytes", "Pkts", "Flows", "IPs", "Ports", "Duration"]
		print('Zeitreihenwerte:')
		query = getselectstr(tables, columns, [ipfilter, portfilter, protofilter, srcfilter, dstfilter], "GROUP BY time "+timecond)
		print(query)
		c.execute(query)
		printresult(c.fetchall(), colnames, [])
	else:
		if proto == "1":
			columns = [timecol, hitter[0], "SUM(bytes) AS sb", "SUM(pkts) AS sp", "COUNT(*) AS c", "COUNT(DISTINCT "+hitter[1]+"Ip) AS di", "AVG(lastSwitched-firstSwitched) AS d"]
			colnames = ["Time      ", hitter[0], "Bytes", "Pkts", "Flows", hitter[1]+"IPs", "Duration"]
		else:
			columns = [timecol, hitter[0], "SUM(bytes) AS sb", "SUM(pkts) AS sp", "COUNT(*) AS c", "COUNT(DISTINCT "+hitter[1]+"Ip) AS di", "COUNT(DISTINCT "+hitter[1]+"Port) AS dp", "AVG(lastSwitched-firstSwitched) AS d"]
			colnames = ["Time      ", hitter[0], "Bytes", "Pkts", "Flows", hitter[1]+"IPs", hitter[1]+"Ports", "Duration"]
		if hitter[0][3:5] == "Ip":
			ipcols = [1]
			colnames[1] = colnames[1]+"    "
		else:
			ipcols = []

		skipdistinctports = False
		skipdistinctips = False
		singlequery = False
		if hitter[0][0:3] == "src":
			if srcendpoint != []:
				if (hitter[0]=="srcIp" and srcendpoint[0]!='') or (hitter[0]=="srcPort" and srcendpoint[1]!=''):
					singlequery = True
			if dstendpoint != []:
				if dstendpoint[0] != '':
					skipdistinctips = True
				if dstendpoint[1] != '':
					skipdistinctports = True
		if hitter[0][0:3] == "dst":
			if dstendpoint != []:
				if (hitter[0]=="dstIp" and dstendpoint[0]!='') or (hitter[0]=="dstPort" and dstendpoint[1]!=''):
					singlequery = True
			if srcendpoint != []:
				if srcendpoint[0] != '':
					skipdistinctips = True
				if srcendpoint[1] != '':
					skipdistinctports = True
		if proto == "1":
			skipdistinctports = True

		if singlequery:
			print("Filter ermöglichen maximal eine Zeile je Zeitintervall:")
			query = getselectstr(tables, columns, [ipfilter, portfilter, protofilter, srcfilter, dstfilter], "GROUP BY time, "+hitter[0]+" "+timecond)
			print(query)
			c.execute(query)
			printresult(c.fetchall(), colnames, ipcols)
			return

		print("Heavy-Hitters: "+hitter[0]+" mit meisten Bytes")
		query = getselectstr(tables, columns, [ipfilter, portfilter, protofilter, srcfilter, dstfilter], "GROUP BY time, "+hitter[0]+" "+timecond)+" ORDER BY sb DESC LIMIT 10"
		print(query)
		c.execute(query)
		printresult(c.fetchall(), colnames, ipcols)
		print('')

		print("Heavy-Hitters: "+hitter[0]+" mit meisten Paketen")
		query = getselectstr(tables, columns, [ipfilter, portfilter, protofilter, srcfilter, dstfilter], "GROUP BY time, "+hitter[0]+" "+timecond)+" ORDER BY sp DESC LIMIT 10"
		print(query)
		c.execute(query)
		printresult(c.fetchall(), colnames, ipcols)
		print('')

		print("Heavy-Hitters: "+hitter[0]+" mit meisten Flows")
		query = getselectstr(tables, columns, [ipfilter, portfilter, protofilter, srcfilter, dstfilter], "GROUP BY time, "+hitter[0]+" "+timecond)+" ORDER BY c DESC LIMIT 10"
		print(query)
		c.execute(query)
		printresult(c.fetchall(), colnames, ipcols)
		print('')

		if skipdistinctips == False:
			print("Heavy-Hitters: "+hitter[0]+" mit meisten "+hitter[1]+"Ips")
			query = getselectstr(tables, columns, [ipfilter, portfilter, protofilter, srcfilter, dstfilter], "GROUP BY time, "+hitter[0]+" "+timecond)+" ORDER BY di DESC LIMIT 10"
			print(query)
			c.execute(query)
			printresult(c.fetchall(), colnames, ipcols)
		else:
			print(hitter[1]+"IP-Filter gesetzt, ueberspringe "+hitter[0]+" mit meisten "+hitter[1]+"Ips")
		print('')

		if skipdistinctports == False:
			print("Heavy-Hitters: "+hitter[0]+" mit meisten "+hitter[1]+"Ports")
			query = getselectstr(tables, columns, [ipfilter, portfilter, protofilter, srcfilter, dstfilter], "GROUP BY time, "+hitter[0]+" "+timecond)+" ORDER BY dp DESC LIMIT 10"
			print(query)
			c.execute(query)
			printresult(c.fetchall(), colnames, ipcols)
		else:
			print(hitter[1]+"Port-Filter gesetzt oder Protokoll=ICMP, ueberspringe "+hitter[0]+" mit meisten "+hitter[1]+"Ips")
	return
    

def usage():
    print ('''Benutzung: ''' + sys.argv[0] + ''' -d <database> [options]
        Datenbankoptionen:
        -d, --database      Name der Datenbank
        -h, --host=         Hostname
        -u, --user=         Benutzername
        -p, --password=     Passwort
        Zeitintervaloptionen:
        -T, --time=         Startzeit (in Unix-Sekunden)
        -I, --interval=     Intervalllaenge
        -L, --length=       Anzahl zu untersuchender Intervalle
        Filteroptionen:
        -A, --address=      IP-Adresse (Quelle oder Ziel muss uebereinstimmen)
        -M, --mask=         Adressmaske
        -P, --port=         Port (Quelle oder Ziel muss uebereinstimmen)
        -R, --protocol=     Protokoll
	-S, --src=          Quell-Endpunkt in der Form "A.B.C.D/M:Port" ("*"=Wildcard für Adresse)
	-D, --dst=          Ziel-Endpunkt in der Form "A.B.C.D/M:Port" ("*"=Wildcard für Adresse)
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
	srcendpoint=[]
	dstendpoint=[]

        try:
                opts, args = getopt.gnu_getopt(sys.argv[1:], "u:h:p:d:T:I:L:A:M:P:R:H:S:D:", ["user=", "host=", "password=", "database=", "time=", "interval=", "length=", "address=", "mask=", "port=", "protocol=", "hitters=", "src=", "dst="])
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
			srcendpoint=a.split(':')
			if srcendpoint[0]=='*':
				srcendpoint[0]=['', '32']
			else:
				srcendpoint[0]=srcendpoint[0].split("/")
				if len(srcendpoint[0])==1:
					srcendpoint[0].append('32')
			if len(srcendpoint)==1:
				srcendpoint.append('')
                if o in ("-D", "--dst"):
			dstendpoint=a.split(':')
			if dstendpoint[0]=='*':
				dstendpoint[0]=['', '32']
			else:
				dstendpoint[0]=dstendpoint[0].split("/")
				if len(dstendpoint[0])==1:
					dstendpoint[0].append('32')
			if len(dstendpoint)==1:
				dstendpoint.append('')

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
                inspect(c, starttime, interval, length, addr, mask, port, proto, hitter, srcendpoint, dstendpoint)
        else:
                usage()

if __name__ == "__main__":
    main()
