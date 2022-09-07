#!/bin/sh
set -e

# inputs: target_branch, target_directory, github_token, message

AUTHOR_EMAIL='github-actions[bot]@users.noreply.github.com'
AUTHOR_NAME='github-actions[bot]'
INPUT_BRANCH=${target_branch:-GITHUB_REF_NAME}
INPUT_DIRECTORY=${target_directory:-'.'}
REPOSITORY=$GITHUB_REPOSITORY

echo "Push to branch $INPUT_BRANCH";
[ -z "${github_token}" ] && {
    echo 'Missing input "github_token: ${{ secrets.GITHUB_TOKEN }}".';
    exit 1;
};

cd "${INPUT_DIRECTORY}"

remote_repo="https://${GITHUB_ACTOR}:${github_token}@github.com/${REPOSITORY}.git"

git config http.sslVerify false
git config --local user.email "${AUTHOR_EMAIL}"
git config --local user.name "${AUTHOR_NAME}"

git add -A
git commit -m "${message}"

until git push "${remote_repo}" HEAD:"${INPUT_BRANCH}"
do
  git pull --rebase || exit 1
done
