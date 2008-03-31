#!/usr/bin/python
# -*- coding: utf-8 -*-
# Export von Zeitreihen aus NASTY-Tabellen
# erstellt von Gerhard Muenz, August 2007

import MySQLdb, getopt, os, sys, string

def query_metrics(user, host, password, database, interval, scale, addr, mask, port, proto, topas, distinct):
        # konvertiert eine IP im dotted-quad Format in einen Integer
        def ipZuZahl(adresse):
                adresse=adresse.split('.')
                return 2**24*long(adresse[0])+2**16*long(adresse[1])+2**8*long(adresse[2])+long(adresse
                        [3])

        def create_where_expression(src_or_dst, addr, mask, port, proto):
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
                                result=result+'('+src_or_dst+'Ip & '+bitmask[mask]+')='+str(ipZuZahl(addr))
                                needand = True
                        elif mask == 32:
                                result=result+src_or_dst+'Ip='+str(ipZuZahl(addr))
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
                

        try:
                connection = MySQLdb.connect(host,user,password,database)
        except MySQLdb.OperationalError, message:  
                print ('%d: Konnte nicht mit Datenbank verbinden: %s' % (message[0], message[1]))
                return

        c = connection.cursor()

        # alle bereits vorhandenen Tabellen einlesen
        tables = []
        c.execute('''SHOW TABLES LIKE \'h\\_%\'''')
        for row in c.fetchall():
                tables.append(row[0])

        filter1 = create_where_expression('src', addr, mask, port, proto)
        filter2 = create_where_expression('dst', addr, mask, port, proto)

	if topas:
		if port=='':
			port='-1'
		if proto=='':
			proto='-1'

	earliest = 0 # this is used for topas output only
        for table in tables:
		# to ignore duplicate flow keys: COUNT(DISTINCT srcIp,dstIp,srcPort,dstPort,proto) instead of COUNT(*)
		if distinct:
			countstr='COUNT(DISTINCT srcIp,dstIp,srcPort,dstPort,proto)'
		else:
			countstr='COUNT(*)'

		if filter1==filter2:
			# out and in traffic is the same	
			c.execute('SELECT (firstSwitched DIV '+interval+')*'+interval+', SUM(bytes)'+scale+', SUM(bytes)'+scale+', SUM(pkts)'+scale+', SUM(pkts)'+scale+', '+countstr+scale+', '+countstr+scale+' FROM '+table+filter1+' GROUP BY (firstSwitched DIV '+interval+')')
		else:
			c.execute(\
				    'SELECT t1.i, t1.sb'+scale+', t2.sb'+scale+', t1.sp'+scale+', t2.sp'+scale+', t1.sr'+scale+', t2.sr'+scale+' FROM ('+\
				    'SELECT (firstSwitched DIV '+interval+')*'+interval+' AS i, SUM(bytes) AS sb, SUM(pkts) AS sp, '+countstr+' AS sr FROM '+table+filter1+' GROUP BY (firstSwitched DIV '+interval+')'+\
				    ') AS t1 JOIN ('+\
				    'SELECT (firstSwitched DIV '+interval+')*'+interval+' AS i, SUM(bytes) AS sb, SUM(pkts) AS sp, '+countstr+' AS sr FROM '+table+filter2+' GROUP BY (firstSwitched DIV '+interval+')'+\
				    ') AS t2 ON t1.i=t2.i'\
					)

		for row in c.fetchall():
			if topas:
				# print empty intervals if necessary
				if earliest != 0:
					i = i + 1
					while i < (row[0]-earliest) / string.atoi(interval):
						print('--- '+str(i))
						i = i + 1
					    
				print(addr+':'+port+'|'+proto+'_'+str(row[4])+' '+str(row[3])+' '+str(row[2])+' '+str(row[1])+' '+str(row[6])+' '+str(row[5]))

				if earliest == 0:
					i = 0
					print('--- 0')
					earliest = row[0]
				else:
					print('--- '+str(i))
			else:
				print(str(row[0])+'\t'+str(row[1])+'\t'+str(row[2])+'\t'+str(row[3])+'\t'+str(row[4])+'\t'+str(row[5])+'\t'+str(row[6]))

        return



def usage():
    print ('''Benutzung: ''' + sys.argv[0] + ''' -d <database> [options]
        Optionen:
        -d, --database=                         Name der Datenbank
        -h, --host=                             Hostname
        -u, --user=                             Benutzername
        -p, --password=                         Passwort
        -I, --interval=                         Intervalllaenge
        -S, --scale=                            Alle Metrikwerte werden durch den Wert geteilt
        -A, --address=                          IP-Adresse
        -M, --mask=                             Adressmaske
        -P, --port=                             Port
        -R, --protocol=                         Protokoll
        -T, --topas                             Ausgabe fuer Topas-Stat-Modul
        -D, --distinct                          Nur verschiedene (DISTINCT) IP-5-Tupel zaehlen''')
    print ('''Ausgabe:
	 Intervallstartzeit, bytes_out, bytes_in, pkts_out, pkts_in, rcds_out, rcds_in''')

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
        interval='60'
	topas=False
	distinct=False
	scale=''

        try:
                opts, args = getopt.gnu_getopt(sys.argv[1:], "u:h:p:d:I:S:A:M:P:R:TD", ["user=", "host=", "password=", "database=", "interval=", "scale=", "address=", "mask=", "port=", "protocol=", "topas", "distinct"])
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
                        interval=str(string.atoi(a))
                if o in ("-S", "--scale"):
                        scale=' DIV '+str(string.atoi(a))
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
	
        if (database):
                query_metrics(user, host, password, database, interval, scale, addr, mask, port, proto, topas, distinct)
        else:
                usage()

if __name__ == "__main__":
    main()
