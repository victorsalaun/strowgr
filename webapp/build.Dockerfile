FROM golang:1.5

ENV SRC /go/src/gitlab.socrate.vsct.fr/dt/haaas

RUN mkdir -p $SRC
WORKDIR $SRC

COPY . ./