#!/usr/local/bin/python
# -*- coding: utf-8 -*-
#Programm zur Transformation mit Flow-Tools erzeugter Netzverkehrsdaten in mySQL-Tabellen
#erstellt am: 10.7.2006
#letzte Ã„nderung am 16.1.2007
#erstellt von: Manuel Meiborg (manuel.meiborg@gmx.de)
#verbessert von: Dominik Brettnacher
#Version 2.1
#
#Dieses Script arbeitet auf den ASCII-Format Ausgaben der netflow-Tools. Die einzelnen Verkehrsdaten werden angepasst und entsprechend
#ihrer Zeitstempel in 11 spaltige MySQL-Tabellen geschrieben, die mit den tools aggregator.py und insert.py weiterbearbeitet werden kÃ¶nnen 

import MySQLdb, getopt, os, sys, time

def transform(files, user, host, password, database, bin):
        # konvertiert eine IP im dotted-quad Format in einen Integer
        def ipZuZahl(adresse):
                adresse=adresse.split('.')
                return 2**24*long(adresse[0])+2**16*long(adresse[1])+2**8*long(adresse[2])+long(adresse
                        [3])

        # wandelt in UNIX-Zeit um
        # 
        def gettime(sysUpTime, unixSecs, timestamp):
                return int(unixSecs-(sysUpTime-timestamp)/1000)

        try:
                connection = MySQLdb.connect(host,user,password,database)
        except MySQLdb.OperationalError, message:  
                print ('%d: Konnte nicht mit Datenbank verbinden: %s' % (message[0], message[1]))
                return

        # liefert die Exporter-ID zu einer IP
        # legt eine in der Exporter-Tabelle an, falls keine vorhanden ist
        def exporterID(expAddr):
            if(not exporters.has_key(expAddr)):
                c = connection.cursor()
                c.execute('INSERT INTO exporter (srcIP) VALUES (%s)', (expAddr))
                c.execute('SELECT LAST_INSERT_ID()')
                exporters[expAddr] = int(c.fetchone()[0])
            return exporters[expAddr]

        def locktables():
           connection.cursor().execute('LOCK TABLES ' + ','.join(map (lambda x: x + ' WRITE', tables)))

        def createtable(t):
            if(t not in tables):
                c = connection.cursor()
                c.execute('''CREATE TABLE ''' + t + '''(
                        srcIP integer(10) unsigned,
                        dstIP integer(10) unsigned,
                        srcPort smallint (5) unsigned,
                        dstPort smallint (5) unsigned,
                        proto tinyint (3) unsigned,
                        dstTos tinyint (3) unsigned,
                        bytes bigint (20) unsigned,
                        pkts bigint (20) unsigned, 
                        firstSwitched integer (10) unsigned,
                        lastSwitched integer (10) unsigned,
                        exporterID smallint (5) unsigned)''')
                tables.append(t)
                locktables()

        c = connection.cursor()
        c.execute('''CREATE TABLE IF NOT EXISTS `exporter` (
                `id` smallint(6) NOT NULL auto_increment,
            `sourceID` int(10) unsigned default NULL,
                `srcIP` int(10) unsigned default NULL,
                PRIMARY KEY  (`id`)
                ) ENGINE=MyISAM DEFAULT CHARSET=latin1''')

        # alle bereits vorhandenen Exporter einlesen
        exporters = {}
        c.execute('''SELECT srcIP,id FROM exporter''')
        for row in c.fetchall():
            exporters[int(row[0])] = int(row[1])

        # alle bereits vorhandenen Tabellen einlesen
        tables = []
        c.execute('''SHOW TABLES''')
        for row in c.fetchall():
            tables.append(row[0])

        locktables()
           
        for file in files:
                if bin:
                        handle = os.popen('flow-export -f2 <' + file)
                else:
                        handle = open(file,'r')

                kopf = handle.readline()
                for line in handle:
#:unix_secs,unix_nsecs,sysuptime,exaddr,dpkts,doctets,first,last,engine_type,engine_id,srcaddr,dstaddr,nexthop,input,output,srcport,dstport,prot,tos,tcp_flags,src_mask,dst_mask,src_as,dst_as
#1157476947,457,1029495428,212.88.128.35,7,1028,1029406775,1029407306,0,0,217.227.213.44,212.88.157.230,0.0.0.0,0,255,2740,80,6,0,27,0,0,9063,3320
                    zeile = line.split(',')
                    firstSwitched = gettime (int(zeile[2]),int(zeile[0]),int(zeile[6]))
                    lastSwitched = gettime (int(zeile[2]),int(zeile[0]),int(zeile[7]))
                    startzeit = time.localtime(firstSwitched)
                    tabellenname = 'h_'
                    tabellenname+=str(startzeit[0]) # Jahr
                    tabellenname+=str(startzeit[1]).rjust(2).replace(' ','0') # Monat
                    tabellenname+=str(startzeit[2]).rjust(2).replace(' ','0') # Tag
                    tabellenname+='_'+str(startzeit[3]).rjust(2).replace(' ','0') #Stunde
                    if startzeit[4]<30: #halbe Stunde                    
                        tabellenname+='_0'
                    else:
                        tabellenname+='_1'

                    createtable(tabellenname)

                    connection.cursor().execute('INSERT INTO ' + tabellenname + ' VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)',
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
                             exporterID(ipZuZahl(zeile[3]))))
                        
                handle.close()

        return


def usage():
    print ('''Benutzung: ''' + sys.argv[0] + ''' -d <database> [options] <file> ...
        Optionen:
        -d, --database=                         Name der Datenbank
        -h, --host=                             Hostname
        -u, --user=                             Benutzername
        -p, --password=                         Passwort
        -b, --binary                            Eingabe im Binärformat (benötigt flow-export)''')

def main():
        start=(time.time())

        user=os.environ['LOGNAME']
        host='localhost'
        password=''
        database=''
        bin=False

        try:
                opts, args = getopt.gnu_getopt(sys.argv[1:], "u:p:d:b", ["user=", "host=", "password=", "database=","binary"])
        except getopt.GetoptError:
                print "Ungültige Option."
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
                if o in ("-d", "--database"):
                        database=a

        if (database and len(args)):
                transform(args, user, host, password, database, bin)
                print("Laufzeit: "+str(int(time.time()-start))+" Sekunden")
        else:
                usage()

if __name__ == "__main__":
    main()
