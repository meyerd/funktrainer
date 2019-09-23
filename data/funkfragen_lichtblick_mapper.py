#!/usr/bin/env python

# read the reference_id.csv file and get the question_ids and
# corresponding reference and topic (1: technische fragen E, 2: technische fragen A)
# look up the question in Lichtblick-A.pdf and Lichtblick-E.pdf and add the
# corresponding page number and lichtblick type (0: A, 1: E) to the generated sql.
#
# Warning: very slow and stupid implementation in inner loop

from __future__ import print_function
import sys
import re
import PyPDF2
import pandas as pd


LICHTBLICK_FILE_A = 'Lichtblick-A.pdf'
LICHTBLICK_FILE_E = 'Lichtblick-E.pdf'


def parse_and_output(funkfragen):
    df = pd.read_csv(funkfragen)

    rowidx = 1

    for index, row in df.iterrows():
        id = row["_id"]
        reference = row["reference"]
        question_id = row["question_id"]
        fn = LICHTBLICK_FILE_E
        if row["topic_id"] == 2:
            fn = LICHTBLICK_FILE_A
        page = 0
        with open(fn, 'rb') as lf:
            fr = PyPDF2.PdfFileReader(lf, strict=False)
            n_pages = fr.getNumPages()
            for i in xrange(n_pages):
                pg = fr.getPage(i)
                txt = pg.extractText()
                idx = txt.find(reference)
                if idx > -1:
                    page = i
                    break
        lb_type = 0
        if row["topic_id"] == 1:
            lb_type = 1
        print('INSERT INTO question_to_lichtblick VALUES ({}, {}, {}, {});'.format(
            str(rowidx), question_id, str(page), str(lb_type)))
        rowidx += 1


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("usage: %s <reference_id.csv> > question_to_lichtblick.sql", file=sys.stderr)
        sys.exit(1)
    
    funkfragen = sys.argv[1]

    print("BEGIN;")
    print("")
    print("DROP TABLE IF EXISTS question_to_lichtblick;")
    print("")
    print("CREATE TABLE question_to_lichtblick (_id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(_id), lichtblick INT, lichtblick_type INT);")

    parse_and_output(funkfragen)

    print("")
    print("COMMIT;")

