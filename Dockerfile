FROM openjdk:11

WORKDIR /

COPY slack-shuffle-bot-0.2.0-SNAPSHOT-standalone.jar .
EXPOSE 3000

CMD java -cp slack-shuffle-bot-0.2.0-SNAPSHOT-standalone.jar: clojure.main -m davidsierradz.slack-shuffle-bot
