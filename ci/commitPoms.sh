#!/bin/bash
set -e # fail fast

#H Usage:
#H %FILE% -h | %FILE% --help
#H
#H prints this help and exits
#H
#H %FILE% [additional files...]
#H
#H   commits and pushes all *.pom files (+ additional files)
#H
#H Requirements:
#H   current commit is a HEAD commit
#H   GH_TOKEN - GitHub token for authentication
#H   GITHUB_REF (format refs/tags/v[0-9]+\.[0-9]+\.[0-9]+)
# Arguments:
#   $1: exit code
function helpAndExit() {
  cat "$0" | grep "^#H" | cut -c4- | sed -e "s/%FILE%/$(basename "$0")/g"
  exit "$1"
}

# takes a version (without leading v) and increments its
# last number by one.
# Arguments:
#   $1: version (without leading v) which will be patched
# Return:
#   version with last number incremented
function increment_version() {
  if [[ ! "$1" =~ [0-9]+\.[0-9]+\.[0-9]+ ]]; then
    echo "'$1' does not match tag pattern." >&2
    exit 1
  fi
  echo "${1%\.*}.$(expr ${1##*\.*\.} + 1)"
}

function main() {
  [[ "$1" == '-h' || "$1" == '--help' ]] && helpAndExit 0
  [[ -z "$GH_TOKEN" ]] && helpAndExit 1
  if [[ "$GITHUB_REF" =~ ^refs/tags/([0-9]+\.[0-9]+\.[0-9]+)/([0-9]+\.[0-9]+\.[0-9]+)$ ]]; then
    #check if tagged commit is a head commit of any branch
    commit=$(git ls-remote -q -t origin | grep "$GITHUB_REF" | cut -c1-40)
    branch=$(git ls-remote -q -h origin | grep "$commit" | sed "s/$commit.*refs\/heads\///")

    if [[ -z "$commit" || -z "$branch" ]]; then
      echo "the commit '$commit' of tag '${GITHUB_REF##refs/tags/}' is not a head commit. Can not release" >&2
      exit 1
    fi

    if [[ $(echo "$branch" | wc -l) != '1' ]]; then
      echo "can not match commit '$commit' to a unique branch." >&2
      echo "Please make sure, that the tag '${GITHUB_REF##refs/tags/}' is the head of a unique branch" >&2
      echo "Branches detected: $branch"
      exit 1
    fi
    set -x
    git config --global user.email "github-actions[bot]@users.noreply.github.com"
    git config --global user.name "github-actions[bot]"

    #commit all poms
    git checkout "$branch"
    NEXT1=$(increment_version "${BASH_REMATCH[1]}")
    NEXT2=$(increment_version "${BASH_REMATCH[2]}")
    NEW_VERSION="${NEXT1}-SNAPSHOT/${NEXT2}-SNAPSHOT"
    PR_BRANCH="auto/version-bump-${NEXT1}-$(date +%s)"

    git checkout -b "$PR_BRANCH"
    git add "./*pom.xml"
    for file in "$@"; do
      [[ -n "$file" ]] && git add "$file"
    done
    git commit -m "Updated poms to version ${NEW_VERSION}"
    git push origin "$PR_BRANCH"

    gh pr create \
          --base "$branch" \
          --title "chore: version bump to ${NEW_VERSION}" \
          --body "Automated version update after release. All tests have passed in the release workflow." \
          --label "dependencies"
  else
    echo "Nothing to push - this is not a release!"
  fi

}

main "$@"
