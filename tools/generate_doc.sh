#!/bin/bash
if [ ! -d "external" ]
then
    echo "You must run this tool from the root directory of your repo clone"
else
    javadoc -d doc -author -version -verbose -use -doctitle "SalesforceSDK 2.2 API" -sourcepath "native/SalesforceSDK/src:hybrid/SmartStore/src" -subpackages com
fi
