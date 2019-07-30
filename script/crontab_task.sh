#!/usr/bin/env bash

echo "Start in `date`"

node login_by_puppeteer/index.js config.json

java -jar build/libs/bugly_crash_spider.jar config.json


echo "End in `date`"