#!/bin/bash
#
# Requirements:
# - Platypus `brew install platypus` http://www.sveinbjorn.org/platypus
# - LCMC-1.7.8.jar in current working directory

/usr/local/bin/platypus -R  -i '../scripts/lcmc-logo-icon.icns' -a 'LCMC'  -o 'None' -p '/usr/bin/java' -I org.java.LCMC -G '-jar' 'LCMC-1.7.8.jar'
