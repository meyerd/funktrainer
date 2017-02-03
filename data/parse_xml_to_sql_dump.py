#!/usr/bin/env python

import sys
import os
import re
import xml.etree.ElementTree as ET

global_category_id_seq = 0
global_question_id_seq = 0
global_topic_id_seq = 0
global_category_to_topic_id_seq = 0
global_question_to_category_id_seq = 0
global_answer_id_seq = 0
global_question_to_lichtblick_id_seq = 0
global_question_to_topic_id_seq = 0
global_exam_settings_id_seq = 0

topics = []
categories = []
questions = []
answers = []
question_to_category = []
question_to_topic = []
category_to_topic = []
question_to_lichtblick = []
exam_settings = []

mixtopics = {u"Klasse E alle (Technik, Betrieb, Vorschriften)":
             [u"Technische Kenntnisse (Klasse E)",
              u"Betriebliche Kenntnisse",
              u"Kenntnisse von Vorschriften"],
            }

# [ nQuestions, nRequired, nSeconds ]
examsettings = {
    u"Technische Kenntnisse (Klasse E)":
      [34, 34 * .73, 60 * 60],
    u"Technische Kenntnisse (Klasse A)":
      [51, 51 * .73, 60 * 60],
    u"Betriebliche Kenntnisse":
      [34, 34 * .73, 60 * 60],
    u"Kenntnisse von Vorschriften":
      [34, 34 * .73, 60 * 60],
    # TODO: sampling here is definitely wrong, as we uniformly sample
    #       from all categories, but should sample 34 each from each 
    #       category
    u"Klasse E alle (Technik, Betrieb, Vorschriften)":
      [34 * 3, 34 * 3 * .73, 60 * 3 * 60],
}


def cleanuptagtext(text):
    r = re.sub(r'\n', " ", text)
    r = re.sub(r'[\t]{1,}', " ", r)
    r = re.sub(r'\'', "''", r)
    return r


def createExamsettings():
    global global_exam_settings_id_seq
    for k, v in examsettings.iteritems():
        topicname = k
        nquestions, nrequired, nseconds = v
        ref_topic_id = 0
        for topicid, order, name, primary in topics:
            if name == topicname:
                ref_topic_id = topicid
                break
        assert ref_topic_id != 0
        global_exam_settings_id_seq += 1
        exam_settings.append((global_exam_settings_id_seq, ref_topic_id,
                              nquestions, nrequired, nseconds))


def createMixtopics():
    global global_topic_id_seq
    global global_question_to_topic_id_seq
    global global_category_to_topic_id_seq
    for k, v in mixtopics.iteritems():
        mixtopicname = k
        global_topic_id_seq += 1
        mixtopicid = global_topic_id_seq
        mixtopicorder = global_topic_id_seq
        mixtopicprimary = 0
        for submix in v:
            for topicid, order, name, primary in topics:
                if name == submix:
                    for _, qid, tid in question_to_topic:
                        if tid == topicid:
                            global_question_to_topic_id_seq +=1
                            question_to_topic.append((global_question_to_topic_id_seq,
                                                      qid, mixtopicid))
                    for _, cid, tid in category_to_topic:
                        if tid == topicid:
                            global_category_to_topic_id_seq += 1
                            category_to_topic.append((global_category_to_topic_id_seq,
                                                     cid, mixtopicid))

        topics.append((mixtopicid, mixtopicorder, mixtopicname, mixtopicprimary))



def parseChapter(chapter, parent, topic_id, primary=0):
    global global_category_id_seq
    global global_category_to_topic_id_seq
    global global_question_to_category_id_seq
    global global_question_to_lichtblick_id_seq
    global global_question_id_seq
    global global_answer_id_seq
    global global_question_to_topic_id_seq
    name = chapter.get('name')
    reference = chapter.get('id')
    categoryid = global_category_id_seq

    categories.append((categoryid, name, reference, primary, parent))
    global_category_to_topic_id_seq += 1
    category_to_topic.append((global_category_to_topic_id_seq, categoryid,
                              topic_id))

    # if this chapter has only questions
    for question in chapter.findall('{http://funktrainer.hosenhasser.de}question'):
        global_question_id_seq += 1
        reference = question.get('id')
        next_time = 1
        correct = 0
        wrong = 0
        level = 0
        qhelp = ""
        lichtblick = int(question.get('lichtblick'))
        qtext = cleanuptagtext(question.find('{http://funktrainer.hosenhasser.de}textquestion').text)
        for aidx, answer in enumerate(question.findall('{http://funktrainer.hosenhasser.de}textanswer')):
            global_answer_id_seq += 1
            answers.append((global_answer_id_seq, global_question_id_seq, aidx,
                            cleanuptagtext(answer.text), " "))
        questions.append((global_question_id_seq, reference, qtext, level, next_time,
                          wrong, correct, qhelp))
        global_question_to_category_id_seq += 1
        question_to_category.append((global_question_to_category_id_seq,
                                     global_question_id_seq, categoryid))
        global_question_to_lichtblick_id_seq += 1
        question_to_lichtblick.append((global_question_to_lichtblick_id_seq,
                                       global_question_id_seq, lichtblick))
        global_question_to_topic_id_seq += 1
        question_to_topic.append((global_question_to_topic_id_seq,
                                  global_question_id_seq, topic_id))


    # if this chapter has only subchapters
    for subchapter in chapter.findall('{http://funktrainer.hosenhasser.de}chapter'):
        global_category_id_seq += 1
        parseChapter(subchapter, categoryid, topic_id) 


def main(funkfragen, output_dir):
    global global_category_id_seq
    global global_topic_id_seq
    global global_category_to_topic_id_seq
    tree = ET.parse(funkfragen)

    root = tree.getroot()

    for chapter in root.findall('{http://funktrainer.hosenhasser.de}chapter'):
        global_category_id_seq += 1
        global_topic_id_seq += 1
        topics.append((global_topic_id_seq, global_topic_id_seq,
                       chapter.get('name'), 1))
        global_category_to_topic_id_seq += 1
        category_to_topic.append((global_category_to_topic_id_seq,
                                  global_category_id_seq, global_topic_id_seq))
        parseChapter(chapter, 0, global_topic_id_seq, 1)

    createMixtopics()
    createExamsettings()
        
    with open(os.path.join(output_dir, 'scheme_and_data.sql'), 'w') as o:
        print >>o, "BEGIN;"
        print >>o, "DROP TABLE IF EXISTS topic;"
        print >>o, "DROP TABLE IF EXISTS category;"
        print >>o, "DROP TABLE IF EXISTS question;"
        print >>o, "DROP TABLE IF EXISTS answer;"
        print >>o, "DROP TABLE IF EXISTS question_to_category;"
        print >>o, "DROP TABLE IF EXISTS question_to_topic;"
        print >>o, "DROP TABLE IF EXISTS category_to_topic;"
        print >>o, "DROP TABLE IF EXISTS question_to_lichtblick;"
        print >>o, "DROP TABLE IF EXISTS topic_exam_settings;"
#        print >>o, "DROP TABLE IF EXISTS exam;"
        print >>o, ""
        print >>o, "CREATE TABLE topic (_id INT NOT NULL PRIMARY KEY, order_index INT NOT NULL UNIQUE, name TEXT NOT NULL, isprimary INT);"
        print >>o, "CREATE TABLE category (_id INT NOT NULL PRIMARY KEY, name TEXT NOT NULL, reference TEXT NOT NULL, isprimary INT, parent INT REFERENCES category(_id));"
        print >>o, "CREATE TABLE question (_id INT NOT NULL PRIMARY KEY, reference TEXT, question TEXT NOT NULL, level INT NOT NULL, next_time INT NOT NULL, wrong INT, correct INT, help TEXT);"
        print >>o, "CREATE TABLE answer (_id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(_id), order_index INT, answer TEXT NOT NULL, help TEXT);"
        print >>o, "CREATE TABLE question_to_category (_id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(_id), category_id INT NOT NULL REFERENCES category(_id));"
        print >>o, "CREATE TABLE question_to_topic (_id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(_id), topic_id INT NOT NULL REFERENCES topic(_id));"
        print >>o, "CREATE TABLE category_to_topic (_id INT NOT NULL PRIMARY KEY, category_id INT NOT NULL REFERENCES category(_id), topic_id INT NOT NULL REFERENCES topic(_id));"
        print >>o, "CREATE TABLE question_to_lichtblick (_id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(_id), lichtblick INT);"
        print >>o, "CREATE TABLE topic_exam_settings (_id INT NOT NULL PRIMARY KEY, topic_id INT NOT NULL REFERENCES topic(_id), number_questions INT, number_questions_pass INT, seconds_available INT);"
#        print >>o, "CREATE TABLE exam (_id INT NOT NULL PRIMARY KEY, topic_id INT NOT NULL REFERENCES topic(_id), time_started INT NOT NULL, time_left INT, exam_json STRING);"
        print >>o, ""
        # TODO: fix unicode
        for t in topics:
            bla = u"INSERT INTO topic VALUES (%i, %i, '%s', %i);" % t
            print >>o, bla.encode('utf-8')
        for c in categories:
            #print >>o, u"INSERT INTO category VALUES (%i, %s, %i, %i, %s);" % c
            #print >>o, u"INSERT INTO category VALUES ({}, {}, {}, {}, {});".format(*c)
            bla = u"INSERT INTO category VALUES ({}, '{}', '{}', {}, {});".format(*c)
            print >>o, bla.encode('utf-8')
        for q in questions:
            bla = u"INSERT INTO question VALUES (%i, '%s', '%s', %i, %i, %i, %i, '%s');" % q
            print >>o, bla.encode('utf-8')
        for a in answers:
            bla = u"INSERT INTO answer VALUES (%i, %i, %i, '%s', '%s');" % a
            print >>o, bla.encode('utf-8')
        for qtc in question_to_category:
            bla = u"INSERT INTO question_to_category VALUES (%i, %i, %i);" % qtc
            print >>o, bla.encode('utf-8')
        for qtt in question_to_topic:
            bla = u"INSERT INTO question_to_topic VALUES (%i, %i, %i);" % qtt
            print >>o, bla.encode('utf-8')
        for ctt in category_to_topic:
            bla = u"INSERT INTO category_to_topic VALUES (%i, %i, %i);" % ctt
            print >>o, bla.encode('utf-8')
        for qtl in question_to_lichtblick:
            bla = u"INSERT INTO question_to_lichtblick VALUES (%i, %i, %i);" % qtl
            print >>o, bla.encode('utf-8')
        for exs in exam_settings:
            bla = u"INSERT INTO topic_exam_settings VALUES (%i, %i, %i, %i, %i);" % exs
            print >>o, bla.encode('utf-8')
        print >>o, "COMMIT;"

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print >>sys.stderr, "usage: %s <funkfragen_lichtblick.xml> output_directory/"
        sys.exit(1)
    
    funkfragen = sys.argv[1]
    output_dir = sys.argv[2]
    
    main(funkfragen, output_dir)

