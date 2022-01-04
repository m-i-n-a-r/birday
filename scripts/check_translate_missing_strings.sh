#!/usr/bin/env bash

FOLDER_TO_CHECK="${1}"

################################################################################
gather_main_strings() {
  local strings_main_file="app/src/main/res/values/strings.xml"

  STRINGS_MAIN=$(grep "name=" "${strings_main_file}" |  \
    grep -v "translatable" | cut -d '"' -f2)
}

gather_strings_to_be_translated() {
  local strings_to_check_file="app/src/main/res/${FOLDER_TO_CHECK}/strings.xml"

  # Check if file exists, exit otherwise
  if [ ! -f "${strings_to_check_file}" ]; then
    echo "Folder pattern should look like 'values-<language>'"
    echo "File ($strings_to_check_file) does not exist, exiting..."
    exit 1
  fi

  STRINGS_TO_CHECK=$(grep "name=" "${strings_to_check_file}" |  \
    grep -v "translatable" | cut -d '"' -f2)
}
################################################################################

gather_main_strings
gather_strings_to_be_translated

echo "The missing strings on ${FOLDER_TO_CHECK} are: "
echo "${STRINGS_MAIN[@]}" "${STRINGS_TO_CHECK[@]}" | \
  tr ' ' '\n' | sort | uniq -u
