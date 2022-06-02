FROM openjdk:11.0.15-jre
LABEL maintainer="admin@dreifa.com"

EXPOSE 11601

# Install terraform
# See https://mdawar.dev/blog/install-terraform-manually
RUN wget https://releases.hashicorp.com/terraform/1.2.2/terraform_1.2.2_linux_amd64.zip
RUN unzip terraform_1.2.2_linux_amd64.zip
RUN mv terraform /usr/local/bin/terraform


# Install the app
RUN mkdir -p /home/app/
COPY ./docker/run.sh /home/app/run.sh
RUN chmod +x /home/app/run.sh
COPY ./agent/build/libs/agent.jar /home/app/agent.jar
WORKDIR /home/app
ENTRYPOINT ["./run.sh"]
