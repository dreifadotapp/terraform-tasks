#!/bin/bash

exec java -jar -Xmx256m \
  -XX:+HeapDumpOnOutOfMemoryError \
	-XX:+PrintGC \
  /home/app/agent.jar