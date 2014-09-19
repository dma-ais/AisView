cd /AisView

BACKUPP="-backup ${BACKUP}"

if [ -z "${DATABASE}" ]; then 
	DATABASEP=""
    
else
	DATABASEP="-database ${DATABASE}" 
fi

java -jar target/ais-view-0.2-SNAPSHOT.jar $SOURCES $BACKUPP $DATABASEP
