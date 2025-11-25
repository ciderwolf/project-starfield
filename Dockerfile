# build step
FROM fedora:40 AS build

RUN dnf upgrade -y
RUN dnf install -y nodejs 
RUN dnf install -y java-17-openjdk
RUN dnf install -y which
RUN npm install -g yarn

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
RUN ./gradlew buildFatJar

WORKDIR /app/server/build/libs
RUN cp starfield.starfield-all.jar /app/build/server.jar

# Run step
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/build /app

# enable ssh
COPY sshd_config /etc/ssh/
COPY entrypoint.sh ./

# Start and enable SSH
RUN apt-get update \
    && apt-get install -y --no-install-recommends dialog \
    && apt-get install -y --no-install-recommends openssh-server \
    && echo "root:Docker!" | chpasswd \
    && chmod u+x ./entrypoint.sh

EXPOSE 8000 2222

ENTRYPOINT [ "./entrypoint.sh" ] 
