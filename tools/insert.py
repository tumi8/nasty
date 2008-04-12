# -*- coding: utf-8 -*-
#Programm zum Kopieren und Einfügen von Datensätzen in Halbstunden- Tages- und Wochentabellen
#erstellt am: 26.5.2006
#letzte Änderung am 2.7.2006
#erstellt von: Manuel Meiborg (manuel.meiborg@gmx.de)
#Version 2.0
#
#Dieses Script ermöglicht es, bestimmte Intervalle von Netzverkehrsdaten innerhalb einer Datenbank zu kopieren. Die Ursprungs- und
#Zieltabellen sollten mit den Scripten transform.py und aggregator.py erzeugt sein. Kopieren ist nur von Halbstunden- nach Halnstunden-,
#Tages- nach Tages-, und Wochen- nach Wochentabelle möglich, es werden in der Zieltabelle keine Daten überschrieben. Falls für den
#angegebenen Zeitraum noch keine Tabellen existieren werden sie erstellt, so sie benötigt werden.Nach dem Einsetzen findet keine
#erneute Aggregation der Daten statt.
#

# Bemerkung:
# Nasty-Collector legt Tabellennamen im Sinne der lokalen Zeit an, ggf. auch in Sommerzeit.
# Dadurch wird die Umwandlung Zeitstempel<=>Tabellenname erschwert => sollte in zukuenftigen Versionen nicht mehr sein.
# Im Moment ist das im Programm fuer Halbstundentabellen beruecksichtigt.

import sys, time, MySQLdb, os

def insert (src_start_str, src_end_str, dst_start_str, typ):

    #Die vier Eckdaten in Sekundenangaben (interpretation der Zeitangabe als Ortszeit):
    src_start=int(time.mktime([int(src_start_str[0:4]), int(src_start_str[4:6]), int(src_start_str[6:8]), int(src_start_str[9:11]), \
                           int(src_start_str[11:13]), int(src_start_str[13:15]),0,0,-1]))
    src_end=int(time.mktime([int(src_end_str[0:4]), int(src_end_str[4:6]),int(src_end_str[6:8]), int(src_end_str[9:11]),\
                           int(src_end_str[11:13]), int(src_end_str[13:15]),0,0,-1]))
    dst_start=int(time.mktime([int(dst_start_str[0:4]), int(dst_start_str[4:6]), int(dst_start_str[6:8]), int(dst_start_str[9:11]),\
                           int(dst_start_str[11:13]), int(dst_start_str[13:15]),0,0,-1]))
    dst_end=dst_start+src_end-src_start

    print('Startzeit UTC: '+time.asctime(time.gmtime(src_start))+' Ortszeit: '+time.asctime(time.localtime(src_start)))
    print('Endzeit UTC: '+time.asctime(time.gmtime(src_end))+' Ortszeit: '+time.asctime(time.localtime(src_end)))
    print('Zielzeit UTC: '+time.asctime(time.gmtime(dst_start))+' Ortszeit: '+time.asctime(time.localtime(dst_start)))

    mysql.execute('show tables like \''+typ+'\\_%\';')
    tabellen=mysql.fetchall()
    zwischentabelle=typ+'_'+src_start_str+'_tmp'
    if (typ=='h'):
        mysql.execute('drop table if exists '+zwischentabelle+';')
        mysql.execute('create table '+zwischentabelle+'       (srcIP integer(10) unsigned,\
                                                                            dstIP integer(10) unsigned,\
                                                                            srcPort smallint (5) unsigned,\
                                                                            dstPort smallint (5) unsigned,\
                                                                            proto tinyint (3) unsigned,\
                                                                            dstTos tinyint (3) unsigned,\
                                                                            bytes bigint (20) unsigned,\
                                                                            pkts bigint (20) unsigned,\
                                                                            firstSwitched int (10) unsigned,\
                                                                            lastSwitched int (10) unsigned,\
                                                                            exporterID smallint (5) unsigned);')
    elif (typ=='d' or typ=='w'):
        mysql.execute('drop table if exists '+zwischentabelle+';')
        mysql.execute('create table '+zwischentabelle+'       (srcIP integer(10) unsigned,\
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

        
    #Einlesen der Datensätze in eine Zwischentabelle
    for i in range (0, len(tabellen)):
        if ((tabellen [i][0][0:10]>=typ+'_'+src_start_str[0:8]) and (tabellen [i][0][0:10]<=typ+'_'+src_end_str[0:8])):
            mysql.execute('insert into '+zwischentabelle+' (select * from '+tabellen[i][0]+' where firstSwitched>='+str(src_start)+' and  firstSwitched<='+str(src_end)+');')
    #Anpassen der Zeitstempel:
    mysql.execute('update '+zwischentabelle+' set firstSwitched=firstswitched-'+str(src_start)+'+'+str(dst_start)+',\
                  lastswitched=lastSwitched-'+str(src_start)+'+'+str(dst_start)+';')

    
    if (typ=='h'):
    #Einsetzen der veränderten Datensätze in die Zieltabellen, alle Halbstundentabellen im Zeitraum werden erzeugt,
    #die Datensätze werden eingetragen und leere Tabellen anschließend gelöscht    
        print("Veränderte Halbstundentabellen:")
        #tab_beginn=erste Sekunde der ersten halben Stunde im einzusetzenden Bereich
        tab_beginn=int(time.mktime([time.localtime(dst_start)[0],time.localtime(dst_start)[1],time.localtime(dst_start)[2],time.localtime(dst_start)[3],0,0,0,0,-1])) 
        #tab_ende=letzte Sekunde der letzen halben Stunde im einzusetzenden Bereich
        tab_ende=int(time.mktime([time.localtime(dst_end)[0],time.localtime(dst_end)[1],time.localtime(dst_end)[2],time.localtime(dst_end)[3],59,59,0,0,-1])) 
        for i in range (tab_beginn,tab_ende,1800): 
            datum=time.localtime(i)
            tabname='h_'+str(datum[0])+str(datum[1]).rjust(2).replace(' ','0')+str(datum[2]).rjust(2).replace(' ','0')+'_'+str(datum[3]).rjust(2).replace(' ','0')+'_'
            if (datum[4]<30):
                tabname=tabname+'0'
           
            else:
                tabname=tabname+'1'
               
            mysql.execute('create table IF NOT EXISTS '+tabname+'       (srcIP integer(10) unsigned,\
                                                                            dstIP integer(10) unsigned,\
                                                                            srcPort smallint (5) unsigned,\
                                                                            dstPort smallint (5) unsigned,\
                                                                            proto tinyint (3) unsigned,\
                                                                            dstTos tinyint (3) unsigned,\
                                                                            bytes bigint (20) unsigned,\
                                                                            pkts bigint (20) unsigned,\
                                                                            firstSwitched int (10) unsigned,\
                                                                            lastSwitched int (10) unsigned,\
                                                                            exporterID smallint (5) unsigned);')
            mysql.execute('insert into '+tabname+' (select * from '+zwischentabelle+' where \
            firstSwitched>='+str(i)+' and firstSwitched<='+str(i+1800)+');')  
            mysql.execute('select * from '+tabname+';')
            tabelle=mysql.fetchall()
            if not tabelle:
                mysql.execute('drop table '+tabname+';')            
            else:
                print(tabname)

    if (typ=='d'):
    #Einsetzen der veränderten Datensätze in die Zieltabellen, alle Tagestabellen im Zeitraum werden erzeugt,
    #die Datensätze werden eingetragen und leere Tabellen anschließend gelöscht    
        print("Veränderte Tagestabellen:")
        #tab_beginn=erste Sekunde des ersten Tages im einzusetzenden Bereich
        tab_beginn=int(time.mktime([time.localtime(dst_start)[0],time.localtime(dst_start)[1],time.localtime(dst_start)[2],0,0,0,0,0,-1])) 
        #tab_ende=letzte Sekunde des letzten Tages im einzusetzenden Bereich
        tab_ende=int(time.mktime([time.localtime(dst_end)[0],time.localtime(dst_end)[1],time.localtime(dst_end)[2],23,59,59,0,0,-1]))         
        for i in range (tab_beginn,tab_ende,86400):
            datum=time.localtime(i)
            tabname='d_'+str(datum[0])+str(datum[1]).rjust(2).replace(' ','0')+str(datum[2]).rjust(2).replace(' ','0')
            mysql.execute('create table IF NOT EXISTS '+tabname+'       (srcIP integer(10) unsigned,\
                                                                            dstIP integer(10) unsigned,\
                                                                            srcPort smallint (5) unsigned,\
                                                                            dstPort smallint (5) unsigned,\
                                                                            proto tinyint (3) unsigned,\
                                                                            bytes bigint (20) unsigned,\
                                                                            pkts bigint (20) unsigned,\
                                                                            firstSwitched int (10) unsigned,\
                                                                            lastSwitched int (10) unsigned,\
                                                                            exporterID smallint (5) unsigned, \
                                                                            aggFlows SMALLINT(5) UNSIGNED);')
            mysql.execute('insert into '+tabname+' (select * from '+zwischentabelle+' where \
	    firstSwitched>='+str(i)+' and firstSwitched<='+str(i+86400)+');')  
            mysql.execute('select * from '+tabname+';')
            tabelle=mysql.fetchall()
            if not tabelle:
                mysql.execute('drop table '+tabname+';')            
            else:
                print(tabname)
                
    if (typ=='w'):
    #Einsetzen der veränderten Datensätze in die Zieltabellen, alle Wochentabellen im Zeitraum werden erzeugt,
    #die Datensätze werden eingetragen und leere Tabellen anschließend gelöscht    
        print("Veränderte Wochentabellen:")
        #tab_beginn=sicherheitshalber eine Woche vor der erste Sekunde des angegebenen ersten Tages im einzusetzenden Bereich
        tab_beginn=int(time.mktime([time.localtime(dst_start)[0],time.localtime(dst_start)[1],time.localtime(dst_start)[2],0,0,0,0,0,-1]))-604800 
        #tab_ende=sicherheitshalber eine Woche nach der letzten Sekunde des angegebenen letzten Tages im einzusetzenden Bereich
        tab_ende=int(time.mktime([time.localtime(dst_end)[0],time.localtime(dst_end)[1],time.localtime(dst_end)[2],23,59,59,0,0,-1]))+604800 
        for i in range (dst_start,dst_end,604800):
            #Berechnen des Montags
            wochentag=time.localtime(i)[6]            
            montag=time.localtime(i - (wochentag * 24 * 3600)) 
            

            tabname='w_'+str(montag[0])+str(montag[1]).rjust(2).replace(' ','0')+str(montag[2]).rjust(2).replace(' ','0')

            mysql.execute('create table IF NOT EXISTS '+tabname+'       (srcIP integer(10) unsigned,\
                                                                            dstIP integer(10) unsigned,\
                                                                            srcPort smallint (5) unsigned,\
                                                                            dstPort smallint (5) unsigned,\
                                                                            proto tinyint (3) unsigned,\
                                                                            bytes bigint (20) unsigned,\
                                                                            pkts bigint (20) unsigned,\
                                                                            firstSwitched int (10) unsigned,\
                                                                            lastSwitched int (10) unsigned,\
                                                                            exporterID smallint (5) unsigned, \
                                                                            aggFlows SMALLINT(5) UNSIGNED);')
            mysql.execute('insert into '+tabname+' (select * from '+zwischentabelle+' where firstSwitched>='+str(i)+' and firstSwitched<=\
            '+str(i+604800)+');')  
            mysql.execute('select * from '+tabname+';')
            tabelle=mysql.fetchall()
            if not tabelle:
                mysql.execute('drop table '+tabname+';') 
            else:
                print(tabname)
    mysql.execute('drop table '+zwischentabelle+';')




pw=''
db=''
user=os.environ['LOGNAME']
src_start=''
src_end=''
dst_start=''
typ=''
for i in range(1, len(sys.argv)):  # Einlesen der Kommandozeilenargumente
    if   sys.argv[i]=='-db':
         db=sys.argv[i+1]
    elif sys.argv[i]=='-user':
         user=sys.argv[i+1]
    elif sys.argv[i]=='-pw':
         pw=sys.argv[i+1]
    elif sys.argv[i]=='-d':         
         typ='d'  
    elif sys.argv[i]=='-w':
         typ='w'
    elif sys.argv[i]=='-h':
         typ='h'
    elif sys.argv[i]=='-src_start':
         src_start=sys.argv[i+1]
    elif sys.argv[i]=='-src_end':
         src_end=sys.argv[i+1]
    elif sys.argv[i]=='-dst_start':
         dst_start=sys.argv[i+1]


print('Achtung! Dieses Skript geht noch von Tabellennamen gemaess Ortszeit aus.')


if (db and user and typ and (len(src_start)==15) and (len(src_end)==15) and (len(dst_start)==15)):
    try:
        connection = MySQLdb.connect('localhost',user,pw,db)
        mysql=connection.cursor()
        insert(src_start, src_end, dst_start, typ)
        mysql.close()

    except:
        print('Datenbank '+db+' konnte unter Benutzername '+user+' nicht geöffnet werden!')


        
    
else:
    print('''Angaben unvollständig oder fehlerhaft ,bitte beachten sie folgende Syntax:
             -db            (Name der Datenbank)
             -user          (Benutzername)
             -pw            (Passwort)
             -h | -d | -w   (Bearbeiten von Halbstunden- Tages- oder Wochentabellen)

             -src_start     (Beginn des zu kopierenden Intervalls in der Form yyyymmdd_hhmmss)
             -src_end       (Ende des zu kopierenden Intervalls in der Form yyyymmdd_hhmmss)
             -dst_start     (Zeitpunkt, ab dem das Intervall eingefügt werden soll (in der Form yyyymmdd_hhmmss))
             ''')
