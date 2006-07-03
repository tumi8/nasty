# -*- coding: utf-8 -*-
#Programm zur Aggregation von Halbstundentabllen in Tages- und Wochentabellen
#erstellt am: 10.5.2006
#letzte Änderung am 1.7.2006
#erstellt von: Manuel Meiborg (manuel.meiborg@gmx.de)
#Version 3.0
#
#Dieses Script aggregiert Netzverkehrsdaten, die in Halbstundentabellen vorliegen (mit dem script transform.py erzeugt) in
#Tagestabellen und diese wiederum in Wochentabellen. Dabei wird Verkehr, der in den Attributen srcIP, dstIP, srcPort, dstPort, proto und exporterID
#gleich ist, zusammengefasst, bytes und pkts werden addiert und nur die Eckdaten in firstSwitched und lastSwitched gespeichert.
#Bei der Aggregation in Wochentabellen werden zusätzlich noch uninteressante (i.e. größer als 1024) zusammengefasst. Das Script erlaubt auch
#die automatische Aggregation aller Daten in einem bestimmten Zeitraum
#



import MySQLdb, sys, time

def erzeugeTagestabelle (day): #day enthält das Datum des zu bearbeitenden Tages als string der Form yyyymmdd

    def leseEin (): #Speichern aller Tabellen des Tages in einer Zwischenliste
        geloescht=0
        for i in range (0, len(tabellen)):
            mysql.execute('INSERT INTO '+tagestabelle+'_tmp (select sql_big_result srcIP, dstIP, srcPort, dstPort, proto, bytes,\
                          pkts, firstSwitched, lastSwitched, exporterID from '+tabellen[i][0]+');')
            mysql.execute('drop table '+tabellen[i][0]+';')
            geloescht=geloescht+1
        return geloescht
        
    def fuegeEin(): #Aggregation der Daten
        
        mysql.execute("INSERT INTO " + tagestabelle + " (SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto,\
                        SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, COUNT(*)\
                        FROM "+tagestabelle+"_tmp GROUP BY srcIP, dstIP, srcPort, dstPort, proto, exporterID);")
        mysql.execute('drop table '+tagestabelle+'_tmp;')


    
    tagestabelle='d_'+day
    mysql.execute ('show tables like \''+ 'h_'+ day +'%\';')
    tabellen=mysql.fetchall()
    if tabellen:
        mysql.execute('create table IF NOT EXISTS '+tagestabelle+'_tmp   (srcIP integer(10) unsigned,\
                                                                            dstIP integer(10) unsigned,\
                                                                            srcPort smallint (5) unsigned,\
                                                                            dstPort smallint (5) unsigned,\
                                                                            proto tinyint (3) unsigned,\
                                                                            bytes bigint (20) unsigned,\
                                                                            pkts bigint (20) unsigned,\
                                                                            firstSwitched int (10) unsigned,\
                                                                            lastSwitched int (10) unsigned,\
                                                                            exporterID smallint (5) unsigned);')

        mysql.execute('create table IF NOT EXISTS '+tagestabelle+' (srcIP integer(10) unsigned,\
                                                                            dstIP integer(10) unsigned,\
                                                                            srcPort smallint (5) unsigned,\
                                                                            dstPort smallint (5) unsigned,\
                                                                            proto tinyint (3) unsigned,\
                                                                            bytes bigint (20) unsigned,\
                                                                            pkts bigint (20) unsigned,\
                                                                            firstSwitched int (10) unsigned,\
                                                                            lastSwitched int (10) unsigned,\
                                                                            exporterID smallint (5) unsigned,\
                                                                            aggFlows SMALLINT(5) UNSIGNED);')
        geloescht=leseEin ()
        fuegeEin ()
  
        print('Tagestabelle '+tagestabelle+' erzeugt. '+str(geloescht)+' Tabellen entfernt.')






def erzeugeWochentabelle (year,month,day):
    
    def leseEin (monday_year,monday_month,monday_day): #Speichern aller Tabellen der Woche in einer Zwischenliste
        mysql.execute ('show tables like \'d_%\';')
        tabellen=mysql.fetchall()
        sonntag=time.gmtime(time.mktime([monday_year, monday_month, monday_day,0,0,0,0,0,0])+518400) #518400 sek von Montag bis Sonntag morgen
        geloescht=0
        for i in range (0, len(tabellen)):
            monat=int(tabellen[i][0][6]+tabellen[i][0][7]) #Zusammenfassen der Ziffern der Monatssangabe

            tag=int(tabellen[i][0][8]+tabellen[i][0][9]) #Zusammenfassen der Ziffern der Tagesangabe

            jahr = int(tabellen[i][0][2]+tabellen[i][0][3]+tabellen[i][0][4]+tabellen[i][0][5])
                
            # Tabelle innerhalb der sechs Tage nach dem Montag
            if (((jahr==monday_year) and (monat==monday_month) and (tag in range (monday_day, monday_day+7)))
            # oder Tabelle innerhalb der sechs Tage vor dem Sonntag
            or ((jahr==sonntag[0]) and (monat==sonntag[1]) and (tag in range(sonntag[2]-6,sonntag[2]+1)))):
                mysql.execute('create table IF NOT EXISTS '+wochentabelle+'_tmp             (srcIP integer(10) unsigned,\
                                                                            dstIP integer(10) unsigned,\
                                                                            srcPort smallint (5) unsigned,\
                                                                            dstPort smallint (5) unsigned,\
                                                                            proto tinyint (3) unsigned,\
                                                                            bytes bigint (20) unsigned,\
                                                                            pkts bigint (20) unsigned,\
                                                                            firstSwitched int (10) unsigned,\
                                                                            lastSwitched int (10) unsigned,\
                                                                            exporterID smallint (5) unsigned,\
                                                                            aggFlows SMALLINT(5) UNSIGNED);')

                mysql.execute('INSERT INTO '+wochentabelle+'_tmp (select srcIP, dstIP, srcPort, dstPort, proto, bytes,\
                              pkts, firstSwitched, lastSwitched, exporterID, aggFlows from '+tabellen[i][0]+');')
                mysql.execute('drop table '+tabellen[i][0]+';')
                geloescht=geloescht+1
            
        return geloescht

    def fuegeEin(): #Aggregation der Daten
        
       mysql.execute('create table IF NOT EXISTS '+wochentabelle+'         (srcIP integer(10) unsigned,\
                                                                            dstIP integer(10) unsigned,\
                                                                            srcPort smallint (5) unsigned,\
                                                                            dstPort smallint (5) unsigned,\
                                                                            proto tinyint (3)unsigned,\
                                                                            bytes bigint (20) unsigned,\
                                                                            pkts bigint (20) unsigned,\
                                                                            firstSwitched int (10) unsigned,\
                                                                            lastSwitched int (10) unsigned,\
                                                                            exporterID smallint (5) unsigned,\
                                                                            aggFlows SMALLINT(5) UNSIGNED);')

       mysql.execute("INSERT INTO " + wochentabelle + " (SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto,\
                        SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, SUM(aggFlows) \
                        FROM "+wochentabelle+"_tmp WHERE srcPort<1024 AND dstPort<1024 GROUP BY srcIP, dstIP, srcPort, dstPort, proto, exporterID);")

       mysql.execute("INSERT INTO " + wochentabelle + " (SELECT SQL_BIG_RESULT srcIP, dstIP, 0, dstPort, proto,\
                        SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, SUM(aggFlows) \
                        FROM "+wochentabelle+"_tmp WHERE srcPort>=1024 AND dstPort<1024 GROUP BY srcIP, dstIP, dstPort, proto, exporterID);")

       mysql.execute("INSERT INTO " + wochentabelle + " (SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, 0, proto,\
                        SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID,SUM(aggFlows) \
                        FROM "+wochentabelle+"_tmp WHERE srcPort<1024 AND dstPort>=1024 GROUP BY srcIP, dstIP, srcPort, proto, exporterID);")

       mysql.execute("INSERT INTO " + wochentabelle + " (SELECT SQL_BIG_RESULT srcIP, dstIP, 0, 0, proto,\
                        SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, SUM(aggFlows) \
                        FROM "+wochentabelle+"_tmp WHERE srcPort>=1024 AND dstPort>=1024 GROUP BY srcIP, dstIP, proto, exporterID);")


       print('Wochentabelle ' +wochentabelle+ ' erzeugt. '+str(geloescht)+' Tabellen entfernt.')

    starttag=time.gmtime(time.mktime([year, month, day,0,0,0,0,0,0]))                   #angegebener Tag
    montag=time.mktime([year, month, day,0,0,0,0,0,0]) - starttag[6] * 24 * 3600        #Montag, 0 Uhr
    montag_jahr=time.gmtime(montag)[0]                                                  #   
    montag_monat=time.gmtime(montag)[1]                                                 #Datum des Montags
    montag_tag=time.gmtime(montag)[2]                                                   #   

    wochentabelle='w_'+str(montag_jahr)+str(montag_monat).rjust(2).replace(' ','0')+str(montag_tag).rjust(2).replace(' ','0')
    
    geloescht=leseEin (montag_jahr,montag_monat,montag_tag)
    if geloescht>0:
        fuegeEin ()
        mysql.execute('drop table if exists '+wochentabelle+'_tmp;')

#Hauptprogramm
beginn=0                #beginn auf 1.1.1972 setzen
ende=int(time.time())   #end end auf aktuelle Systemzeit setzen
pw =''
db =''
user=''
day=''
week=''
startday=''
startweek=''
endday=''
endweek=''
ok=True

for i in range(1, len(sys.argv)):  # Einlesen der Kommandozeilenargumente
    if   sys.argv[i]=='-db':
         db=sys.argv[i+1]
    elif sys.argv[i]=='-user':
         user=sys.argv[i+1]
    elif sys.argv[i]=='-pw':
         pw=sys.argv[i+1]
    elif sys.argv[i]=='-day':
         day=sys.argv[i+1]
    elif sys.argv[i]=='-week':
         week=sys.argv[i+1]
    elif sys.argv[i]=='-startday':
         startday=sys.argv[i+1]
    elif sys.argv[i]=='-endday':        
         endday=sys.argv[i+1]  
    elif sys.argv[i]=='-startweek':
         startweek=sys.argv[i+1]
    elif sys.argv[i]=='-endweek':
         endweek=sys.argv[i+1]
                    
if user and db and (week or day or startday or endday or startweek or endweek):
    
    try:
        connection = MySQLdb.connect('localhost',user,pw,db)
        mysql=connection.cursor()
        ok=True
    except:
        ok= False
        print('Datenbank '+db+' konnte unter Benutzername '+user+' nicht geöffnet werden!')

else:
    print ('''Unzureichende oder fehlende Argumente, bitte beachten sie folgende Syntax:    
                  aggregator.py
                  -db       (Name der Datenbank)
                  -user     (Benutzername)
                  -pw       (Passwort)
                  
                  -day |     (im Format yyyymmdd anzugeben, erzeugt eine aggregierte Tagestabelle)
                  -week |    (einen Tag der Woche im format yyyymmdd angeben, erzeugt eine aggregierte Wochentabelle)
                  -startday (yyyymmdd, erzeugt alle Tagestabellen ab diesem Datum bis heute bzw bis endday)
                  -endday |  (yyyymmdd, erzeugt alle Tagestabellen bis diesem Datum seit dem 1.1.1972 bzw ab startday)
                  -startweek(yyyymmdd, erzeugt alle Wochentabellen ab diesem Datum bis heute bzw bis endweek)
                  -endweek  (yyyymmdd, erzeugt alle Wochentabellen bis diesem Datum seit dem 1.1.1972 bzw ab startweek.
                             Die letzte Wochentabelle wird dabei nur erzeugt, wenn der angegebene Tag ein Sonntag ist)


                  
                  ''')
    ok=False
if ok:
    
    #Erzeugen einer Tagestabelle
    if day:                              
        erzeugeTagestabelle (day)
    


    #Erzeugen einer Wochentabelle      
    elif week:                              
        erzeugeWochentabelle (int(week[0:4]), int(week[4:6]), int(week[6:8]))


    #Erzeugen aller Tagestabellen innnerhalb eines bestimmten Bereichs
    elif startday or endday:
        if startday:
            beginn=int(time.mktime([int(startday[0:4]), int(startday[4:6]), int(startday[6:8]),0,0,0,0,0,0]))
        if endday:
            ende=int(time.mktime([int(endday[0:4]), int(endday[4:6]), int(endday[6:8]),23,59,59,0,0,0]))
        for i in range (beginn+86400,ende+86400,86400):
            erzeugeTagestabelle (str(time.gmtime(i)[0]) + str(time.gmtime(i)[1]).rjust(2).replace(' ','0') + str(time.gmtime(i)[2]).rjust(2).replace(' ','0'))
           
    #Erzeugen aller Wochentabellen innerhalb eines bestimmten Zeitraums
    elif startweek or endweek:
        if startweek:
            beginn=int(time.mktime([int(startweek[0:4]), int(startweek[4:6]), int(startweek[6:8]),0,0,0,0,0,0]))
        if endweek:
            ende=int(time.mktime([int(endweek[0:4]), int(endweek[4:6]), int(endweek[6:8]),23,59,59,0,0,0]))

        endWochentag=time.gmtime(ende)[6]      #letzte Wochentabelle wird nur erzeugt, wenn 'ende' ein Sonntag ist
        if (endWochentag!=6):
            ende=ende-(endWochentag+1)*86400
        
        for i in range (beginn,ende,604800):
            erzeugeWochentabelle (time.gmtime(i)[0], time.gmtime(i)[1], time.gmtime(i)[2])       

                      
             
    mysql.close()
