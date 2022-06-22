import argparse
import os.path
import time

import requests
from urllib import request
import json
import bs4
import javalang

from codeforces import CodeforcesAPI


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--problem_count", dest='problem_count', type=int, default=100)
    parser.add_argument("--submission_count", dest='submission_count', type=int, default=10)
    parser.add_argument("--min_rating", dest='min_rating', type=int, default=0)
    parser.add_argument("--max_rating", dest="max_rating", type=int, default=1500)
    parser.add_argument("--output_dir", dest="output_dir", type=str, default=".")
    return parser.parse_args()


args = get_args()


def check_json(answer):
    values = json.loads(answer)

    if values['status'] == 'OK':
        return values['result']


def save_source_code(contest_id, submission_id):
    url = request.Request(f"http://codeforces.com/contest/{contest_id}/submission/{submission_id}")
    with request.urlopen(url) as req:
        soup = bs4.BeautifulSoup(req.read(), "html.parser")
        path = os.path.join(args.output_dir, f"p{contest_id}", f"p{submission_id}")
        if not os.path.exists(path):
            os.makedirs(path)
        code = ""
        for p in soup.find_all("pre", {"class": "program-source"}):
            code += p.get_text()
        tree = javalang.parse.parse(code)
        try:
            name = next(klass.name for klass in tree.types
                        if isinstance(klass, javalang.tree.ClassDeclaration)
                        for m in klass.methods
                        if m.name == 'main' and m.modifiers.issuperset({'public', 'static'}))
            with open(os.path.join(path, f"{name}.java"), 'w') as f:
                print(f"package p{contest_id}.p{submission_id};", file=f)
                f.write(code)
        except StopIteration:
            print("Sleeping...")
            time.sleep(300)


def main():
    codeforces = "http://codeforces.com/api/"
    api = CodeforcesAPI()

    with request.urlopen(f"{codeforces}problemset.problems") as req:
        all_problems = check_json(req.read().decode('utf-8'))

    problems = []
    cur_problem = 0
    for p in all_problems['problems']:
        if cur_problem >= args.problem_count:
            break
        if p.get('rating') is None:
            continue
        if p['rating'] < args.min_rating or p['rating'] > args.max_rating:
            continue
        cur_problem += 1
        problems.append({'contest_id': p['contestId'], 'index': p['index']})

    print(f"Get {len(problems)} problems: {problems[0]}")

    all_submission = 0
    for i, p in enumerate(problems):
        cur_submission = 0
        iteration = 0
        page_size = 1000
        while cur_submission < args.submission_count:
            length = 0
            for s in api.contest_status(contest_id=p['contest_id'], from_=page_size * iteration + 1, count=page_size):
                if cur_submission >= args.submission_count:
                    break
                length += 1
                if s.problem.contest_id != p['contest_id'] or s.problem.index != p['index']:
                    continue
                if s.programming_language != "Java 8":
                    continue
                if s.verdict.name != "ok":
                    continue
                save_source_code(p['contest_id'], s.id)
                cur_submission += 1
                all_submission += 1
                print(f"Get new {all_submission} program")
            iteration += 1
            if length == 0:
                break


if __name__ == "__main__":
    main()
