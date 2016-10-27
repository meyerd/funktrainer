#!/usr/bin/env python

# read the funkfragen_nontransform.xml and find the corresponding
# page in the Lichtblick-A.pdf and add to the question tag
# as a new attribute lichtblick
#
# generate Lichtblick-A.txt with pdftotext Lichtblick-A.pdf Lichtblick-A.txt
#
# Warning: very slow and stupid implementation in inner loop

import sys
import re
import PyPDF2


LICHTBLICK_FILE = 'Lichtblick-A.pdf'

question_re = re.compile(r'(.*?)<question id="(.*?)".*')


def parse_and_output(funkfragen):
    with open(funkfragen, 'r') as f:
        for line in f:
            ret = question_re.match(line)
            if ret:
                spaces = ret.groups()[0]
                reference = ret.groups()[1]
                page = 0
                with open(LICHTBLICK_FILE, 'rb') as lf:
                    fr = PyPDF2.PdfFileReader(lf, strict=False)
                    n_pages = fr.getNumPages()
                    for i in xrange(n_pages):
                        pg = fr.getPage(i)
                        txt = pg.extractText()
                        idx = txt.find(reference)
                        if idx > -1:
                            page = i
                            break
                print '%s<question id="%s" lichtblick="%i" >' % (spaces,
                                                                 reference,
                                                                 page)
            else:
                print line,


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print >>sys.stderr, "usage: %s <funkfragen_nontransform.xml> > funkfragen_nontransform_lichtblick.xml"
        sys.exit(1)
    
    funkfragen = sys.argv[1]
    parse_and_output(funkfragen)

