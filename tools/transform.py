# -*- coding: utf-8 -*-
#Programm zur Transformation simulierter Netzverkehrsdaten in mySQL-Tabellen
#erstellt am: 10.3.2006
#letzte Änderung am 31.6.2006
#erstellt von: Manuel Meiborg (manuel.meiborg@gmx.de)
#Version 4.0
#
#Dieses Script liest aus einer 13-spaltigen Textdatei Netzverkehrsdaten aus und speichert sie in einer 10-spaltigen MySQL-Tabelle.
#Dabei werden die Attribute firstSwitched und lastSwitched der angegebenen Zeit angepasst, weiterhin werden in die Tabelle exporter (muß in
#der Datenbank vorhanden sein) Paare aus srcID und expAddr unter einer fortlaufenden ID gespeichert.
#

import MySQLdb, sys, copy, time
def transformation (filename, db, user, passwd, starttime):

 
    def leseDaten(filename):
    # Einlesen der Daten aus der Ursprungsdatei eintragen in eine Liste

            def ipZuZahl(adresse):
            # umwandeln einer IP-Adresse im Format xxx.xxx.xxx.xxx in eine integer-Zahl

                adresse=adresse.split('.')
                return 2**24*long(adresse[0])+2**16*long(adresse[1])+2**8*long(adresse[2])+long(adresse[3])

            def exporterID(sourceId, expAddr):
            #liefert die exporterID eines Paars aus sourceId und expAddr bzw erzeugt eine neue exporterID wenn das Paar
            #noch nicht vorhanden ist und liefert diese zurück

                expTab =connection.cursor()
                expTab.execute('select id from exporter where sourceID = %s and srcIP = %s', (sourceId, expAddr))
                inhalt= expTab.fetchone()
                if inhalt == None:  # unbekannter Exporter
                    expTab.execute('insert into exporter (sourceID, srcIP) values (%s,%s);',(sourceId, expAddr))
                    expTab.execute('select id from exporter where sourceID = %s and srcIP = %s', (sourceId, expAddr))
                    ID= int(expTab.fetchone()[0])
                else:
                    ID= int(inhalt[0])
                return ID
            
            def ermittleStartzeit(starttime):
            #ermitteln der Startzeit der Tabelle in Sekunden seit dem 1.1.1972, abzüglich 1 einer Stunde um auf CEST zu kommen

                try :
                    if starttime[12] == '0':
                        startzeit=int(time.mktime([int(starttime[0:4]), int(starttime[4:6]), int(starttime[6:8]), int(starttime[9:11]),30,0,0,0,0])-3600) 
                        return startzeit;
                    elif starttime[12] == '1':
                        startzeit=int(time.mktime([int(starttime[0:4]), int(starttime[4:6]), int(starttime[6:8]), int(starttime[9:11]),30,0,0,0,0])-3600)  
                        return startzeit;
                    else:
                        print ("Ungültige Zeitangabe, bitte Format yyyymmdd_hh_a verwenden (a=0 für erste, a=1 für zweite Hälfte der stunde)")
                        return -1
                except:
                    print("Ungültige Zeitangabe, bitte Format yyyymmdd_hh_a verwenden (a=0 für erste, a=1 für zweite Hälfte der stunde)")
                    return -1

            allesInOrdnung=True            
            startzeit=ermittleStartzeit(starttime) 
            if startzeit<0:      #Fehler bei Zeitangabe
                allesInOrdnung=False
            if allesInOrdnung:
                try :
                    daten = file (filename,"r") #Daten aus Datei auslesen                                 
                    liste = daten.readlines()
                    daten.close()
                except:
                    print("Datei "+filename+" konnte nicht geöffnet werden!")
                    allesInOrdnung=False
                    
            if allesInOrdnung:
                try :
                    connection = MySQLdb.connect('localhost',user,passwd,db) #Herstellen der Verbindung zur Datenbank:
    
                    zwischenListe=[0,0,0,0,0,0,0,0,0,0,0]
                    for zeile in range(1, len(liste)):
                        liste[zeile]= liste[zeile].split('\t')
                        zwischenListe[0]=ipZuZahl(liste[zeile][1])                                                #srcIP
                        zwischenListe[1]=ipZuZahl(liste[zeile][2])                                                #dstIP
                        zwischenListe[2]=int(liste[zeile][4])                                                     #srcPort
                        zwischenListe[3]=int(liste[zeile][5])                                                     #dstPort
                        zwischenListe[4]=int(liste[zeile][6])                                                     #proto
                        zwischenListe[5]=0                                                                        #dstTos
                        zwischenListe[6]=int(liste[zeile][7])                                                     #bytes
                        zwischenListe[7]=int(liste[zeile][8])                                                     #pkts

                        liste[zeile][9]=liste[zeile][9].split()             # entfernen der Millisek.-Angabe
                        zwischenListe[8]=int(float(liste[zeile][9][0]))+startzeit                                 #firstSwitched

                        liste[zeile][10]=liste[zeile][10].split()           # entfernen der Millisek.-Angabe                                             
                        zwischenListe[9]=int(float(liste[zeile][10][0]))+startzeit                                #lastSwitched

                        zwischenListe[10]=exporterID(long(liste[zeile][11]), ipZuZahl(liste[zeile][12]))          #exporterID
                    
                        liste[zeile]=copy.deepcopy(zwischenListe)
                                     
                    connection.cursor().close()
                    return liste
                except:
                    print ("Datenbank "+db+" konnte als Benutzer "+user+" nicht geöffnet werden!")
                    return False
            else:
                return False

    def erzeugeTabelle (liste):

        #Herstellen der Verbindung zur Datenbank:
        connection = MySQLdb.connect('localhost',user,passwd,db)

        #Erzeugen der Tabelle 
        connection.cursor().execute('create table if not exists h_'+starttime+' (srcIP integer(10) unsigned,\
                                                                    dstIP integer(10) unsigned,\
                                                                    srcPort smallint (5),\
                                                                    dstPort smallint (5),\
                                                                    proto tinyint (3),\
                                                                    dstTos tinyint (3),\
                                                                    bytes bigint (20),\
                                                                    pkts bigint (20),\
                                                                    firstSwitched int (10),\
                                                                    lastSwitched int (10),\
                                                                    exporterID smallint (5));')
        # Eintragen der Daten in die Tabelle
        for zeile in range (1, len(liste)):
            connection.cursor().execute ('insert into h_'+starttime+' values (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s);',
            (liste[zeile][0],liste[zeile][1],liste[zeile][2],liste[zeile][3],liste[zeile][4],\
             liste[zeile][5],liste[zeile][6],liste[zeile][7],liste[zeile][8],liste[zeile][9],liste[zeile][10]))

    
        connection.cursor().close()

    #Programmaufruf

    liste=leseDaten(filename)
    #Wenn Daten fehlerfrei eingelesen(liste!=0), trage sie in die Tabelle ein
    if liste:           
        erzeugeTabelle (liste)
        print ("Umwandlung erfolgreich, Halbstundentabelle unter h_"+ starttime+" abgespeichert.")
   

passwd ='' 
filename=''
user=''
db=''
starttime=''

for i in range(1, len(sys.argv)):  # Einlesen der Kommandozeilenargumente
    if   sys.argv[i]=='-file':
         filename=sys.argv[i+1]
    elif sys.argv[i]=='-db':
         db=sys.argv[i+1]
    elif sys.argv[i]=='-user':
         user=sys.argv[i+1]
    elif sys.argv[i]=='-pw':
         passwd=sys.argv[i+1]
    elif sys.argv[i]=='-starttime':
         starttime=sys.argv[i+1]




if (filename and db and user and starttime):                              
    transformation (filename, db, user, passwd, starttime)
    
else:
    print ('''Unzureichende oder fehlende Argumente, bitte beachten sie folgende Syntax:    
    transform.py -file (Name der Quelldatei)
                 -db (Name der Datenbank)
                 -user (Benutzername)
                 (-pw (Passwort))
                 -starttime (im Format yyyymmdd_hh_a (a=0 für erste, a=1 für zweite Hälfte der stunde)''')
