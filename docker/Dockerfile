FROM kaspernielsen/javabox

RUN apt-get install maven -y
RUN mkdir -p /data
RUN git clone https://github.com/dma-ais/AisView /AisView
RUN (cd /AisView; mvn clean install)
RUN mkdir -p /aisview-backup
ADD ./start.sh /start.sh
EXPOSE 8090
ENV BACKUP /aisview-backup
CMD ["/bin/bash", "/start.sh"]
