#!/usr/bin/python
# -*- coding: utf-8 -*-
# Inspect anomalous time intervals in NASTY tables
# Gerhard Muenz, April 2008
# -*- coding: utf-8 -*-

import MySQLdb, getopt, os, sys, string, calendar, time

def findhosts(c, starttime, endtime, addr, mask, proto):
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

	def time2tables(starttime, endtime):
        # sucht relevante Halbstundentabellen
		tables = []
		c.execute("SHOW TABLES LIKE 'h\\_%'")
		for row in c.fetchall():
			tabletime = calendar.timegm([string.atoi(row[0][2:6]), string.atoi(row[0][6:8]), string.atoi(row[0][8:10]), \
				string.atoi(row[0][11:13]), string.atoi(row[0][14])*30, 0, 0, 0, 0])
			if (tabletime < endtime and tabletime+30*60 > starttime):
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

		
	tables = time2tables(starttime, endtime)
	
	if tables==[]:
		print('Keine Tabellen für '+str(starttime)+'<=firstSwitched<='+str(endtime)+' gefunden.')
		return
	else:
		print('Relevante Tabellen für '+str(starttime)+'<=firstSwitched<='+str(endtime)+':')
		tablesstr = '  '
		for t in tables:
			tablesstr = tablesstr + t + " "
		print(tablesstr)
	print('')

	timecond = "HAVING firstSwitched BETWEEN "+str(starttime)+" AND "+str(endtime)
	filter = ""
	protofilter = ""

	print('Filter:')
	if addr != "":
		print("  Quell-IP-Adresse:  "+addr+"/"+str(mask))
		filter = filter2where("src", addr, mask, "")
	if proto != "":
		print("  Protokoll:         "+proto)
		protofilter = "proto="+proto
	print('')

	columns = ["srcIp", "FROM_UNIXTIME(MIN(firstSwitched))", "FROM_UNIXTIME(MAX(lastSwitched))", "SUM(pkts)", "SUM(bytes)", "COUNT(*)"]
	colnames = ["SrcIP    ", "FirstSeen         ", "LastSeen         ", "Bytes", "Pkts", "Flows"]
	query = getselectstr(tables, columns, [filter, protofilter], "GROUP BY srcIp ORDER BY srcIp")
	print(query)
	c.execute(query)
	printresult(c.fetchall(), colnames, [0])
	return
    

def usage():
    print ('''Benutzung: ''' + sys.argv[0] + ''' -d <database> [options]
        Datenbankoptionen:
        -d, --database      Name der Datenbank
        -h, --host=         Hostname
        -u, --user=         Benutzername
        -p, --password=     Passwort
        Zeitintervaloptionen:
        -S, --start=        Startzeit YYYYMMDDhhmm (Default: now-30min)
        -E, --end=          Endzeit YYYYMMDDhhmm (Default: now)
        Filteroptionen:
        -A, --address=      IP-Adresse (Quelle oder Ziel muss uebereinstimmen)
        -M, --mask=         Adressmaske
        -R, --protocol=     Protokoll''')


def main():
        user=os.environ['LOGNAME']
        host='localhost'
        password=''
        database=''
        endtime=int(time.time())
	starttime=endtime-30*60
        addr=''
        mask=32
        port=''
        proto=''

        try:
                opts, args = getopt.gnu_getopt(sys.argv[1:], "u:h:p:d:S:E:A:M:R", ["user=", "host=", "password=", "database=", "start=", "end=", "address=", "mask=", "protocol="])
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
                if o in ("-S", "--start"):
			starttime = calendar.timegm(time.strptime(a,"%Y%m%d%H%M"))
                if o in ("-E", "--end"):
			endtime = calendar.timegm(time.strptime(a,"%Y%m%d%H%M"))
                if o in ("-A", "--address"):
                        addr=a
                if o in ("-M", "--mask"):
                        mask=string.atoi(a)
                if o in ("-R", "--protocol"):
                        proto=str(string.atoi(a))

        if (database):
		try:
			connection = MySQLdb.connect(host,user,password,database)
		except MySQLdb.OperationalError, message:  
			print ('%d: Konnte nicht mit Datenbank verbinden: %s' % (message[0], message[1]))
			return
		c = connection.cursor()
                findhosts(c, starttime, endtime, addr, mask, proto)
        else:
                usage()

if __name__ == "__main__":
    main()
