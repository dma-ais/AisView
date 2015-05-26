BACKUPP="-backup ${BACKUP}"

if [ -z "${DATABASE}" ]; then 
	DATABASEP=""
    
else
	DATABASEP="-seeds ${DATABASE}"
fi
LATEST=`ls /archive/target/ais-view*SNAPSHOT.jar`
java -jar $LATEST $SOURCES $BACKUPP $DATABASEP
