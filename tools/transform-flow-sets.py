# -*- coding: utf-8 -*-
#Programm zur Transformation mit Flow-Tools erzeugter Netzverkehrsdaten in mySQL-Tabellen
#mit Verwendung von Sets (spart ein SQL-Befehle, wird aber erst ab Python 2.4 unterstützt)
#erstellt am: 10.7.2006
#letzte Änderung am 13.8.2006
#erstellt von: Manuel Meiborg (manuel.meiborg@gmx.de)
#Version 2.0
#
#Dieses Script arbeitet auf den ASCII-Format Ausgaben der netflow-Tools. Die einzelnen Verkehrsdaten 
#werden angepasst und entsprechend ihrer Zeitstempel in 11 spaltige MySQL-Tabellen geschrieben, 
#die mit den tools aggregator.py und insert.py weiterbearbeitet werden können 
#
#FIXME: Zeitstempel sind UTC, Tabellennamen aber gemäß lokaler Zeit

import MySQLdb, sys, copy, time, os,sets
def transformation (filename, db, user, passwd):
        
    def ipZuZahl(adresse):
    # umwandeln einer IP-Adresse im Format xxx.xxx.xxx.xxx in eine integer-Zahl
        adresse=adresse.split('.')
        return 2**24*long(adresse[0])+2**16*long(adresse[1])+2**8*long(adresse[2])+long(adresse[3])

    def gettime(sysUpTime, unixSecs, timestamp):
    #wandelt in UNIX-Zeit um
        return int(unixSecs-(sysUpTime-timestamp)/1000)

    def exporterID(expAddr):
    #liefert die exporterID eines Paars aus sourceId und expAddr bzw erzeugt eine neue exporterID wenn das Paar
    #noch nicht vorhanden ist und liefert diese zurück
        expTab =connection.cursor()
        expTab.execute('select id from exporter where  srcIP = %s', (expAddr))
        inhalt= expTab.fetchone()
        if inhalt == None:  # unbekannter Exporter
            expTab.execute('insert into exporter (srcIP) values (%s);',(expAddr))
            expTab.execute('select id from exporter where srcIP = %s', (expAddr))
            ID= int(expTab.fetchone()[0])
        else:
            ID= int(inhalt[0])
        return ID



    allesInOrdnung=True
    print('')
    if bin:
        try:
            os.system('flow-export -f2 <'+filename+'> '+filename+'_tmp')
            filename=filename+'_tmp'
        except:
            print('Umwandlung der Binärdatei '+filename+' fehlgeschlagen. Datei muß im cflowd-Format vorliegen.')
    #Öffnen der Datei 
    try:
        daten = file (filename,"r") #Daten aus Datei auslesen                                 
    except:
        print ('Datei '+filename+' konnte nicht geöffnet werden!')
        allesInOrdnung=False

    #Herstellen der Verbindung zur Datenbank:
    try:
        connection = MySQLdb.connect('localhost',user,passwd,db)
    except:
        print ('Datenbank '+db+' konnte als user '+user+' nicht geöffnet werden!')
        allesInOrdnung=False

    # Eintragen der Daten in die Tabelle
    if allesInOrdnung:

        namenserzeugung=0
        tabtest=0
        einfuegen=0
        i=0



        erzeugteTabellen=set()  #verringert die Laufzeit etwas, da nicht für jede einzelnen Durchlauf die Existenz der Tabelle geprüft werden muß
        kopf=daten.readline()
        while True:
            zeile=daten.readline().split(',')
            if zeile==['']: break  #=leere Liste wenn eof erreicht ist

            #Erzeugen des Tabellennamens
            firstSwitched =gettime (int(zeile[2]),int(zeile[0]),int(zeile[6]))
            startzeit=time.localtime(firstSwitched) #Ortszeit!!!
            tabellenname='h_'
            tabellenname+=str(startzeit[0])                                 #Jahr
            tabellenname+=str(startzeit[1]).rjust(2).replace(' ','0')       #Monat
            tabellenname+=str(startzeit[2]).rjust(2).replace(' ','0')       #Tag
            tabellenname+='_'+str(startzeit[3]).rjust(2).replace(' ','0')   #Stunde
            if startzeit[4]<30:                                             #halbe Stunde                    
                tabellenname+='_0'
            else:
                tabellenname+='_1'

            #Erzeugen der Tabelle 
            if tabellenname not in erzeugteTabellen:
                connection.cursor().execute('create table if not exists '+tabellenname+' (srcIP integer(10) unsigned,\
                                                                    dstIP integer(10) unsigned,\
                                                                    srcPort smallint (5) unsigned,\
                                                                    dstPort smallint (5) unsigned,\
                                                                    proto tinyint (3) unsigned,\
                                                                    dstTos tinyint (3) unsigned,\
                                                                    bytes bigint (20) unsigned,\
                                                                    pkts bigint (20) unsigned,\
                                                                    firstSwitched integer (10) unsigned,\
                                                                    lastSwitched integer (10) unsigned,\
                                                                    exporterID smallint (5) unsigned);')
                erzeugteTabellen.add(tabellenname)

            connection.cursor().execute ('insert into '+tabellenname+' values (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s);',
            (ipZuZahl(zeile[10]),\
             ipZuZahl(zeile[11]),\
             int(zeile[15]),\
             int(zeile[16]),\
             int(zeile[17]),\
             int(zeile[18]),\
             int(zeile[5]),\
             int(zeile[4]),\
             firstSwitched,\
             gettime (int(zeile[2]),int(zeile[0]),int(zeile[7])),\
             exporterID(ipZuZahl(zeile[3]))))

        connection.cursor().close()
        daten.close()
        if bin:
            os.system('rm '+filename) #Löschen der temporären ascii-Datei
        print('Umwandlung erfolgreich, folgende Tabellen erzeugt:')
        print(erzeugteTabellen)
        print("Laufzeit: "+str(int(time.time()-start))+" Sekunden")

start=(time.time())
passwd ='' 
filename=''
user=''
db=''
bin = False

for i in range(1, len(sys.argv)):  # Einlesen der Kommandozeilenargumente
    if   sys.argv[i]=='-file':
         filename=sys.argv[i+1]
    elif sys.argv[i]=='-db':
         db=sys.argv[i+1]
    elif sys.argv[i]=='-user':
         user=sys.argv[i+1]
    elif sys.argv[i]=='-pw':
         passwd=sys.argv[i+1]
    elif sys.argv[i]=='-bin':
        bin =True


print('Achtung! Dieses Skript geht noch von Tabellennamen gemaess Ortszeit aus.')

if (filename and db and user):                              
    transformation (filename, db, user, passwd)
    
else:
    print ('''Unzureichende oder fehlende Argumente, bitte beachten sie folgende Syntax:    
    transform.py -file (Name der Quelldatei)
                 -db (Name der Datenbank)
                 -user (Benutzername)
                 -pw (Passwort)
                 -bin (falls Quelldatei nicht in ASCII-, sondern in Binärformat vorliegt flow-export muß im path verfügbar sein)
                 ''')
