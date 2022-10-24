FROM clojure AS builder

WORKDIR /app

COPY . .

RUN clojure -M:uberjar

FROM openjdk:17

WORKDIR /app

COPY --from=builder /app/slack-shuffle-bot.jar .

CMD java -cp slack-shuffle-bot.jar: clojure.main -m davidsierradz.slack-shuffle-bot
