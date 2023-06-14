FROM bellsoft/liberica-openjre-alpine-musl:11.0.19
COPY jars/trader-assembly-0.1.0-SNAPSHOT.jar app.jar
CMD ["java", "-cp", "app.jar", "ru.mikhaildruzhinin.trader.Trader"]
