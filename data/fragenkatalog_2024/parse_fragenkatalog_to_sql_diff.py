#!/usr/bin/env python3

import sys
import json
import re
import sqlite3
import matplotlib.pyplot as plt
from latex2svg import latex2svg
import pathlib
import os
import itertools

INPUT = "fragenkatalog3b_03_2024.json"

OUTPUT = "fragenkatalog3b_03_2024_sql_diff.sql"
IMAGE_CACHE = "generated_svg"

pathlib.Path(IMAGE_CACHE).mkdir(parents=True, exist_ok=True)

# update from database version 15 -> 16
# assuming the topics table already was extended with an integer column "version"

last_topic_id = 5
last_category_id = 176
last_question_id = 1924
last_answer_id = 7696
last_question_to_category_id = 1924
last_question_to_topic_id = 2787
last_category_to_topic_id = 288
last_topic_exam_settings_id = 5


fragenkatalog = json.load(open(INPUT, 'r'))

def quote_sqlite(text):
    # a crutch just to be used here to handle quoting
    ret = re.sub(r'\'', "''", text)
    return ret

def latex2image(src, output_name):
    out = latex2svg(src)
    with open(output_name, "w") as s:
        s.write(out['svg'])

# def latex2image(src, image_name, image_size_in=(3, 0.5), fontsize=16, dpi=200):
#     fig = plt.figure(figsize=image_size_in, dpi=dpi)
#     text = fig.text(x=0.5, y=0.5, s=src, horizontalalignment="center", verticalalignment="center", fontsize=fontsize)
#     plt.savefig(image_name)
#     return fig

# latex_rendered_ref = {}

# AD114


def qatext_to_html(qa, p_before_picture=True):
    # global latex_rendered_ref
    ret = qa.text
    # TODO: latex rendering
    pos = 0
    latex_render = 0
    latex_fname = "texrender_"
    debug = False
    if isinstance(qa, Question):
        latex_fname += f"question_{qa.id}_{qa.number}"
        if qa.number == "AD114":
            debug = True
    if isinstance(qa, Answer):
        latex_fname += f"answer_{qa.id}"
    if debug:
        print(ret)
    newstr = ""
    while True:
        if pos >= len(ret):
            break
        if ret[pos].lower() == '$':
            for posend in range(pos+1, len(ret)):
                # if debug: print(ret[posend])
                if ret[posend].lower() == '$':
                    texexp = ret[pos:min(posend+1, len(ret))]
                    final_latex_fname = f"{latex_fname}_{latex_render}.svg"
                    if debug: print(f"Texrender: {texexp}")
                    if texexp == "$μ$":
                        texexp = "$\mu$"
                    # replace = latex_rendered_ref.get(texexp, None)
                    # if replace is None:
                    output_name = os.path.join(IMAGE_CACHE, "v2_" + final_latex_fname)
                    if not os.path.exists(output_name):
                        latex2image(texexp, output_name)
                    latex_render += 1
                    replace = f"<img src='{final_latex_fname}'>"
                        # latex_rendered_ref[texexp] = replace
                    newstr = newstr + replace
                    # + ret[min(posend+1, len(ret)):-1]                    
                    pos = posend
                    break
        else:
            newstr = newstr + str(ret[pos])
        pos += 1
    ret = newstr
    if debug:
        print(ret)
    if qa.picture is not None:
        ret += f"{'<p>' if p_before_picture else ''}<img src='{qa.picture}.svg'>"
    return quote_sqlite(ret)


class Topic(object):
    title = ""
    id = -1
    order_index = -1
    categories = None
    exam_questions = 25
    exam_questions_pass = 19
    exam_time = 60*45

    def __init__(self, title, id):
        self.title = title
        self.id = id
        self.categories = []

    def __str__(self):
        return f"Topic({self.id, self.title, len(self.categories)})"
    
    def to_sql_insert(self):
        return f"INSERT INTO topic VALUES({self.id}, {self.order_index}, '{quote_sqlite(self.title)}', 1, 2);"


class Category(object):
    title = ""
    id = -1
    categories = None
    questions = None
    parent_id = -1
    topic_id = -1

    def __init__(self, title, id, parent_id, topic_id):
        self.title = title
        self.id = id
        self.parent_id = parent_id
        self.topic_id = topic_id
        self.questions = []
        self.categories = []

    def to_sql_insert(self):
        return f"INSERT INTO category VALUES({self.id}, '{quote_sqlite(self.title)}', ' ', 0, {self.parent_id});"
    
    def cattot_to_sql_insert(self, dump_topic_id):
        global category_to_topic_id
        category_to_topic_id += 1
        return f"INSERT INTO category_to_topic VALUES({category_to_topic_id}, {self.id}, {dump_topic_id});"


class Question(object):
    number = ""
    id = -1
    cls = -1
    text = ""
    category_id = -1
    topic_id = -1
    picture = None
    answers = None

    def __init__(self, number, id, cls, text, picture, category_id, topic_id):
        self.number = number
        self.id = id
        self.cls = cls
        self.text = text
        self.picture = picture
        self.category_id = category_id
        self.topic_id = topic_id
        self.answers = []

    def __str__(self):
        return '\n'.join([f"Question({self.id}, {self.number}, {self.cls}, {self.text}, {self.picture})"] +
                         [' ' + str(ans) for ans in self.answers])
    
    def to_sql_insert(self):
        return f"INSERT INTO question VALUES({self.id}, '{self.number}', '{qatext_to_html(self)}', 0, 1, 0, 0, '');"
    
    def qtc_to_sql_insert(self, dump_category_id):
        global question_to_category_id
        question_to_category_id += 1
        return f"INSERT INTO question_to_category VALUES({question_to_category_id}, {self.id}, {dump_category_id});"
    
    def qtt_to_sql_insert(self, dump_topic_id):
        global question_to_topic_id
        question_to_topic_id += 1
        return f"INSERT INTO question_to_topic VALUES({question_to_topic_id}, {self.id}, {dump_topic_id});"



class Answer(object):
    text = ""
    id = -1
    picture = None
    order_index = -1
    question_id = -1

    def __init__(self, id, order_index, question_id, text, picture):
        self.id = id
        self.text = text
        self.picture = picture
        self.question_id = question_id
        self.order_index = order_index

    def __str__(self):
        return f"Answer({self.id}, {self.text}, {self.picture})"
    
    def to_sql_insert(self):
        return f"INSERT INTO answer VALUES({self.id}, {self.question_id}, {self.order_index}, '{qatext_to_html(self, p_before_picture=False)}', '');"


pkat = []

local_topic_id = 0
category_id = last_category_id
question_id = last_question_id
answer_id = last_answer_id
question_to_category_id = last_question_to_category_id
question_to_topic_id = last_question_to_topic_id
category_to_topic_id = last_category_to_topic_id
topic_exam_settings_id = last_topic_exam_settings_id

def parse_questions(category, category_id, topic_id, sections):
    global question_id
    global answer_id
    for section in sections:
        number = section['number']
        cls = section['class']
        question = section['question']
        picture = section.get('picture_question', None)
        question_id += 1
        q = Question(number, question_id, cls, question, picture, category_id, topic_id)
        category.questions.append(q)
        for i, (atext, apict) in enumerate([('answer_a', 'picture_a'), ('answer_b', 'picture_b'), ('answer_c', 'picture_c'), ('answer_d', 'picture_d')]):
            answer_text = section[atext]
            answer_picture = section.get(apict, None)
            answer_id += 1
            a = Answer(answer_id, i+1, q.id, answer_text, answer_picture)
            q.answers.append(a)


def parse_category(parent, parent_id, topic_id, sections):
    global category_id
    for section in sections:
        title = section['title']
        print(f"category title: {title}")
        category_id += 1
        c = Category(title, category_id, parent_id, topic_id)
        parent.categories.append(c)
        if "sections" in section:
            parse_category(c, c.id, topic_id, section['sections'])
        if "questions" in section:
            parse_questions(c, c.id, topic_id, section['questions'])


for topic in fragenkatalog["sections"]:
    title = topic['title']
    print(f"topic title: {title}")
    local_topic_id += 1
    t = Topic(title, local_topic_id)
    pkat.append(t)
    if "sections" in topic:
        parse_category(t, t.id, 0, topic['sections'])    


# assumption: 
# technik (mixed all classes)
# betrieblich (N)
# vorschiften (N)

def collect_classes(root):
    classes = set()
    if isinstance(root, Topic):
        for category in root.categories:
            classes = classes.union(collect_classes(category))
    if isinstance(root, Category):
        for category in root.categories:
            classes = classes.union(collect_classes(category))
        for question in root.questions:
            classes = classes.union(collect_classes(question))
    if isinstance(root, Question):
        return set([root.cls])
    return classes

for t in pkat:
    print(f"{t} classes: {collect_classes(t)}")

# classfilter: 1:N, 2:E, 3:A

def filter_categories_and_questions(root, classfilter):
    global category_id
    if isinstance(root, Topic):
        return [filter_categories_and_questions(category, classfilter) for category in root.categories]
    if isinstance(root, Category):
        category_id += 1
        c = Category(root.title, category_id, root.parent_id, root.topic_id)
        filtered_categories = [filter_categories_and_questions(category, classfilter) for category in root.categories]
        filtered_questions = [filter_categories_and_questions(question, classfilter) for question in root.questions]
        c.categories = filtered_categories
        c.questions = list(filter(lambda item: item is not None, filtered_questions))
        return c
    if isinstance(root, Question):
        if root.cls in classfilter:
            return root
        else:
            return None
        
def merge_categories_and_questions(topics):
    ret = []
    for t in topics:
        ret += t.categories
    return ret


def categories_to_sql_insert(root):
    if isinstance(root, Topic):
        return list(itertools.chain(*[categories_to_sql_insert(c) for c in root.categories]))
    if isinstance(root, Question):
        return [None]
    if isinstance(root, Category):
        return [root.to_sql_insert()] + list(itertools.chain(*[categories_to_sql_insert(c) for c in root.categories]))


def questions_to_sql_insert(root):
    if isinstance(root, Topic):
        return list(itertools.chain(*[questions_to_sql_insert(c) for c in root.categories]))
    if isinstance(root, Category):
        return list(itertools.chain(*[questions_to_sql_insert(c) for c in root.categories])) + \
            list(itertools.chain(*[questions_to_sql_insert(c) for c in root.questions]))
    if isinstance(root, Question):
        return [root.to_sql_insert()]


def answers_to_sql_insert(root):
    if isinstance(root, Topic):
        return list(itertools.chain(*[answers_to_sql_insert(c) for c in root.categories]))
    if isinstance(root, Category):
        return list(itertools.chain(*[answers_to_sql_insert(c) for c in root.categories])) + \
            list(itertools.chain(*[answers_to_sql_insert(q) for q in root.questions]))
    if isinstance(root, Question):
        return [answers_to_sql_insert(a) for a in root.answers]
    if isinstance(root, Answer):
        return root.to_sql_insert()


def question_to_category_to_sql_insert(root, dump_category_id=None):
    if isinstance(root, Topic):
        return list(itertools.chain(*[question_to_category_to_sql_insert(c) for c in root.categories]))
    if isinstance(root, Category):
        return list(itertools.chain(*[question_to_category_to_sql_insert(c) for c in root.categories])) + \
            list(itertools.chain(*[question_to_category_to_sql_insert(q, dump_category_id=root.id) for q in root.questions]))
    if isinstance(root, Question):
        return [root.qtc_to_sql_insert(dump_category_id)]


def question_to_topic_to_sql_insert(root, dump_topic_id=None):
    if isinstance(root, Topic):
        return list(itertools.chain(*[question_to_topic_to_sql_insert(c, dump_topic_id=root.id) for c in root.categories]))
    if isinstance(root, Category):
        return list(itertools.chain(*[question_to_topic_to_sql_insert(c, dump_topic_id=dump_topic_id) for c in root.categories])) + \
            list(itertools.chain(*[question_to_topic_to_sql_insert(q, dump_topic_id=dump_topic_id) for q in root.questions]))
    if isinstance(root, Question):
        return [root.qtt_to_sql_insert(dump_topic_id)]


def category_to_topic_to_sql_insert(root, dump_topic_id=None):
    if isinstance(root, Topic):
        return list(itertools.chain(*[category_to_topic_to_sql_insert(c, dump_topic_id=root.id) for c in root.categories]))
    if isinstance(root, Category):
        return [c.cattot_to_sql_insert(dump_topic_id) for c in root.categories]
    
    

with open(OUTPUT, "w") as out:
    # out = sys.stdout
    print("BEGIN;", file=out)
    tid = last_topic_id + 1
    # create artificial topic with questions filtered by class
    technische_kenntnisse_n = Topic('Technische Kenntnisse (Klasse N)', tid)
    technische_kenntnisse_n.order_index = tid
    technische_kenntnisse_n.categories = filter_categories_and_questions(pkat[0], ['1'])
    print(technische_kenntnisse_n.to_sql_insert(), file=out)

    tid = last_topic_id + 2
    technische_kenntnisse_e = Topic('Technische Kenntnisse (Klasse E)', tid)
    technische_kenntnisse_e.order_index = tid
    technische_kenntnisse_e.categories = filter_categories_and_questions(pkat[0], ['2'])
    print(technische_kenntnisse_e.to_sql_insert(), file=out)

    tid = last_topic_id + 3
    technische_kenntnisse_a = Topic('Technische Kenntnisse (Klasse A)', tid)
    technische_kenntnisse_a.order_index = tid
    technische_kenntnisse_a.categories = filter_categories_and_questions(pkat[0], ['3'])
    technische_kenntnisse_a.exam_time = 60*60
    print(technische_kenntnisse_a.to_sql_insert(), file=out)

    tid = last_topic_id + 4
    betriebliche_kenntnisse = pkat[1]
    betriebliche_kenntnisse.title = "Betriebliche Kenntnisse"
    betriebliche_kenntnisse.order_index = tid
    betriebliche_kenntnisse.id = tid
    print(betriebliche_kenntnisse.to_sql_insert(), file=out)

    tid = last_topic_id + 5
    vorschriften = pkat[2]
    vorschriften.title = "Kenntnisse Vorschriften"
    vorschriften.order_index = tid
    vorschriften.id = tid 
    print(vorschriften.to_sql_insert(), file=out)

    tid = last_topic_id + 6
    mixtopic_n = Topic('Klasse N alle (Technik, Betrieb, Vorschriften)', tid)
    mixtopic_n.order_index = tid
    mixtopic_n.categories = merge_categories_and_questions([technische_kenntnisse_n, betriebliche_kenntnisse, vorschriften])
    print(mixtopic_n.to_sql_insert(), file=out)

    tid = last_topic_id + 7
    mixtopic_e = Topic('Klasse E alle (Technik, Betrieb, Vorschriften)', tid)
    mixtopic_e.order_index = tid
    mixtopic_e.categories = merge_categories_and_questions([technische_kenntnisse_e, betriebliche_kenntnisse, vorschriften])
    print(mixtopic_e.to_sql_insert(), file=out)

    # dump categories
    catlist = sum(map(categories_to_sql_insert, [technische_kenntnisse_n, technische_kenntnisse_e, 
                                                        technische_kenntnisse_a, betriebliche_kenntnisse, vorschriften,
                                                        mixtopic_n, mixtopic_e]), [])
    catfilter = dict()
    for cat in catlist:
        if catfilter.get(cat, None) is None:
            print(cat, file=out)
            catfilter[cat] = 1
        else:
            catfilter[cat] += 1
    print(f"filtered cats: {sum(map(lambda x: 1 if x > 1 else 0, catfilter.values()))}")


    # dump questions
    queslist = sum(map(questions_to_sql_insert, [technische_kenntnisse_n, technische_kenntnisse_e, 
                                                        technische_kenntnisse_a, betriebliche_kenntnisse, vorschriften,
                                                        mixtopic_n, mixtopic_e]), [])
    quesfilter = dict()
    for ques in queslist:
        if quesfilter.get(ques, None) is None:
            print(ques, file=out)
            quesfilter[ques] = 1
        else:
            quesfilter[ques] += 1
    print(f"filtered ques: {sum(map(lambda x: 1 if x > 1 else 0, quesfilter.values()))}")

    # dump answers
    anslist = sum(map(answers_to_sql_insert, [technische_kenntnisse_n, technische_kenntnisse_e, 
                                                        technische_kenntnisse_a, betriebliche_kenntnisse, vorschriften,
                                                        mixtopic_n, mixtopic_e]), [])
    ansfilter = dict()
    for ans in anslist:
        if ansfilter.get(ans, None) is None:
            print(ans, file=out)
            ansfilter[ans] = 1
        else:
            ansfilter[ans] += 1
    print(f"filtered ans: {sum(map(lambda x: 1 if x > 1 else 0, ansfilter.values()))}")

    # dump question_to_category
    qtclist = sum(map(question_to_category_to_sql_insert, [technische_kenntnisse_n, technische_kenntnisse_e, 
                                                        technische_kenntnisse_a, betriebliche_kenntnisse, vorschriften,
                                                        mixtopic_n, mixtopic_e]), [])
    for qtc in qtclist:
        print(qtc, file=out)

    # dump question_to_topic
    qttlist = sum(map(question_to_topic_to_sql_insert, [technische_kenntnisse_n, technische_kenntnisse_e, 
                                                        technische_kenntnisse_a, betriebliche_kenntnisse, vorschriften,
                                                        mixtopic_n, mixtopic_e]), [])
    for qtt in qttlist:
        print(qtt, file=out)

    # dump category_to_topic
    cattotlist = sum(map(category_to_topic_to_sql_insert, [technische_kenntnisse_n, technische_kenntnisse_e, 
                                                        technische_kenntnisse_a, betriebliche_kenntnisse, vorschriften,
                                                        mixtopic_n, mixtopic_e]), [])
    for cattot in cattotlist:
        print(cattot, file=out)\
        

    # fill topic_exam_settings
    for t in [technische_kenntnisse_n, technische_kenntnisse_e,
            technische_kenntnisse_a, betriebliche_kenntnisse, vorschriften,
            mixtopic_n, mixtopic_e]:
        topic_exam_settings_id += 1
        print(f"INSERT INTO topic_exam_settings VALUES({topic_exam_settings_id}, {t.id}, {t.exam_questions}, {t.exam_questions_pass}, {t.exam_time});", file=out)


    print("COMMIT;", file=out)