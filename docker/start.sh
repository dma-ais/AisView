BACKUPP="-backup ${BACKUP}"

if [ -z "${DATABASE}" ]; then 
	DATABASEP=""
    
else
	DATABASEP="-database ${DATABASE}" 
fi
LATEST=`ls /archive/target/ais-view*SNAPSHOT.jar`
java -jar $LATEST $SOURCES $BACKUPP $DATABASEP
