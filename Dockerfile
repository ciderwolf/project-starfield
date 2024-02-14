# build step
FROM alpine:3.19 AS build

RUN apk add nodejs
RUN apk add yarn
RUN apk add openjdk17
RUN apk add gradle

WORKDIR /app
COPY client /app/client

WORKDIR /app/client
RUN yarn
RUN yarn build

WORKDIR /app
RUN mkdir -p /app/build
RUN cp -r /app/client/dist /app/build/client-app


COPY server /app/server
WORKDIR /app/server
RUN gradle buildFatJar

WORKDIR /app/server/build/libs
RUN cp starfield.starfield-all.jar /app/build/server.jar

# Run step
FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/build /app

EXPOSE 8080 2222
ENTRYPOINT ["java", "-jar", "server.jar"]
