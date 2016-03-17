FROM golang:1.5

RUN go get -v github.com/BurntSushi/toml/cmd/tomlv
RUN go get -v github.com/bitly/go-nsq

ENV SRC $GOPATH/src/gitlab.socrate.vsct.fr/dt/haaasd

RUN mkdir -p $SRC
WORKDIR /$SRC

COPY src ./