FROM azul/zulu-openjdk:11

VOLUME /tmp
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/energy-webapp/energy-webapp.jar

ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/share/energy-webapp/energy-webapp.jar"]

