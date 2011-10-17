for f in `find . -name \*.java`; do if `file --mime-encoding $f|grep -si ISO-8859 >/dev/null`; then echo $f; perl -pi -e 'use Encode; $_=encode_utf8($_)' $f;fi;done
