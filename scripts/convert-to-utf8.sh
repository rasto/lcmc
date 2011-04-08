for f in `find . -name \*.java`; do if `file $f|grep -s ISO-8859 >/dev/null`; then echo $f; perl -pi -e 'use Encode; $_=encode_utf8($_)' $f;fi;done
