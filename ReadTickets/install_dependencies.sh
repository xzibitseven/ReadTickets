#!/bin/bash

sudo apt update
sudo apt install default-jre
sudo apt install default-jdk
sudo apt install libjson-simple-java
sudo apt install libjcommander-java
export CLASSPATH=/usr/share/java/json-simple-2.3.0.jar:/usr/share/java/jcommander-1.71.jar
